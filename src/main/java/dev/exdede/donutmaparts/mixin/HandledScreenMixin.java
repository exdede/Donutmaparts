package dev.exdede.donutmaparts.mixin;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.capture.MapCaptureTracker;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.queue.CaptureState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development-only slot overlay, active only with debugMode on.
 * Colors per state: gray unprocessed, yellow queued/uploading,
 * green uploaded new, blue duplicate, red failed, purple banned/deleted.
 */
@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    // Yarn 1.21.11: HandledScreen.drawSlot(DrawContext, Slot, int mouseX, int mouseY),
    // not (DrawContext, Slot) as originally assumed. Confirmed via bytecode: drawSlots
    // forwards its own mouseX/mouseY params straight through to drawSlot.
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void donutmaparts$colorSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        try {
            if (!Configs.General.DEBUG_MODE.getBooleanValue()) return;
            if (MapCaptureTracker.INSTANCE == null) return;
            ItemStack stack = slot.getStack();
            MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
            if (mapId == null) return;
            CaptureState state = MapCaptureTracker.INSTANCE.displayStateFor(mapId.id());
            int color = switch (state == null ? CaptureState.DISCOVERED : state) {
                case DISCOVERED, RENDERING -> 0x60808080; // gray
                case READY, QUEUED, UPLOADING, RETRY -> 0x60FFFF00; // yellow
                case UPLOADED -> 0x6000FF00; // green
                case DUPLICATE -> 0x600080FF; // blue
                case FAILED -> 0x60FF0000; // red
                case BANNED -> 0x60A020F0; // purple
            };
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in drawSlot mixin handler", t);
        }
    }
}
