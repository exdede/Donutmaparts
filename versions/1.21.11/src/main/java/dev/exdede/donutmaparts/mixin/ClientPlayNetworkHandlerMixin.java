package dev.exdede.donutmaparts.mixin;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.capture.MapCaptureTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onMapUpdate", at = @At("TAIL"))
    private void donutmaparts$onMapUpdate(MapUpdateS2CPacket packet, CallbackInfo ci) {
        try {
            if (MapCaptureTracker.INSTANCE == null) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;
            MapState state = mc.world.getMapState(packet.mapId());
            if (state == null) return;
            MapCaptureTracker.INSTANCE.onMapUpdate(packet.mapId().id(), state.colors.clone());
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in onMapUpdate mixin handler", t);
        }
    }
}
