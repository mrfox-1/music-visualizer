package com.sob.musicviz;

/** Manual Windows integration check; logs only levels and event counts, never audio. */
public class LoopbackSmoke
{
    public static void main(String[] args) throws Exception
    {
        java.util.concurrent.atomic.AtomicInteger events = new java.util.concurrent.atomic.AtomicInteger();
        PcAudioCapture capture = new PcAudioCapture();
        capture.start(() -> 50, note -> events.incrementAndGet());
        double peak = 0;
        try
        {
            for (int i = 0; i < 8; i++)
            {
                Thread.sleep(500);
                peak = Math.max(peak, capture.levelPercent());
                System.out.printf(java.util.Locale.ROOT, "%s | level=%.2f%% | events=%d%n",
                    capture.status(), capture.levelPercent(), events.get());
            }
            if (!capture.status().startsWith("Listening")) throw new AssertionError(capture.status());
        }
        finally { capture.stop(); }
        Thread.sleep(200);
        boolean alive = Thread.getAllStackTraces().keySet().stream()
            .anyMatch(thread -> thread.isAlive() && thread.getName().equals("musicviz-pc-audio"));
        if (alive) throw new AssertionError("Capture worker did not stop");
        System.out.printf(java.util.Locale.ROOT, "Capture opened and stopped. Peak %.2f%%, %d events.%n", peak, events.get());
    }
}
