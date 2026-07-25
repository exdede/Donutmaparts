package dev.exdede.donutmaparts.net;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class HwidProviderTest {
    @Test
    void producesStable64CharHex(@TempDir Path dir) {
        String h1 = HwidProvider.hwidHash(dir);
        String h2 = HwidProvider.hwidHash(dir);
        assertEquals(h1, h2);
        assertTrue(h1.matches("^[0-9a-f]{64}$"));
    }

    @Test
    void fallbackFilePersistsAcrossInstances(@TempDir Path dir) throws Exception {
        // Force the fallback path by asking for the fallback id directly.
        String id1 = HwidProvider.fallbackId(dir);
        String id2 = HwidProvider.fallbackId(dir);
        assertEquals(id1, id2);
        assertTrue(Files.isRegularFile(dir.resolve("hwid_fallback.txt")));
    }
}
