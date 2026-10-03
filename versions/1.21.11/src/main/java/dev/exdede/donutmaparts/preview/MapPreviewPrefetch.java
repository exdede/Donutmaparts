package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.screen.slot.Slot;

/**
 * Optional warm-up: asks for the wall picture of every map in the open
 * container each tick, so hovering one is instant. The cache's in-flight cap
 * does the pacing (a few requests at a time, the rest on later ticks) and
 * remembers misses, so a page of maps costs one lookup each per session.
 */
public final class MapPreviewPrefetch {
    private MapPreviewPrefetch() {}

    public static void tick(MinecraftClient mc) {
        try {
            if (!Configs.Preview.PREFETCH_VISIBLE.getBooleanValue()) return;
            if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
            if (!MapPreviewGate.active(mc) || !MapPreviewGate.remoteAllowed(mc)) return;
            for (Slot slot : screen.getScreenHandler().slots) {
                MapIdComponent mapId = slot.getStack().get(DataComponentTypes.MAP_ID);
                if (mapId == null) continue;
                if (mc.world != null && FilledMapItem.getMapState(mapId, mc.world) != null) continue;
                MapPreviewTextures.INSTANCE.request(mapId.id());
            }
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in map preview prefetch", t);
        }
    }
}
