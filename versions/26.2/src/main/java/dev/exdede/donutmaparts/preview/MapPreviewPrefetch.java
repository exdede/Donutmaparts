package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.inventory.Slot;

/**
 * Optional warm-up: asks for the wall picture of every map in the open
 * container each tick, so hovering one is instant. The cache's in-flight cap
 * does the pacing (a few requests at a time, the rest on later ticks) and
 * remembers misses, so a page of maps costs one lookup each per session.
 */
public final class MapPreviewPrefetch {
    private MapPreviewPrefetch() {}

    public static void tick(Minecraft mc) {
        try {
            if (!Configs.Preview.PREFETCH_VISIBLE.getBooleanValue()) return;
            if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
            if (!MapPreviewGate.active(mc) || !MapPreviewGate.remoteAllowed(mc)) return;
            for (Slot slot : screen.getMenu().slots) {
                MapId mapId = slot.getItem().get(DataComponents.MAP_ID);
                if (mapId == null) continue;
                if (mc.level != null && MapItem.getSavedData(mapId, mc.level) != null) continue;
                MapPreviewTextures.INSTANCE.request(mapId.id());
            }
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in map preview prefetch", t);
        }
    }
}
