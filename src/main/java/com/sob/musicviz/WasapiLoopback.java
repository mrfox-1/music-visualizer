package com.sob.musicviz;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.UUID;

/** All COM calls, including disposal, must run on the capture worker. */
final class WasapiLoopback implements AutoCloseable
{
    private final NativeLibrary ole32 = NativeLibrary.getInstance("Ole32");
    private Pointer enumerator, device, client, capture;
    private boolean initialized, started;
    private String deviceId;
    private int sampleRate, channels, bits, blockAlign, formatTag;

    WasapiLoopback()
    {
        try
        {
            check(ole("CoInitializeEx").invokeInt(new Object[] {null, 0}), "CoInitializeEx");
            initialized = true;
            PointerByReference out = new PointerByReference();
            check(ole("CoCreateInstance").invokeInt(new Object[] {
                guid("BCDE0395-E52F-467C-8E3D-C4579291692E"), null, 1,
                guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), out}), "Create device enumerator");
            enumerator = out.getValue();
            device = defaultDevice();
            deviceId = id(device);
            check(call(device, 3, guid("1CB9AD4C-DBFA-4C32-B178-C2F568A703B2"), 1, null, out), "Activate audio client");
            client = out.getValue();
            check(call(client, 8, out), "Get mix format");
            Pointer format = out.getValue();
            try
            {
                formatTag = format.getShort(0) & 65535;
                channels = format.getShort(2) & 65535;
                sampleRate = format.getInt(4);
                blockAlign = format.getShort(12) & 65535;
                bits = format.getShort(14) & 65535;
                if (formatTag == 65534 && (format.getShort(16) & 65535) >= 22)
                    formatTag = format.getInt(24); // PCM or IEEE_FLOAT subtype
                if (sampleRate < 16000 || sampleRate > 384000 || channels < 1 || channels > 32
                    || blockAlign != channels * (bits / 8)
                    || !(formatTag == 3 && (bits == 32 || bits == 64)
                        || formatTag == 1 && (bits == 8 || bits == 16 || bits == 24 || bits == 32)))
                    throw new IllegalStateException("Unsupported Windows mix format");
                // Shared-mode rendering endpoint, LOOPBACK, 100 ms capacity, native mix format.
                check(call(client, 3, 0, 0x20000, 1000000L, 0L, format, null), "Initialize loopback");
            }
            finally { free(format); }
            check(call(client, 14, guid("C8ADBD64-E71E-48A0-A4DE-185C395CD317"), out), "Get capture service");
            capture = out.getValue();
            check(call(client, 10), "Start loopback");
            started = true;
        }
        catch (RuntimeException | LinkageError ex) { close(); throw ex; }
    }

    int sampleRate() { return sampleRate; }
    int channels() { return channels; }

    boolean isCurrentDefault()
    {
        Pointer current = defaultDevice();
        try { return deviceId.equals(id(current)); }
        finally { release(current); }
    }

    private Pointer defaultDevice()
    {
        PointerByReference out = new PointerByReference();
        // eRender, eMultimedia: Windows' default for music and PC playback.
        check(call(enumerator, 4, 0, 1, out), "Find default playback device");
        return out.getValue();
    }

    private String id(Pointer endpoint)
    {
        PointerByReference out = new PointerByReference();
        check(call(endpoint, 5, out), "Read playback device ID");
        try { return out.getValue().getWideString(0); }
        finally { free(out.getValue()); }
    }

    interface Samples { void accept(double[] channels, boolean discontinuity); }

    /** Reads one packet without blocking; its memory is released before returning. */
    int read(Samples sink)
    {
        IntByReference next = new IntByReference();
        check(call(capture, 5, next), "Get packet size");
        if (next.getValue() == 0) return 0;
        PointerByReference data = new PointerByReference();
        IntByReference frames = new IntByReference(), flags = new IntByReference();
        check(call(capture, 3, data, frames, flags, null, null), "Read loopback packet");
        try
        {
            double[] values = new double[channels];
            boolean silent = (flags.getValue() & 2) != 0;
            boolean discontinuity = (flags.getValue() & 1) != 0;
            for (int frame = 0; frame < frames.getValue(); frame++)
            {
                for (int channel = 0; channel < channels; channel++)
                {
                    long offset = (long) frame * blockAlign + channel * (bits / 8);
                    values[channel] = silent ? 0 : decode(data.getValue(), offset, formatTag, bits);
                }
                sink.accept(values, discontinuity && frame == 0);
            }
            return frames.getValue();
        }
        finally { check(call(capture, 4, frames.getValue()), "Release loopback packet"); }
    }

    static double decode(Pointer data, long offset, int tag, int bits)
    {
        double sample;
        if (tag == 3) sample = bits == 32 ? data.getFloat(offset) : data.getDouble(offset);
        else if (bits == 8) sample = ((data.getByte(offset) & 255) - 128) / 128.0;
        else if (bits == 16) sample = data.getShort(offset) / 32768.0;
        else if (bits == 24)
        {
            int packed = (data.getByte(offset) & 255) | ((data.getByte(offset + 1) & 255) << 8)
                | (data.getByte(offset + 2) << 16);
            sample = packed / 8388608.0;
        }
        else sample = data.getInt(offset) / 2147483648.0;
        return Double.isFinite(sample) ? Math.max(-1, Math.min(1, sample)) : 0;
    }

    private Function ole(String name) { return ole32.getFunction(name, Function.ALT_CONVENTION); }
    private void free(Pointer pointer) { ole("CoTaskMemFree").invokeVoid(new Object[] {pointer}); }

    private static int call(Pointer object, int slot, Object... args)
    {
        Object[] parameters = new Object[args.length + 1];
        parameters[0] = object;
        System.arraycopy(args, 0, parameters, 1, args.length);
        Pointer function = object.getPointer(0).getPointer((long) slot * Native.POINTER_SIZE);
        return Function.getFunction(function, Function.ALT_CONVENTION).invokeInt(parameters);
    }

    private static Memory guid(String value)
    {
        UUID uuid = UUID.fromString(value);
        long high = uuid.getMostSignificantBits(), low = uuid.getLeastSignificantBits();
        Memory result = new Memory(16);
        result.setInt(0, (int) (high >>> 32));
        result.setShort(4, (short) (high >>> 16));
        result.setShort(6, (short) high);
        for (int i = 0; i < 8; i++) result.setByte(8 + i, (byte) (low >>> (56 - i * 8)));
        return result;
    }

    private static void check(int result, String operation)
    {
        if (result < 0) throw new IllegalStateException(operation + " (0x" + Integer.toHexString(result) + ")");
    }

    private static void release(Pointer object) { if (object != null) call(object, 2); }

    @Override
    public void close()
    {
        if (started) { call(client, 11); started = false; }
        release(capture); capture = null;
        release(client); client = null;
        release(device); device = null;
        release(enumerator); enumerator = null;
        if (initialized) { ole("CoUninitialize").invokeVoid(new Object[0]); initialized = false; }
    }
}
