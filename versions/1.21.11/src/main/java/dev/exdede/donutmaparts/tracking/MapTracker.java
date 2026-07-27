package dev.exdede.donutmaparts.tracking;

import dev.exdede.donutmaparts.DonutMapartsMod;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import dev.exdede.donutmaparts.net.BackendClient;
import dev.exdede.donutmaparts.session.UploadSession;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Watches the open inventory screen for maps on the tracked ID list.
 *
 * Deliberately independent of the upload pipeline for the tracked-id wishlist
 * scan: no UploadSession check, no ServerDetector check, no
 * Configs.General.ENABLED check there. Tracking sends nothing anywhere, so the
 * mod's DonutSMP-only rule has nothing to protect for that half of this class,
 * and it works in singleplayer and with uploads switched off.
 *
 * Auto-collection (Configs.Tracking.AUTO_COLLECT) is the one exception, per a
 * locked project decision: it submits to the backend, so it depends on
 * UploadSession (for the API token and BackendClient reused from the upload
 * pipeline's session) and is gated on ServerDetector's DonutSMP check via
 * UploadSession.isOnDonutSmp(). That gate applies only to auto-collection --
 * the tracked-id scan above it must stay ungated.
 */
public final class MapTracker {
    public static MapTracker INSTANCE;

    private final ScreenAlertLog alertLog = new ScreenAlertLog();
    private List<String> snapshotSource = List.of();
    private Set<Integer> trackedIds = Set.of();

    /**
     * In-memory only, cleared on relaunch. Auto-collection's backend endpoint
     * is already idempotent (INSERT OR IGNORE), so the only thing local dedup
     * needs to prevent is re-firing the same request every tick while one
     * container stays open in one play session -- there is no correctness
     * reason to persist this across relaunches like SentHashCache does for
     * the upload pipeline.
     */
    private final Set<Integer> autoCollectedThisSession = new HashSet<>();

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
    public void tickScreen(MinecraftClient mc) {
        try {
            if (mc == null) return;
            boolean trackingEnabled = Configs.Tracking.TRACKING_ENABLED.getBooleanValue();
            boolean autoCollectEnabled = Configs.Tracking.AUTO_COLLECT.getBooleanValue();
            if (!trackingEnabled && !autoCollectEnabled) return;

            if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                this.alertLog.syncToken(null);
                this.scopeAllowedThisTick = false;
                return;
            }

            this.alertLog.syncToken(screen);
            refreshSnapshot();

            // Auto-collection has no tracked-list prerequisite: it inspects
            // every map slot regardless of wishlist membership. Only bail
            // here when neither feature has a reason to scan this tick.
            boolean hasTrackedIds = trackingEnabled && !this.trackedIds.isEmpty();
            if (!hasTrackedIds && !autoCollectEnabled) return;

            // Scope allowlist: classify the open screen once per tick (not per
            // slot) and bail before scanning at all if this category of GUI is
            // switched off, same effect as if the screen weren't open. Cached
            // on scopeAllowedThisTick so shouldHighlight() below (and
            // isAutoCollectEligible()) can apply the same gate to the
            // persistent slot highlight and to auto-collection, not just
            // this scan.
            String title = screen.getTitle().getString();
            TrackingScope scope = TrackingScope.classify(classifyContainerKind(screen), title);
            this.scopeAllowedThisTick = isScopeAllowed(scope);
            if (!this.scopeAllowedThisTick) {
                DebugLog.tracking("scope " + scope + " disabled, skipping alert scan");
                return;
            }

            // Computed once per tick, not per slot, same as scopeAllowedThisTick
            // above: the DonutSMP gate the locked project decision requires for
            // auto-collection only, never for the tracked-id scan below.
            boolean onDonutSmp = autoCollectEnabled && UploadSession.INSTANCE.isOnDonutSmp();

            boolean dirty = false;
            List<Slot> slots = screen.getScreenHandler().slots;
            for (int i = 0; i < slots.size(); i++) {
                ItemStack stack = slots.get(i).getStack();
                MapIdComponent component = stack.get(DataComponentTypes.MAP_ID);
                if (component == null) continue;

                int mapId = component.id();

                if (autoCollectEnabled) {
                    maybeAutoCollect(mc, mapId, onDonutSmp);
                }

                if (!hasTrackedIds) continue;
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
     * Fires an auto-collection submission for one slot's map id, unless
     * already ineligible (wrong scope, not on DonutSMP, or already submitted
     * this session) or the upload session has no token yet. Marks the id
     * submitted optimistically, before the response comes back: a slow or
     * failed request must not be retried every subsequent tick while the
     * container stays open.
     */
    private void maybeAutoCollect(MinecraftClient mc, int mapId, boolean onDonutSmp) {
        if (!isAutoCollectEligible(mapId, onDonutSmp)) return;

        String apiToken = UploadSession.INSTANCE.tokenOrNull();
        BackendClient client = UploadSession.INSTANCE.clientOrNull();
        if (apiToken == null || client == null) {
            DebugLog.tracking("auto-collect skipped for map " + mapId + ", no active session yet");
            return;
        }

        this.autoCollectedThisSession.add(mapId);
        DebugLog.tracking("auto-collect submitting map " + mapId);
        client.submitCollectionEvent(apiToken, mapId).thenAccept(success -> {
            if (success) {
                mc.execute(() -> TrackingNotifier.autoCollected(mc, mapId));
            } else {
                DebugLog.tracking("auto-collect not accepted for map " + mapId);
            }
        });
    }

    /**
     * The Configs-free half of the auto-collect decision (scope + session
     * dedup + the passed-in DonutSMP signal), split out the same way
     * isHighlightEligible is split from shouldHighlight so it is unit
     * testable without a live Fabric Loader or UploadSession/Minecraft
     * instance. Package-private for tests.
     */
    boolean isAutoCollectEligible(int mapId, boolean onDonutSmp) {
        if (!this.scopeAllowedThisTick) return false;
        if (!onDonutSmp) return false;
        return !this.autoCollectedThisSession.contains(mapId);
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
     * gets its own dedicated ScreenHandlerType; a plain chest, an ender chest,
     * and a generic server GUI (e.g. the Auction House) all share the same
     * generic 9xN handler type on the wire and are only told apart by title,
     * which classify() handles from here.
     */
    private static TrackingScope.ContainerKind classifyContainerKind(HandledScreen<?> screen) {
        ScreenHandlerType<?> type = screen.getScreenHandler().getType();
        if (type == ScreenHandlerType.SHULKER_BOX) return TrackingScope.ContainerKind.SHULKER_BOX;
        if (isGenericContainer(type)) return TrackingScope.ContainerKind.GENERIC_CONTAINER;
        return TrackingScope.ContainerKind.OTHER;
    }

    private static boolean isGenericContainer(ScreenHandlerType<?> type) {
        return type == ScreenHandlerType.GENERIC_9X1
            || type == ScreenHandlerType.GENERIC_9X2
            || type == ScreenHandlerType.GENERIC_9X3
            || type == ScreenHandlerType.GENERIC_9X4
            || type == ScreenHandlerType.GENERIC_9X5
            || type == ScreenHandlerType.GENERIC_9X6;
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
