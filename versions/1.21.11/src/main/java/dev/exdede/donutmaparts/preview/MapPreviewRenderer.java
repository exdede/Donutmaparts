package dev.exdede.donutmaparts.preview;

import dev.exdede.donutmaparts.config.Configs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

/**
 * Draws one preview box: optional flat border, background, the picture (or a
 * placeholder) on optional map paper, then up to two text lines underneath.
 * Shared by the tooltip and the side panel, which differ only in picture size
 * and padding.
 *
 * The picture is the client's own copy of the map whenever the server sent
 * one, drawn with vanilla map rendering exactly like an item frame (and like
 * other map tooltip mods). Only when the client has no copy, and has gone
 * without one for the configured delay, does it fall back to the wall's last
 * capture, marked as such.
 */
public final class MapPreviewRenderer {
    private static final int MAP_PIXELS = 128;
    private static final int LINE_GAP = 2;
    private static final Identifier MAP_PAPER = Identifier.ofVanilla("textures/map/map_background.png");
    private static final FallbackDelay FALLBACK_DELAY = new FallbackDelay();

    private MapPreviewRenderer() {}

    /** A resolved box: what to draw and how big it is. */
    public record Box(MapIdComponent mapId, PreviewText.Source source, MapState localState,
                      int pictureSize, int padding, int paper, List<String> lines,
                      int width, int height) {}

    public static void clearFallbackDelay() {
        FALLBACK_DELAY.clear();
    }

    /**
     * Resolves what is drawable for this map right now, asking the wall if
     * needed, and lays the box out. Null when there is nothing to show and
     * placeholders are off.
     */
    public static Box resolve(MinecraftClient mc, MapIdComponent mapId, int pictureSize, int padding) {
        MapState state = mc.world == null ? null : FilledMapItem.getMapState(mapId, mc.world);
        PreviewText.Source source;
        if (state != null) {
            source = PreviewText.Source.LOCAL;
        } else if (!MapPreviewGate.remoteAllowed(mc)) {
            source = PreviewText.Source.NONE;
        } else if (!FALLBACK_DELAY.ready(mapId.id(), System.currentTimeMillis(),
                Configs.Preview.REMOTE_DELAY_MILLIS.getIntegerValue())) {
            source = PreviewText.Source.LOADING;
        } else {
            source = switch (MapPreviewTextures.INSTANCE.request(mapId.id())) {
                case READY -> PreviewText.Source.WALL;
                case LOADING -> PreviewText.Source.LOADING;
                default -> PreviewText.Source.NONE;
            };
        }
        if (PreviewText.placeholder(source) != null && !Configs.Preview.SHOW_PLACEHOLDER.getBooleanValue()) {
            return null;
        }

        List<String> lines = new ArrayList<>(2);
        String label = PreviewText.label(
            (PreviewOptions.Label) Configs.Preview.LABEL.getOptionListValue(), mapId.id(), source);
        if (label != null) lines.add(label);
        String credit = PreviewText.credit(source, Configs.Preview.WALL_CREDIT.getBooleanValue());
        if (credit != null) lines.add(credit);

        TextRenderer font = mc.textRenderer;
        int paper = frameStyle() == PreviewOptions.FrameStyle.MAP_PAPER ? Math.max(3, Math.round(pictureSize * 0.07F)) : 0;
        int border = borderWidth();
        int innerWidth = pictureSize + 2 * paper;
        for (String line : lines) innerWidth = Math.max(innerWidth, font.getWidth(line));
        int innerHeight = pictureSize + 2 * paper + lines.size() * (LINE_GAP + font.fontHeight);
        int width = innerWidth + 2 * (border + padding);
        int height = innerHeight + 2 * (border + padding);
        return new Box(mapId, source, state, pictureSize, padding, paper, lines, width, height);
    }

    public static void draw(DrawContext ctx, MinecraftClient mc, Box box, int x, int y) {
        int border = borderWidth();
        if (border > 0) {
            int c = Configs.Preview.BORDER_COLOR.getIntegerValue();
            ctx.fill(x, y, x + box.width(), y + border, c);
            ctx.fill(x, y + box.height() - border, x + box.width(), y + box.height(), c);
            ctx.fill(x, y + border, x + border, y + box.height() - border, c);
            ctx.fill(x + box.width() - border, y + border, x + box.width(), y + box.height() - border, c);
        }
        ctx.fill(x + border, y + border, x + box.width() - border, y + box.height() - border,
            Configs.Preview.BACKGROUND_COLOR.getIntegerValue());

        int size = box.pictureSize();
        int px = x + (box.width() - size) / 2;
        int py = y + border + box.padding() + box.paper();

        if (box.paper() > 0) {
            int paperSize = size + 2 * box.paper();
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, MAP_PAPER, px - box.paper(), py - box.paper(),
                0.0F, 0.0F, paperSize, paperSize, paperSize, paperSize);
        }

        switch (box.source()) {
            case LOCAL -> drawLocal(ctx, mc, box, px, py, size);
            case WALL -> {
                MapPreviewTextures.PreviewTexture tex = MapPreviewTextures.INSTANCE.texture(box.mapId().id());
                if (tex != null) {
                    ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex.id(), px, py, 0.0F, 0.0F,
                        size, size, size, size);
                }
            }
            default -> {
                String text = PreviewText.placeholder(box.source());
                if (text != null) {
                    TextRenderer font = mc.textRenderer;
                    int color = box.paper() > 0 ? 0xFF6B5B45 : 0xFF808080;
                    ctx.drawText(font, text, px + (size - font.getWidth(text)) / 2,
                        py + (size - font.fontHeight) / 2, color, box.paper() == 0);
                }
            }
        }

        TextRenderer font = mc.textRenderer;
        int lineY = py + size + box.paper() + LINE_GAP;
        int labelColor = Configs.Preview.LABEL_COLOR.getIntegerValue();
        for (int i = 0; i < box.lines().size(); i++) {
            String line = box.lines().get(i);
            // The credit line, when present, is always last and drawn dimmer.
            int color = i == box.lines().size() - 1 && box.source() == PreviewText.Source.WALL
                && Configs.Preview.WALL_CREDIT.getBooleanValue() ? dim(labelColor) : labelColor;
            ctx.drawTextWithShadow(font, line, x + (box.width() - font.getWidth(line)) / 2, lineY, color);
            lineY += font.fontHeight + LINE_GAP;
        }
    }

    private static void drawLocal(DrawContext ctx, MinecraftClient mc, Box box, int x, int y, int size) {
        // A fresh state per draw: the GUI render state may keep a reference
        // until the frame is submitted, and the tooltip and the side panel can
        // both draw in one frame.
        MapRenderState state = new MapRenderState();
        mc.getMapRenderer().update(box.mapId(), box.localState(), state);
        if (Configs.Preview.HIDE_MAP_MARKERS.getBooleanValue()) state.decorations.clear();
        float scale = size / (float) MAP_PIXELS;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawMap(state);
        ctx.getMatrices().popMatrix();
    }

    private static int dim(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = ((argb >> 16) & 0xFF) * 3 / 4;
        int g = ((argb >> 8) & 0xFF) * 3 / 4;
        int b = (argb & 0xFF) * 3 / 4;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static PreviewOptions.FrameStyle frameStyle() {
        return (PreviewOptions.FrameStyle) Configs.Preview.FRAME_STYLE.getOptionListValue();
    }

    private static int borderWidth() {
        return frameStyle() == PreviewOptions.FrameStyle.FLAT ? Configs.Preview.BORDER_WIDTH.getIntegerValue() : 0;
    }
}
