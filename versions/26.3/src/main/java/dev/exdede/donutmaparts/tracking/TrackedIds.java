package dev.exdede.donutmaparts.tracking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure list maths behind the tracked map ID list. Deliberately free of any
 * Minecraft or malilib import so the unit tests can exercise it without a
 * client, matching the rest of this mod's pure core.
 */
public final class TrackedIds {
    private static final Pattern SEPARATORS = Pattern.compile("[,;\\s]+");

    private TrackedIds() {}

    /**
     * Parses user typed or pasted text into map IDs. Accepts newline, comma,
     * semicolon and whitespace separators in any mix, tolerates a leading '#',
     * and silently drops anything that is not a non negative int. Duplicates
     * are collapsed, first occurrence wins.
     */
    public static List<Integer> parseIds(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null) return out;
        for (String token : SEPARATORS.split(raw.trim())) {
            Integer id = parseId(token);
            if (id != null && !out.contains(id)) out.add(id);
        }
        return out;
    }

    /**
     * Appends incoming IDs to the stored list, dropping duplicates. Existing
     * entries are normalised first, so a hand edited json holding both "#7"
     * and "7" collapses to one entry on the next edit.
     */
    public static List<String> merge(List<String> existing, List<Integer> incoming) {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>(toIdList(existing));
        if (incoming != null) ids.addAll(incoming);
        return toStringList(ids);
    }

    /** Returns a copy of the stored list without the given ID. */
    public static List<String> remove(List<String> existing, int id) {
        List<Integer> kept = new ArrayList<>(toIdList(existing));
        kept.remove(Integer.valueOf(id));
        return toStringList(kept);
    }

    /** Lookup set for the hot path. Unparseable stored entries are skipped. */
    public static Set<Integer> toIdSet(List<String> stored) {
        return new LinkedHashSet<>(toIdList(stored));
    }

    private static List<Integer> toIdList(List<String> stored) {
        List<Integer> out = new ArrayList<>();
        if (stored == null) return out;
        for (String entry : stored) {
            Integer id = parseId(entry);
            if (id != null && !out.contains(id)) out.add(id);
        }
        return out;
    }

    private static List<String> toStringList(Iterable<Integer> ids) {
        List<String> out = new ArrayList<>();
        for (Integer id : ids) out.add(String.valueOf(id));
        return out;
    }

    private static Integer parseId(String token) {
        if (token == null) return null;
        String text = token.trim();
        if (text.startsWith("#")) text = text.substring(1);
        if (text.isEmpty()) return null;
        // Digits only, which is also what rejects negatives and stray signs.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') return null;
        }
        try {
            return Integer.valueOf(text);
        }
        catch (NumberFormatException e) {
            // Overflows int. Not a real map ID, drop it.
            return null;
        }
    }
}
