package dev.exdede.donutmaparts.mixin;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.capture.MapCaptureTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleMapItemData", at = @At("TAIL"))
    private void donutmaparts$onMapItemData(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        try {
            if (MapCaptureTracker.INSTANCE == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            MapItemSavedData state = mc.level.getMapData(packet.mapId());
            if (state == null) return;
            MapCaptureTracker.INSTANCE.onMapUpdate(packet.mapId().id(), state.colors.clone());
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in onMapUpdate mixin handler", t);
        }
    }
}
