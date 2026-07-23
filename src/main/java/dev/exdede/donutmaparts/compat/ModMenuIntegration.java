package dev.exdede.donutmaparts.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.exdede.donutmaparts.config.gui.GuiConfig;

/**
 * Mod Menu integration. This class is referenced only by the optional "modmenu"
 * entrypoint in fabric.mod.json, so it is loaded exclusively by Mod Menu itself.
 * When Mod Menu is not installed, this class is never loaded and the mod runs
 * unaffected. Keep all Mod Menu imports confined to this file.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // parent -> new GuiConfig(parent) so the Done button returns to the mod list.
        return GuiConfig::new;
    }
}
