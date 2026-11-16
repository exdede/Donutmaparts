package dev.exdede.donutmaparts.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrackedGroupsTest {
    @Test
    void roundTripsNameWithPipes() {
        List<String> stored = TrackedGroups.add(List.of(), "meow | copies", List.of(1, 2, 3));
        List<TrackedGroups.Group> groups = TrackedGroups.decode(stored);
        assertEquals(1, groups.size());
        assertEquals("meow | copies", groups.get(0).name());
        assertEquals(List.of(1, 2, 3), groups.get(0).ids());
        assertEquals(TrackedGroups.PALETTE[0], groups.get(0).color());
    }

    @Test
    void handsOutColorsInOrderAndNamesBlankGroups() {
        List<String> stored = TrackedGroups.add(List.of(), "a", List.of(1));
        stored = TrackedGroups.add(stored, "  ", List.of(2));
        List<TrackedGroups.Group> groups = TrackedGroups.decode(stored);
        assertEquals(TrackedGroups.PALETTE[1], groups.get(1).color());
        assertEquals("Group 2", groups.get(1).name());
    }

    @Test
    void ignoresEmptyIdListsAndGarbage() {
        assertTrue(TrackedGroups.add(List.of(), "x", List.of()).isEmpty());
        assertTrue(TrackedGroups.decode(List.of("nope", "ZZZZZZ|1|x", "FFAA00||x")).isEmpty());
    }

    @Test
    void removeContainingDropsEveryGroupWithTheMap() {
        List<String> stored = TrackedGroups.add(List.of(), "a", List.of(1, 2));
        stored = TrackedGroups.add(stored, "b", List.of(2, 3));
        stored = TrackedGroups.add(stored, "c", List.of(4));
        List<TrackedGroups.Group> left = TrackedGroups.decode(TrackedGroups.removeContaining(stored, 2));
        assertEquals(1, left.size());
        assertEquals("c", left.get(0).name());
    }

    @Test
    void removeRenameAndCycle() {
        List<String> stored = TrackedGroups.add(List.of(), "a", List.of(1));
        stored = TrackedGroups.add(stored, "b", List.of(2));
        stored = TrackedGroups.rename(stored, 1, "bee");
        stored = TrackedGroups.cycleColor(stored, 1);
        List<TrackedGroups.Group> groups = TrackedGroups.decode(stored);
        assertEquals("bee", groups.get(1).name());
        assertEquals(TrackedGroups.PALETTE[2], groups.get(1).color());
        assertEquals(List.of("a"), TrackedGroups.decode(TrackedGroups.removeAt(stored, 1)).stream().map(TrackedGroups.Group::name).toList());
    }

    @Test
    void byIdKeepsTheFirstGroup() {
        List<String> stored = TrackedGroups.add(List.of(), "a", List.of(1, 2));
        stored = TrackedGroups.add(stored, "b", List.of(2, 3));
        var byId = TrackedGroups.byId(TrackedGroups.decode(stored));
        assertEquals("a", byId.get(2).name());
        assertEquals(3, byId.size());
    }

    @Test
    void migratesTheFlatList() {
        List<String> stored = TrackedGroups.migrate(List.of(), List.of("7", "#8", "junk"));
        List<TrackedGroups.Group> groups = TrackedGroups.decode(stored);
        assertEquals(List.of(7, 8), groups.get(0).ids());
        assertEquals(List.of(), TrackedGroups.migrate(List.of(), List.of()));
    }
}
