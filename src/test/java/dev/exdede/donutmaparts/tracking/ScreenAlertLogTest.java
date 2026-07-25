package dev.exdede.donutmaparts.tracking;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenAlertLogTest {
    private static final Object SCREEN_A = new Object();
    private static final Object SCREEN_B = new Object();

    @Test
    void alertsOnceForAnIdUnderOneScreenToken() {
        ScreenAlertLog log = new ScreenAlertLog();
        assertTrue(log.shouldAlert(SCREEN_A, 42), "first sighting alerts");
        assertFalse(log.shouldAlert(SCREEN_A, 42), "second sighting is silent");
        assertFalse(log.shouldAlert(SCREEN_A, 42), "and stays silent");
    }

    @Test
    void differentIdsUnderTheSameTokenEachAlertOnce() {
        ScreenAlertLog log = new ScreenAlertLog();
        assertTrue(log.shouldAlert(SCREEN_A, 1));
        assertTrue(log.shouldAlert(SCREEN_A, 2));
        assertFalse(log.shouldAlert(SCREEN_A, 1));
    }

    @Test
    void alertsAgainAfterTheTokenChanges() {
        ScreenAlertLog log = new ScreenAlertLog();
        assertTrue(log.shouldAlert(SCREEN_A, 42));
        assertFalse(log.shouldAlert(SCREEN_A, 42));
        assertTrue(log.shouldAlert(SCREEN_B, 42), "reopening a screen re-alerts");
    }

    @Test
    void tokenChangeClearsPreviousHighlights() {
        ScreenAlertLog log = new ScreenAlertLog();
        log.shouldAlert(SCREEN_A, 42);
        log.noteHighlight(42);
        assertTrue(log.isHighlighted(42));
        log.syncToken(SCREEN_B);
        assertFalse(log.isHighlighted(42), "highlights do not leak across screens");
    }

    @Test
    void highlightSurvivesRemovalFromTheTrackedList() {
        // The auto remove case: MapTracker drops the ID from config right after
        // noting the highlight, and the slot must keep glowing until the screen closes.
        ScreenAlertLog log = new ScreenAlertLog();
        log.shouldAlert(SCREEN_A, 42);
        log.noteHighlight(42);
        assertTrue(log.isHighlighted(42));
        assertTrue(log.isHighlighted(42), "repeat reads stay true");
    }

    @Test
    void clearDropsEverything() {
        ScreenAlertLog log = new ScreenAlertLog();
        log.shouldAlert(SCREEN_A, 42);
        log.noteHighlight(42);
        log.clear();
        assertFalse(log.isHighlighted(42));
        assertTrue(log.shouldAlert(SCREEN_A, 42), "cleared log alerts again");
    }

    @Test
    void syncingTheSameTokenTwiceDoesNotClear() {
        ScreenAlertLog log = new ScreenAlertLog();
        log.shouldAlert(SCREEN_A, 42);
        log.noteHighlight(42);
        log.syncToken(SCREEN_A);
        assertTrue(log.isHighlighted(42), "same screen keeps its state");
        assertFalse(log.shouldAlert(SCREEN_A, 42));
    }
}
