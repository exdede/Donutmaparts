package dev.exdede.donutmaparts.capture;

import dev.exdede.donutmaparts.cache.SentHashCache;
import dev.exdede.donutmaparts.config.Configs;
import dev.exdede.donutmaparts.debug.DebugLog;
import dev.exdede.donutmaparts.hash.PixelHasher;
import dev.exdede.donutmaparts.queue.CaptureState;
import dev.exdede.donutmaparts.queue.MapCapture;
import dev.exdede.donutmaparts.queue.UploadQueue;
import dev.exdede.donutmaparts.session.UploadSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges map update packets to the upload queue. Completeness is decided
 * by SettleTracker: a map is captured only once its pixels have stopped
 * changing for the configured settle window (and are not blank), which
 * avoids capturing a still-streaming map and works for maparts that use
 * color index 0.
 */
public final class MapCaptureTracker {
    public static MapCaptureTracker INSTANCE;

    private final UploadQueue queue;
    private final SentHashCache sentCache;
    private final SettleTracker settleTracker = new SettleTracker();
    private final Map<Integer, CaptureState> preQueueStates = new ConcurrentHashMap<>();

    public MapCaptureTracker(UploadQueue queue, SentHashCache sentCache) {
        this.queue = queue;
        this.sentCache = sentCache;
    }

    public void onMapUpdate(int mapId, byte[] colorsSnapshot) {
        if (!Configs.General.ENABLED.getBooleanValue()) return;
        if (!UploadSession.INSTANCE.isActive()) return;
        // A non-null queue state means the map is already queued/uploaded/etc,
        // past the capture stage, so ignore further packets for it.
        if (queue.stateOf(mapId) != null) return;
        if (colorsSnapshot == null || colorsSnapshot.length != PixelHasher.PIXEL_BYTES) {
            preQueueStates.put(mapId, CaptureState.DISCOVERED);
            DebugLog.capture("map " + mapId + " discovered, no full color array yet");
            return;
        }
        settleTracker.observe(mapId, colorsSnapshot, System.currentTimeMillis());
        preQueueStates.put(mapId, CaptureState.RENDERING);
        DebugLog.render("map " + mapId + " observed, settling");
    }

    public void tick(long nowMillis, long settleDelayMillis) {
        for (SettleTracker.Promotion p : settleTracker.collectSettled(nowMillis, settleDelayMillis)) {
            int mapId = p.mapId();
            if (queue.stateOf(mapId) != null) continue;
            String hash = PixelHasher.sha256Hex(p.pixels());
            DebugLog.hash("map " + mapId + " settled, hash " + hash.substring(0, 12));
            if (sentCache.contains(hash)) {
                queue.setDisplayState(mapId, CaptureState.DUPLICATE);
                preQueueStates.put(mapId, CaptureState.DUPLICATE);
                DebugLog.cache("map " + mapId + " already sent from this client, skipping");
                continue;
            }
            queue.enqueue(new MapCapture(mapId, hash, p.pixels()));
            DebugLog.queue("map " + mapId + " queued, pending " + queue.pendingCount());
        }
    }

    public void reset() {
        settleTracker.clear();
        preQueueStates.clear();
    }

    public CaptureState displayStateFor(int mapId) {
        CaptureState queueState = queue.stateOf(mapId);
        if (queueState != null) return queueState;
        return preQueueStates.get(mapId);
    }

    public UploadQueue queue() { return queue; }
    public SentHashCache sentCache() { return sentCache; }
}
