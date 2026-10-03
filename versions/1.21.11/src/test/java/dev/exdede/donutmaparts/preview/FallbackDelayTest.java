package dev.exdede.donutmaparts.preview;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FallbackDelayTest {
    @Test
    void waitsTheDelayFromTheFirstHover() {
        FallbackDelay d = new FallbackDelay();
        assertFalse(d.ready(1, 1_000, 1_000));
        assertFalse(d.ready(1, 1_999, 1_000));
        assertTrue(d.ready(1, 2_000, 1_000));
    }

    @Test
    void zeroDelayIsImmediate() {
        assertTrue(new FallbackDelay().ready(1, 0, 0));
    }

    @Test
    void mapsWaitIndependently() {
        FallbackDelay d = new FallbackDelay();
        d.ready(1, 0, 500);
        assertFalse(d.ready(2, 400, 500));
        assertTrue(d.ready(1, 500, 500));
        assertFalse(d.ready(2, 800, 500));
        assertTrue(d.ready(2, 900, 500));
    }

    @Test
    void forgetsMapsNotHoveredForAMinute() {
        FallbackDelay d = new FallbackDelay();
        d.ready(1, 0, 500);
        assertTrue(d.ready(1, 600, 500));
        assertFalse(d.ready(1, 70_000, 500));
    }
}
