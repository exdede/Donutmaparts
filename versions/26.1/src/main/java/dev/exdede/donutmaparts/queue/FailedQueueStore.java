package dev.exdede.donutmaparts.queue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Persists FAILED captures across launches so they can be retried
 * next session. JSON array of {mapId, hash, pixelsBase64}.
 */
public class FailedQueueStore {
    private final Path file;
    private final Gson gson = new Gson();

    public FailedQueueStore(Path file) {
        this.file = file;
    }

    public void save(List<MapCapture> items) {
        JsonArray arr = new JsonArray();
        for (MapCapture c : items) {
            JsonObject o = new JsonObject();
            o.addProperty("mapId", c.mapId());
            o.addProperty("hash", c.hashHex());
            o.addProperty("pixelsBase64", Base64.getEncoder().encodeToString(c.pixels()));
            arr.add(o);
        }
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("failed writing " + file, e);
        }
    }

    public List<MapCapture> load() {
        List<MapCapture> out = new ArrayList<>();
        if (!Files.isRegularFile(file)) return out;
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonArray arr = JsonParser.parseString(text).getAsJsonArray();
            for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                out.add(new MapCapture(
                    o.get("mapId").getAsInt(),
                    o.get("hash").getAsString(),
                    Base64.getDecoder().decode(o.get("pixelsBase64").getAsString())));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return out;
    }
}
