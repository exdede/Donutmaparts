package dev.exdede.donutmaparts;

import dev.exdede.donutmaparts.cache.SentHashCache;
import dev.exdede.donutmaparts.capture.MapCaptureTracker;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.config.gui.GuiConfig;
import dev.exdede.donutmaparts.debug.DebugLog;
import dev.exdede.donutmaparts.queue.FailedQueueStore;
import dev.exdede.donutmaparts.queue.MapCapture;
import dev.exdede.donutmaparts.queue.UploadQueue;
import dev.exdede.donutmaparts.session.ToastNotifier;
import dev.exdede.donutmaparts.session.UploadSession;
import dev.exdede.donutmaparts.tracking.MapTracker;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.List;

public class DonutMapartsMod implements ClientModInitializer {
    public static final String MOD_ID = "donutmaparts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private FailedQueueStore failedStore;
    private int tickCounter;
    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("DonutMaparts initializing");
        InitializationHandler.getInstance().registerInitializationHandler(
            () -> ConfigManager.getInstance().registerConfigHandler(MOD_ID, new Configs()));

        // Unbound keybind (GLFW_KEY_UNKNOWN = -1) shown under a "DonutMaparts"
        // category in Options > Controls > Key Binds. The category self-registers
        // via KeyMapping.Category.create, so create it exactly once here.
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.donutmaparts.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));

        Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        UploadQueue queue = new UploadQueue(System::currentTimeMillis);
        SentHashCache cache = new SentHashCache(dataDir.resolve("sent_hashes.txt"));
        failedStore = new FailedQueueStore(dataDir.resolve("failed_queue.json"));
        MapCaptureTracker.INSTANCE = new MapCaptureTracker(queue, cache);
        MapTracker.INSTANCE = new MapTracker();

        // Failed items from previous launches go straight back into the queue.
        List<MapCapture> carried = failedStore.load();
        for (MapCapture c : carried) queue.enqueue(c);
        if (!carried.isEmpty()) {
            LOGGER.info("requeued {} failed uploads from previous session", carried.size());
            failedStore.save(List.of());
        }

        ClientPlayConnectionEvents.JOIN.register(
            (handler, sender, client) -> UploadSession.INSTANCE.onJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            persistFailed(queue);
            UploadSession.INSTANCE.onLeave();
            if (MapCaptureTracker.INSTANCE != null) MapCaptureTracker.INSTANCE.reset();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                client.gui.setScreen(new GuiConfig());
            }
            if (MapTracker.INSTANCE != null) {
                MapTracker.INSTANCE.tickScreen(client);
            }
            if (MapCaptureTracker.INSTANCE != null) {
                MapCaptureTracker.INSTANCE.tick(System.currentTimeMillis(),
                    Configs.General.SETTLE_DELAY_MILLIS.getIntegerValue());
            }
            if (++tickCounter % 20 != 0) return;
            flushIfDue(queue);
        });

        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (!Configs.General.DEBUG_MODE.getBooleanValue()) return;
            if (MapCaptureTracker.INSTANCE == null) return;
            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null) return;
            var state = MapCaptureTracker.INSTANCE.displayStateFor(mapId.id());
            lines.add(Component.literal("Map #" + mapId.id() + " " + (state == null ? "untracked" : state)));
            Integer pos = MapCaptureTracker.INSTANCE.queue().queuePositionOf(mapId.id());
            if (pos != null) lines.add(Component.literal("Queue position: " + pos));
        });
    }

    private void flushIfDue(UploadQueue queue) {
        UploadSession session = UploadSession.INSTANCE;
        if (!session.isActive() || session.clientOrNull() == null) return;
        long intervalMillis = Configs.General.BATCH_INTERVAL_SECONDS.getIntegerValue() * 1000L;
        int minItems = Configs.General.BATCH_MIN_ITEMS.getIntegerValue();
        if (!queue.shouldFlush(minItems, intervalMillis)) return;
        List<MapCapture> batch = queue.takeBatch(100);
        if (batch.isEmpty()) return;
        DebugLog.http("uploading batch of " + batch.size());
        session.clientOrNull().uploadBatch(session.tokenOrNull(), batch, session.brandHintOrNull())
            .thenAccept(results -> {
                queue.onBatchResult(batch, true);
                int uploaded = 0;
                for (var r : results) {
                    switch (r.status()) {
                        case "uploaded", "duplicate", "banned_content" -> {
                            MapCaptureTracker.INSTANCE.sentCache()
                                .add(hashFor(batch, r.mapId()));
                            if (r.status().equals("uploaded")) {
                                uploaded++;
                            } else if (r.status().equals("duplicate")) {
                                queue.setDisplayState(r.mapId(), dev.exdede.donutmaparts.queue.CaptureState.DUPLICATE);
                            } else if (r.status().equals("banned_content")) {
                                queue.setDisplayState(r.mapId(), dev.exdede.donutmaparts.queue.CaptureState.BANNED);
                            }
                        }
                        case "rejected" -> DebugLog.http("map " + r.mapId() + " rejected");
                        default -> DebugLog.http("map " + r.mapId() + " unknown status " + r.status());
                    }
                }
                ToastNotifier.recordUploads(uploaded);
                persistFailed(queue);
            })
            .exceptionally(e -> {
                DebugLog.http("batch failed: " + e.getMessage());
                queue.onBatchResult(batch, false);
                persistFailed(queue);
                return null;
            });
    }

    private static String hashFor(List<MapCapture> batch, int mapId) {
        for (MapCapture c : batch) {
            if (c.mapId() == mapId) return c.hashHex();
        }
        return "";
    }

    private synchronized void persistFailed(UploadQueue queue) {
        List<MapCapture> newlyFailed = queue.drainFailed();
        if (newlyFailed.isEmpty()) return;
        List<MapCapture> all = new java.util.ArrayList<>(failedStore.load());
        all.addAll(newlyFailed);
        failedStore.save(all);
        DebugLog.queue("persisted " + newlyFailed.size() + " failed uploads for next launch");
    }
}
