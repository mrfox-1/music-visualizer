package com.sob.musicviz;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VisualActivityTest
{
    @Test public void floorCoverageScalesToTheWholeScene()
    {
        assertEquals(0, VisualActivity.floorCount(0, 300, 0));
        assertEquals(1, VisualActivity.floorCount(25, 300, 1));
        assertEquals(7, VisualActivity.floorCount(50, 300, 4));
        assertEquals(300, VisualActivity.floorCount(100, 300, 12));
        assertEquals(0, VisualActivity.floorCount(100, 0, 12));
    }

    @Test public void largeScenesHaveGentleLowEndAndMonotonicCoverage()
    {
        assertEquals(1, VisualActivity.floorCount(25, 10000, 1));
        assertEquals(1, VisualActivity.floorCount(26, 10000, 1));
        assertEquals(1, VisualActivity.floorCount(27, 10000, 1));
        assertEquals(2, VisualActivity.floorCount(30, 10000, 1));
        int previous = 1;
        for (int level = 26; level <= 100; level++)
        {
            int count = VisualActivity.floorCount(level, 10000, 1);
            org.junit.Assert.assertTrue(count >= previous && count <= 10000);
            previous = count;
        }
        assertEquals(10000, previous);
    }
    @Test public void supportsQuietOriginalAndDenseActivity()
    {
        VisualActivity activity = new VisualActivity();
        assertEquals(0, activity.nextCount(0));
        assertEquals(1, activity.nextCount(25));
        assertEquals(4, activity.nextCount(50));
        assertEquals(12, activity.nextCount(100));
        int total = 0;
        for (int i = 0; i < 25; i++) total += activity.nextCount(1);
        assertEquals(1, total);
    }

    @Test public void clampsAndResetsWhenChangingActivity()
    {
        VisualActivity activity = new VisualActivity();
        assertEquals(0, activity.nextCount(-1));
        assertEquals(12, activity.nextCount(101));
        assertEquals(0, activity.nextCount(24));
        assertEquals(0, activity.nextCount(1));
        activity.reset();
        assertEquals(0, activity.nextCount(1));
    }
}
