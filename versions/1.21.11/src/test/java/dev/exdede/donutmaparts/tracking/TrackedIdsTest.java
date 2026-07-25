package dev.exdede.donutmaparts.tracking;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class TrackedIdsTest {
    @Test
    void parsesNewlineSeparatedIds() {
        assertEquals(List.of(1, 2, 3), TrackedIds.parseIds("1\n2\n3"));
    }

    @Test
    void parsesCommaSeparatedIds() {
        assertEquals(List.of(10, 20, 30), TrackedIds.parseIds("10,20,30"));
    }

    @Test
    void parsesMixedSeparatorsAndWhitespace() {
        assertEquals(List.of(4, 5, 6, 7),
            TrackedIds.parseIds("  4, 5 ;6\n\n 7  "));
    }

    @Test
    void stripsLeadingHash() {
        assertEquals(List.of(4471, 12), TrackedIds.parseIds("#4471, #12"));
    }

    @Test
    void dedupesWithinOneParse() {
        assertEquals(List.of(8, 9), TrackedIds.parseIds("8, 9, 8, #9"));
    }

    @Test
    void rejectsNonNumericTokens() {
        assertEquals(List.of(5), TrackedIds.parseIds("map, 5, banana"));
    }

    @Test
    void rejectsNegativeNumbers() {
        assertEquals(List.of(), TrackedIds.parseIds("-5"));
    }

    @Test
    void rejectsIntegerOverflow() {
        assertEquals(List.of(), TrackedIds.parseIds("99999999999"));
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertEquals(List.of(), TrackedIds.parseIds(""));
        assertEquals(List.of(), TrackedIds.parseIds("   \n  "));
        assertEquals(List.of(), TrackedIds.parseIds(null));
    }

    @Test
    void mergeAppendsPreservingFirstSeenOrder() {
        List<String> merged = TrackedIds.merge(List.of("3", "1"), List.of(2, 3));
        assertEquals(List.of("3", "1", "2"), merged);
    }

    @Test
    void mergeNormalisesInconsistentlySpelledExistingEntries() {
        List<String> merged = TrackedIds.merge(List.of("#7", "7", " 7 "), List.of(8));
        assertEquals(List.of("7", "8"), merged);
    }

    @Test
    void removeDropsOnlyTheRequestedIdAndKeepsOrder() {
        assertEquals(List.of("1", "3"), TrackedIds.remove(List.of("1", "2", "3"), 2));
    }

    @Test
    void removeOfAbsentIdIsANoOp() {
        assertEquals(List.of("1", "2"), TrackedIds.remove(List.of("1", "2"), 99));
    }

    @Test
    void toIdSetSkipsUnparseableStoredEntries() {
        Set<Integer> ids = TrackedIds.toIdSet(List.of("1", "junk", "#2", "", "-3"));
        assertEquals(Set.of(1, 2), ids);
    }

    @Test
    void toIdSetHandlesNull() {
        assertEquals(Set.of(), TrackedIds.toIdSet(null));
    }
}
