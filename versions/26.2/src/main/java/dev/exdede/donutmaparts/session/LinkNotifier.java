package dev.exdede.donutmaparts.session;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

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

    public static void linked(Minecraft mc) {
        if (mc == null) return;
        SystemToast.addOrUpdate(
            mc.gui.toastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Account linked"),
            Component.literal("Your DonutMaparts account is now linked"));
    }

    public static void linkFailed(Minecraft mc) {
        if (mc == null) return;
        SystemToast.addOrUpdate(
            mc.gui.toastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Link failed"),
            Component.literal("Invalid or expired code, try again"));
    }

    /** Shown instead of opening a blank entry dialog when there is no active session yet. */
    public static void noSession(Minecraft mc) {
        if (mc == null) return;
        SystemToast.addOrUpdate(
            mc.gui.toastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Not connected"),
            Component.literal("Join a server first to link your account"));
    }
}
