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

    /**
     * The mod being disabled is a distinct dead end from not being on
     * DonutSMP: onJoin() returns before the handshake, so no token ever
     * exists, and rejoining changes nothing until the option is flipped.
     * Handshaking anyway would mean sending UUID/IGN/hwid to the backend
     * from a mod the player explicitly turned off, so the fix is to say so
     * rather than to quietly phone home.
     */
    public static void modDisabled(MinecraftClient mc) {
        if (mc == null) return;
        SystemToast.show(
            mc.getToastManager(),
            new SystemToast.Type(5000L),
            Text.literal("Mod disabled"),
            Text.literal("Enable the mod in settings, then rejoin to link"));
    }

    /**
     * On DonutSMP with the mod enabled, but the handshake has not landed
     * (still in flight, or it failed and left the session inactive). Unlike
     * the two above this one usually resolves on its own within a second or
     * two of joining, so it asks the player to retry rather than to change
     * anything.
     */
    public static void notReady(MinecraftClient mc) {
        if (mc == null) return;
        SystemToast.show(
            mc.getToastManager(),
            new SystemToast.Type(5000L),
            Text.literal("Not ready yet"),
            Text.literal("Still connecting to the backend, try again shortly"));
    }
}
