package dev.exdede.donutmaparts.tracking;

import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import java.util.List;
import java.util.Set;

/**
 * Watches the open inventory screen for maps on the tracked ID list.
 *
 * Deliberately independent of the upload pipeline: no UploadSession check, no
 * ServerDetector check, no Configs.General.ENABLED check. Tracking sends nothing
 * anywhere, so the mod's DonutSMP-only rule has nothing to protect here and the
 * feature works in singleplayer and with uploads switched off.
 */
public final class MapTracker {
    public static MapTracker INSTANCE;

    private final ScreenAlertLog alertLog = new ScreenAlertLog();
    private List<String> snapshotSource = List.of();
    private Set<Integer> trackedIds = Set.of();

    /**
     * Called every client tick. Rescans rather than hooking screen open, so maps
     * that arrive after the screen opened (a shulker preview filling in, a hopper
     * feeding the chest, the player moving a stack) are caught by the same path.
     */
    public void tickScreen(MinecraftClient mc) {
        if (mc == null) return;
        if (!Configs.Tracking.TRACKING_ENABLED.getBooleanValue()) return;

        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
            this.alertLog.syncToken(null);
            return;
        }

        this.alertLog.syncToken(screen);
        refreshSnapshot();
        if (this.trackedIds.isEmpty()) return;

        List<Slot> slots = screen.getScreenHandler().slots;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i).getStack();
            MapIdComponent component = stack.get(DataComponentTypes.MAP_ID);
            if (component == null) continue;

            int mapId = component.id();
            if (!this.trackedIds.contains(mapId)) continue;
            if (!this.alertLog.shouldAlert(screen, mapId)) continue;

            this.alertLog.noteHighlight(mapId);
            String title = screen.getTitle().getString();
            DebugLog.tracking("tracked map " + mapId + " found in " + title + " slot " + i);
            TrackingNotifier.alert(mc, mapId, title, i);

            if (Configs.Tracking.AUTO_REMOVE_ON_MATCH.getBooleanValue()) {
                removeTracked(mapId);
            }
        }
    }

    /**
     * True while the slot should pulse. The union of "still tracked" and "matched
     * during this screen opening" is what keeps a slot glowing after auto remove
     * has already pulled the ID out of the list.
     */
    public boolean shouldHighlight(int mapId) {
        if (!Configs.Tracking.TRACKING_ENABLED.getBooleanValue()) return false;
        return this.alertLog.isHighlighted(mapId) || this.trackedIds.contains(mapId);
    }

    private void removeTracked(int mapId) {
        List<String> updated = TrackedIds.remove(
            Configs.Tracking.TRACKED_MAP_IDS.getStrings(), mapId);
        Configs.Tracking.TRACKED_MAP_IDS.setStrings(updated);
        // Written straight away so an auto removal survives a crash rather than
        // waiting for a clean shutdown to flush.
        Configs.saveToFile();
        refreshSnapshot();
        DebugLog.tracking("auto-removed map " + mapId + ", " + updated.size() + " still tracked");
    }

    private void refreshSnapshot() {
        List<String> current = Configs.Tracking.TRACKED_MAP_IDS.getStrings();
        if (current.equals(this.snapshotSource)) return;
        this.snapshotSource = List.copyOf(current);
        this.trackedIds = TrackedIds.toIdSet(current);
        DebugLog.tracking("tracked ID snapshot rebuilt, " + this.trackedIds.size() + " IDs");
    }
}
