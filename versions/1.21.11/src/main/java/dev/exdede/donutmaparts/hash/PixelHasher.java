package dev.exdede.donutmaparts.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PixelHasher {
    public static final int PIXEL_BYTES = 16384;

    private PixelHasher() {}

    public static String sha256Hex(byte[] pixels) {
        if (pixels == null || pixels.length != PIXEL_BYTES) {
            throw new IllegalArgumentException(
                "expected " + PIXEL_BYTES + " bytes, got " + (pixels == null ? "null" : pixels.length));
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(pixels);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
