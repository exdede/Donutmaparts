package dev.exdede.donutmaparts.config.gui;

import java.util.List;
import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import dev.exdede.donutmaparts.tracking.TrackedIds;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.GuiTextInputMultiLine;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;

/**
 * malilib config screen. malilib auto-renders a widget per option (booleans as
 * toggles, option lists as cycling buttons, ints/strings as editable fields)
 * from the ConfigOptionWrapper list returned by getConfigs().
 *
 * Tabs follow masa's own pattern from litematica: a static current-tab field, a
 * row of ButtonGeneric built in initGui, and getConfigs() switching on it. The
 * tracked ID list is not one of the rendered options; it gets its own button row
 * so the tab can offer single add and bulk paste next to the list editor.
 */
public class GuiConfig extends GuiConfigsBase {
    private static ConfigGuiTab tab = ConfigGuiTab.GENERAL;

    public GuiConfig() {
        this(null);
    }

    public GuiConfig(Screen parent) {
        super(10, 70, DonutMapartsMod.MOD_ID, parent, "donutmaparts.gui.title.configs");
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<fi.dy.masa.malilib.config.IConfigBase> options = tab == ConfigGuiTab.TRACKING
            ? Configs.Tracking.GUI_OPTIONS
            : Configs.General.GUI_OPTIONS;
        return ConfigOptionWrapper.createFor(options);
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = 10;
        int y = 26;
        for (ConfigGuiTab value : ConfigGuiTab.values()) {
            x += createTabButton(x, y, value);
        }

        if (tab == ConfigGuiTab.TRACKING) {
            createTrackingButtons(10, 48);
        }
    }

    private int createTabButton(int x, int y, ConfigGuiTab value) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, value.getDisplayName());
        button.setEnabled(tab != value);
        this.addButton(button, (pressed, mouseButton) -> {
            tab = value;
            GuiBase.openGui(new GuiConfig(this.getParent()));
        });
        return button.getWidth() + 2;
    }

    private void createTrackingButtons(int x, int y) {
        ButtonGeneric add = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.add_id"));
        // The IStringConsumerFeedback cast is required, not decorative: both text
        // input classes also take an IStringConsumer, and a boolean returning method
        // reference is compatible with a void target too, so the call is ambiguous
        // without it.
        this.addButton(add, (pressed, mouseButton) -> GuiBase.openGui(new GuiTextInput(
            32, "donutmaparts.gui.title.add_id", "", this,
            (IStringConsumerFeedback) this::onIdsEntered)));
        x += add.getWidth() + 2;

        ButtonGeneric bulk = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.bulk_add"));
        this.addButton(bulk, (pressed, mouseButton) -> GuiBase.openGui(new GuiTextInputMultiLine(
            8192, 12, 64, "donutmaparts.gui.title.bulk_add", "", this,
            (IStringConsumerFeedback) this::onIdsEntered)));
        x += bulk.getWidth() + 2;

        int count = TrackedIds.toIdSet(Configs.Tracking.TRACKED_MAP_IDS.getStrings()).size();
        ButtonGeneric edit = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.tracked_ids", count));
        this.addButton(edit, (pressed, mouseButton) -> GuiBase.openGui(new GuiStringListEdit(
            Configs.Tracking.TRACKED_MAP_IDS, this, null, this)));
    }

    /**
     * Shared by the single add and bulk add inputs: both are just text that gets
     * parsed, deduped and merged. Returning false keeps the input screen open,
     * which is how the user finds out they typed something that is not a map ID.
     */
    private boolean onIdsEntered(String text) {
        List<Integer> parsed = TrackedIds.parseIds(text);
        if (parsed.isEmpty()) return false;

        List<String> merged = TrackedIds.merge(
            Configs.Tracking.TRACKED_MAP_IDS.getStrings(), parsed);
        Configs.Tracking.TRACKED_MAP_IDS.setStrings(merged);
        Configs.saveToFile();
        DebugLog.tracking("added " + parsed.size() + " IDs, " + merged.size() + " now tracked");
        return true;
    }
}
