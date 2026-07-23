package dev.exdede.donutmaparts.config.gui;

import java.util.List;
import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import net.minecraft.client.gui.screen.Screen;

/**
 * malilib config screen for the mod's General options. malilib auto-renders a
 * widget per option (booleans as toggles, ints/strings as editable fields) from
 * the ConfigOptionWrapper list returned by getConfigs().
 */
public class GuiConfig extends GuiConfigsBase {
    public GuiConfig() {
        this(null);
    }

    public GuiConfig(Screen parent) {
        super(10, 50, DonutMapartsMod.MOD_ID, parent, "donutmaparts.gui.title.configs");
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(Configs.General.GUI_OPTIONS);
    }
}
