package dev.exdede.donutmaparts.tracking;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * shouldHighlight(int) itself is not exercised here: its first line touches
 * Configs.Tracking.TRACKING_ENABLED, and Configs cannot be loaded outside a
 * running Fabric Loader (malilib's ConfigBase reaches into
 * FabricLoaderImpl.getGameDir() during class init) -- not something a plain
 * JUnit run provides, which is why MapTracker had no tests before this scope
 * gate needed one. isHighlightEligible(int) is the Configs-free half of that
 * method (trackedIds + the per-tick scope cache) and is what these tests
 * drive, poking tickScreen()'s would-be state via reflection since
 * tickScreen() itself needs a live MinecraftClient/HandledScreen to run.
 */
class MapTrackerTest {
    private static void setTrackedIds(MapTracker tracker, Set<Integer> ids) throws Exception {
        Field field = MapTracker.class.getDeclaredField("trackedIds");
        field.setAccessible(true);
        field.set(tracker, ids);
    }

    private static void setScopeAllowed(MapTracker tracker, boolean allowed) throws Exception {
        Field field = MapTracker.class.getDeclaredField("scopeAllowedThisTick");
        field.setAccessible(true);
        field.setBoolean(tracker, allowed);
    }

    private static ScreenAlertLog alertLogOf(MapTracker tracker) throws Exception {
        Field field = MapTracker.class.getDeclaredField("alertLog");
        field.setAccessible(true);
        return (ScreenAlertLog) field.get(tracker);
    }

    @Test
    void eligibleForATrackedIdWhenScopeIsAllowed() throws Exception {
        MapTracker tracker = new MapTracker();
        setTrackedIds(tracker, Set.of(42));
        setScopeAllowed(tracker, true);

        assertTrue(tracker.isHighlightEligible(42));
    }

    @Test
    void notEligibleWhenScopeIsNotAllowedEvenThoughStillTracked() throws Exception {
        // The gap this fixes: with TRACK_SHULKER_BOX off, tickScreen() already
        // skipped the sound/toast alert, but the highlight check previously
        // only looked at trackedIds.contains(mapId), so the slot kept glowing
        // anyway. scopeAllowedThisTick=false is what tickScreen() now caches
        // for a disallowed screen; the highlight must be suppressed too.
        MapTracker tracker = new MapTracker();
        setTrackedIds(tracker, Set.of(42));
        setScopeAllowed(tracker, false);

        assertFalse(tracker.isHighlightEligible(42));
    }

    @Test
    void notEligibleForAnUntrackedIdEvenWhenScopeIsAllowed() throws Exception {
        MapTracker tracker = new MapTracker();
        setTrackedIds(tracker, Set.of(42));
        setScopeAllowed(tracker, true);

        assertFalse(tracker.isHighlightEligible(99));
    }

    @Test
    void stillEligibleAfterAutoRemoveWhenScopeRemainsAllowed() throws Exception {
        // Non-regression: the sticky "still glowing after auto remove pulled
        // the ID out of trackedIds" behaviour must survive this change as
        // long as the screen's scope is still the allowed one it was matched
        // under (tickScreen leaves scopeAllowedThisTick untouched across the
        // trackedIds-empty early return for exactly this reason).
        MapTracker tracker = new MapTracker();
        setTrackedIds(tracker, Set.of());
        setScopeAllowed(tracker, true);
        alertLogOf(tracker).noteHighlight(42);

        assertTrue(tracker.isHighlightEligible(42));
    }

    @Test
    void stickyHighlightAfterAutoRemoveIsStillSuppressedWhenScopeIsDisallowed() throws Exception {
        MapTracker tracker = new MapTracker();
        setTrackedIds(tracker, Set.of());
        setScopeAllowed(tracker, false);
        alertLogOf(tracker).noteHighlight(42);

        assertFalse(tracker.isHighlightEligible(42));
    }
}
