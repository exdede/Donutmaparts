package dev.exdede.donutmaparts.preview;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Holds off the wall fallback for a map until it has gone without local data
 * for a little while. When a container opens, the server's map packets often
 * arrive a moment after the items do; asking the wall in that gap would both
 * flash a possibly outdated picture and send a request that was never needed.
 *
 * A map counts as wanted from the first time it is hovered without local
 * data; entries not touched for a minute are forgotten, so hovering the same
 * map again much later starts a fresh wait. Pure Java, client thread only.
 */
public final class FallbackDelay {
    private static final long FORGET_AFTER_MILLIS = 60_000L;

    private final Map<Integer, long[]> wanted = new HashMap<>();
    private long lastPruneMillis;

    /** True once this map has been wanted for at least delayMillis. */
    public boolean ready(int mapId, long nowMillis, long delayMillis) {
        prune(nowMillis);
        long[] times = this.wanted.get(mapId);
        if (times == null) {
            this.wanted.put(mapId, new long[] {nowMillis, nowMillis});
            return delayMillis <= 0;
        }
        times[1] = nowMillis;
        return nowMillis - times[0] >= delayMillis;
    }

    public void clear() {
        this.wanted.clear();
    }

    private void prune(long nowMillis) {
        if (nowMillis - this.lastPruneMillis < 5_000L) return;
        this.lastPruneMillis = nowMillis;
        Iterator<long[]> it = this.wanted.values().iterator();
        while (it.hasNext()) {
            if (nowMillis - it.next()[1] > FORGET_AFTER_MILLIS) it.remove();
        }
    }
}
