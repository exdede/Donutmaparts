package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/**
 * The preview inside the item tooltip, below the item name like a bundle's
 * contents. Resolved once at construction, which vanilla does every frame,
 * so a wall picture that finishes loading shows up on the next frame.
 */
public final class MapPreviewTooltipComponent implements ClientTooltipComponent {
    private static final int TOP_GAP = 2;
    private final MapPreviewRenderer.Box box;

    public MapPreviewTooltipComponent(MapPreviewTooltipData data) {
        this.box = MapPreviewRenderer.resolve(Minecraft.getInstance(), data.mapId(),
            Configs.Preview.TOOLTIP_SIZE.getIntegerValue(), 0);
    }

    @Override
    public int getHeight(Font font) {
        return this.box == null ? 0 : this.box.height() + TOP_GAP * 2;
    }

    @Override
    public int getWidth(Font font) {
        return this.box == null ? 0 : this.box.width();
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor context) {
        if (this.box == null) return;
        MapPreviewRenderer.draw(context, Minecraft.getInstance(), this.box, x, y + TOP_GAP);
    }
}
