package dev.exdede.donutmaparts.session;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

/**
 * Toasts for the account-link flow. Deliberately never says *why* a link
 * attempt failed -- an expired code, an already-used code and a malformed
 * code all resolve to the same false from BackendClient.submitLinkCode, so
 * showing a specific reason here would imply a distinction the backend
 * itself refuses to make (anti-enumeration posture, same as the site's
 * login flow).
 */
public final class LinkNotifier {
    private LinkNotifier() {}

    public static void linked(MinecraftClient mc) {
        if (mc == null) return;
        SystemToast.show(
            mc.getToastManager(),
            new SystemToast.Type(5000L),
            Text.literal("Account linked"),
            Text.literal("Your DonutMaparts account is now linked"));
    }

    public static void linkFailed(MinecraftClient mc) {
        if (mc == null) return;
        SystemToast.show(
            mc.getToastManager(),
            new SystemToast.Type(5000L),
            Text.literal("Link failed"),
            Text.literal("Invalid or expired code, try again"));
    }

    /** Shown instead of opening a blank entry dialog when there is no active session yet. */
    public static void noSession(MinecraftClient mc) {
        if (mc == null) return;
        SystemToast.show(
            mc.getToastManager(),
            new SystemToast.Type(5000L),
            Text.literal("Not connected"),
            Text.literal("Join DonutSMP first to link your account"));
    }
}
