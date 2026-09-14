package com.sob.musicviz;

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ColorSchemeTest
{
    @Test public void customGradientIncludesEndpointsAndIntermediateShades()
    {
        Color start = new Color(0, 110, 220);
        Color end = new Color(220, 0, 110);
        assertEquals(start, ColorScheme.CUSTOM.forNote(60, start, end));
        assertEquals(end, ColorScheme.CUSTOM.forNote(71, start, end));
        assertEquals(new Color(100, 60, 170), ColorScheme.CUSTOM.forNote(65, start, end));
        assertEquals(start, ColorScheme.CUSTOM.forNote(72, start, end));
        assertEquals(start, ColorScheme.CUSTOM.forNote(65, start, start));
    }
}
