package dev.exdede.donutmaparts.preview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
    /** The wall had this map but took it down (low quality, banned, hidden for review). */
    public static final class RemovedFromWall extends RuntimeException {
        public RemovedFromWall() {
            super("map removed from the wall", null, false, false);
        }
    }

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
     * wall" (remember for a long time) from "try again later". A map the
     * wall took down completes exceptionally with RemovedFromWall.
     */
    public CompletableFuture<Optional<byte[]>> fetchPng(int mapId) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/mod/map-preview/" + Integer.toUnsignedString(mapId)))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
            .thenCompose(resp -> {
                if (resp.statusCode() == 404) return CompletableFuture.completedFuture(Optional.empty());
                if (resp.statusCode() == 410) return CompletableFuture.failedFuture(new RemovedFromWall());
                if (resp.statusCode() / 100 != 2) {
                    return CompletableFuture.failedFuture(new IllegalStateException("map preview HTTP " + resp.statusCode()));
                }
                return CompletableFuture.completedFuture(Optional.of(resp.body()));
            });
    }

    /** One wall art and the pasted map ids that show it now. */
    public record ArtGroup(int imageId, List<Integer> ids) {}

    /** Pasted ids sorted by wall art, biggest group first, plus the ids the wall never saw. */
    public record Split(List<ArtGroup> groups, List<Integer> unknown) {}

    /**
     * Asks the wall which art each map id shows, so a paste covering several
     * arts becomes one tracking group per art. Fails on any non 200, and the
     * caller falls back to keeping the paste as one group.
     */
    public CompletableFuture<Split> splitByArt(List<Integer> ids) {
        JsonArray arr = new JsonArray();
        for (Integer id : ids) arr.add(Integer.toUnsignedLong(id));
        JsonObject body = new JsonObject();
        body.add("ids", arr);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/mod/map-groups"))
            .timeout(Duration.ofSeconds(10))
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            if (resp.statusCode() != 200) throw new IllegalStateException("map groups HTTP " + resp.statusCode());
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            List<ArtGroup> groups = new ArrayList<>();
            for (JsonElement g : root.getAsJsonArray("groups")) {
                JsonObject o = g.getAsJsonObject();
                groups.add(new ArtGroup(o.get("imageId").getAsInt(), ints(o.getAsJsonArray("ids"))));
            }
            return new Split(groups, ints(root.getAsJsonArray("unknown")));
        });
    }

    private static List<Integer> ints(JsonArray arr) {
        List<Integer> out = new ArrayList<>();
        for (JsonElement e : arr) out.add((int) e.getAsLong());
        return out;
    }
}
