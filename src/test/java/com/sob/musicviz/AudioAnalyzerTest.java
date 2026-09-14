package com.sob.musicviz;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class AudioAnalyzerTest
{
    @Test
    public void silenceAndQuietNoiseDoNotFlash()
    {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        List<NoteEvent> notes = new ArrayList<>();
        for (int i = 0; i < 100; i++) analyzer.accept(tone(20, 0.0001, false), 100, notes::add);
        assertTrue(notes.isEmpty());
    }

    @Test
    public void toneMapsToExpectedBandAndPitch()
    {
        for (int bin : new int[] {5, 20, 140})
        {
            List<NoteEvent> notes = new ArrayList<>();
            new AudioAnalyzer().accept(tone(bin, 0.25, false), 50, notes::add);
            assertEquals(1, notes.size());
            NoteEvent note = notes.get(0);
            assertEquals(bin == 5 ? 0 : bin == 20 ? 1 : 2, note.channel);
            double frequency = bin * (double) AudioAnalyzer.SAMPLE_RATE / AudioAnalyzer.SIZE;
            assertEquals((int) Math.round(69 + 12 * Math.log(frequency / 440) / Math.log(2)), note.note);
        }
    }

    @Test
    public void oppositeStereoPhaseDoesNotCancel()
    {
        List<NoteEvent> notes = new ArrayList<>();
        new AudioAnalyzer().accept(tone(20, 0.25, true), 50, notes::add);
        assertEquals(1, notes.size());
    }

    @Test
    public void rightOnlyAudioIsDetected()
    {
        byte[] pcm = tone(20, 0.25, false);
        for (int i = 0; i < pcm.length; i += 4) { pcm[i] = 0; pcm[i + 1] = 0; }
        List<NoteEvent> notes = new ArrayList<>();
        new AudioAnalyzer().accept(pcm, 50, notes::add);
        assertEquals(1, notes.size());
    }

    @Test
    public void heldTonesRepeatWithoutUnboundedFlashes()
    {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        List<NoteEvent> notes = new ArrayList<>();
        for (int i = 0; i < 100; i++) analyzer.accept(tone(20, 0.25, false), 50, notes::add);
        assertTrue(notes.size() > 10);
        assertTrue(notes.size() < 30);
        for (int i = 1; i < notes.size(); i++)
        {
            assertTrue(notes.get(i).timestampMs - notes.get(i - 1).timestampMs >= 90);
        }
        int before = notes.size();
        for (int i = 0; i < 30; i++) analyzer.accept(new byte[AudioAnalyzer.SIZE * 4], 50, notes::add);
        assertEquals(before, notes.size());
    }

    @Test
    public void sensitivityControlsQuietSignalThreshold()
    {
        List<NoteEvent> low = new ArrayList<>(), high = new ArrayList<>();
        byte[] pcm = tone(20, 0.01, false);
        new AudioAnalyzer().accept(pcm, 1, low::add);
        new AudioAnalyzer().accept(pcm, 100, high::add);
        assertTrue(low.isEmpty());
        assertFalse(high.isEmpty());
    }

    @Test
    public void nativeSampleRatesMapPitchCorrectly()
    {
        for (int rate : new int[] {44100, 48000, 96000})
        {
            double[][] pcm = new double[1][AudioAnalyzer.SIZE];
            for (int i = 0; i < AudioAnalyzer.SIZE; i++) pcm[0][i] = 0.25 * Math.sin(2 * Math.PI * 440 * i / rate);
            List<NoteEvent> notes = new ArrayList<>();
            new AudioAnalyzer(rate).accept(pcm, 50, notes::add);
            assertEquals(1, notes.size());
            assertEquals(69, notes.get(0).note);
        }
    }

    @Test
    public void surroundChannelsAreIncluded()
    {
        double[][] pcm = new double[8][AudioAnalyzer.SIZE];
        for (int i = 0; i < AudioAnalyzer.SIZE; i++) pcm[6][i] = 0.25 * Math.sin(2 * Math.PI * 440 * i / 48000);
        List<NoteEvent> notes = new ArrayList<>();
        AudioAnalyzer analyzer = new AudioAnalyzer(48000);
        analyzer.accept(pcm, 50, notes::add);
        assertEquals(1, notes.size());
        assertTrue(analyzer.signalLevel() > 0.1);
    }

    private static byte[] tone(int bin, double amplitude, boolean oppositePhase)
    {
        byte[] pcm = new byte[AudioAnalyzer.SIZE * 4];
        for (int i = 0; i < AudioAnalyzer.SIZE; i++)
        {
            short left = (short) (Math.sin(2 * Math.PI * bin * i / AudioAnalyzer.SIZE) * amplitude * 32767);
            short right = (short) (oppositePhase ? -left : left);
            pcm[i * 4] = (byte) left; pcm[i * 4 + 1] = (byte) (left >> 8);
            pcm[i * 4 + 2] = (byte) right; pcm[i * 4 + 3] = (byte) (right >> 8);
        }
        return pcm;
    }
}
