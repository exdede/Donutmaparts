package dev.exdede.donutmaparts.session;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import dev.exdede.donutmaparts.net.BackendClient;
import dev.exdede.donutmaparts.net.HwidProvider;
import dev.exdede.donutmaparts.server.ServerDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Tracks whether we are on DonutSMP with a valid backend session.
 * Handshake and status run once per server join, off-thread.
 */
public final class UploadSession {
    public static final UploadSession INSTANCE = new UploadSession();

    private volatile boolean active;
    private volatile boolean restricted;
    private volatile String apiToken;
    private volatile BackendClient client;

    private UploadSession() {}

    public boolean isActive() { return active && apiToken != null; }
    public boolean isRestricted() { return restricted; }
    public String tokenOrNull() { return apiToken; }
    public BackendClient clientOrNull() { return client; }

    public String brandHintOrNull() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.getNetworkHandler() != null ? mc.getNetworkHandler().getBrand() : null;
    }

    public String modVersion() {
        return FabricLoader.getInstance().getModContainer(DonutMapartsMod.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    public void onJoin(MinecraftClient mc) {
        active = false;
        restricted = false;
        apiToken = null;
        if (!Configs.General.ENABLED.getBooleanValue()) return;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || !ServerDetector.isDonutAddress(info.address)) {
            DebugLog.security("not DonutSMP, capture disabled for this session");
            return;
        }
        UUID uuid = mc.getSession().getUuidOrNull();
        String ign = mc.getSession().getUsername();
        if (uuid == null) return;
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(DonutMapartsMod.MOD_ID);
        String hwid = HwidProvider.hwidHash(configDir);
        BackendClient bc = new BackendClient(Configs.General.BACKEND_URL);
        this.client = bc;
        String version = modVersion();
        bc.handshake(uuid.toString(), ign, hwid, version)
            .thenCompose(token -> {
                this.apiToken = token;
                DebugLog.http("handshake ok");
                return bc.status(token, version);
            })
            .thenAccept(isRestricted -> {
                this.restricted = isRestricted;
                this.active = !isRestricted;
                if (isRestricted) {
                    DebugLog.security("status restricted, uploads paused");
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.sendMessage(
                                Text.literal("Map uploading is currently disabled"), false);
                        }
                    });
                } else {
                    DebugLog.http("status ok, session active");
                }
            })
            .exceptionally(e -> {
                DebugLog.http("handshake/status failed: " + e.getMessage());
                // Leave session inactive, uploads simply do not happen.
                return null;
            });
    }

    public void onLeave() {
        active = false;
        restricted = false;
        apiToken = null;
        client = null;
    }
}
