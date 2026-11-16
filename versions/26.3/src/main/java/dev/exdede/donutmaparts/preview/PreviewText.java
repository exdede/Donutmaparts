package dev.exdede.donutmaparts.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/** String decisions for the preview, kept free of Minecraft imports for tests. */
public final class PreviewText {
    private PreviewText() {}

    /** Where the picture being shown came from, for the optional label. */
    public enum Source { LOCAL, WALL, LOADING, REMOVED, NONE }

    /**
     * Broader than TrackingScope's "auction house" substring on purpose: a
     * preview filter that misses the real AH title is far more annoying than
     * one that also matches some other "auction" GUI.
     */
    public static boolean isAuctionHouseTitle(String title) {
        if (title == null) return false;
        String t = title.toLowerCase(Locale.ROOT).trim();
        return t.contains("auction") || t.equals("ah") || t.startsWith("ah ") || t.startsWith("ah(");
    }

    /** The label line, or null when labels are off. */
    public static String label(PreviewOptions.Label mode, int mapId, Source source) {
        return switch (mode) {
            case NONE -> null;
            case MAP_ID -> "Map #" + mapId;
            case MAP_ID_AND_SOURCE -> "Map #" + mapId + " · " + sourceName(source);
        };
    }

    /**
     * Credit line for a picture that came from the wall rather than from the
     * server, or null. Doubles as a heads-up that it is the last captured
     * state, not necessarily what the map looks like right now.
     */
    public static String credit(Source source, boolean enabled) {
        return enabled && source == Source.WALL ? "via exdede.xyz/maparts" : null;
    }

    /** Placeholder text drawn in place of a picture, or null when there is a picture. */
    public static String placeholder(Source source) {
        return switch (source) {
            case LOADING -> "Loading...";
            case REMOVED -> "Removed from wall";
            case NONE -> "No preview";
            default -> null;
        };
    }

    /** Greedy word wrap so a placeholder fits inside a small picture. A single too-long word gets its own line. */
    public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> width) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && width.applyAsInt(next) > maxWidth) {
                lines.add(line);
                line = word;
            } else {
                line = next;
            }
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }

    private static String sourceName(Source source) {
        return switch (source) {
            case LOCAL -> "server";
            case WALL -> "wall";
            case LOADING -> "loading";
            case REMOVED -> "removed";
            case NONE -> "unavailable";
        };
    }
}
