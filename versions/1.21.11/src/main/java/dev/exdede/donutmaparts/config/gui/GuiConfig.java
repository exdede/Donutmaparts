package dev.exdede.donutmaparts.config.gui;

import java.util.List;
import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import dev.exdede.donutmaparts.net.BackendClient;
import dev.exdede.donutmaparts.net.LinkCodes;
import dev.exdede.donutmaparts.session.LinkNotifier;
import dev.exdede.donutmaparts.session.UploadSession;
import dev.exdede.donutmaparts.tracking.TrackedIds;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.GuiTextInputMultiLine;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

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
        else {
            createGeneralButtons(10, 48);
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

    private void createGeneralButtons(int x, int y) {
        ButtonGeneric link = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.link_account"));
        this.addButton(link, (pressed, mouseButton) -> openLinkAccountDialog());
    }

    /**
     * Linking an account needs a live API token from a completed handshake,
     * which today only exists after joining a server (UploadSession.onJoin
     * only calls BackendClient.handshake once ServerDetector confirms
     * DonutSMP). A blank entry dialog with nothing to submit against would
     * be worse than no dialog at all, so this is guarded on session state
     * the same way the rest of the mod gates features, e.g.
     * flushIfDue's session.isActive() check.
     */
    private void openLinkAccountDialog() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String apiToken = UploadSession.INSTANCE.tokenOrNull();
        BackendClient client = UploadSession.INSTANCE.clientOrNull();
        if (apiToken == null || client == null) {
            // Three different dead ends land here with a null token, and each
            // needs a different action from the player, so they get different
            // toasts. Order matters: ENABLED is checked first because onJoin()
            // returns on it before ever reaching the address check, so a
            // disabled mod also reports itself as not on DonutSMP.
            if (!Configs.General.ENABLED.getBooleanValue()) LinkNotifier.modDisabled(mc);
            else if (!UploadSession.INSTANCE.isOnDonutSmp()) LinkNotifier.noSession(mc);
            else LinkNotifier.notReady(mc);
            return;
        }
        // Must be at least the 32 chars LinkCodes expects: malilib passes this
        // straight to the text field's setMaxLength, so anything smaller
        // silently truncates a pasted code into one normalize() then rejects.
        GuiBase.openGui(new GuiTextInput(
            32, "donutmaparts.gui.title.link_account", "", this,
            (IStringConsumerFeedback) raw -> onLinkCodeEntered(mc, client, apiToken, raw)));
    }

    /**
     * Format-checks locally (nice-to-have UX, the backend is the real
     * authority) then fires the submission and closes the dialog
     * immediately -- submitLinkCode is a genuine network round trip, and
     * malilib's IStringConsumerFeedback callback is synchronous, so unlike
     * onIdsEntered above there is nothing to wait on here. The outcome
     * (linked or not, never which reason) arrives later as a toast, the
     * same fire-and-forget-then-toast shape MapTracker.maybeAutoCollect
     * uses for auto-collection submissions. Returning false only for an
     * obviously malformed code keeps the dialog open so the player can fix
     * a typo without losing what they typed.
     */
    private boolean onLinkCodeEntered(MinecraftClient mc, BackendClient client, String apiToken, String raw) {
        String code = LinkCodes.normalize(raw);
        if (code == null) {
            LinkNotifier.linkFailed(mc);
            return false;
        }

        client.submitLinkCode(apiToken, code).thenAccept(success -> mc.execute(() -> {
            if (success) LinkNotifier.linked(mc);
            else LinkNotifier.linkFailed(mc);
        }));
        return true;
    }
}
