package dev.exdede.donutmaparts.debug;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;

/**
 * Tagged console logging. Every line is dead silent unless the single
 * debugMode flag is on. Tags are kept so debug output stays readable.
 */
public final class DebugLog {
    private DebugLog() {}

    private static void log(String tag, String msg) {
        if (Configs.General.DEBUG_MODE.getBooleanValue()) {
            DonutMapartsMod.LOGGER.info("[{}] {}", tag, msg);
        }
    }

    public static void capture(String msg) { log("Capture", msg); }
    public static void render(String msg) { log("Render", msg); }
    public static void hash(String msg) { log("Hash", msg); }
    public static void cache(String msg) { log("Cache", msg); }
    public static void queue(String msg) { log("Queue", msg); }
    public static void http(String msg) { log("HTTP", msg); }
    public static void security(String msg) { log("Security", msg); }
    public static void tracking(String msg) { log("Tracking", msg); }
}
