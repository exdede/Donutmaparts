package dev.exdede.donutmaparts.session;

import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.execute(() -> flush(mc, count));
    }

    private static void flush(MinecraftClient mc, int count) {
        pending += count;
        long now = System.currentTimeMillis();
        if (now - lastShownAt < THROTTLE_MILLIS) return;
        int n = pending;
        pending = 0;
        lastShownAt = now;
        Text title = Text.literal("DonutMaparts");
        Text body = Text.literal(n == 1
            ? "Catalogued 1 new mapart"
            : "Catalogued " + n + " new maparts");
        SystemToast.show(mc.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION, title, body);
    }
}
