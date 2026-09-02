package combat_tracker.record;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class SavedRecordings {
    private static final Logger LOGGER = LoggerFactory.getLogger("combat_tracker/record");
    private static final Gson GSON = new Gson();

    private static final int MAX_LISTED = 60;

    private SavedRecordings() {
    }

    public record Entry(Path json, Path html, SessionData data) {

        private static final int NAMES_SHOWN = 2;

        public String label() {
            return when() + "  " + opponents();
        }

        public String tooltip() {
            StringBuilder s = new StringBuilder();
            s.append(data.startUtc == null ? "?" : data.startUtc);
            s.append("\nRecorded by ").append(data.player == null ? "?" : data.player);
            if (!data.opponents.isEmpty()) {
                s.append("\nAgainst ").append(String.join(", ", data.opponents));
            }
            s.append("\n").append(data.swings).append(" swings, ")
                    .append(data.jumpAttempts).append(" jump resets, ")
                    .append(data.comboIntervals).append(" combo hits");
            return s.toString();
        }

        private String when() {
            String w = data.startUtc == null ? "?" : data.startUtc.replace(" UTC", "");
            return w.length() >= 16 ? w.substring(5, 16) : w;
        }

        private String opponents() {
            List<String> names = data.opponents;
            if (names == null || names.isEmpty()) {
                return "no opponents";
            }
            List<String> clean = names.stream().map(SessionRecorder::stripFormatting).toList();
            if (clean.size() <= NAMES_SHOWN) {
                return "vs " + String.join(", ", clean);
            }
            return "vs " + String.join(", ", clean.subList(0, NAMES_SHOWN))
                    + " +" + (clean.size() - NAMES_SHOWN);
        }
    }

    public static List<Entry> list() {
        return list(SessionRecorder.dir());
    }

    public static List<Entry> list(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                Entry e = read(p);
                if (e != null) {
                    out.add(e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not list recordings", e);
            return List.of();
        }
        out.sort(Comparator.comparingLong((Entry e) -> e.data().startEpochMs).reversed());
        return out.size() > MAX_LISTED ? out.subList(0, MAX_LISTED) : out;
    }

    private static Entry read(Path json) {
        try {
            JsonObject root = GSON.fromJson(Files.readString(json), JsonObject.class);
            if (root == null || !root.has("canonicalData")) {
                return null;
            }
            SessionData data = GSON.fromJson(root.get("canonicalData").getAsString(), SessionData.class);
            if (data == null) {
                return null;
            }
            String name = json.getFileName().toString();
            Path html = json.resolveSibling(name.substring(0, name.length() - 5) + ".html");
            return new Entry(json, html, data);
        } catch (Exception e) {
            LOGGER.warn("Skipping unreadable recording {}", json.getFileName(), e);
            return null;
        }
    }
}
