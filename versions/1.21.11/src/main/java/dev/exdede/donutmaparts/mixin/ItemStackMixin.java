package dev.exdede.donutmaparts.mixin;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.preview.MapPreviewGate;
import dev.exdede.donutmaparts.preview.MapPreviewTooltipData;
import dev.exdede.donutmaparts.preview.PreviewOptions;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives a filled map tooltip data, which vanilla never does, so the tooltip
 * grows a picture the same way a bundle's tooltip grows its item grid. Only
 * fills in an empty result, so another mod's tooltip data always wins.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "getTooltipData", at = @At("RETURN"), cancellable = true)
    private void donutmaparts$mapPreview(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        try {
            if (cir.getReturnValue().isPresent()) return;
            PreviewOptions.Mode mode = (PreviewOptions.Mode) Configs.Preview.PREVIEW_MODE.getOptionListValue();
            if (!mode.tooltip()) return;
            MapIdComponent mapId = ((ItemStack) (Object) this).get(DataComponentTypes.MAP_ID);
            if (mapId == null) return;
            if (!MapPreviewGate.active(MinecraftClient.getInstance())) return;
            cir.setReturnValue(Optional.of(new MapPreviewTooltipData(mapId)));
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in map preview tooltip mixin", t);
        }
    }
}
