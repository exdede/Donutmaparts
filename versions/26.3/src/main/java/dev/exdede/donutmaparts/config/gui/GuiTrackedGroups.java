package dev.exdede.donutmaparts.config.gui;

import java.util.ArrayList;
import java.util.List;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.preview.MapPreviewRenderer;
import dev.exdede.donutmaparts.tracking.TrackedGroups;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;

/**
 * One row per tracked group: color swatch (click to change), name (click to
 * rename), map count, and a remove button. Hovering a row shows the group's
 * picture from the wall, so a player knows what they are hunting for before
 * they ever see it in game.
 */
public class GuiTrackedGroups extends GuiBase {
    private static final int TOP = 50;
    private static final int ROW_HEIGHT = 22;
    private static final int NAME_WIDTH = 170;
    private static final int PREVIEW_SIZE = 96;

    private static int page;

    private record Row(int x, int y, int width, TrackedGroups.Group group) {}

    private final List<Row> rows = new ArrayList<>();

    public GuiTrackedGroups(Screen parent) {
        this.setParent(parent);
        this.title = StringUtils.translate("donutmaparts.gui.title.tracked_groups");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.rows.clear();
        List<TrackedGroups.Group> groups = groups();

        int x = 10;
        ButtonGeneric back = new ButtonGeneric(x, 26, -1, 20, StringUtils.translate("donutmaparts.gui.button.back"));
        this.addButton(back, (b, m) -> GuiBase.openGui(this.getParent()));
        x += back.getWidth() + 2;

        ButtonGeneric clear = new ButtonGeneric(x, 26, -1, 20, StringUtils.translate("donutmaparts.gui.button.remove_all"));
        clear.setEnabled(!groups.isEmpty());
        this.addButton(clear, (b, m) -> GuiBase.openGui(new GuiConfirmAction(260,
            "donutmaparts.gui.title.remove_all", new IConfirmationListener() {
                @Override
                public boolean onActionConfirmed() {
                    // The dialog reopens this screen, which rereads the config.
                    Configs.Tracking.TRACKED_GROUPS.setStrings(List.of());
                    Configs.saveToFile();
                    return true;
                }

                @Override
                public boolean onActionCancelled() {
                    return true;
                }
            }, this, "donutmaparts.gui.message.remove_all", groups.size())));

        int perPage = Math.max(1, (this.height - TOP - 30) / ROW_HEIGHT);
        int pages = Math.max(1, (groups.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);

        int y = TOP;
        for (int i = page * perPage; i < Math.min(groups.size(), (page + 1) * perPage); i++) {
            int index = i;
            TrackedGroups.Group group = groups.get(i);
            int rx = 10;

            ButtonGeneric swatch = new ButtonGeneric(rx, y, 20, 20, "");
            swatch.setHoverStrings(StringUtils.translate("donutmaparts.gui.hover.swatch"));
            this.addButton(swatch, (b, m) -> save(TrackedGroups.cycleColor(stored(), index)));
            rx += 22;

            ButtonGeneric name = new ButtonGeneric(rx, y, NAME_WIDTH, 20, shorten(group.name()));
            name.setHoverStrings(StringUtils.translate("donutmaparts.gui.hover.rename"));
            this.addButton(name, (b, m) -> GuiBase.openGui(GuiConfig.textInput(64,
                "donutmaparts.gui.title.group_name", group.name(), this,
                (IStringConsumerFeedback) text -> {
                    Configs.Tracking.TRACKED_GROUPS.setStrings(TrackedGroups.rename(stored(), index, text));
                    Configs.saveToFile();
                    return true;
                })));
            rx += NAME_WIDTH + 2;

            String count = StringUtils.translate("donutmaparts.gui.label.map_count", group.ids().size());
            this.addLabel(rx + 4, y + 6, 60, 10, 0xFFC0C0C0, count);
            rx += 66;

            ButtonGeneric remove = new ButtonGeneric(rx, y, -1, 20, StringUtils.translate("donutmaparts.gui.button.remove"));
            this.addButton(remove, (b, m) -> save(TrackedGroups.removeAt(stored(), index)));

            this.rows.add(new Row(10, y, rx + remove.getWidth() - 10, group));
            y += ROW_HEIGHT;
        }

        if (groups.isEmpty()) {
            this.addLabel(10, TOP + 6, 300, 10, 0xFFA0A0A0, StringUtils.translate("donutmaparts.gui.label.no_groups"));
        }

        if (pages > 1) {
            int by = this.height - 26;
            ButtonGeneric prev = new ButtonGeneric(10, by, 20, 20, "<");
            prev.setEnabled(page > 0);
            this.addButton(prev, (b, m) -> { page--; GuiBase.openGui(new GuiTrackedGroups(this.getParent())); });
            ButtonGeneric next = new ButtonGeneric(32, by, 20, 20, ">");
            next.setEnabled(page < pages - 1);
            this.addButton(next, (b, m) -> { page++; GuiBase.openGui(new GuiTrackedGroups(this.getParent())); });
            this.addLabel(58, by + 6, 80, 10, 0xFFC0C0C0, (page + 1) + " / " + pages);
        }
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        Row hovered = null;
        for (Row row : this.rows) {
            ctx.fill(row.x() + 3, row.y() + 3, row.x() + 17, row.y() + 17, 0xFF000000 | row.group().color());
            if (mouseX >= row.x() && mouseX < row.x() + row.width() && mouseY >= row.y() && mouseY < row.y() + 20) {
                hovered = row;
            }
        }
        if (hovered == null) return;
        MapPreviewRenderer.Box box = MapPreviewRenderer.resolveGroup(this.mc, hovered.group().ids(),
            hovered.group().name(), PREVIEW_SIZE, 4);
        int x = Math.max(hovered.x() + hovered.width() + 8, this.width - box.width() - 10);
        int y = Math.min(Math.max(TOP, hovered.y() - box.height() / 2), this.height - box.height() - 4);
        MapPreviewRenderer.draw(ctx, this.mc, box, x, y);
    }

    private static List<String> stored() {
        return Configs.Tracking.TRACKED_GROUPS.getStrings();
    }

    private static List<TrackedGroups.Group> groups() {
        return TrackedGroups.decode(stored());
    }

    private void save(List<String> updated) {
        Configs.Tracking.TRACKED_GROUPS.setStrings(updated);
        Configs.saveToFile();
        GuiBase.openGui(new GuiTrackedGroups(this.getParent()));
    }

    private static String shorten(String name) {
        return name.length() <= 26 ? name : name.substring(0, 25) + "...";
    }
}
