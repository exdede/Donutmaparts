package dev.exdede.donutmaparts.preview;

import java.util.Locale;

/** String decisions for the preview, kept free of Minecraft imports for tests. */
public final class PreviewText {
    private PreviewText() {}

    /** Where the picture being shown came from, for the optional label. */
    public enum Source { LOCAL, WALL, LOADING, NONE }

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
            case NONE -> "No preview";
            default -> null;
        };
    }

    private static String sourceName(Source source) {
        return switch (source) {
            case LOCAL -> "server";
            case WALL -> "wall";
            case LOADING -> "loading";
            case NONE -> "unavailable";
        };
    }
}
