package dev.exdede.donutmaparts.preview;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RemotePreviewClientTest {
    HttpServer server;
    String baseUrl;

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

    void respond(String path, int status, byte[] body) {
        server.createContext(path, ex -> {
            ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) ex.getResponseBody().write(body);
            ex.close();
        });
    }

    @Test
    void fetchesThePngInOneRequest() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        respond("/api/mod/map-preview/5655060", 200, png);
        Optional<byte[]> result = new RemotePreviewClient(baseUrl + "/").fetchPng(5655060).get();
        assertArrayEquals(png, result.orElseThrow());
    }

    @Test
    void notFoundIsMissing() throws Exception {
        respond("/api/mod/map-preview/8", 404, "{\"error\":\"not found\"}".getBytes(StandardCharsets.UTF_8));
        assertTrue(new RemotePreviewClient(baseUrl).fetchPng(8).get().isEmpty());
    }

    @Test
    void goneIsRemovedFromTheWall() {
        respond("/api/mod/map-preview/11", 410, "{\"error\":\"removed from the wall\"}".getBytes(StandardCharsets.UTF_8));
        ExecutionException e = assertThrows(ExecutionException.class, () -> new RemotePreviewClient(baseUrl).fetchPng(11).get());
        assertTrue(e.getCause() instanceof RemotePreviewClient.RemovedFromWall);
    }

    @Test
    void rateLimitingFailsRatherThanCountsAsMissing() {
        respond("/api/mod/map-preview/9", 429, "{\"error\":\"rate limited\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> new RemotePreviewClient(baseUrl).fetchPng(9).get());
    }

    @Test
    void serverErrorsFail() {
        respond("/api/mod/map-preview/10", 503, "{\"error\":\"maintenance\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> new RemotePreviewClient(baseUrl).fetchPng(10).get());
    }

    @Test
    void splitsIdsByArt() throws Exception {
        respond("/api/mod/map-groups", 200,
            "{\"groups\":[{\"imageId\":1812,\"ids\":[1,2]},{\"imageId\":7,\"ids\":[3]}],\"removed\":[5],\"unknown\":[4]}".getBytes(StandardCharsets.UTF_8));
        RemotePreviewClient.Split split = new RemotePreviewClient(baseUrl).splitByArt(java.util.List.of(1, 2, 3, 4)).get();
        assertEquals(1812, split.groups().get(0).imageId());
        assertEquals(java.util.List.of(1, 2), split.groups().get(0).ids());
        assertEquals(java.util.List.of(5), split.removed());
        assertEquals(java.util.List.of(4), split.unknown());
    }
}
