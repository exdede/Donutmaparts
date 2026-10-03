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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches a map's picture from the DonutMaparts wall when the server never
 * sent the client its pixels, which is the normal case for Auction House
 * listings: vanilla only streams map data for maps in your own inventory or
 * in item frames near you.
 *
 * Two public, unauthenticated reads: the map id lookup, then the image. The
 * lookup falls back to treating the number as a wall image id when no map
 * instance matches, so the reply's minecraft_map_ids is checked before
 * trusting it, otherwise an unknown map would preview some unrelated art.
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
     * Completes with the PNG bytes, or empty when the wall has no visible
     * picture for this map. Completes exceptionally only on transport or
     * server errors, so the caller can tell "not on the wall" (cache for a
     * long time) from "network hiccup" (retry soon).
     */
    public CompletableFuture<Optional<byte[]>> fetchPng(int mapId) {
        HttpRequest lookup = HttpRequest.newBuilder(URI.create(baseUrl + "/api/bot/lookup/" + mapId))
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/json")
            .GET()
            .build();
        return http.sendAsync(lookup, HttpResponse.BodyHandlers.ofString())
            .thenCompose(resp -> {
                if (resp.statusCode() == 404) return CompletableFuture.completedFuture(Optional.empty());
                if (resp.statusCode() / 100 != 2) {
                    return CompletableFuture.failedFuture(new IllegalStateException("lookup HTTP " + resp.statusCode()));
                }
                Integer imageId = imageIdFor(resp.body(), mapId);
                if (imageId == null) return CompletableFuture.completedFuture(Optional.empty());
                return fetchImage(imageId);
            });
    }

    private CompletableFuture<Optional<byte[]>> fetchImage(int imageId) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/maparts/" + imageId + "/image.png"))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
            .thenCompose(resp -> {
                if (resp.statusCode() == 404) return CompletableFuture.completedFuture(Optional.empty());
                if (resp.statusCode() / 100 != 2) {
                    return CompletableFuture.failedFuture(new IllegalStateException("image HTTP " + resp.statusCode()));
                }
                return CompletableFuture.completedFuture(Optional.of(resp.body()));
            });
    }

    /**
     * The wall image id for this map, or null when the lookup reply is not
     * really about this map (fallback match on image id, or removed). Whether
     * a pending-review image may be shown is left to the image route, which
     * 404s for those. Package-private for tests.
     */
    static Integer imageIdFor(String body, int mapId) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) return null;
            JsonObject json = root.getAsJsonObject();
            if (!json.has("image_id") || !json.has("minecraft_map_ids")) return null;
            if (json.has("status") && !"active".equals(json.get("status").getAsString())) return null;
            JsonArray ids = json.getAsJsonArray("minecraft_map_ids");
            for (JsonElement id : ids) {
                if (id.getAsInt() == mapId) return json.get("image_id").getAsInt();
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
