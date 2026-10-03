package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;

/**
 * The preview inside the item tooltip, below the item name like a bundle's
 * contents. Resolved once at construction, which vanilla does every frame,
 * so a wall picture that finishes loading shows up on the next frame.
 */
public final class MapPreviewTooltipComponent implements TooltipComponent {
    private static final int TOP_GAP = 2;
    private final MapPreviewRenderer.Box box;

    public MapPreviewTooltipComponent(MapPreviewTooltipData data) {
        this.box = MapPreviewRenderer.resolve(MinecraftClient.getInstance(), data.mapId(),
            Configs.Preview.TOOLTIP_SIZE.getIntegerValue(), 0);
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return this.box == null ? 0 : this.box.height() + TOP_GAP * 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return this.box == null ? 0 : this.box.width();
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        if (this.box == null) return;
        MapPreviewRenderer.draw(context, MinecraftClient.getInstance(), this.box, x, y + TOP_GAP);
    }
}
