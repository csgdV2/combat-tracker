package combat_tracker.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StatsTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("combat_tracker/stats");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static StatsTracker instance;

    private static final int MS_FORMAT = 1;

    public int format = MS_FORMAT;

    public List<Attempt> attempts = new ArrayList<>();

    private transient long successes = 0;
    private transient double sumDelta = 0;
    private transient double sumDeltaSq = 0;
    private transient String lastResult = "—";
    private transient int lastResultColor = 0xFFAAAAAA;

    public static StatsTracker get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path statsFile() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("combat_tracker")
                .resolve("stats.json");
    }

    public static StatsTracker load() {
        StatsTracker s;
        Path file = statsFile();
        try {
            if (Files.exists(file)) {
                s = GSON.fromJson(Files.readString(file), StatsTracker.class);
                if (s == null) {
                    s = new StatsTracker();
                }
            } else {
                s = new StatsTracker();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load stats, starting fresh", e);
            s = new StatsTracker();
        }
        if (s.attempts == null) {
            s.attempts = new ArrayList<>();
        }
        if (s.format > MS_FORMAT && !s.attempts.isEmpty()) {
            LOGGER.info("Clearing {} jump-reset attempts recorded as tick offsets; "
                    + "timing is measured in milliseconds again and the two cannot be combined.",
                    s.attempts.size());
            s.attempts.clear();
        }
        s.format = MS_FORMAT;
        s.recompute();
        return s;
    }

    public void save() {
        try {
            Files.createDirectories(statsFile().getParent());
            Files.writeString(statsFile(), GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.warn("Failed to save stats", e);
        }
    }

    private void recompute() {
        successes = 0;
        sumDelta = 0;
        sumDeltaSq = 0;
        for (Attempt a : attempts) {
            if (a.success) {
                successes++;
            }
            sumDelta += a.deltaMs;
            sumDeltaSq += (double) a.deltaMs * a.deltaMs;
        }
    }

    public void record(long deltaMs, boolean success) {
        attempts.add(new Attempt(deltaMs, success, System.currentTimeMillis()));
        if (success) {
            successes++;
        }
        sumDelta += deltaMs;
        sumDeltaSq += (double) deltaMs * deltaMs;
    }

    public void reset() {
        attempts.clear();
        successes = 0;
        sumDelta = 0;
        sumDeltaSq = 0;
        lastResult = "—";
        lastResultColor = 0xFFAAAAAA;
    }

    public int total() {
        return attempts.size();
    }

    public long hits() {
        return successes;
    }

    public long misses() {
        return total() - successes;
    }

    public double successRate() {
        return total() == 0 ? 0.0 : (successes * 100.0 / total());
    }

    public double averageDelta() {
        return total() == 0 ? 0.0 : sumDelta / total();
    }

    public double stdDev() {
        int n = total();
        if (n == 0) {
            return 0.0;
        }
        double mean = sumDelta / n;
        double variance = (sumDeltaSq / n) - (mean * mean);
        return Math.sqrt(Math.max(0.0, variance));
    }

    public String lastResult() {
        return lastResult;
    }

    public int lastResultColor() {
        return lastResultColor;
    }

    public void setLastResult(String text, int color) {
        this.lastResult = text;
        this.lastResultColor = color;
    }
}
