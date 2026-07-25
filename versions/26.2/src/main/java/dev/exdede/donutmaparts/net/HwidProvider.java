package dev.exdede.donutmaparts.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Computes a stable machine identifier hash. Sources, in order:
 * Linux /etc/machine-id (or /var/lib/dbus/machine-id),
 * Windows registry MachineGuid, macOS IOPlatformUUID.
 * Falls back to a random UUID persisted in the config dir, so the
 * value is at least stable per install when no OS id is readable.
 */
public final class HwidProvider {
    private HwidProvider() {}

    public static String hwidHash(Path configDir) {
        String raw = readMachineId();
        if (raw == null || raw.isBlank()) raw = fallbackId(configDir);
        return sha256Hex(raw.trim().getBytes(StandardCharsets.UTF_8));
    }

    static String readMachineId() {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("linux")) {
                for (String p : new String[]{"/etc/machine-id", "/var/lib/dbus/machine-id"}) {
                    Path path = Path.of(p);
                    if (Files.isRegularFile(path)) {
                        String s = Files.readString(path).trim();
                        if (!s.isEmpty()) return s;
                    }
                }
            } else if (os.contains("windows")) {
                String out = exec("reg", "query",
                    "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid");
                if (out != null) {
                    for (String line : out.split("\\R")) {
                        if (line.contains("MachineGuid")) {
                            String[] parts = line.trim().split("\\s+");
                            return parts[parts.length - 1];
                        }
                    }
                }
            } else if (os.contains("mac")) {
                String out = exec("ioreg", "-rd1", "-c", "IOPlatformExpertDevice");
                if (out != null) {
                    for (String line : out.split("\\R")) {
                        if (line.contains("IOPlatformUUID")) {
                            int q = line.lastIndexOf('"');
                            int q2 = line.lastIndexOf('"', q - 1);
                            if (q2 >= 0 && q > q2) return line.substring(q2 + 1, q);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String exec(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    static String fallbackId(Path configDir) {
        Path file = configDir.resolve("hwid_fallback.txt");
        try {
            if (Files.isRegularFile(file)) {
                String s = Files.readString(file).trim();
                if (!s.isEmpty()) return s;
            }
            String fresh = UUID.randomUUID().toString();
            Files.createDirectories(configDir);
            Files.writeString(file, fresh);
            return fresh;
        } catch (IOException e) {
            // Last resort, unstable but never crashes the mod.
            return UUID.randomUUID().toString();
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
