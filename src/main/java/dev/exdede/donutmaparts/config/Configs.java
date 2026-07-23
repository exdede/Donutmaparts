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
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import dev.exdede.donutmaparts.DonutMapartsMod;

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
        JsonUtils.writeJsonToFile(root, dir.resolve(CONFIG_FILE_NAME));
    }

    @Override
    public void load() { loadFromFile(); }

    @Override
    public void save() { saveToFile(); }
}
