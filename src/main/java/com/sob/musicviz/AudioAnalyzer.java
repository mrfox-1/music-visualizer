package com.sob.musicviz;

import java.util.function.Consumer;

/** FFT front end for live audio at the playback device's native sample rate. */
final class AudioAnalyzer
{
    static final int SIZE = 2048;
    static final int SAMPLE_RATE = 44100;
    private final double[] real = new double[SIZE];
    private final double[] imaginary = new double[SIZE];
    private final double[] window = new double[SIZE];
    private final double[] previous = new double[3];
    private final long[] lastFlash = {-100000, -100000, -100000};
    private long samples;
    private double signalLevel;
    private final int sampleRate;
    private final int[] edges = new int[4];

    double signalLevel() { return signalLevel; }

    AudioAnalyzer()
    {
        this(SAMPLE_RATE);
    }

    AudioAnalyzer(int sampleRate)
    {
        if (sampleRate < 16000 || sampleRate > 384000) throw new IllegalArgumentException("Invalid sample rate");
        this.sampleRate = sampleRate;
        int[] frequencies = {40, 250, 2000, 8000};
        for (int i = 0; i < edges.length; i++)
            edges[i] = Math.min(SIZE / 2, Math.max(1, (int) Math.ceil(frequencies[i] * (double) SIZE / sampleRate)));
        for (int i = 0; i < SIZE; i++)
        {
            window[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (SIZE - 1));
        }
    }

    void accept(byte[] pcm, int sensitivity, Consumer<NoteEvent> sink)
    {
        if (pcm.length != SIZE * 4) throw new IllegalArgumentException("Expected a full stereo PCM frame");
        double[][] channels = new double[2][SIZE];
        for (int i = 0; i < SIZE; i++)
        {
            int p = i * 4;
            channels[0][i] = (short) ((pcm[p] & 255) | (pcm[p + 1] << 8)) / 32768.0;
            channels[1][i] = (short) ((pcm[p + 2] & 255) | (pcm[p + 3] << 8)) / 32768.0;
        }
        accept(channels, sensitivity, sink);
    }

    void accept(double[][] channels, int sensitivity, Consumer<NoteEvent> sink)
    {
        if (channels.length == 0) throw new IllegalArgumentException("No audio channels");
        double gain = Math.max(1, Math.min(100, sensitivity)) / 50.0;
        double energyMax = -1;
        int loudest = 0;
        for (int channel = 0; channel < channels.length; channel++)
        {
            if (channels[channel].length != SIZE) throw new IllegalArgumentException("Incomplete audio block");
            double energy = 0;
            for (double value : channels[channel]) energy += value * value;
            if (energy > energyMax) { energyMax = energy; loudest = channel; }
        }
        // Include surround channels, without canceling opposite-phase stereo.
        signalLevel = Math.sqrt(energyMax / SIZE);
        for (int i = 0; i < SIZE; i++)
        {
            real[i] = channels[loudest][i] * window[i];
            imaginary[i] = 0;
        }
        fft();
        long nowMs = samples * 1000 / sampleRate;
        samples += SIZE;
        for (int band = 0; band < 3; band++)
        {
            double energy = 0, peak = 0;
            int peakBin = edges[band];
            for (int k = edges[band]; k < edges[band + 1]; k++)
            {
                double power = real[k] * real[k] + imaginary[k] * imaginary[k];
                energy += power;
                if (power > peak) { peak = power; peakBin = k; }
            }
            double level = Math.sqrt(energy) * 4 / SIZE * gain;
            long elapsed = nowMs - lastFlash[band];
            boolean onset = level > previous[band] * 1.35;
            // Repeat held sounds slowly; cap bursts at one flash per band per 90 ms.
            if (level > 0.015 && elapsed >= 90 && (onset || elapsed >= 250))
            {
                // Interpolate the spectral peak so colors stay stable at 48/96 kHz.
                double refinedBin = peakBin;
                if (peakBin > 0 && peakBin < SIZE / 2 - 1)
                {
                    double a = logPower(peakBin - 1), b = logPower(peakBin), c = logPower(peakBin + 1);
                    double denominator = a - 2 * b + c;
                    if (Math.abs(denominator) > 1e-12)
                        refinedBin += Math.max(-0.5, Math.min(0.5, 0.5 * (a - c) / denominator));
                }
                double frequency = refinedBin * sampleRate / SIZE;
                int note = (int) Math.round(69 + 12 * Math.log(frequency / 440) / Math.log(2));
                sink.accept(new NoteEvent(nowMs, band, Math.max(0, Math.min(127, note)),
                    Math.max(1, Math.min(127, (int) (level * 127)))));
                lastFlash[band] = nowMs;
            }
            previous[band] = previous[band] * 0.8 + level * 0.2;
        }
    }

    private double logPower(int bin)
    {
        return Math.log(Math.max(1e-20, real[bin] * real[bin] + imaginary[bin] * imaginary[bin]));
    }

    private void fft()
    {
        for (int i = 1, j = 0; i < SIZE; i++)
        {
            int bit = SIZE >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j)
            {
                double t = real[i]; real[i] = real[j]; real[j] = t;
            }
        }
        for (int length = 2; length <= SIZE; length <<= 1)
        {
            double angle = -2 * Math.PI / length;
            double stepR = Math.cos(angle), stepI = Math.sin(angle);
            for (int start = 0; start < SIZE; start += length)
            {
                double wr = 1, wi = 0;
                for (int j = 0; j < length / 2; j++)
                {
                    int a = start + j, b = a + length / 2;
                    double br = real[b] * wr - imaginary[b] * wi;
                    double bi = real[b] * wi + imaginary[b] * wr;
                    real[b] = real[a] - br; imaginary[b] = imaginary[a] - bi;
                    real[a] += br; imaginary[a] += bi;
                    double nextR = wr * stepR - wi * stepI;
                    wi = wr * stepI + wi * stepR; wr = nextR;
                }
            }
        }
    }
}
