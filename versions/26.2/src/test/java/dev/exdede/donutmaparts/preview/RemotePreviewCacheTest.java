package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.preview.RemotePreviewCache.Status;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RemotePreviewCacheTest {
    final List<String> evicted = new ArrayList<>();

    RemotePreviewCache<String> cache(int maxLoaded, int maxInFlight) {
        return new RemotePreviewCache<>(maxLoaded, maxInFlight, 1000, 100, evicted::add);
    }

    @Test
    void beginsOnceAndReportsLoading() {
        var c = cache(10, 3);
        assertTrue(c.tryBegin(1, 0));
        assertFalse(c.tryBegin(1, 0));
        assertEquals(Status.LOADING, c.status(1, 0));
        assertEquals(1, c.inFlight());
    }

    @Test
    void capsConcurrentFetches() {
        var c = cache(10, 2);
        assertTrue(c.tryBegin(1, 0));
        assertTrue(c.tryBegin(2, 0));
        assertFalse(c.tryBegin(3, 0));
        assertEquals(Status.ABSENT, c.status(3, 0));
        c.complete(1, c.generation(), "a");
        assertTrue(c.tryBegin(3, 0));
    }

    @Test
    void completeMakesValueReady() {
        var c = cache(10, 3);
        c.tryBegin(1, 0);
        c.complete(1, c.generation(), "a");
        assertEquals(Status.READY, c.status(1, 0));
        assertEquals("a", c.value(1));
        assertEquals(0, c.inFlight());
    }

    @Test
    void missesAndFailuresExpireAtDifferentRates() {
        var c = cache(10, 3);
        c.tryBegin(1, 0);
        c.missing(1, c.generation(), 0);
        c.tryBegin(2, 0);
        c.failed(2, c.generation(), 0);
        assertEquals(Status.MISSING, c.status(1, 500));
        assertEquals(Status.ABSENT, c.status(2, 500));
        assertFalse(c.tryBegin(1, 500));
        assertTrue(c.tryBegin(2, 500));
        assertEquals(Status.ABSENT, c.status(1, 1000));
    }

    @Test
    void evictsLeastRecentlyUsedAndHandsBackTheValue() {
        var c = cache(2, 3);
        for (int id = 1; id <= 2; id++) {
            c.tryBegin(id, 0);
            c.complete(id, c.generation(), "v" + id);
        }
        c.value(1); // touch 1, so 2 is now the oldest
        c.tryBegin(3, 0);
        c.complete(3, c.generation(), "v3");
        assertEquals(List.of("v2"), evicted);
        assertEquals("v1", c.value(1));
        assertNull(c.value(2));
        assertEquals(2, c.loadedCount());
    }

    @Test
    void clearReleasesEverythingAndOrphansInFlightFetches() {
        var c = cache(10, 3);
        c.tryBegin(1, 0);
        long oldGen = c.generation();
        c.complete(1, oldGen, "a");
        c.tryBegin(2, 0);
        c.clear();
        assertEquals(List.of("a"), evicted);
        assertEquals(0, c.inFlight());

        // Same map requested again after the reconnect, then the stale fetch lands.
        assertTrue(c.tryBegin(2, 0));
        c.complete(2, oldGen, "stale");
        assertEquals(Status.LOADING, c.status(2, 0));
        assertEquals(1, c.inFlight());
        assertEquals(List.of("a", "stale"), evicted);
    }

    @Test
    void reportsForUnknownMapsAreIgnored() {
        var c = cache(10, 3);
        c.missing(9, c.generation(), 0);
        c.failed(9, c.generation(), 0);
        assertEquals(Status.ABSENT, c.status(9, 0));
        assertEquals(0, c.inFlight());
    }
}
