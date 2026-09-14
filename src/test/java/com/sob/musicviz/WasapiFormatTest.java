package com.sob.musicviz;

import com.sun.jna.Memory;
import org.junit.Test;
import static org.junit.Assert.*;

public class WasapiFormatTest
{
    @Test
    public void signedPcmAndFloatAreDecoded()
    {
        Memory data = new Memory(8);
        {
            data.setShort(0, (short) -16384);
            assertEquals(-0.5, WasapiLoopback.decode(data, 0, 1, 16), 0.000001);
            data.setByte(0, (byte) 0); data.setByte(1, (byte) 0); data.setByte(2, (byte) 0xC0);
            assertEquals(-0.5, WasapiLoopback.decode(data, 0, 1, 24), 0.000001);
            data.setInt(0, 1073741824);
            assertEquals(0.5, WasapiLoopback.decode(data, 0, 1, 32), 0.000001);
            data.setFloat(0, -0.25f);
            assertEquals(-0.25, WasapiLoopback.decode(data, 0, 3, 32), 0.000001);
            data.setDouble(0, 0.125);
            assertEquals(0.125, WasapiLoopback.decode(data, 0, 3, 64), 0.000001);
        }
    }

    @Test
    public void invalidFloatAndUnsignedSilenceAreSafe()
    {
        Memory data = new Memory(8);
        {
            data.setFloat(0, Float.NaN);
            assertEquals(0, WasapiLoopback.decode(data, 0, 3, 32), 0);
            data.setFloat(0, 4);
            assertEquals(1, WasapiLoopback.decode(data, 0, 3, 32), 0);
            data.setByte(0, (byte) 128);
            assertEquals(0, WasapiLoopback.decode(data, 0, 1, 8), 0);
        }
    }
}
