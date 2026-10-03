package dev.exdede.donutmaparts.preview;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RemotePreviewClientTest {
    HttpServer server;
    String baseUrl;
    final AtomicInteger imageHits = new AtomicInteger();

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

    void respond(String path, int status, String contentType, byte[] body) {
        server.createContext(path, ex -> {
            if (ex.getRequestURI().getPath().endsWith("image.png")) imageHits.incrementAndGet();
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) ex.getResponseBody().write(body);
            ex.close();
        });
    }

    void lookup(int mapId, String json) {
        respond("/api/bot/lookup/" + mapId, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void fetchesTheImageOfTheMatchingMap() throws Exception {
        lookup(5655060, "{\"image_id\":1517,\"status\":\"active\",\"minecraft_map_ids\":[5655060,9094847]}");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        respond("/api/maparts/1517/image.png", 200, "image/png", png);
        Optional<byte[]> result = new RemotePreviewClient(baseUrl + "/").fetchPng(5655060).get();
        assertArrayEquals(png, result.orElseThrow());
    }

    @Test
    void lookupFallingBackToAnImageIdIsNotTrusted() throws Exception {
        // The backend answers an unknown map id with the image whose wall id equals it.
        lookup(123, "{\"image_id\":123,\"status\":\"active\",\"minecraft_map_ids\":[6832545]}");
        respond("/api/maparts/123/image.png", 200, "image/png", new byte[]{1});
        assertTrue(new RemotePreviewClient(baseUrl).fetchPng(123).get().isEmpty());
        assertEquals(0, imageHits.get());
    }

    @Test
    void removedImagesAreMissing() throws Exception {
        lookup(7, "{\"image_id\":3,\"status\":\"deleted_forever\",\"minecraft_map_ids\":[7]}");
        assertTrue(new RemotePreviewClient(baseUrl).fetchPng(7).get().isEmpty());
    }

    @Test
    void unknownMapIsMissing() throws Exception {
        respond("/api/bot/lookup/8", 404, "application/json", "{\"error\":\"not found\"}".getBytes(StandardCharsets.UTF_8));
        assertTrue(new RemotePreviewClient(baseUrl).fetchPng(8).get().isEmpty());
    }

    @Test
    void hiddenImageIsMissing() throws Exception {
        lookup(9, "{\"image_id\":4,\"status\":\"active\",\"minecraft_map_ids\":[9]}");
        respond("/api/maparts/4/image.png", 404, "application/json", "{\"error\":\"not found\"}".getBytes(StandardCharsets.UTF_8));
        assertTrue(new RemotePreviewClient(baseUrl).fetchPng(9).get().isEmpty());
    }

    @Test
    void serverErrorsFailRatherThanCountAsMissing() {
        respond("/api/bot/lookup/10", 503, "application/json", "{\"error\":\"maintenance\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> new RemotePreviewClient(baseUrl).fetchPng(10).get());
    }

    @Test
    void garbageLookupBodyIsMissing() {
        assertNull(RemotePreviewClient.imageIdFor("not json", 1));
        assertNull(RemotePreviewClient.imageIdFor("[]", 1));
        assertNull(RemotePreviewClient.imageIdFor("{\"image_id\":1}", 1));
    }
}
