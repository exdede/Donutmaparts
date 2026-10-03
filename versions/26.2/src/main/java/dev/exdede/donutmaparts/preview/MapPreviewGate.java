package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.server.ServerDetector;
import dev.exdede.donutmaparts.tracking.MapTracker;
import dev.exdede.donutmaparts.tracking.TrackingScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Whether a preview should show right now: master toggle, trigger key and
 * screen scope. Like map tracking, previews work on any server; only the
 * wall fallback is tied to DonutSMP (see remoteAllowed).
 */
public final class MapPreviewGate {
    private MapPreviewGate() {}

    public static boolean active(Minecraft mc) {
        if (mc == null || !Configs.Preview.PREVIEW_ENABLED.getBooleanValue()) return false;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return false;
        return triggerHeld(mc) && scopeAllows(screen);
    }

    public static boolean remoteAllowed(Minecraft mc) {
        if (!Configs.Preview.REMOTE_FALLBACK.getBooleanValue()) return false;
        if (!Configs.Preview.REMOTE_ONLY_ON_DONUT.getBooleanValue()) return true;
        ServerData server = mc.getCurrentServer();
        return server != null && ServerDetector.isDonutAddress(server.ip);
    }

    private static boolean triggerHeld(Minecraft mc) {
        PreviewOptions.Trigger trigger = (PreviewOptions.Trigger) Configs.Preview.PREVIEW_TRIGGER.getOptionListValue();
        return switch (trigger) {
            case ALWAYS -> true;
            case SHIFT -> keyDown(mc, GLFW.GLFW_KEY_LEFT_SHIFT) || keyDown(mc, GLFW.GLFW_KEY_RIGHT_SHIFT);
            case CONTROL -> keyDown(mc, GLFW.GLFW_KEY_LEFT_CONTROL) || keyDown(mc, GLFW.GLFW_KEY_RIGHT_CONTROL);
            case ALT -> keyDown(mc, GLFW.GLFW_KEY_LEFT_ALT) || keyDown(mc, GLFW.GLFW_KEY_RIGHT_ALT);
        };
    }

    private static boolean keyDown(Minecraft mc, int key) {
        return InputConstants.isKeyDown(mc.getWindow(), key);
    }

    private static boolean scopeAllows(AbstractContainerScreen<?> screen) {
        PreviewOptions.Scope scope = (PreviewOptions.Scope) Configs.Preview.PREVIEW_SCOPE.getOptionListValue();
        if (scope == PreviewOptions.Scope.ALL_SCREENS) return true;
        TrackingScope.ContainerKind kind = MapTracker.classifyContainerKind(screen);
        if (kind == TrackingScope.ContainerKind.OTHER) return false;
        if (scope == PreviewOptions.Scope.CONTAINERS) return true;
        return PreviewText.isAuctionHouseTitle(screen.getTitle().getString());
    }
}
