package dev.exdede.donutmaparts.capture;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SettleTrackerTest {
    static byte[] pixels(int fill) {
        byte[] p = new byte[16384];
        java.util.Arrays.fill(p, (byte) fill);
        return p;
    }

    @Test
    void promotesAfterStableWindow() {
        SettleTracker t = new SettleTracker();
        t.observe(1, pixels(5), 1000);
        assertTrue(t.collectSettled(1000 + 1499, 1500).isEmpty(), "not settled before window");
        List<SettleTracker.Promotion> settled = t.collectSettled(1000 + 1500, 1500);
        assertEquals(1, settled.size());
        assertEquals(1, settled.get(0).mapId());
        assertEquals(5, settled.get(0).pixels()[0]);
        assertFalse(t.isTracking(1), "promoted map is removed from tracking");
    }

    @Test
    void changeResetsTheClock() {
        SettleTracker t = new SettleTracker();
        t.observe(1, pixels(5), 1000);
        t.observe(1, pixels(6), 2000); // changed, resets to 2000
        assertTrue(t.collectSettled(3000, 1500).isEmpty(), "1000ms since change < 1500");
        List<SettleTracker.Promotion> settled = t.collectSettled(3500, 1500);
        assertEquals(1, settled.size());
        assertEquals(6, settled.get(0).pixels()[0], "promotes the latest pixels");
    }

    @Test
    void unchangedObserveDoesNotResetClock() {
        SettleTracker t = new SettleTracker();
        t.observe(1, pixels(5), 1000);
        t.observe(1, pixels(5), 1400); // identical, must NOT reset
        List<SettleTracker.Promotion> settled = t.collectSettled(2500, 1500);
        assertEquals(1, settled.size(), "clock stays at 1000, settled by 2500");
    }

    @Test
    void allBlankNeverPromotes() {
        SettleTracker t = new SettleTracker();
        t.observe(1, pixels(0), 1000);
        assertTrue(t.collectSettled(9999, 1500).isEmpty(), "blank map is not a mapart");
        assertFalse(t.isTracking(1), "blank map dropped, not left tracking forever");
    }

    @Test
    void forgetAndClearRemoveTracking() {
        SettleTracker t = new SettleTracker();
        t.observe(1, pixels(5), 1000);
        t.observe(2, pixels(7), 1000);
        t.forget(1);
        assertFalse(t.isTracking(1));
        assertTrue(t.isTracking(2));
        t.clear();
        assertFalse(t.isTracking(2));
    }
}
