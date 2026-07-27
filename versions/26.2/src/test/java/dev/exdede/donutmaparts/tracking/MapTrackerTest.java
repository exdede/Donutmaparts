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

    @SuppressWarnings("unchecked")
    private static Set<Integer> autoCollectedOf(MapTracker tracker) throws Exception {
        Field field = MapTracker.class.getDeclaredField("autoCollectedThisSession");
        field.setAccessible(true);
        return (Set<Integer>) field.get(tracker);
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

    /**
     * isAutoCollectEligible(int, boolean) is the Configs-free half of the
     * auto-collect decision (see the split between shouldHighlight and
     * isHighlightEligible above for why): it never touches
     * Configs.Tracking.AUTO_COLLECT itself, only scopeAllowedThisTick, the
     * onDonutSmp signal passed in by the caller, and the in-memory
     * per-session dedup set.
     */
    @Test
    void eligibleForAutoCollectWhenScopeAllowedAndOnDonutSmpAndNotYetSubmitted() throws Exception {
        MapTracker tracker = new MapTracker();
        setScopeAllowed(tracker, true);

        assertTrue(tracker.isAutoCollectEligible(42, true));
    }

    @Test
    void notEligibleForAutoCollectWhenScopeIsNotAllowed() throws Exception {
        MapTracker tracker = new MapTracker();
        setScopeAllowed(tracker, false);

        assertFalse(tracker.isAutoCollectEligible(42, true));
    }

    @Test
    void notEligibleForAutoCollectWhenNotOnDonutSmp() throws Exception {
        MapTracker tracker = new MapTracker();
        setScopeAllowed(tracker, true);

        assertFalse(tracker.isAutoCollectEligible(42, false));
    }

    @Test
    void notEligibleForAutoCollectWhenAlreadySubmittedThisSession() throws Exception {
        MapTracker tracker = new MapTracker();
        setScopeAllowed(tracker, true);
        autoCollectedOf(tracker).add(42);

        assertFalse(tracker.isAutoCollectEligible(42, true));
    }

    @Test
    void autoCollectEligibilityIsPerMapId() throws Exception {
        MapTracker tracker = new MapTracker();
        setScopeAllowed(tracker, true);
        autoCollectedOf(tracker).add(42);

        assertTrue(tracker.isAutoCollectEligible(99, true));
    }

    /**
     * shouldScanThisTick(boolean, boolean, boolean) is the pure form of
     * tickScreen()'s own top-of-method gating decision (see the javadoc on
     * both), split out specifically because tickScreen() itself needs a
     * live MinecraftClient/Configs and can't be driven directly here. These
     * cover all five scenarios from the TRACKING_ENABLED/AUTO_COLLECT gating
     * fix: TRACKING_ENABLED=false must block everything regardless of
     * AUTO_COLLECT (the bug), and every TRACKING_ENABLED=true combination
     * must behave exactly as it did before that bug existed.
     */
    @Test
    void trackingDisabledBlocksScanEvenWhenAutoCollectEnabled() {
        // Scenario 1: the bug. TRACKING_ENABLED=false must produce the same
        // early return regardless of AUTO_COLLECT or trackedIds.
        MapTracker tracker = new MapTracker();

        assertFalse(tracker.shouldScanThisTick(false, false, true));
        assertFalse(tracker.shouldScanThisTick(false, true, true));
        assertFalse(tracker.shouldScanThisTick(false, false, false));
    }

    @Test
    void trackingEnabledWithNoTrackedIdsAndAutoCollectOffDoesNotScan() {
        // Scenario 2: unchanged, early return.
        MapTracker tracker = new MapTracker();

        assertFalse(tracker.shouldScanThisTick(true, false, false));
    }

    @Test
    void trackingEnabledWithNoTrackedIdsButAutoCollectOnProceeds() {
        // Scenario 3: unchanged, proceeds -- the case the last commit added.
        MapTracker tracker = new MapTracker();

        assertTrue(tracker.shouldScanThisTick(true, false, true));
    }

    @Test
    void trackingEnabledWithTrackedIdsAndAutoCollectOffProceeds() {
        // Scenario 4: unchanged, tracked-id scan runs.
        MapTracker tracker = new MapTracker();

        assertTrue(tracker.shouldScanThisTick(true, true, false));
    }

    @Test
    void trackingEnabledWithTrackedIdsAndAutoCollectOnProceeds() {
        // Scenario 5: unchanged, both paths run independently per slot.
        MapTracker tracker = new MapTracker();

        assertTrue(tracker.shouldScanThisTick(true, true, true));
    }
}
