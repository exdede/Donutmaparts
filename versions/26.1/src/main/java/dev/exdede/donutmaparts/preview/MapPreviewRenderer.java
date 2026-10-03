package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Draws one preview box: optional frame, background, the picture (or a
 * placeholder) and an optional label underneath. Shared by the tooltip and
 * the side panel, which differ only in picture size and padding.
 *
 * The picture comes from the client's own copy of the map when the server
 * sent one (vanilla map rendering, exactly what an item frame shows), else
 * from the DonutMaparts wall when the fallback is allowed.
 */
public final class MapPreviewRenderer {
    private static final int MAP_PIXELS = 128;
    private static final int LABEL_GAP = 2;

    private MapPreviewRenderer() {}

    /** A resolved box: what to draw and how big it is. */
    public record Box(MapId mapId, PreviewText.Source source, MapItemSavedData localState,
                      int pictureSize, int padding, String label, int width, int height) {}

    /**
     * Resolves what is drawable for this map right now, starting a wall fetch
     * if needed, and lays the box out. Null when there is nothing to show and
     * placeholders are off.
     */
    public static Box resolve(Minecraft mc, MapId mapId, int pictureSize, int padding) {
        MapItemSavedData state = mc.level == null ? null : MapItem.getSavedData(mapId, mc.level);
        PreviewText.Source source;
        if (state != null) {
            source = PreviewText.Source.LOCAL;
        } else if (MapPreviewGate.remoteAllowed(mc)) {
            source = switch (MapPreviewTextures.INSTANCE.request(mapId.id())) {
                case READY -> PreviewText.Source.WALL;
                case LOADING -> PreviewText.Source.LOADING;
                default -> PreviewText.Source.NONE;
            };
        } else {
            source = PreviewText.Source.NONE;
        }
        if (PreviewText.placeholder(source) != null && !Configs.Preview.SHOW_PLACEHOLDER.getBooleanValue()) {
            return null;
        }

        String label = PreviewText.label(
            (PreviewOptions.Label) Configs.Preview.LABEL.getOptionListValue(), mapId.id(), source);
        Font font = mc.font;
        int frame = frameWidth();
        int innerWidth = Math.max(pictureSize, label == null ? 0 : font.width(label));
        int innerHeight = pictureSize + (label == null ? 0 : LABEL_GAP + font.lineHeight);
        int width = innerWidth + 2 * (frame + padding);
        int height = innerHeight + 2 * (frame + padding);
        return new Box(mapId, source, state, pictureSize, padding, label, width, height);
    }

    public static void draw(GuiGraphicsExtractor ctx, Minecraft mc, Box box, int x, int y) {
        int frame = frameWidth();
        if (frame > 0) {
            int c = Configs.Preview.BORDER_COLOR.getIntegerValue();
            ctx.fill(x, y, x + box.width(), y + frame, c);
            ctx.fill(x, y + box.height() - frame, x + box.width(), y + box.height(), c);
            ctx.fill(x, y + frame, x + frame, y + box.height() - frame, c);
            ctx.fill(x + box.width() - frame, y + frame, x + box.width(), y + box.height() - frame, c);
        }
        ctx.fill(x + frame, y + frame, x + box.width() - frame, y + box.height() - frame,
            Configs.Preview.BACKGROUND_COLOR.getIntegerValue());

        int inner = frame + box.padding();
        int px = x + (box.width() - box.pictureSize()) / 2;
        int py = y + inner;
        int size = box.pictureSize();

        switch (box.source()) {
            case LOCAL -> drawLocal(ctx, mc, box, px, py, size);
            case WALL -> {
                MapPreviewTextures.PreviewTexture tex = MapPreviewTextures.INSTANCE.texture(box.mapId().id());
                if (tex != null) {
                    ctx.blit(RenderPipelines.GUI_TEXTURED, tex.id(), px, py, 0.0F, 0.0F,
                        size, size, size, size);
                }
            }
            default -> {
                String text = PreviewText.placeholder(box.source());
                if (text != null) {
                    Font font = mc.font;
                    ctx.text(font, text, px + (size - font.width(text)) / 2,
                        py + (size - font.lineHeight) / 2, 0xFF808080);
                }
            }
        }

        if (box.label() != null) {
            Font font = mc.font;
            ctx.text(font, box.label(), x + (box.width() - font.width(box.label())) / 2,
                py + size + LABEL_GAP, Configs.Preview.LABEL_COLOR.getIntegerValue());
        }
    }

    private static void drawLocal(GuiGraphicsExtractor ctx, Minecraft mc, Box box, int x, int y, int size) {
        // A fresh state per draw: the GUI render state may keep a reference
        // until the frame is submitted, and the tooltip and the side panel can
        // both draw in one frame.
        MapRenderState state = new MapRenderState();
        mc.getMapRenderer().extractRenderState(box.mapId(), box.localState(), state);
        if (Configs.Preview.HIDE_MAP_MARKERS.getBooleanValue()) state.decorations.clear();
        float scale = size / (float) MAP_PIXELS;
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(scale, scale);
        ctx.map(state);
        ctx.pose().popMatrix();
    }

    private static int frameWidth() {
        return Configs.Preview.SHOW_BORDER.getBooleanValue() ? Configs.Preview.BORDER_WIDTH.getIntegerValue() : 0;
    }
}
