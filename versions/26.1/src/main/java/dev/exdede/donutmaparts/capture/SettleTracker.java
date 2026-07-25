package dev.exdede.donutmaparts.capture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Decides when a map's pixels have stopped changing (settled) and are
 * therefore safe to capture. Pure, no Minecraft imports, unit tested.
 *
 * A map is promoted once its pixels have been byte-for-byte unchanged for
 * at least the settle delay AND are not entirely blank (all zero). This
 * replaces the earlier fullRegion/allNonZero gate: it tolerates maparts
 * that legitimately use color index 0, and it will not capture a map that
 * is still streaming in, because any pixel change resets the settle clock.
 */
public class SettleTracker {
    public record Promotion(int mapId, byte[] pixels) {}

    private static final class Entry {
        byte[] pixels;
        long lastChangeMillis;
        Entry(byte[] pixels, long now) {
            this.pixels = pixels;
            this.lastChangeMillis = now;
        }
    }

    private final Map<Integer, Entry> tracked = new HashMap<>();

    public synchronized void observe(int mapId, byte[] pixels, long nowMillis) {
        Entry e = tracked.get(mapId);
        if (e == null) {
            tracked.put(mapId, new Entry(pixels.clone(), nowMillis));
            return;
        }
        if (!Arrays.equals(e.pixels, pixels)) {
            e.pixels = pixels.clone();
            e.lastChangeMillis = nowMillis;
        }
    }

    public synchronized List<Promotion> collectSettled(long nowMillis, long settleDelayMillis) {
        List<Promotion> out = new ArrayList<>();
        Iterator<Map.Entry<Integer, Entry>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Entry> me = it.next();
            Entry e = me.getValue();
            if (nowMillis - e.lastChangeMillis < settleDelayMillis) continue;
            it.remove();
            if (!isAllZero(e.pixels)) {
                out.add(new Promotion(me.getKey(), e.pixels));
            }
        }
        return out;
    }

    public synchronized void forget(int mapId) {
        tracked.remove(mapId);
    }

    public synchronized void clear() {
        tracked.clear();
    }

    public synchronized boolean isTracking(int mapId) {
        return tracked.containsKey(mapId);
    }

    private static boolean isAllZero(byte[] pixels) {
        for (byte b : pixels) {
            if (b != 0) return false;
        }
        return true;
    }
}
