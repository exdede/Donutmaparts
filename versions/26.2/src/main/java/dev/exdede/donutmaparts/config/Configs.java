package dev.exdede.donutmaparts.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.preview.PreviewOptions;

public class Configs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = DonutMapartsMod.MOD_ID + ".json";

    public static class General {
        // Fixed production endpoint. The mod always talks to exdede.xyz's backend;
        // there is intentionally no user-facing setting for this in the public build.
        public static final String BACKEND_URL = "https://api.exdede.xyz";

        public static final ConfigBoolean ENABLED = new ConfigBoolean(
            "enabled", true, "Master toggle for mapart capture and upload");
        public static final ConfigBoolean TOASTS = new ConfigBoolean(
            "toasts", true, "Show an occasional toast when maparts are uploaded, so you know it is working");
        public static final ConfigInteger BATCH_INTERVAL_SECONDS = new ConfigInteger(
            "batchIntervalSeconds", 30, 5, 300, "Seconds between upload batch flushes");
        public static final ConfigInteger BATCH_MIN_ITEMS = new ConfigInteger(
            "batchMinItems", 10, 1, 100, "Queue size that triggers an immediate flush");
        public static final ConfigBoolean DEBUG_MODE = new ConfigBoolean(
            "debugMode", false, "Debug tooling: slot coloring, tooltips, and verbose console logging. Off by default");
        public static final ConfigInteger SETTLE_DELAY_MILLIS = new ConfigInteger(
            "settleDelayMillis", 1500, 250, 10000, "How long a map's pixels must stay unchanged before it is captured");

        // Everything persisted to disk. Hidden knobs (batch tuning, settle delay)
        // still round-trip through the json so power users can edit them there.
        public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLED, TOASTS, BATCH_INTERVAL_SECONDS, BATCH_MIN_ITEMS,
            DEBUG_MODE, SETTLE_DELAY_MILLIS);

        // What the in-game config screen shows. A deliberate subset of OPTIONS.
        public static final List<IConfigBase> GUI_OPTIONS = ImmutableList.of(
            ENABLED, TOASTS, DEBUG_MODE);
    }

    public static class Tracking {
        public static final ConfigBoolean TRACKING_ENABLED = new ConfigBoolean(
            "trackingEnabled", true, "Watch open inventories for maps on your tracked ID list");
        public static final ConfigStringList TRACKED_MAP_IDS = new ConfigStringList(
            "trackedMapIds", ImmutableList.of(), "Map IDs to watch for. Edited from the Tracking tab");
        public static final ConfigBoolean AUTO_REMOVE_ON_MATCH = new ConfigBoolean(
            "autoRemoveOnMatch", false, "Drop a map ID from the tracked list once it has been found");
        public static final ConfigBoolean ALERT_SOUND_ENABLED = new ConfigBoolean(
            "alertSoundEnabled", true, "Play a sound when a tracked map is found");
        public static final ConfigOptionList ALERT_SOUND = new ConfigOptionList(
            "alertSound", AlertSound.PLING, "Which sound plays when a tracked map is found");
        public static final ConfigBoolean TRACKING_TOASTS = new ConfigBoolean(
            "trackingToasts", true, "Show a toast when a tracked map is found");

        // Per-GUI scope allowlist: which kinds of open container tracking is
        // allowed to alert in. All default true so upgrading an existing
        // install changes nothing until the player deliberately narrows their
        // scope -- the gate is live, but "allow everything" matches prior
        // (pre-scope) behaviour exactly.
        public static final ConfigBoolean TRACK_CHEST = new ConfigBoolean(
            "trackChest", true, "Alert for tracked maps found in a plain chest");
        public static final ConfigBoolean TRACK_ENDER_CHEST = new ConfigBoolean(
            "trackEnderChest", true, "Alert for tracked maps found in your ender chest");
        public static final ConfigBoolean TRACK_SHULKER_BOX = new ConfigBoolean(
            "trackShulkerBox", true, "Alert for tracked maps found in a shulker box");
        public static final ConfigBoolean TRACK_AUCTION_HOUSE = new ConfigBoolean(
            "trackAuctionHouse", true, "Alert for tracked maps found in the Auction House");
        public static final ConfigBoolean TRACK_OTHER = new ConfigBoolean(
            "trackOther", true, "Alert for tracked maps found in any other container");

        // Independent of the scope allowlist above and of TRACKING_ENABLED: this
        // submits newly seen maps to the player's online collection regardless of
        // the tracked-id wishlist. Defaults false (unlike the scope booleans) since
        // this is a brand new feature that starts sending data to the backend, so
        // it needs an explicit opt-in rather than being on by default.
        public static final ConfigBoolean AUTO_COLLECT = new ConfigBoolean(
            "autoCollect", false,
            "Automatically add newly seen maps to your online collection while on DonutSMP");

        // Everything persisted to disk.
        public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            TRACKING_ENABLED, TRACKED_MAP_IDS, AUTO_REMOVE_ON_MATCH,
            ALERT_SOUND_ENABLED, ALERT_SOUND, TRACKING_TOASTS,
            TRACK_CHEST, TRACK_ENDER_CHEST, TRACK_SHULKER_BOX,
            TRACK_AUCTION_HOUSE, TRACK_OTHER, AUTO_COLLECT);

        // What the Tracking tab renders as widgets. TRACKED_MAP_IDS is deliberately
        // absent: it gets its own button row so the tab can offer add and bulk add
        // alongside the list editor.
        public static final List<IConfigBase> GUI_OPTIONS = ImmutableList.of(
            TRACKING_ENABLED, AUTO_REMOVE_ON_MATCH, ALERT_SOUND_ENABLED,
            ALERT_SOUND, TRACKING_TOASTS,
            TRACK_CHEST, TRACK_ENDER_CHEST, TRACK_SHULKER_BOX,
            TRACK_AUCTION_HOUSE, TRACK_OTHER, AUTO_COLLECT);
    }

    public static class Preview {
        public static final ConfigBoolean PREVIEW_ENABLED = new ConfigBoolean(
            "previewEnabled", true, "Show a picture of the map when you hover a filled map in any inventory, including the Auction House");
        public static final ConfigOptionList PREVIEW_MODE = new ConfigOptionList(
            "previewMode", PreviewOptions.Mode.TOOLTIP,
            "Inside tooltip: the picture sits inside the item tooltip.\nSide panel: a bigger picture next to the open GUI.\nBoth: both at once");
        public static final ConfigOptionList PREVIEW_TRIGGER = new ConfigOptionList(
            "previewTrigger", PreviewOptions.Trigger.ALWAYS, "Show the preview always, or only while holding a key");
        public static final ConfigOptionList PREVIEW_SCOPE = new ConfigOptionList(
            "previewScope", PreviewOptions.Scope.ALL_SCREENS,
            "Where previews appear: every inventory screen, only containers (chests, shulkers, server GUIs), or only the Auction House");
        public static final ConfigInteger TOOLTIP_SIZE = new ConfigInteger(
            "tooltipSize", 96, 32, 256, "Size in GUI pixels of the picture inside the tooltip. 128 is one map pixel per GUI pixel");
        public static final ConfigInteger PANEL_SIZE = new ConfigInteger(
            "panelSize", 160, 32, 512, "Size in GUI pixels of the side panel picture");
        public static final ConfigOptionList PANEL_POSITION = new ConfigOptionList(
            "panelPosition", PreviewOptions.PanelPosition.RIGHT_OF_GUI,
            "Where the side panel goes. Next-to-GUI positions flip sides automatically when there is no room");
        public static final ConfigInteger PANEL_MARGIN = new ConfigInteger(
            "panelMargin", 6, 0, 64, "Gap in GUI pixels between the side panel and the GUI, cursor or screen edge");
        public static final ConfigBoolean SHOW_BORDER = new ConfigBoolean(
            "previewBorder", true, "Draw a frame around the preview");
        public static final ConfigColor BORDER_COLOR = new ConfigColor(
            "previewBorderColor", "#FF5A5A5A", "Frame color (ARGB)");
        public static final ConfigInteger BORDER_WIDTH = new ConfigInteger(
            "previewBorderWidth", 1, 1, 8, "Frame thickness in GUI pixels");
        public static final ConfigColor BACKGROUND_COLOR = new ConfigColor(
            "previewBackgroundColor", "#E0101010", "Color behind the picture, visible through transparent map pixels and around the side panel (ARGB)");
        public static final ConfigInteger PANEL_PADDING = new ConfigInteger(
            "panelPadding", 4, 0, 32, "Padding in GUI pixels between the side panel's frame and the picture");
        public static final ConfigOptionList LABEL = new ConfigOptionList(
            "previewLabel", PreviewOptions.Label.MAP_ID, "Text under the picture: nothing, the map ID, or the map ID plus where the picture came from");
        public static final ConfigColor LABEL_COLOR = new ConfigColor(
            "previewLabelColor", "#FFAAAAAA", "Label text color (ARGB)");
        public static final ConfigBoolean HIDE_MAP_MARKERS = new ConfigBoolean(
            "hideMapMarkers", true, "Hide player arrows, banners and other markers on the preview");
        public static final ConfigBoolean REMOTE_FALLBACK = new ConfigBoolean(
            "remoteFallback", true,
            "When the server has not sent the map's pixels (usual for Auction House listings), load the picture from the DonutMaparts wall instead");
        public static final ConfigBoolean REMOTE_ONLY_ON_DONUT = new ConfigBoolean(
            "remoteOnlyOnDonut", true, "Only use the wall fallback while connected to DonutSMP, since map IDs from other servers would match the wrong art");
        public static final ConfigBoolean PREFETCH_VISIBLE = new ConfigBoolean(
            "prefetchVisible", false, "Load wall pictures for every map in the open container up front, so hovering is instant. Uses more requests");
        public static final ConfigBoolean SHOW_PLACEHOLDER = new ConfigBoolean(
            "showPlaceholder", true, "Show a 'no preview' box when no picture is available, instead of nothing");

        public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            PREVIEW_ENABLED, PREVIEW_MODE, PREVIEW_TRIGGER, PREVIEW_SCOPE,
            TOOLTIP_SIZE, PANEL_SIZE, PANEL_POSITION, PANEL_MARGIN,
            SHOW_BORDER, BORDER_COLOR, BORDER_WIDTH, BACKGROUND_COLOR, PANEL_PADDING,
            LABEL, LABEL_COLOR, HIDE_MAP_MARKERS,
            REMOTE_FALLBACK, REMOTE_ONLY_ON_DONUT, PREFETCH_VISIBLE, SHOW_PLACEHOLDER);

        public static final List<IConfigBase> GUI_OPTIONS = OPTIONS;
    }

    public static void loadFromFile() {
        // NOTE: FileUtils.getConfigDirectory() returns java.nio.file.Path in malilib
        // 0.27.16, not java.io.File as older malilib versions did. The File-based
        // fi.dy.masa.malilib.util.JsonUtils is deprecated in this version too, so this
        // uses the Path-based replacement, fi.dy.masa.malilib.util.data.json.JsonUtils.
        Path configFile = FileUtils.getConfigDirectory().resolve(CONFIG_FILE_NAME);
        if (Files.isRegularFile(configFile) && Files.isReadable(configFile)) {
            try {
                JsonElement element = JsonUtils.parseJsonFile(configFile);
                if (element != null && element.isJsonObject()) {
                    JsonObject root = element.getAsJsonObject();
                    ConfigUtils.readConfigBase(root, "General", General.OPTIONS);
                    ConfigUtils.readConfigBase(root, "Tracking", Tracking.OPTIONS);
                    ConfigUtils.readConfigBase(root, "Preview", Preview.OPTIONS);
                }
            }
            catch (RuntimeException e) {
                DonutMapartsMod.LOGGER.warn("Failed to parse config file {}, using defaults", configFile, e);
            }
        }
    }

    public static void saveToFile() {
        Path dir = FileUtils.getConfigDirectory();
        try {
            Files.createDirectories(dir);
        }
        catch (IOException e) {
            DonutMapartsMod.LOGGER.warn("Failed to create config directory {}, config not saved", dir, e);
            return;
        }
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "General", General.OPTIONS);
        ConfigUtils.writeConfigBase(root, "Tracking", Tracking.OPTIONS);
        ConfigUtils.writeConfigBase(root, "Preview", Preview.OPTIONS);
        JsonUtils.writeJsonToFile(root, dir.resolve(CONFIG_FILE_NAME));
    }

    @Override
    public void load() { loadFromFile(); }

    @Override
    public void save() { saveToFile(); }
}
