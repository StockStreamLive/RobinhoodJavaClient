package com.cheddar.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathUtilTest {

    @Test
    public void testRoundToTick_alreadyRounded_expectEqual() {
        double rounded = MathUtil.roundToTick(12.03678, .05f);
        assertEquals(12.05, rounded, .0001d);

        rounded = MathUtil.roundToTick(12.01678, .05f);
        assertEquals(12.00, rounded, .0001d);
    }

}
