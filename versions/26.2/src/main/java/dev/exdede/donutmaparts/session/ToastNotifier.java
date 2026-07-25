package dev.exdede.donutmaparts.session;

import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * Occasional, throttled "it is working" toast. Aggregates upload counts and
 * shows at most one toast per THROTTLE_MILLIS, so the player gets reassurance
 * without being spammed. Fully silent when the toasts config is off.
 */
public final class ToastNotifier {
    private static final long THROTTLE_MILLIS = 60_000L;
    private static long lastShownAt;
    private static int pending;

    private ToastNotifier() {}

    /**
     * Records newly uploaded maparts and shows a toast if the throttle window
     * has elapsed. Safe to call from the HTTP callback thread; the toast itself
     * is shown on the client thread.
     */
    public static void recordUploads(int count) {
        if (count <= 0) return;
        if (!Configs.General.TOASTS.getBooleanValue()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> flush(mc, count));
    }

    private static void flush(Minecraft mc, int count) {
        pending += count;
        long now = System.currentTimeMillis();
        if (now - lastShownAt < THROTTLE_MILLIS) return;
        int n = pending;
        pending = 0;
        lastShownAt = now;
        Component title = Component.literal("DonutMaparts");
        Component body = Component.literal(n == 1
            ? "Catalogued 1 new mapart"
            : "Catalogued " + n + " new maparts");
        SystemToast.addOrUpdate(mc.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, body);
    }
}
