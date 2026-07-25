package dev.exdede.donutmaparts.tracking;

import dev.exdede.donutmaparts.config.AlertSound;
import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

/**
 * Sound and toast for a tracked map sighting. Mirrors ToastNotifier's shape but
 * without its throttle: these alerts are the whole point of the feature, and the
 * per screen dedupe in ScreenAlertLog already bounds how many can fire.
 */
public final class TrackingNotifier {
    private TrackingNotifier() {}

    public static void alert(MinecraftClient mc, int mapId, String screenTitle, int slotIndex) {
        if (mc == null) return;

        if (Configs.Tracking.ALERT_SOUND_ENABLED.getBooleanValue()) {
            AlertSound sound = (AlertSound) Configs.Tracking.ALERT_SOUND.getOptionListValue();
            mc.getSoundManager().play(PositionedSoundInstance.ui(sound.soundEvent(), 1.0F));
        }

        if (Configs.Tracking.TRACKING_TOASTS.getBooleanValue()) {
            // A fresh Type per alert on purpose: SystemToast replaces any queued
            // toast sharing a Type, so a shared constant would mean only the last
            // hit is visible when a chest holds several tracked maps.
            SystemToast.show(
                mc.getToastManager(),
                new SystemToast.Type(5000L),
                Text.literal("Tracked mapart found"),
                Text.literal("Map #" + mapId + " in " + screenTitle + ", slot " + slotIndex));
        }
    }
}
