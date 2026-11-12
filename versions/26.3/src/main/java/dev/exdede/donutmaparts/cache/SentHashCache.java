package dev.exdede.donutmaparts.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Append-only local record of pixel hashes this client has already sent.
 * One 64-char hex hash per line. Loaded fully into memory at startup.
 */
public class SentHashCache {
    private final Path file;
    private final Set<String> hashes = new HashSet<>();

    public SentHashCache(Path file) {
        this.file = file;
        if (Files.isRegularFile(file)) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) hashes.add(trimmed);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("failed reading " + file, e);
            }
        }
    }

    public synchronized boolean contains(String hashHex) {
        return hashes.contains(hashHex);
    }

    public synchronized void add(String hashHex) {
        if (hashHex == null || hashHex.length() != 64) return;
        if (!hashes.add(hashHex)) return;
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, hashHex + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("failed appending to " + file, e);
        }
    }

    public synchronized int size() {
        return hashes.size();
    }
}
