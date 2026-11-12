package dev.exdede.donutmaparts.preview;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Per map id bookkeeping for remote previews, generic over whatever the
 * caller keeps for a loaded picture (a texture id in game, a string in
 * tests). Not thread safe: every call happens on the client thread.
 *
 * Three things it has to get right, since it is consulted every frame for
 * every hovered map:
 *  - never start a second fetch for a map already in flight,
 *  - cap how many fetches run at once, so sweeping the cursor across a full
 *    Auction House page does not fire 45 requests in one go,
 *  - remember misses and failures for a while (misses much longer), so a map
 *    that is not on the wall costs one lookup per session, not one per frame.
 *
 * Loaded entries are evicted least recently used past maxLoaded, handing the
 * value to onEvict so the caller can free the texture.
 */
public final class RemotePreviewCache<T> {
    public enum Status { ABSENT, LOADING, READY, MISSING, FAILED }

    private static final class Entry<T> {
        Status status;
        T value;
        long retryAtMillis;

        Entry(Status status) {
            this.status = status;
        }
    }

    private final int maxLoaded;
    private final int maxInFlight;
    private final long missingRetryMillis;
    private final long failedRetryMillis;
    private final Consumer<T> onEvict;
    private final LinkedHashMap<Integer, Entry<T>> entries = new LinkedHashMap<>(64, 0.75f, true);
    private int inFlight;
    private long generation;

    public RemotePreviewCache(int maxLoaded, int maxInFlight, long missingRetryMillis,
                              long failedRetryMillis, Consumer<T> onEvict) {
        this.maxLoaded = Math.max(1, maxLoaded);
        this.maxInFlight = Math.max(1, maxInFlight);
        this.missingRetryMillis = missingRetryMillis;
        this.failedRetryMillis = failedRetryMillis;
        this.onEvict = onEvict;
    }

    /** Current status, treating an expired miss or failure as ABSENT again. */
    public Status status(int mapId, long nowMillis) {
        Entry<T> e = this.entries.get(mapId);
        if (e == null) return Status.ABSENT;
        if ((e.status == Status.MISSING || e.status == Status.FAILED) && nowMillis >= e.retryAtMillis) {
            return Status.ABSENT;
        }
        return e.status;
    }

    /** The loaded value, or null. Counts as a use for LRU purposes. */
    public T value(int mapId) {
        Entry<T> e = this.entries.get(mapId);
        return e != null && e.status == Status.READY ? e.value : null;
    }

    /**
     * Bumped by clear(). A fetch reports back with the generation it started
     * under, so one orphaned by a disconnect can never land on a newer entry
     * for the same map id or corrupt the in-flight count.
     */
    public long generation() { return this.generation; }

    /**
     * Claims a fetch slot for this map. True means the caller must start a
     * fetch and later report it through complete/missing/failed exactly once,
     * passing the generation() read alongside this call.
     */
    public boolean tryBegin(int mapId, long nowMillis) {
        if (status(mapId, nowMillis) != Status.ABSENT) return false;
        if (this.inFlight >= this.maxInFlight) return false;
        this.entries.put(mapId, new Entry<>(Status.LOADING));
        this.inFlight++;
        return true;
    }

    public void complete(int mapId, long generation, T value) {
        Entry<T> e = finish(mapId, generation);
        if (e == null) {
            // Cleared while in flight (disconnect): nothing owns the value now.
            if (value != null && this.onEvict != null) this.onEvict.accept(value);
            return;
        }
        e.status = Status.READY;
        e.value = value;
        evictOverflow();
    }

    public void missing(int mapId, long generation, long nowMillis) {
        Entry<T> e = finish(mapId, generation);
        if (e == null) return;
        e.status = Status.MISSING;
        e.retryAtMillis = nowMillis + this.missingRetryMillis;
    }

    public void failed(int mapId, long generation, long nowMillis) {
        Entry<T> e = finish(mapId, generation);
        if (e == null) return;
        e.status = Status.FAILED;
        e.retryAtMillis = nowMillis + this.failedRetryMillis;
    }

    /** Drops everything, handing every loaded value to onEvict. In-flight fetches are orphaned. */
    public void clear() {
        for (Entry<T> e : this.entries.values()) {
            if (e.status == Status.READY && e.value != null && this.onEvict != null) this.onEvict.accept(e.value);
        }
        this.entries.clear();
        this.inFlight = 0;
        this.generation++;
    }

    public int inFlight() { return this.inFlight; }

    public int loadedCount() {
        int n = 0;
        for (Entry<T> e : this.entries.values()) if (e.status == Status.READY) n++;
        return n;
    }

    private Entry<T> finish(int mapId, long generation) {
        if (generation != this.generation) return null;
        Entry<T> e = this.entries.get(mapId);
        if (e == null || e.status != Status.LOADING) return null;
        this.inFlight = Math.max(0, this.inFlight - 1);
        return e;
    }

    private void evictOverflow() {
        int loaded = loadedCount();
        Iterator<Map.Entry<Integer, Entry<T>>> it = this.entries.entrySet().iterator();
        while (loaded > this.maxLoaded && it.hasNext()) {
            Entry<T> e = it.next().getValue();
            if (e.status != Status.READY) continue;
            if (e.value != null && this.onEvict != null) this.onEvict.accept(e.value);
            it.remove();
            loaded--;
        }
    }
}
