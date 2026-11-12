package dev.exdede.donutmaparts.queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FailedQueueStoreTest {
    @Test
    void roundTripsItems(@TempDir Path dir) {
        Path file = dir.resolve("failed_queue.json");
        byte[] pixels = new byte[16384];
        pixels[0] = 42;
        MapCapture item = new MapCapture(7, "c".repeat(64), pixels);
        new FailedQueueStore(file).save(List.of(item));
        List<MapCapture> loaded = new FailedQueueStore(file).load();
        assertEquals(1, loaded.size());
        assertEquals(7, loaded.get(0).mapId());
        assertEquals("c".repeat(64), loaded.get(0).hashHex());
        assertEquals(42, loaded.get(0).pixels()[0]);
    }

    @Test
    void loadReturnsEmptyWhenMissingOrCorrupt(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("failed_queue.json");
        assertTrue(new FailedQueueStore(file).load().isEmpty());
        java.nio.file.Files.writeString(file, "not json{{{");
        assertTrue(new FailedQueueStore(file).load().isEmpty());
    }
}
