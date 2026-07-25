package dev.exdede.donutmaparts.mixin;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.capture.MapCaptureTracker;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.queue.CaptureState;
import dev.exdede.donutmaparts.tracking.MapTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two overlays on map item slots.
 *
 * The debug overlay is development only, active only with debugMode on. Colors
 * per state: gray unprocessed, yellow queued/uploading, green uploaded new,
 * blue duplicate, red failed, purple banned/deleted.
 *
 * The tracking highlight is a user facing feature and is not tied to debugMode.
 * It pulses so a matched slot is findable in a double chest, and it draws after
 * the debug fill so the two never hide each other.
 */
@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {
    // Yarn 1.21.11: AbstractContainerScreen.drawSlot(GuiGraphicsExtractor, Slot, int mouseX, int mouseY),
    // not (GuiGraphicsExtractor, Slot) as originally assumed. Confirmed via bytecode: drawSlots
    // forwards its own mouseX/mouseY params straight through to drawSlot.
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void donutmaparts$colorSlot(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        try {
            if (!Configs.General.DEBUG_MODE.getBooleanValue() && !Configs.Tracking.TRACKING_ENABLED.getBooleanValue()) {
                return;
            }

            ItemStack stack = slot.getItem();
            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null) return;

            if (Configs.General.DEBUG_MODE.getBooleanValue() && MapCaptureTracker.INSTANCE != null) {
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
            }

            if (MapTracker.INSTANCE != null && MapTracker.INSTANCE.shouldHighlight(mapId.id())) {
                donutmaparts$drawTrackedHighlight(context, slot);
            }
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in drawSlot mixin handler", t);
        }
    }

    @Unique
    private static void donutmaparts$drawTrackedHighlight(GuiGraphicsExtractor context, Slot slot) {
        // One second sine, alpha 0.35 to 1.0. The motion is what makes the slot
        // findable; a static border reads too much like the vanilla hover box.
        double phase = (System.currentTimeMillis() % 1000L) / 1000.0;
        double wave = 0.5 + 0.5 * Math.sin(phase * 2.0 * Math.PI);
        int alpha = (int) ((0.35 + 0.65 * wave) * 255.0);
        int border = (alpha << 24) | 0x00FFAA00;
        int tint = ((alpha / 5) << 24) | 0x00FFAA00;

        int x = slot.x;
        int y = slot.y;
        context.fill(x - 1, y - 1, x + 17, y + 1, border);   // top
        context.fill(x - 1, y + 15, x + 17, y + 17, border); // bottom
        context.fill(x - 1, y + 1, x + 1, y + 15, border);   // left
        context.fill(x + 15, y + 1, x + 17, y + 15, border); // right
        context.fill(x, y, x + 16, y + 16, tint);            // interior
    }
}
