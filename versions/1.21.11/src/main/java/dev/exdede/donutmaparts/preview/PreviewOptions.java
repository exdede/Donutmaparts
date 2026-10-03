package dev.exdede.donutmaparts.preview;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * The cycling option lists behind the Preview tab. Each is a malilib option
 * list entry like AlertSound, kept free of Minecraft imports so the layout and
 * gate logic that switch on them stays unit testable.
 */
public final class PreviewOptions {
    private PreviewOptions() {}

    /** Where the preview is drawn. */
    public enum Mode implements IConfigOptionListEntry {
        TOOLTIP("tooltip", "Inside tooltip"),
        PANEL("panel", "Side panel"),
        BOTH("both", "Tooltip + panel");

        private final String configString;
        private final String displayName;

        Mode(String configString, String displayName) {
            this.configString = configString;
            this.displayName = displayName;
        }

        public boolean tooltip() { return this != PANEL; }
        public boolean panel() { return this != TOOLTIP; }

        @Override public String getStringValue() { return this.configString; }
        @Override public String getDisplayName() { return this.displayName; }
        @Override public IConfigOptionListEntry cycle(boolean forward) { return cycleOf(values(), ordinal(), forward); }
        @Override public IConfigOptionListEntry fromString(String value) { return parse(values(), value, TOOLTIP); }
    }

    /** Where the side panel sits. The GUI-relative ones flip sides if they would leave the screen. */
    public enum PanelPosition implements IConfigOptionListEntry {
        RIGHT_OF_GUI("right_of_gui", "Right of GUI"),
        LEFT_OF_GUI("left_of_gui", "Left of GUI"),
        ABOVE_GUI("above_gui", "Above GUI"),
        BELOW_GUI("below_gui", "Below GUI"),
        TOP_LEFT("top_left", "Top left corner"),
        TOP_RIGHT("top_right", "Top right corner"),
        BOTTOM_LEFT("bottom_left", "Bottom left corner"),
        BOTTOM_RIGHT("bottom_right", "Bottom right corner"),
        CURSOR("cursor", "Follow cursor");

        private final String configString;
        private final String displayName;

        PanelPosition(String configString, String displayName) {
            this.configString = configString;
            this.displayName = displayName;
        }

        @Override public String getStringValue() { return this.configString; }
        @Override public String getDisplayName() { return this.displayName; }
        @Override public IConfigOptionListEntry cycle(boolean forward) { return cycleOf(values(), ordinal(), forward); }
        @Override public IConfigOptionListEntry fromString(String value) { return parse(values(), value, RIGHT_OF_GUI); }
    }

    /** What has to be held for the preview to show. */
    public enum Trigger implements IConfigOptionListEntry {
        ALWAYS("always", "Always"),
        SHIFT("shift", "Hold Shift"),
        CONTROL("control", "Hold Ctrl"),
        ALT("alt", "Hold Alt");

        private final String configString;
        private final String displayName;

        Trigger(String configString, String displayName) {
            this.configString = configString;
            this.displayName = displayName;
        }

        @Override public String getStringValue() { return this.configString; }
        @Override public String getDisplayName() { return this.displayName; }
        @Override public IConfigOptionListEntry cycle(boolean forward) { return cycleOf(values(), ordinal(), forward); }
        @Override public IConfigOptionListEntry fromString(String value) { return parse(values(), value, ALWAYS); }
    }

    /** Which screens previews appear in. */
    public enum Scope implements IConfigOptionListEntry {
        ALL_SCREENS("all", "All inventories"),
        CONTAINERS("containers", "Containers only"),
        AUCTION_HOUSE("auction_house", "Auction House only");

        private final String configString;
        private final String displayName;

        Scope(String configString, String displayName) {
            this.configString = configString;
            this.displayName = displayName;
        }

        @Override public String getStringValue() { return this.configString; }
        @Override public String getDisplayName() { return this.displayName; }
        @Override public IConfigOptionListEntry cycle(boolean forward) { return cycleOf(values(), ordinal(), forward); }
        @Override public IConfigOptionListEntry fromString(String value) { return parse(values(), value, ALL_SCREENS); }
    }

    /** What surrounds the picture. */
    public enum FrameStyle implements IConfigOptionListEntry {
        MAP_PAPER("map_paper", "Map paper (vanilla)"),
        FLAT("flat", "Flat border"),
        NONE("none", "No frame");

        private final String configString;
        private final String displayName;

        FrameStyle(String configString, String displayName) {
            this.configString = configString;
            this.displayName = displayName;
        }

        @Override public String getStringValue() { return this.configString; }
        @Override public String getDisplayName() { return this.displayName; }
        @Override public IConfigOptionListEntry cycle(boolean forward) { return cycleOf(values(), ordinal(), forward); }
        @Override public IConfigOptionListEntry fromString(String value) { return parse(values(), value, MAP_PAPER); }
    }

    /** Text line under the preview. */
    public enum Label implements IConfigOptionListEntry {
        NONE("none", "No label"),
        MAP_ID("map_id", "Map ID"),
        MAP_ID_AND_SOURCE("map_id_source", "Map ID + source");

        private final String configString;
        private final String displayName;

        Label(String configString, String displayName) {
            this.configString = configString;
            this.displayName = displayName;
        }

        @Override public String getStringValue() { return this.configString; }
        @Override public String getDisplayName() { return this.displayName; }
        @Override public IConfigOptionListEntry cycle(boolean forward) { return cycleOf(values(), ordinal(), forward); }
        @Override public IConfigOptionListEntry fromString(String value) { return parse(values(), value, MAP_ID); }
    }

    static <E extends Enum<E>> E cycleOf(E[] values, int ordinal, boolean forward) {
        int index = ordinal + (forward ? 1 : -1);
        if (index < 0) index = values.length - 1;
        else if (index >= values.length) index = 0;
        return values[index];
    }

    static <E extends Enum<E> & IConfigOptionListEntry> E parse(E[] values, String value, E fallback) {
        for (E e : values) {
            if (e.getStringValue().equalsIgnoreCase(value)) return e;
        }
        return fallback;
    }
}
