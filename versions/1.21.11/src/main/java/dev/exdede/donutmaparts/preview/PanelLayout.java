package dev.exdede.donutmaparts.preview;

/**
 * Where the side panel goes, as pure arithmetic so every position can be
 * tested without a client. All values are scaled GUI pixels.
 *
 * GUI-relative positions flip to the opposite side when the preferred side
 * has no room (a wide double chest on a small window), and every result is
 * finally clamped onto the screen so the panel can never be drawn off it.
 */
public final class PanelLayout {
    private PanelLayout() {}

    public record Point(int x, int y) {}

    public static Point place(PreviewOptions.PanelPosition position,
                              int screenW, int screenH,
                              int guiX, int guiY, int guiW, int guiH,
                              int boxW, int boxH,
                              int mouseX, int mouseY,
                              int margin) {
        int x;
        int y;
        switch (position) {
            case RIGHT_OF_GUI -> {
                x = guiX + guiW + margin;
                if (x + boxW > screenW && guiX - margin - boxW >= 0) x = guiX - margin - boxW;
                y = guiY;
            }
            case LEFT_OF_GUI -> {
                x = guiX - margin - boxW;
                if (x < 0 && guiX + guiW + margin + boxW <= screenW) x = guiX + guiW + margin;
                y = guiY;
            }
            case ABOVE_GUI -> {
                x = guiX + (guiW - boxW) / 2;
                y = guiY - margin - boxH;
                if (y < 0 && guiY + guiH + margin + boxH <= screenH) y = guiY + guiH + margin;
            }
            case BELOW_GUI -> {
                x = guiX + (guiW - boxW) / 2;
                y = guiY + guiH + margin;
                if (y + boxH > screenH && guiY - margin - boxH >= 0) y = guiY - margin - boxH;
            }
            case TOP_LEFT -> {
                x = margin;
                y = margin;
            }
            case TOP_RIGHT -> {
                x = screenW - margin - boxW;
                y = margin;
            }
            case BOTTOM_LEFT -> {
                x = margin;
                y = screenH - margin - boxH;
            }
            case BOTTOM_RIGHT -> {
                x = screenW - margin - boxW;
                y = screenH - margin - boxH;
            }
            case CURSOR -> {
                // Up and to the left of the cursor, so it stays clear of the
                // vanilla tooltip which opens down and to the right.
                x = mouseX - margin - boxW;
                if (x < 0) x = mouseX + margin;
                y = mouseY - margin - boxH;
                if (y < 0) y = mouseY + margin;
            }
            default -> {
                x = margin;
                y = margin;
            }
        }
        return new Point(clamp(x, 0, screenW - boxW), clamp(y, 0, screenH - boxH));
    }

    /**
     * The largest picture size, up to the configured one, whose box still
     * fits where the panel goes: the screen height always, and for left or
     * right of the GUI the wider of the two side gaps (place() picks the side
     * with room). extraW and extraH are the box's size minus the picture,
     * i.e. frame, padding and text lines. Never below minSize, so a tiny
     * window degrades to a small preview rather than none.
     */
    public static int fitSize(PreviewOptions.PanelPosition position, int configured, int minSize,
                              int screenW, int screenH, int guiX, int guiW,
                              int extraW, int extraH, int margin) {
        int maxH = screenH - 2 * margin - extraH;
        int maxW = screenW - 2 * margin - extraW;
        if (position == PreviewOptions.PanelPosition.RIGHT_OF_GUI
                || position == PreviewOptions.PanelPosition.LEFT_OF_GUI) {
            int right = screenW - (guiX + guiW) - 2 * margin;
            int left = guiX - 2 * margin;
            maxW = Math.max(left, right) - extraW;
        }
        int fit = Math.min(configured, Math.min(maxW, maxH));
        return Math.max(minSize, fit);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }
}
