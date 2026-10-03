package dev.exdede.donutmaparts.preview;

import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.tooltip.TooltipData;

/** Marker the ItemStack mixin attaches to a filled map's tooltip; turned into a component on the Fabric callback. */
public record MapPreviewTooltipData(MapIdComponent mapId) implements TooltipData {}
