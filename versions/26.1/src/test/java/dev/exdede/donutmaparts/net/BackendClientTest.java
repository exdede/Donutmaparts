package dev.exdede.donutmaparts.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import dev.exdede.donutmaparts.queue.MapCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class BackendClientTest {
    HttpServer server;
    String baseUrl;
    AtomicReference<String> lastAuth = new AtomicReference<>();
    AtomicReference<JsonObject> lastBody = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    void respondWith(String path, String json) {
        server.createContext(path, ex -> {
            lastAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] req = ex.getRequestBody().readAllBytes();
            if (req.length > 0) {
                lastBody.set(JsonParser.parseString(new String(req, StandardCharsets.UTF_8)).getAsJsonObject());
            }
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
    }

    @Test
    void handshakeSendsIdentityAndReturnsToken() throws Exception {
        respondWith("/api/mod/handshake", "{\"api_token\":\"" + "a".repeat(64) + "\"}");
        BackendClient client = new BackendClient(baseUrl);
        String token = client.handshake(
            "11111111-2222-3333-4444-555555555555", "ExDeDe", "b".repeat(64), "0.1.0").get();
        assertEquals("a".repeat(64), token);
        JsonObject body = lastBody.get();
        assertEquals("11111111-2222-3333-4444-555555555555", body.get("minecraft_uuid").getAsString());
        assertEquals("ExDeDe", body.get("ign").getAsString());
        assertEquals("b".repeat(64), body.get("hwid_hash").getAsString());
        assertEquals("0.1.0", body.get("mod_version").getAsString());
        assertFalse(body.has("hash"), "must never send a client-side hash");
    }

    @Test
    void statusSendsBearerTokenAndParsesRestricted() throws Exception {
        respondWith("/api/mod/status", "{\"restricted\":true}");
        BackendClient client = new BackendClient(baseUrl);
        boolean restricted = client.status("c".repeat(64), "0.1.0").get();
        assertTrue(restricted);
        assertEquals("Bearer " + "c".repeat(64), lastAuth.get());
    }

    @Test
    void uploadBatchSendsBase64PixelsAndParsesResults() throws Exception {
        respondWith("/api/mod/upload-batch",
            "{\"results\":[{\"minecraft_map_id\":5,\"status\":\"uploaded\"}]}");
        BackendClient client = new BackendClient(baseUrl + "/");
        byte[] pixels = new byte[16384];
        var results = client.uploadBatch("c".repeat(64),
            List.of(new MapCapture(5, "d".repeat(64), pixels)), "Paper").get();
        assertEquals(1, results.size());
        assertEquals(5, results.get(0).mapId());
        assertEquals("uploaded", results.get(0).status());
        JsonObject body = lastBody.get();
        var item = body.getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals(5, item.get("minecraft_map_id").getAsInt());
        assertEquals(21848, item.get("pixel_data").getAsString().length());
        assertEquals("Paper", item.get("server_brand_hint").getAsString());
        assertFalse(item.has("hash"), "must never send a client-side hash");
    }

    @Test
    void non200CompletesExceptionally() throws Exception {
        server.createContext("/api/mod/status", ex -> {
            ex.sendResponseHeaders(401, -1);
            ex.close();
        });
        BackendClient client = new BackendClient(baseUrl);
        assertThrows(Exception.class, () -> client.status("c".repeat(64), "0.1.0").get());
    }

    @Test
    void submitCollectionEventSendsBearerTokenAndMapIdAndResolvesTrueOn200() throws Exception {
        respondWith("/api/mod/collections/add", "{\"ok\":true}");
        BackendClient client = new BackendClient(baseUrl);
        boolean ok = client.submitCollectionEvent("c".repeat(64), 5).get();
        assertTrue(ok);
        assertEquals("Bearer " + "c".repeat(64), lastAuth.get());
        assertEquals(5, lastBody.get().get("minecraft_map_id").getAsInt());
    }

    @Test
    void submitCollectionEventResolvesFalseOnNon200InsteadOfThrowing() throws Exception {
        // 403 (unverified account), 404 (unknown map id) and 429 (rate limited)
        // are all expected, silent-skip outcomes for this feature, not errors,
        // so the future must resolve rather than complete exceptionally like
        // status()/handshake()/uploadBatch() do on a non-200.
        server.createContext("/api/mod/collections/add", ex -> {
            ex.sendResponseHeaders(403, -1);
            ex.close();
        });
        BackendClient client = new BackendClient(baseUrl);
        boolean ok = client.submitCollectionEvent("c".repeat(64), 5).get();
        assertFalse(ok);
    }
}
