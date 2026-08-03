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
            mc.getToastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Account linked"),
            Component.literal("Your DonutMaparts account is now linked"));
    }

    public static void linkFailed(Minecraft mc) {
        if (mc == null) return;
        SystemToast.addOrUpdate(
            mc.getToastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Link failed"),
            Component.literal("Invalid or expired code, try again"));
    }

    /** Shown instead of opening a blank entry dialog when there is no active session yet. */
    public static void noSession(Minecraft mc) {
        if (mc == null) return;
        SystemToast.addOrUpdate(
            mc.getToastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Not connected"),
            Component.literal("Join DonutSMP first to link your account"));
    }

    /**
     * The mod being disabled is a distinct dead end from not being on
     * DonutSMP: onJoin() returns before the handshake, so no token ever
     * exists, and rejoining changes nothing until the option is flipped.
     * Handshaking anyway would mean sending UUID/IGN/hwid to the backend
     * from a mod the player explicitly turned off, so the fix is to say so
     * rather than to quietly phone home.
     */
    public static void modDisabled(Minecraft mc) {
        if (mc == null) return;
        SystemToast.addOrUpdate(
            mc.getToastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Mod disabled"),
            Component.literal("Enable the mod in settings, then rejoin to link"));
    }

    /**
     * On DonutSMP with the mod enabled, but the handshake has not landed
     * (still in flight, or it failed and left the session inactive). Unlike
     * the two above this one usually resolves on its own within a second or
     * two of joining, so it asks the player to retry rather than to change
     * anything.
     */
    public static void notReady(Minecraft mc) {
        if (mc == null) return;
        SystemToast.addOrUpdate(
            mc.getToastManager(),
            new SystemToast.SystemToastId(5000L),
            Component.literal("Not ready yet"),
            Component.literal("Still connecting to the backend, try again shortly"));
    }
}
