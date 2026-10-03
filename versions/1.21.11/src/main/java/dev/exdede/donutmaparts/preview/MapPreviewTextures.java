package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import java.io.IOException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Owns the textures for pictures loaded from the DonutMaparts wall. Fetch and
 * PNG decode run off thread; registering the texture, and every cache call,
 * happen on the client thread.
 */
public final class MapPreviewTextures {
    public static final MapPreviewTextures INSTANCE = new MapPreviewTextures();

    public record PreviewTexture(Identifier id, int width, int height) {}

    private static final long MISSING_RETRY_MILLIS = 30L * 60L * 1000L;
    private static final long FAILED_RETRY_MILLIS = 15L * 1000L;

    private final RemotePreviewCache<PreviewTexture> cache =
        new RemotePreviewCache<>(128, 3, MISSING_RETRY_MILLIS, FAILED_RETRY_MILLIS, MapPreviewTextures::destroy);
    private RemotePreviewClient client;
    private int nextTextureIndex;

    private MapPreviewTextures() {}

    /** The loaded texture, or null. Never starts a fetch. */
    public PreviewTexture texture(int mapId) {
        return this.cache.value(mapId);
    }

    /** Status after starting a fetch if none is running or remembered for this map. */
    public RemotePreviewCache.Status request(int mapId) {
        long now = System.currentTimeMillis();
        if (this.cache.tryBegin(mapId, now)) startFetch(mapId, this.cache.generation());
        return this.cache.status(mapId, now);
    }

    public void clear() {
        this.cache.clear();
    }

    private void startFetch(int mapId, long generation) {
        if (this.client == null) this.client = new RemotePreviewClient(Configs.General.BACKEND_URL);
        MinecraftClient mc = MinecraftClient.getInstance();
        DebugLog.http("preview: fetching map " + mapId + " from the wall");
        this.client.fetchPng(mapId).whenComplete((png, error) -> {
            if (error != null) {
                DebugLog.http("preview: map " + mapId + " fetch failed: " + error.getMessage());
                mc.execute(() -> this.cache.failed(mapId, generation, System.currentTimeMillis()));
                return;
            }
            if (png.isEmpty()) {
                DebugLog.http("preview: map " + mapId + " is not on the wall");
                mc.execute(() -> this.cache.missing(mapId, generation, System.currentTimeMillis()));
                return;
            }
            NativeImage image;
            try {
                image = NativeImage.read(png.get());
            } catch (IOException | RuntimeException e) {
                DebugLog.http("preview: map " + mapId + " returned an unreadable image");
                mc.execute(() -> this.cache.missing(mapId, generation, System.currentTimeMillis()));
                return;
            }
            mc.execute(() -> register(mc, mapId, generation, image));
        });
    }

    private void register(MinecraftClient mc, int mapId, long generation, NativeImage image) {
        try {
            Identifier id = Identifier.of(DonutMapartsMod.MOD_ID, "map_preview/" + mapId + "_" + (this.nextTextureIndex++));
            int width = image.getWidth();
            int height = image.getHeight();
            mc.getTextureManager().registerTexture(id,
                new NativeImageBackedTexture(() -> "donutmaparts map preview " + mapId, image));
            this.cache.complete(mapId, generation, new PreviewTexture(id, width, height));
        } catch (RuntimeException e) {
            image.close();
            DonutMapartsMod.LOGGER.warn("Failed to register map preview texture for map {}", mapId, e);
            this.cache.failed(mapId, generation, System.currentTimeMillis());
        }
    }

    private static void destroy(PreviewTexture texture) {
        MinecraftClient.getInstance().getTextureManager().destroyTexture(texture.id());
    }
}
