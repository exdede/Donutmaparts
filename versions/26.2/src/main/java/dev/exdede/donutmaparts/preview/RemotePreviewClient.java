package dev.exdede.donutmaparts.preview;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches a map's picture from the DonutMaparts wall, only ever used when the
 * server has not sent the client that map's pixels. One request per map:
 * the backend resolves the map id to its most recently captured image
 * itself, so a griefed map shows its griefed state rather than the original
 * art, and answers 404 when that newest capture is not showable.
 *
 * Pure Java with no Minecraft imports, tested against a stub server.
 */
public class RemotePreviewClient {
    private final String baseUrl;
    private final HttpClient http;

    public RemotePreviewClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Completes with the PNG bytes, or empty when the wall has no showable
     * picture for this map. Completes exceptionally on transport errors,
     * server errors and rate limiting, so the caller can tell "not on the
     * wall" (remember for a long time) from "try again later".
     */
    public CompletableFuture<Optional<byte[]>> fetchPng(int mapId) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/mod/map-preview/" + Integer.toUnsignedString(mapId)))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
            .thenCompose(resp -> {
                if (resp.statusCode() == 404) return CompletableFuture.completedFuture(Optional.empty());
                if (resp.statusCode() / 100 != 2) {
                    return CompletableFuture.failedFuture(new IllegalStateException("map preview HTTP " + resp.statusCode()));
                }
                return CompletableFuture.completedFuture(Optional.of(resp.body()));
            });
    }
}
