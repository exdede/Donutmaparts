package dev.exdede.donutmaparts.tracking;

import java.util.HashSet;
import java.util.Set;

/**
 * Per screen opening bookkeeping for the tracking alerts. Two sets, one purpose
 * each: "alerted" stops a map that stays in view from re-firing the sound and
 * toast every tick, "highlighted" keeps a matched slot glowing even after the
 * ID has been auto removed from the tracked list.
 *
 * The screen token is an Object compared by identity, so this class stays free
 * of Minecraft imports and unit testable.
 */
public final class ScreenAlertLog {
    private Object currentToken;
    private final Set<Integer> alerted = new HashSet<>();
    private final Set<Integer> highlighted = new HashSet<>();

    /** True only the first time this map ID is seen for this screen opening. */
    public boolean shouldAlert(Object screenToken, int mapId) {
        syncToken(screenToken);
        return this.alerted.add(mapId);
    }

    public void noteHighlight(int mapId) {
        this.highlighted.add(mapId);
    }

    public boolean isHighlighted(int mapId) {
        return this.highlighted.contains(mapId);
    }

    /** Resets both sets when the open screen changes. Cheap enough to call per tick. */
    public void syncToken(Object screenToken) {
        if (this.currentToken != screenToken) {
            this.currentToken = screenToken;
            this.alerted.clear();
            this.highlighted.clear();
        }
    }

    public void clear() {
        syncToken(null);
    }
}
