package dev.exdede.donutmaparts.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UploadQueueTest {
    long now;
    UploadQueue queue;

    @BeforeEach
    void setup() {
        now = 1_000_000L;
        queue = new UploadQueue(() -> now);
    }

    static MapCapture cap(int id) {
        return new MapCapture(id, String.valueOf(id).repeat(1).concat("f".repeat(63)).substring(0, 64), new byte[16384]);
    }

    @Test
    void enqueueSetsQueuedStateAndPosition() {
        queue.enqueue(cap(1));
        queue.enqueue(cap(2));
        assertEquals(CaptureState.QUEUED, queue.stateOf(1));
        assertEquals(1, queue.queuePositionOf(1));
        assertEquals(2, queue.queuePositionOf(2));
        assertEquals(2, queue.pendingCount());
    }

    @Test
    void flushTriggersOnMinItemsOrInterval() {
        assertFalse(queue.shouldFlush(10, 30_000));
        for (int i = 0; i < 10; i++) queue.enqueue(cap(i));
        assertTrue(queue.shouldFlush(10, 30_000));

        UploadQueue q2 = new UploadQueue(() -> now);
        q2.enqueue(cap(99));
        assertFalse(q2.shouldFlush(10, 30_000));
        now += 30_001;
        assertTrue(q2.shouldFlush(10, 30_000));
    }

    @Test
    void takeBatchMarksUploadingAndCapsSize() {
        for (int i = 0; i < 150; i++) queue.enqueue(cap(i));
        List<MapCapture> batch = queue.takeBatch(100);
        assertEquals(100, batch.size());
        assertEquals(CaptureState.UPLOADING, queue.stateOf(0));
        assertEquals(CaptureState.QUEUED, queue.stateOf(120));
    }

    @Test
    void transportSuccessMarksUploaded() {
        queue.enqueue(cap(1));
        List<MapCapture> batch = queue.takeBatch(100);
        queue.onBatchResult(batch, true);
        assertEquals(CaptureState.UPLOADED, queue.stateOf(1));
        assertEquals(0, queue.pendingCount());
    }

    @Test
    void transportFailureBacksOffThenFails() {
        queue.enqueue(cap(1));
        for (long expectedDelay : new long[]{10_000, 30_000, 120_000, 300_000}) {
            List<MapCapture> batch = queue.takeBatch(100);
            assertEquals(1, batch.size());
            queue.onBatchResult(batch, false);
            assertEquals(CaptureState.RETRY, queue.stateOf(1));
            assertTrue(queue.takeBatch(100).isEmpty(), "not retryable before backoff elapses");
            now += expectedDelay + 1;
        }
        List<MapCapture> batch = queue.takeBatch(100);
        queue.onBatchResult(batch, false);
        assertEquals(CaptureState.FAILED, queue.stateOf(1));
        assertEquals(1, queue.drainFailed().size());
        assertEquals(0, queue.pendingCount());
    }
}
