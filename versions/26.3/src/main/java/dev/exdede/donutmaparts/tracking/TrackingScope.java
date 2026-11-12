package dev.exdede.donutmaparts.tracking;

import java.util.Locale;

/**
 * Classifies the currently open GUI into one of five tracking-scope
 * categories, from a coarse container-type signal plus the screen's plain
 * title text. Deliberately free of any Minecraft or malilib import so the
 * unit tests can exercise it without a client, matching the rest of this
 * mod's pure core (see TrackedIds, ScreenAlertLog).
 *
 * A plain chest and an ender chest are NOT distinguishable by container
 * type alone: verified against the mapped Minecraft jar, both open through
 * the exact same generic 9x3 container handler (GenericContainerScreenHandler
 * / ChestMenu on ScreenHandlerType.GENERIC_9X3 / MenuType.GENERIC_9x3) --
 * there is no dedicated "ender chest" handler type in vanilla, unlike a
 * shulker box which does get its own dedicated type. A server-driven custom
 * GUI such as DonutSMP's Auction House rides the same generic container type
 * too, for the same reason (it is how a vanilla-protocol server shows a
 * custom UI to an unmodified client). So CHEST, ENDER_CHEST and
 * AUCTION_HOUSE all collapse to the same coarse container signal and can
 * only be told apart by the screen's title text.
 */
public enum TrackingScope {
    CHEST,
    ENDER_CHEST,
    SHULKER_BOX,
    AUCTION_HOUSE,
    OTHER;

    /**
     * Coarse, cheap-to-compute signal the Minecraft-side caller provides.
     * Only two real signals exist below "some other screen entirely": the
     * dedicated shulker box handler type, and the family of generic
     * chest-shaped handler types that a plain chest, an ender chest, and a
     * custom server GUI all share.
     */
    public enum ContainerKind {
        /** The vanilla shulker box screen handler / menu type specifically. */
        SHULKER_BOX,
        /** Any of the generic chest-shaped handler types (9x1..9x6). */
        GENERIC_CONTAINER,
        /** Anything else: furnace, anvil, crafting table, horse, etc. */
        OTHER
    }

    /**
     * title is whatever plain-text string this codebase's existing title
     * reading already produces (MapTracker and the debug slot overlay both
     * use Text#getString()) -- pass that exact value, formatting codes and
     * all, since this codebase has no convention for stripping them and
     * matching here is a case-insensitive substring check that tolerates a
     * leading formatting prefix fine.
     */
    public static TrackingScope classify(ContainerKind kind, String title) {
        if (kind == null) return OTHER;
        if (kind == ContainerKind.SHULKER_BOX) return SHULKER_BOX;
        if (kind != ContainerKind.GENERIC_CONTAINER) return OTHER;

        String normalized = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (normalized.contains("auction house")) return AUCTION_HOUSE;
        // Not an exhaustive "is this really a plain chest" detector on
        // purpose: any generic container that isn't recognised as the
        // auction house or the ender chest by title falls into CHEST by
        // default. A custom server GUI riding the same generic handler type
        // with neither phrase in its title reads as CHEST -- a documented
        // limitation, not a defect.
        if (normalized.contains("ender chest")) return ENDER_CHEST;
        return CHEST;
    }
}
