package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.preview.PreviewText.Source;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreviewTextTest {
    @Test
    void auctionHouseTitles() {
        assertTrue(PreviewText.isAuctionHouseTitle("Auction House (Page 1)"));
        assertTrue(PreviewText.isAuctionHouseTitle("§6AUCTION"));
        assertTrue(PreviewText.isAuctionHouseTitle("AH (Page 2)"));
        assertTrue(PreviewText.isAuctionHouseTitle("ah"));
        assertFalse(PreviewText.isAuctionHouseTitle("Chest"));
        assertFalse(PreviewText.isAuctionHouseTitle("Ahoy"));
        assertFalse(PreviewText.isAuctionHouseTitle("Orders (Page 1)"));
        assertFalse(PreviewText.isAuctionHouseTitle(null));
    }

    @Test
    void labels() {
        assertNull(PreviewText.label(PreviewOptions.Label.NONE, 5, Source.LOCAL));
        assertEquals("Map #5", PreviewText.label(PreviewOptions.Label.MAP_ID, 5, Source.WALL));
        assertEquals("Map #5 · wall", PreviewText.label(PreviewOptions.Label.MAP_ID_AND_SOURCE, 5, Source.WALL));
        assertEquals("Map #5 · server", PreviewText.label(PreviewOptions.Label.MAP_ID_AND_SOURCE, 5, Source.LOCAL));
    }

    @Test
    void placeholdersOnlyWithoutAPicture() {
        assertNull(PreviewText.placeholder(Source.LOCAL));
        assertNull(PreviewText.placeholder(Source.WALL));
        assertEquals("Loading...", PreviewText.placeholder(Source.LOADING));
        assertEquals("No preview", PreviewText.placeholder(Source.NONE));
    }

    @Test
    void optionListsCycleAndParse() {
        assertEquals(PreviewOptions.Mode.PANEL, PreviewOptions.Mode.TOOLTIP.cycle(true));
        assertEquals(PreviewOptions.Mode.BOTH, PreviewOptions.Mode.TOOLTIP.cycle(false));
        assertEquals(PreviewOptions.PanelPosition.CURSOR, PreviewOptions.PanelPosition.TOP_LEFT.fromString("cursor"));
        assertEquals(PreviewOptions.Trigger.ALWAYS, PreviewOptions.Trigger.SHIFT.fromString("bogus"));
        assertTrue(PreviewOptions.Mode.BOTH.tooltip() && PreviewOptions.Mode.BOTH.panel());
        assertFalse(PreviewOptions.Mode.PANEL.tooltip());
        assertFalse(PreviewOptions.Mode.TOOLTIP.panel());
    }
}
