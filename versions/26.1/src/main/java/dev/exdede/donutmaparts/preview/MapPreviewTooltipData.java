package dev.exdede.donutmaparts.preview;

import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Marker the ItemStack mixin attaches to a filled map's tooltip; turned into a component on the Fabric callback. */
public record MapPreviewTooltipData(MapId mapId) implements TooltipComponent {}
