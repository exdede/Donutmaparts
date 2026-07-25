package dev.exdede.donutmaparts.config.gui;

import fi.dy.masa.malilib.util.StringUtils;

/** Tabs across the top of the mod's config screen. */
public enum ConfigGuiTab {
    GENERAL("donutmaparts.gui.button.config_gui.general"),
    TRACKING("donutmaparts.gui.button.config_gui.tracking");

    private final String translationKey;

    ConfigGuiTab(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getDisplayName() {
        return StringUtils.translate(this.translationKey);
    }
}
