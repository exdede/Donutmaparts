package dev.exdede.donutmaparts.tracking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracked map IDs come in named, colored groups, because one mapart is
 * usually many map IDs (every copy someone printed has its own), and once
 * any copy turns up the rest are noise. A group is removed in one click, or
 * automatically when one of its maps is found.
 *
 * Stored as one config string per group, "RRGGBB|id,id,id|name", name last
 * so it may contain anything, including '|'. Pure Java, no Minecraft or
 * malilib imports, so the tests run without a client.
 */
public final class TrackedGroups {
    /** Highlight colors handed out in order, picked to stay readable on the gray inventory. */
    public static final int[] PALETTE = {
        0xFFAA00, 0x55FF55, 0x55FFFF, 0xFF55FF, 0xFF5555, 0x5599FF, 0xFFFF55, 0xFFFFFF
    };

    public record Group(String name, int color, List<Integer> ids) {}

    private TrackedGroups() {}

    public static List<Group> decode(List<String> stored) {
        List<Group> out = new ArrayList<>();
        if (stored == null) return out;
        for (String entry : stored) {
            Group g = decodeOne(entry);
            if (g != null) out.add(g);
        }
        return out;
    }

    public static List<String> encode(List<Group> groups) {
        List<String> out = new ArrayList<>();
        for (Group g : groups) {
            StringBuilder ids = new StringBuilder();
            for (Integer id : g.ids()) {
                if (ids.length() > 0) ids.append(',');
                ids.append(id);
            }
            out.add(String.format("%06X", g.color() & 0xFFFFFF) + "|" + ids + "|" + g.name());
        }
        return out;
    }

    /** Appends a group. Empty ID lists are ignored, a blank name falls back to "Group N". */
    public static List<String> add(List<String> stored, String name, List<Integer> ids) {
        List<Group> groups = decode(stored);
        if (ids == null || ids.isEmpty()) return encode(groups);
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) clean = defaultName(groups.size());
        groups.add(new Group(clean, PALETTE[groups.size() % PALETTE.length], List.copyOf(ids)));
        return encode(groups);
    }

    public static String defaultName(int existingCount) {
        return "Group " + (existingCount + 1);
    }

    public static List<String> removeAt(List<String> stored, int index) {
        List<Group> groups = decode(stored);
        if (index >= 0 && index < groups.size()) groups.remove(index);
        return encode(groups);
    }

    /** Drops every group holding this map, which is what "found it" means for a group. */
    public static List<String> removeContaining(List<String> stored, int mapId) {
        List<Group> groups = decode(stored);
        groups.removeIf(g -> g.ids().contains(mapId));
        return encode(groups);
    }

    public static List<String> rename(List<String> stored, int index, String name) {
        List<Group> groups = decode(stored);
        String clean = name == null ? "" : name.trim();
        if (index < 0 || index >= groups.size() || clean.isEmpty()) return encode(groups);
        Group g = groups.get(index);
        groups.set(index, new Group(clean, g.color(), g.ids()));
        return encode(groups);
    }

    /** Moves the group to the next palette color, wrapping around. */
    public static List<String> cycleColor(List<String> stored, int index) {
        List<Group> groups = decode(stored);
        if (index < 0 || index >= groups.size()) return encode(groups);
        Group g = groups.get(index);
        int next = PALETTE[0];
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == g.color()) next = PALETTE[(i + 1) % PALETTE.length];
        }
        groups.set(index, new Group(g.name(), next, g.ids()));
        return encode(groups);
    }

    /** Map ID to the group that tracks it. The first group wins when two share an ID. */
    public static Map<Integer, Group> byId(List<Group> groups) {
        Map<Integer, Group> out = new LinkedHashMap<>();
        for (Group g : groups) {
            for (Integer id : g.ids()) out.putIfAbsent(id, g);
        }
        return out;
    }

    public static int idCount(List<Group> groups) {
        return byId(groups).size();
    }

    /**
     * Folds the pre-groups flat ID list into one group so an upgrade keeps
     * everything. Returns the stored groups unchanged when there is nothing
     * to fold in.
     */
    public static List<String> migrate(List<String> stored, List<String> legacyIds) {
        List<Integer> ids = new ArrayList<>(TrackedIds.toIdSet(legacyIds));
        if (ids.isEmpty()) return stored == null ? List.of() : List.copyOf(stored);
        return add(stored, "Tracked", ids);
    }

    private static Group decodeOne(String entry) {
        if (entry == null) return null;
        String[] parts = entry.split("\\|", 3);
        if (parts.length < 3) return null;
        int color;
        try {
            color = Integer.parseInt(parts[0].trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
        List<Integer> ids = TrackedIds.parseIds(parts[1]);
        if (ids.isEmpty()) return null;
        String name = parts[2].trim();
        return new Group(name.isEmpty() ? "Group" : name, color, List.copyOf(ids));
    }
}
