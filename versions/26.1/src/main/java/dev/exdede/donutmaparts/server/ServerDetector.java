package dev.exdede.donutmaparts.server;

/**
 * Soft signal only. Client-side code can lie about this, the backend
 * treats it as a hint, never as proof (see design doc, Server Verification).
 */
public final class ServerDetector {
    private ServerDetector() {}

    public static boolean isDonutAddress(String address) {
        if (address == null || address.isBlank()) return false;
        String host = address.toLowerCase().trim();
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        return host.equals("donutsmp.net") || host.equals("donutsmp.com")
            || host.endsWith(".donutsmp.net") || host.endsWith(".donutsmp.com");
    }
}
