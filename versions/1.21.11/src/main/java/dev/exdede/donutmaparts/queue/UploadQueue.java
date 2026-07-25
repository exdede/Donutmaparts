package dev.exdede.donutmaparts.queue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Pure state machine for the upload pipeline. Not thread safe by itself,
 * callers synchronize (the mod uses it from the client thread plus HTTP
 * callbacks, so all public methods are synchronized).
 */
public class UploadQueue {
    public static final long[] BACKOFF_MILLIS = {10_000, 30_000, 120_000, 300_000};

    private final LongSupplier clockMillis;
    private final Deque<MapCapture> queued = new ArrayDeque<>();
    private final Map<Integer, CaptureState> states = new HashMap<>();
    private final Map<Integer, Integer> attempts = new HashMap<>();
    private final List<MapCapture> retryPool = new ArrayList<>();
    private final List<MapCapture> failed = new ArrayList<>();
    private long lastFlushMillis;
    private long retryNotBeforeMillis;

    public UploadQueue(LongSupplier clockMillis) {
        this.clockMillis = clockMillis;
        this.lastFlushMillis = clockMillis.getAsLong();
    }

    public synchronized void enqueue(MapCapture c) {
        queued.addLast(c);
        states.put(c.mapId(), CaptureState.QUEUED);
    }

    public synchronized boolean shouldFlush(int minItems, long intervalMillis) {
        if (pendingReady() == 0) return false;
        if (pendingReady() >= minItems) return true;
        return clockMillis.getAsLong() - lastFlushMillis >= intervalMillis;
    }

    private int pendingReady() {
        int retryReady = clockMillis.getAsLong() >= retryNotBeforeMillis ? retryPool.size() : 0;
        return queued.size() + retryReady;
    }

    public synchronized List<MapCapture> takeBatch(int max) {
        List<MapCapture> batch = new ArrayList<>();
        if (clockMillis.getAsLong() >= retryNotBeforeMillis && !retryPool.isEmpty()) {
            for (MapCapture c : retryPool) {
                if (batch.size() >= max) break;
                batch.add(c);
            }
            retryPool.removeAll(batch);
        }
        while (batch.size() < max && !queued.isEmpty()) {
            batch.add(queued.pollFirst());
        }
        for (MapCapture c : batch) states.put(c.mapId(), CaptureState.UPLOADING);
        if (!batch.isEmpty()) lastFlushMillis = clockMillis.getAsLong();
        return batch;
    }

    public synchronized void onBatchResult(List<MapCapture> batch, boolean transportOk) {
        if (transportOk) {
            for (MapCapture c : batch) {
                states.put(c.mapId(), CaptureState.UPLOADED);
                attempts.remove(c.mapId());
            }
            return;
        }
        long maxDelay = 0;
        for (MapCapture c : batch) {
            int attempt = attempts.merge(c.mapId(), 1, Integer::sum);
            if (attempt > BACKOFF_MILLIS.length) {
                states.put(c.mapId(), CaptureState.FAILED);
                failed.add(c);
            } else {
                states.put(c.mapId(), CaptureState.RETRY);
                retryPool.add(c);
                maxDelay = Math.max(maxDelay, BACKOFF_MILLIS[attempt - 1]);
            }
        }
        if (maxDelay > 0) {
            retryNotBeforeMillis = clockMillis.getAsLong() + maxDelay;
        }
    }

    public synchronized void setDisplayState(int mapId, CaptureState state) {
        states.put(mapId, state);
    }

    public synchronized CaptureState stateOf(int mapId) {
        return states.get(mapId);
    }

    public synchronized Integer queuePositionOf(int mapId) {
        int pos = 1;
        for (MapCapture c : queued) {
            if (c.mapId() == mapId) return pos;
            pos++;
        }
        return null;
    }

    public synchronized int pendingCount() {
        return queued.size() + retryPool.size();
    }

    public synchronized List<MapCapture> drainFailed() {
        List<MapCapture> out = new ArrayList<>(failed);
        failed.clear();
        return out;
    }
}
