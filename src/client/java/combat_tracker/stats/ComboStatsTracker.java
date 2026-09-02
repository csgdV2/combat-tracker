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

public class ComboStatsTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("combat_tracker/combo");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ComboStatsTracker instance;

    public List<Long> intervals = new ArrayList<>();
    public int combos = 0;

    private transient double sumMs = 0;
    private transient double sumMsSq = 0;
    private transient long lastIntervalMs = 0;

    public static ComboStatsTracker get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("combat_tracker")
                .resolve("combo_stats.json");
    }

    public static ComboStatsTracker load() {
        ComboStatsTracker s;
        try {
            Path f = file();
            s = Files.exists(f) ? GSON.fromJson(Files.readString(f), ComboStatsTracker.class) : new ComboStatsTracker();
            if (s == null) {
                s = new ComboStatsTracker();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load combo stats, starting fresh", e);
            s = new ComboStatsTracker();
        }
        if (s.intervals == null) {
            s.intervals = new ArrayList<>();
        }
        s.recompute();
        return s;
    }

    public void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.warn("Failed to save combo stats", e);
        }
    }

    private void recompute() {
        sumMs = 0;
        sumMsSq = 0;
        for (long v : intervals) {
            sumMs += v;
            sumMsSq += (double) v * v;
        }
        lastIntervalMs = intervals.isEmpty() ? 0 : intervals.get(intervals.size() - 1);
    }

    public void record(long intervalMs, boolean newCombo) {
        intervals.add(intervalMs);
        sumMs += intervalMs;
        sumMsSq += (double) intervalMs * intervalMs;
        lastIntervalMs = intervalMs;
        if (newCombo) {
            combos++;
        }
    }

    public void reset() {
        intervals.clear();
        combos = 0;
        sumMs = 0;
        sumMsSq = 0;
        lastIntervalMs = 0;
    }

    public int count() {
        return intervals.size();
    }

    public int combos() {
        return combos;
    }

    public long lastInterval() {
        return lastIntervalMs;
    }

    public double averageInterval() {
        return count() == 0 ? 0.0 : sumMs / count();
    }

    public double jitter() {
        int n = count();
        if (n == 0) {
            return 0.0;
        }
        double mean = sumMs / n;
        double variance = (sumMsSq / n) - (mean * mean);
        return Math.sqrt(Math.max(0.0, variance));
    }
}
