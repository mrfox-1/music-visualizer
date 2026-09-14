package com.sob.musicviz;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Windows playback capture stays in-process; audio is never saved or transmitted. */
final class PcAudioCapture
{
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "musicviz-pc-audio");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean closed;
    private volatile String status = "Connecting to PC audio...";
    private volatile double levelPercent;

    String status() { return status; }
    double levelPercent() { return levelPercent; }

    void start(IntSupplier sensitivity, Consumer<NoteEvent> sink)
    {
        worker.submit(() -> capture(sensitivity, sink));
    }

    void stop()
    {
        closed = true;
        // Native resources are released by their owning worker, never the client thread.
        worker.shutdownNow();
    }

    private void capture(IntSupplier sensitivity, Consumer<NoteEvent> sink)
    {
        try
        {
            if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows"))
            {
                status = "PC audio capture requires Windows.";
                return;
            }
            while (!closed)
            {
                try (WasapiLoopback loopback = new WasapiLoopback())
                {
                    AudioAnalyzer analyzer = new AudioAnalyzer(loopback.sampleRate());
                    double[][] samples = new double[loopback.channels()][AudioAnalyzer.SIZE];
                    int[] used = {0};
                    long nextDeviceCheck = System.nanoTime();
                    long lastPacket = System.nanoTime();
                    status = "Listening to PC audio";
                    while (!closed)
                    {
                        int frames = loopback.read((values, discontinuity) -> {
                            if (discontinuity) used[0] = 0;
                            for (int channel = 0; channel < values.length; channel++)
                                samples[channel][used[0]] = values[channel];
                            if (++used[0] == AudioAnalyzer.SIZE)
                            {
                                if (!closed)
                                {
                                    analyzer.accept(samples, sensitivity.getAsInt(), sink);
                                    levelPercent = analyzer.signalLevel() * 100;
                                }
                                used[0] = 0;
                            }
                        });
                        long now = System.nanoTime();
                        if (frames > 0) lastPacket = now;
                        else
                        {
                            if (now - lastPacket > 250000000L) { levelPercent = 0; used[0] = 0; }
                            Thread.sleep(5);
                        }
                        if (now >= nextDeviceCheck)
                        {
                            if (!loopback.isCurrentDefault()) break;
                            nextDeviceCheck = now + 1000000000L;
                        }
                    }
                }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); return; }
                catch (RuntimeException ex)
                {
                    status = "Reconnecting to PC audio: " + ex.getMessage();
                    if (!closed) Thread.sleep(1000);
                }
                finally { levelPercent = 0; }
            }
        }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        catch (LinkageError ex) { status = "Windows audio bridge could not load: " + ex.getMessage(); }
        finally { worker.shutdown(); }
    }
}
