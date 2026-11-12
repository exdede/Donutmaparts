package dev.exdede.donutmaparts.queue;

public enum CaptureState {
    DISCOVERED, RENDERING, READY, QUEUED, UPLOADING, UPLOADED, RETRY, FAILED, DUPLICATE, BANNED
}
