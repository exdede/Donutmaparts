package dev.exdede.donutmaparts.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.exdede.donutmaparts.queue.MapCapture;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Thin async client for the mod-facing backend API. Pure Java, no
 * Minecraft imports, fully covered by unit tests against a stub server.
 */
public class BackendClient {
    public record ItemResult(int mapId, String status) {}

    private final String baseUrl;
    private final HttpClient http;

    public BackendClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public CompletableFuture<String> handshake(String minecraftUuid, String ign, String hwidHash, String modVersion) {
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", minecraftUuid);
        body.addProperty("ign", ign);
        body.addProperty("hwid_hash", hwidHash);
        body.addProperty("mod_version", modVersion);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/mod/handshake"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        return send(req).thenApply(json -> json.get("api_token").getAsString());
    }

    public CompletableFuture<Boolean> status(String apiToken, String modVersion) {
        String query = "?mod_version=" + URLEncoder.encode(modVersion, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/mod/status" + query))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + apiToken)
            .GET()
            .build();
        return send(req).thenApply(json -> json.get("restricted").getAsBoolean());
    }

    public CompletableFuture<List<ItemResult>> uploadBatch(String apiToken, List<MapCapture> items, String serverBrandHint) {
        JsonArray arr = new JsonArray();
        for (MapCapture c : items) {
            JsonObject o = new JsonObject();
            o.addProperty("minecraft_map_id", c.mapId());
            o.addProperty("pixel_data", Base64.getEncoder().encodeToString(c.pixels()));
            if (serverBrandHint != null && !serverBrandHint.isBlank()) {
                o.addProperty("server_brand_hint", serverBrandHint);
            }
            arr.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("items", arr);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/mod/upload-batch"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiToken)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        return send(req).thenApply(json -> {
            List<ItemResult> out = new ArrayList<>();
            for (var el : json.getAsJsonArray("results")) {
                JsonObject o = el.getAsJsonObject();
                out.add(new ItemResult(o.get("minecraft_map_id").getAsInt(), o.get("status").getAsString()));
            }
            return out;
        });
    }

    /**
     * Submits a single "add this map to my collection" event. Idempotent
     * server-side (INSERT OR IGNORE), so the only outcomes that matter here
     * are success/failure -- the body carries nothing the caller needs.
     * 403 (unverified account), 404 (unknown map id), 429 (rate limited) and
     * any other non-200 are all expected, silent-skip outcomes for this
     * feature rather than errors, so they resolve to false instead of
     * propagating send()'s IllegalStateException.
     */
    public CompletableFuture<Boolean> submitCollectionEvent(String apiToken, int minecraftMapId) {
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_map_id", minecraftMapId);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/mod/collections/add"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiToken)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        return send(req)
            .thenApply(json -> true)
            .exceptionally(e -> false);
    }

    private CompletableFuture<JsonObject> send(HttpRequest req) {
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> {
                if (resp.statusCode() != 200) {
                    throw new IllegalStateException("HTTP " + resp.statusCode() + " from " + req.uri().getPath());
                }
                return JsonParser.parseString(resp.body()).getAsJsonObject();
            });
    }
}
