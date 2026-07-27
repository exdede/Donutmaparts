package dev.exdede.donutmaparts.tracking;

import org.junit.jupiter.api.Test;
import static dev.exdede.donutmaparts.tracking.TrackingScope.ContainerKind;
import static org.junit.jupiter.api.Assertions.*;

class TrackingScopeTest {
    @Test
    void shulkerBoxContainerKindNeedsNoTitleCheck() {
        assertEquals(TrackingScope.SHULKER_BOX,
            TrackingScope.classify(ContainerKind.SHULKER_BOX, "anything at all"));
        assertEquals(TrackingScope.SHULKER_BOX,
            TrackingScope.classify(ContainerKind.SHULKER_BOX, null));
    }

    @Test
    void genericContainerWithAuctionHouseTitleIsAuctionHouse() {
        assertEquals(TrackingScope.AUCTION_HOUSE,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "Auction House"));
    }

    @Test
    void auctionHouseTitleMatchIsCaseInsensitive() {
        assertEquals(TrackingScope.AUCTION_HOUSE,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "AUCTION HOUSE"));
    }

    @Test
    void auctionHouseTitleMatchToleratesALeadingFormattingPrefix() {
        // Realistic DonutSMP-style title: literal section-sign codes baked
        // into the text component's plain string content (screen.getTitle()
        // .getString() does not strip these), same as what MapTracker
        // already passes straight through today.
        assertEquals(TrackingScope.AUCTION_HOUSE,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "§6§lAuction House"));
    }

    @Test
    void genericContainerWithEnderChestTitleIsEnderChest() {
        assertEquals(TrackingScope.ENDER_CHEST,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "Ender Chest"));
    }

    @Test
    void enderChestTitleMatchIsCaseInsensitive() {
        assertEquals(TrackingScope.ENDER_CHEST,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "ender chest"));
    }

    @Test
    void genericContainerWithPlainOrUnrecognisedTitleIsChest() {
        assertEquals(TrackingScope.CHEST,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "Chest"));
        assertEquals(TrackingScope.CHEST,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "Large Chest"));
    }

    @Test
    void genericContainerWithEmptyOrNullTitleIsChest() {
        assertEquals(TrackingScope.CHEST,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, ""));
        assertEquals(TrackingScope.CHEST,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, null));
    }

    @Test
    void genericContainerWithSomeOtherCustomGuiTitleDefaultsToChest() {
        // Documented limitation: a custom server GUI on the same generic
        // handler type with neither recognised phrase in its title reads as
        // CHEST rather than as its own category.
        assertEquals(TrackingScope.CHEST,
            TrackingScope.classify(ContainerKind.GENERIC_CONTAINER, "Kit Selector"));
    }

    @Test
    void otherContainerKindIsAlwaysOther() {
        assertEquals(TrackingScope.OTHER,
            TrackingScope.classify(ContainerKind.OTHER, "Furnace"));
        assertEquals(TrackingScope.OTHER,
            TrackingScope.classify(ContainerKind.OTHER, "Auction House"));
    }

    @Test
    void nullContainerKindIsOther() {
        assertEquals(TrackingScope.OTHER, TrackingScope.classify(null, "Chest"));
    }
}
