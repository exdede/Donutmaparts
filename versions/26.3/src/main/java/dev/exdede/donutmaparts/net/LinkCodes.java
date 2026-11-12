package dev.exdede.donutmaparts.net;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure format check for the account link code before it is sent to the
 * backend. The backend is the real authority (schema pattern
 * ^[0-9A-F]{32}$ in routes/mod.ts) and treats a malformed code exactly like
 * an expired or already-used one -- this exists purely as a client-side UX
 * nicety so a typo or a pasted lowercase code shows up immediately instead
 * of after a round trip, matching this mod's pure-core convention (see
 * TrackedIds in the tracking package).
 */
public final class LinkCodes {
    private static final Pattern HEX32 = Pattern.compile("^[0-9A-F]{32}$");

    private LinkCodes() {}

    /**
     * Trims surrounding whitespace and uppercases, then checks the result
     * is exactly 32 hex characters (128 bits -- widened from an original
     * 8-char/32-bit code after a security review flagged it as brute-forceable
     * given the mod-facing rate limit is keyed per handshake identity, and an
     * attacker can mint unlimited identities for free). Returns null for
     * anything else, so callers can treat "invalid format" and "empty input"
     * identically.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim().toUpperCase(Locale.ROOT);
        return HEX32.matcher(trimmed).matches() ? trimmed : null;
    }
}
