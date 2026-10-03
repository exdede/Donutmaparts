package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.preview.PreviewOptions.PanelPosition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelLayoutTest {
    // 480x270 screen, 176x166 GUI centered at (152, 52), 100x110 box, 6px margin.
    private PanelLayout.Point at(PanelPosition p, int mouseX, int mouseY) {
        return PanelLayout.place(p, 480, 270, 152, 52, 176, 166, 100, 110, mouseX, mouseY, 6);
    }

    @Test
    void rightOfGuiSitsNextToTheGui() {
        assertEquals(new PanelLayout.Point(334, 52), at(PanelPosition.RIGHT_OF_GUI, 0, 0));
    }

    @Test
    void leftOfGuiSitsNextToTheGui() {
        assertEquals(new PanelLayout.Point(46, 52), at(PanelPosition.LEFT_OF_GUI, 0, 0));
    }

    @Test
    void rightFlipsLeftWhenThereIsNoRoom() {
        // GUI pushed right so its right side has 20px left.
        PanelLayout.Point p = PanelLayout.place(PanelPosition.RIGHT_OF_GUI, 480, 270,
            284, 52, 176, 166, 100, 110, 0, 0, 6);
        assertEquals(178, p.x());
    }

    @Test
    void leftFlipsRightWhenThereIsNoRoom() {
        PanelLayout.Point p = PanelLayout.place(PanelPosition.LEFT_OF_GUI, 480, 270,
            20, 52, 176, 166, 100, 110, 0, 0, 6);
        assertEquals(202, p.x());
    }

    @Test
    void noRoomEitherSideClampsOntoTheScreen() {
        PanelLayout.Point p = PanelLayout.place(PanelPosition.RIGHT_OF_GUI, 200, 200,
            10, 10, 180, 180, 100, 100, 0, 0, 6);
        assertEquals(100, p.x());
        assertTrue(p.y() >= 0 && p.y() + 100 <= 200);
    }

    @Test
    void cornersRespectTheMargin() {
        assertEquals(new PanelLayout.Point(6, 6), at(PanelPosition.TOP_LEFT, 0, 0));
        assertEquals(new PanelLayout.Point(374, 6), at(PanelPosition.TOP_RIGHT, 0, 0));
        assertEquals(new PanelLayout.Point(6, 154), at(PanelPosition.BOTTOM_LEFT, 0, 0));
        assertEquals(new PanelLayout.Point(374, 154), at(PanelPosition.BOTTOM_RIGHT, 0, 0));
    }

    @Test
    void aboveFlipsBelowWhenTheGuiTouchesTheTop() {
        PanelLayout.Point p = PanelLayout.place(PanelPosition.ABOVE_GUI, 480, 400,
            152, 10, 176, 166, 100, 110, 0, 0, 6);
        assertEquals(182, p.y());
        assertEquals(190, p.x());
    }

    @Test
    void cursorGoesUpLeftThenFlipsNearTheEdge() {
        assertEquals(new PanelLayout.Point(194, 84), at(PanelPosition.CURSOR, 300, 200));
        PanelLayout.Point corner = at(PanelPosition.CURSOR, 20, 20);
        assertEquals(new PanelLayout.Point(26, 26), corner);
    }

    @Test
    void boxBiggerThanTheScreenPinsToTheOrigin() {
        PanelLayout.Point p = PanelLayout.place(PanelPosition.BOTTOM_RIGHT, 100, 100,
            0, 0, 50, 50, 300, 300, 0, 0, 6);
        assertEquals(new PanelLayout.Point(0, 0), p);
    }

    @Test
    void fitKeepsTheConfiguredSizeWhenThereIsRoom() {
        assertEquals(100, PanelLayout.fitSize(PanelPosition.TOP_LEFT, 100, 32, 480, 270, 152, 176, 10, 20, 6));
    }

    @Test
    void fitShrinksToTheScreenHeight() {
        // 180 tall screen, 30 of frame and text, 6 margin each side: 138 left.
        assertEquals(138, PanelLayout.fitSize(PanelPosition.TOP_LEFT, 160, 32, 320, 180, 72, 176, 10, 30, 6));
    }

    @Test
    void fitShrinksToTheWiderSideGap() {
        // GUI 176 wide at x=72 on a 320 screen: 72 right gap, 72 left gap, minus margins and frame.
        assertEquals(50, PanelLayout.fitSize(PanelPosition.RIGHT_OF_GUI, 160, 32, 320, 400, 72, 176, 10, 30, 6));
    }

    @Test
    void fitNeverGoesBelowTheMinimum() {
        assertEquals(32, PanelLayout.fitSize(PanelPosition.LEFT_OF_GUI, 160, 32, 200, 400, 10, 180, 10, 30, 6));
    }
}
