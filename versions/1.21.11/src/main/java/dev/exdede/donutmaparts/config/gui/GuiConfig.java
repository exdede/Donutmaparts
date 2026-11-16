package dev.exdede.donutmaparts.config.gui;

import java.util.List;
import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import dev.exdede.donutmaparts.net.BackendClient;
import dev.exdede.donutmaparts.net.LinkCodes;
import dev.exdede.donutmaparts.preview.RemotePreviewClient;
import dev.exdede.donutmaparts.session.LinkNotifier;
import dev.exdede.donutmaparts.session.UploadSession;
import dev.exdede.donutmaparts.tracking.TrackedGroups;
import dev.exdede.donutmaparts.tracking.TrackedIds;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.Screen;

/**
 * malilib config screen. malilib auto-renders a widget per option (booleans as
 * toggles, option lists as cycling buttons, ints/strings as editable fields)
 * from the ConfigOptionWrapper list returned by getConfigs().
 *
 * Tabs follow masa's own pattern from litematica: a static current-tab field, a
 * row of ButtonGeneric built in initGui, and getConfigs() switching on it. The
 * tracked groups are not rendered options; they get their own button row and
 * their own screen, GuiTrackedGroups.
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
        List<fi.dy.masa.malilib.config.IConfigBase> options = switch (tab) {
            case TRACKING -> Configs.Tracking.GUI_OPTIONS;
            case PREVIEW -> Configs.Preview.GUI_OPTIONS;
            default -> Configs.General.GUI_OPTIONS;
        };
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
        else if (tab == ConfigGuiTab.PREVIEW) {
            createPreviewButtons(10, 48);
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
        ButtonGeneric paste = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.paste_ids"));
        paste.setHoverStrings(StringUtils.translate("donutmaparts.gui.hover.paste_ids"));
        this.addButton(paste, (pressed, mouseButton) -> onIdsEntered(MinecraftClient.getInstance().keyboard.getClipboard()));
        x += paste.getWidth() + 2;

        ButtonGeneric add = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.add_id"));
        // The IStringConsumerFeedback cast is required, not decorative: both text
        // input classes also take an IStringConsumer, and a boolean returning method
        // reference is compatible with a void target too, so the call is ambiguous
        // without it.
        this.addButton(add, (pressed, mouseButton) -> GuiBase.openGui(textInput(
            65536, "donutmaparts.gui.title.add_id", "", this,
            // Always false: on true malilib reopens this screen over the name
            // dialog onIdsEntered just opened. False on bad input keeps it open.
            (IStringConsumerFeedback) text -> {
                onIdsEntered(text);
                return false;
            })));
        x += add.getWidth() + 2;

        List<TrackedGroups.Group> groups = TrackedGroups.decode(Configs.Tracking.TRACKED_GROUPS.getStrings());
        ButtonGeneric edit = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.tracked_groups", groups.size(), TrackedGroups.idCount(groups)));
        this.addButton(edit, (pressed, mouseButton) -> GuiBase.openGui(new GuiTrackedGroups(this)));
    }

    /**
     * Shared by the clipboard button and the typed input. A paste covering
     * several arts (a whole collection copied from the site) is split into one
     * group per art by asking the wall which art each map shows; a single art,
     * or no answer from the wall, falls back to one group the player names.
     */
    private void onIdsEntered(String text) {
        List<Integer> parsed = TrackedIds.parseIds(text);
        if (parsed.isEmpty()) {
            this.addMessage(Message.MessageType.ERROR, 4000, "donutmaparts.gui.message.no_ids");
            return;
        }
        int existing = TrackedGroups.decode(Configs.Tracking.TRACKED_GROUPS.getStrings()).size();
        if (parsed.size() == 1) {
            askGroupName(parsed, TrackedGroups.defaultName(existing));
            return;
        }
        this.addMessage(Message.MessageType.INFO, 4000, "donutmaparts.gui.message.sorting");
        MinecraftClient mc = MinecraftClient.getInstance();
        new RemotePreviewClient(Configs.General.BACKEND_URL).splitByArt(parsed)
            .whenComplete((split, error) -> mc.execute(() -> {
                if (error != null) {
                    DebugLog.http("map groups lookup failed: " + error.getMessage());
                    askGroupName(parsed, TrackedGroups.defaultName(existing));
                    return;
                }
                int parts = split.groups().size() + (split.unknown().isEmpty() ? 0 : 1);
                if (parts <= 1) {
                    askGroupName(parsed, split.groups().isEmpty()
                        ? TrackedGroups.defaultName(existing) : "Art #" + split.groups().get(0).imageId());
                    return;
                }
                List<String> stored = Configs.Tracking.TRACKED_GROUPS.getStrings();
                for (RemotePreviewClient.ArtGroup group : split.groups()) {
                    stored = TrackedGroups.add(stored, "Art #" + group.imageId(), group.ids());
                }
                stored = TrackedGroups.add(stored, "Not on wall", split.unknown());
                Configs.Tracking.TRACKED_GROUPS.setStrings(stored);
                Configs.saveToFile();
                DebugLog.tracking("split " + parsed.size() + " IDs into " + parts + " groups");
                GuiBase.openGui(new GuiTrackedGroups(this));
            }));
    }

    private void askGroupName(List<Integer> ids, String defaultName) {
        GuiBase.openGui(textInput(64, "donutmaparts.gui.title.group_name", defaultName, this,
            (IStringConsumerFeedback) name -> {
                Configs.Tracking.TRACKED_GROUPS.setStrings(
                    TrackedGroups.add(Configs.Tracking.TRACKED_GROUPS.getStrings(), name, ids));
                Configs.saveToFile();
                DebugLog.tracking("added a group of " + ids.size() + " IDs");
                return true;
            }));
    }

    /**
     * One reset button: the Preview tab has the most knobs in the mod, and
     * malilib only offers per-option resets, so getting back to the stock look
     * after experimenting would otherwise take twenty clicks.
     */
    private void createPreviewButtons(int x, int y) {
        ButtonGeneric reset = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.reset_preview"));
        this.addButton(reset, (pressed, mouseButton) -> {
            for (fi.dy.masa.malilib.config.IConfigBase option : Configs.Preview.OPTIONS) {
                if (option instanceof fi.dy.masa.malilib.config.IConfigResettable resettable) {
                    resettable.resetToDefault();
                }
            }
            Configs.saveToFile();
            GuiBase.openGui(new GuiConfig(this.getParent()));
        });
    }

    private void createGeneralButtons(int x, int y) {
        ButtonGeneric link = new ButtonGeneric(x, y, -1, 20,
            StringUtils.translate("donutmaparts.gui.button.link_account"));
        link.setHoverStrings(StringUtils.translate("donutmaparts.gui.hover.link_account").split("\n"));
        this.addButton(link, (pressed, mouseButton) -> openLinkAccountDialog());
        this.addLabel(x + link.getWidth() + 6, y + 6, -1, 10, 0xFFA0A0A0,
            StringUtils.translate("donutmaparts.gui.label.link_account"));
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
        GuiBase.openGui(textInput(
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

    /**
     * A malilib single line text dialog whose box actually takes typing on 26.3.
     *
     * malilib focuses the box in the dialog's constructor, before the dialog is
     * the open screen. 26.3 reads the keyboard through SDL3, which only delivers
     * typed characters once text input is switched on for a widget focused on
     * the open screen, so that early focus is lost: the box shows a cursor but
     * ignores every key until it is clicked. Focusing it again once the dialog
     * is up switches text input on, the same thing a click does.
     */
    static GuiTextInput textInput(int maxLength, String titleKey, String defaultText, Screen parent,
                                  IStringConsumerFeedback consumer) {
        return new GuiTextInput(maxLength, titleKey, defaultText, parent, consumer) {
            @Override
            public void initGui() {
                super.initGui();
                refocus(this.textField);
            }
        };
    }

    private static void refocus(ClickableWidget field) {
        field.setFocused(false);
        field.setFocused(true);
    }
}
