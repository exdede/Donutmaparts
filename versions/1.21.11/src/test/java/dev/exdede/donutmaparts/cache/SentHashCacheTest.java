package dev.exdede.donutmaparts.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SentHashCacheTest {
    static final String H1 = "a".repeat(64);
    static final String H2 = "b".repeat(64);

    @Test
    void startsEmptyWhenFileMissing(@TempDir Path dir) {
        SentHashCache cache = new SentHashCache(dir.resolve("sent_hashes.txt"));
        assertEquals(0, cache.size());
        assertFalse(cache.contains(H1));
    }

    @Test
    void addPersistsAndDeduplicates(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("sent_hashes.txt");
        SentHashCache cache = new SentHashCache(file);
        cache.add(H1);
        cache.add(H1);
        cache.add(H2);
        assertTrue(cache.contains(H1));
        assertEquals(2, cache.size());
        assertEquals(List.of(H1, H2), Files.readAllLines(file));
    }

    @Test
    void reloadsFromDisk(@TempDir Path dir) {
        Path file = dir.resolve("sent_hashes.txt");
        new SentHashCache(file).add(H1);
        SentHashCache reloaded = new SentHashCache(file);
        assertTrue(reloaded.contains(H1));
        assertEquals(1, reloaded.size());
    }

    @Test
    void ignoresMalformedHashes(@TempDir Path dir) {
        SentHashCache cache = new SentHashCache(dir.resolve("sent_hashes.txt"));
        cache.add("");
        cache.add(null);
        cache.add("zz");
        assertEquals(0, cache.size());
    }
}
