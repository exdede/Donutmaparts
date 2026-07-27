package dev.exdede.donutmaparts.tracking;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
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
     * Cached once per tick, right where the open screen's TrackingScope is
     * classified below, so shouldHighlight() (called per-slot, per-frame, from
     * a mixin that has and needs no screen context of its own) can gate the
     * persistent highlight the same way the sound/toast alert already gates
     * itself on scope. Deliberately not reset in the trackedIds-empty early
     * return: that branch is also hit the tick after an auto-removal empties
     * trackedIds on the *same* still-open screen, and the sticky
     * "still highlighted after auto remove" behaviour below depends on this
     * flag still reflecting that screen's real, already-confirmed-allowed
     * scope rather than being clobbered back to false.
     */
    private boolean scopeAllowedThisTick = false;

    /**
     * Called every client tick. Rescans rather than hooking screen open, so maps
     * that arrive after the screen opened (a shulker preview filling in, a hopper
     * feeding the chest, the player moving a stack) are caught by the same path.
     */
    public void tickScreen(Minecraft mc) {
        try {
            if (mc == null) return;
            if (!Configs.Tracking.TRACKING_ENABLED.getBooleanValue()) return;

            if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) {
                this.alertLog.syncToken(null);
                this.scopeAllowedThisTick = false;
                return;
            }

            this.alertLog.syncToken(screen);
            refreshSnapshot();
            if (this.trackedIds.isEmpty()) return;

            // Scope allowlist: classify the open screen once per tick (not per
            // slot) and bail before scanning at all if this category of GUI is
            // switched off, same effect as if the screen weren't open. Cached
            // on scopeAllowedThisTick so shouldHighlight() below can apply the
            // same gate to the persistent slot highlight, not just this scan.
            String title = screen.getTitle().getString();
            TrackingScope scope = TrackingScope.classify(classifyContainerKind(screen), title);
            this.scopeAllowedThisTick = isScopeAllowed(scope);
            if (!this.scopeAllowedThisTick) {
                DebugLog.tracking("scope " + scope + " disabled, skipping alert scan");
                return;
            }

            boolean dirty = false;
            List<Slot> slots = screen.getMenu().slots;
            for (int i = 0; i < slots.size(); i++) {
                ItemStack stack = slots.get(i).getItem();
                MapId component = stack.get(DataComponents.MAP_ID);
                if (component == null) continue;

                int mapId = component.id();
                if (!this.trackedIds.contains(mapId)) continue;
                if (!this.alertLog.shouldAlert(screen, mapId)) continue;

                this.alertLog.noteHighlight(mapId);
                DebugLog.tracking("tracked map " + mapId + " found in " + title + " slot " + i);
                TrackingNotifier.alert(mc, mapId, title, i);

                if (Configs.Tracking.AUTO_REMOVE_ON_MATCH.getBooleanValue()) {
                    removeTracked(mapId);
                    dirty = true;
                }
            }

            if (dirty) {
                // Written straight away so an auto removal survives a crash rather
                // than waiting for a clean shutdown to flush, but only once per
                // tick no matter how many slots matched.
                Configs.saveToFile();
            }
        } catch (Throwable t) {
            DonutMapartsMod.LOGGER.error("Unhandled exception in tracking screen scan", t);
        }
    }

    /**
     * True while the slot should pulse. The union of "still tracked" and "matched
     * during this screen opening" is what keeps a slot glowing after auto remove
     * has already pulled the ID out of the list. Also gated on the scope cached
     * by the last tickScreen() call, so a disallowed GUI category (e.g.
     * TRACK_SHULKER_BOX off) suppresses the highlight the same way it already
     * suppresses the sound/toast alert -- without threading screen context
     * through this per-slot, per-frame call.
     */
    public boolean shouldHighlight(int mapId) {
        if (!Configs.Tracking.TRACKING_ENABLED.getBooleanValue()) return false;
        return isHighlightEligible(mapId);
    }

    /**
     * The trackedIds/scope half of shouldHighlight(), split out from the
     * Configs.Tracking.TRACKING_ENABLED check above so it can be unit tested
     * directly: touching Configs at all requires a live Fabric Loader
     * (malilib's ConfigBase reaches into FabricLoaderImpl during class init),
     * which a plain JUnit run does not provide -- the same reason this class
     * had no tests before this method existed. Package-private for tests.
     */
    boolean isHighlightEligible(int mapId) {
        if (!this.scopeAllowedThisTick) return false;
        return this.alertLog.isHighlighted(mapId) || this.trackedIds.contains(mapId);
    }

    private void removeTracked(int mapId) {
        List<String> updated = TrackedIds.remove(
            Configs.Tracking.TRACKED_MAP_IDS.getStrings(), mapId);
        Configs.Tracking.TRACKED_MAP_IDS.setStrings(updated);
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

    /**
     * Coarse Minecraft-side signal for TrackingScope.classify(). A shulker box
     * gets its own dedicated MenuType; a plain chest, an ender chest, and a
     * generic server GUI (e.g. the Auction House) all share the same generic
     * 9xN menu type on the wire and are only told apart by title, which
     * classify() handles from here.
     */
    private static TrackingScope.ContainerKind classifyContainerKind(AbstractContainerScreen<?> screen) {
        MenuType<?> type = screen.getMenu().getType();
        if (type == MenuType.SHULKER_BOX) return TrackingScope.ContainerKind.SHULKER_BOX;
        if (isGenericContainer(type)) return TrackingScope.ContainerKind.GENERIC_CONTAINER;
        return TrackingScope.ContainerKind.OTHER;
    }

    private static boolean isGenericContainer(MenuType<?> type) {
        return type == MenuType.GENERIC_9x1
            || type == MenuType.GENERIC_9x2
            || type == MenuType.GENERIC_9x3
            || type == MenuType.GENERIC_9x4
            || type == MenuType.GENERIC_9x5
            || type == MenuType.GENERIC_9x6;
    }

    private static boolean isScopeAllowed(TrackingScope scope) {
        return switch (scope) {
            case CHEST -> Configs.Tracking.TRACK_CHEST.getBooleanValue();
            case ENDER_CHEST -> Configs.Tracking.TRACK_ENDER_CHEST.getBooleanValue();
            case SHULKER_BOX -> Configs.Tracking.TRACK_SHULKER_BOX.getBooleanValue();
            case AUCTION_HOUSE -> Configs.Tracking.TRACK_AUCTION_HOUSE.getBooleanValue();
            case OTHER -> Configs.Tracking.TRACK_OTHER.getBooleanValue();
        };
    }
}
