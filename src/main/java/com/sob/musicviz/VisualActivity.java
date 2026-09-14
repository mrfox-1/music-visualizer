package com.sob.musicviz;

/** Converts activity into target counts without changing audio detection. */
final class VisualActivity
{
    private double remainder;
    private int previous = -1;

    int nextCount(int value)
    {
        int level = Math.max(0, Math.min(100, value));
        if (level != previous) { remainder = 0; previous = level; }
        // Below 25, thin out events. Above 25, grow from one to twelve targets.
        double density = level <= 25 ? level / 25.0
            : level <= 50 ? 1 + (level - 25) * 3 / 25.0
            : 4 + (level - 50) * 8 / 50.0;
        remainder += density;
        int count = (int) (remainder + 1e-9);
        remainder -= count;
        return count;
    }

    void reset() { remainder = 0; previous = -1; }

    static int floorCount(int activity, int available, int eventCount)
    {
        if (eventCount == 0 || available == 0) return 0;
        double progress = Math.max(0, Math.min(75, activity - 25)) / 75.0;
        // Scale target counts proportionally, rather than adding a percentage of the
        // entire scene per step. This keeps 25 -> 26 gentle even at a large radius.
        return Math.min(available, Math.max(1, (int) Math.round(Math.pow(available, progress))));
    }
}
