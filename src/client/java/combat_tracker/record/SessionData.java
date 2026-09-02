package combat_tracker.record;

import java.util.ArrayList;
import java.util.List;

public final class SessionData {
    public String mod;
    public String mcVersion;
    public List<ModEntry> mods = new ArrayList<>();
    public String player;
    public String playerUuid;
    public String title;
    public long startEpochMs;
    public long endEpochMs;
    public String startUtc;
    public String endUtc;

    public int jumpAttempts;
    public int jumpHits;
    public int jumpMisses;
    public double jumpAvgMs;
    public double jumpSdMs;

    public int comboIntervals;
    public int combos;
    public double comboAvgMs;
    public double comboJitterMs;

    public int swings;
    public int swingHits;
    public int swingMisses;
    public double reachAvgBlocks;
    public double reachMaxBlocks;
    public double reachSdBlocks;
    public double aimAvgDeg;
    public double aimSdDeg;

    public int shieldBreaks;
    public int shieldMisses;
    public double shieldBreakRate;
    public List<ShieldSwapEvent> shieldSwapEvents = new ArrayList<>();

    public int hits;
    public int criticalHits;
    public int sprintHits;
    public int sweepHits;
    public int weakHits;

    public double pingMs;

    public String accountType;

    public double hitAvgMs;
    public double hitSdMs;
    public long hitMinMs;
    public long hitMaxMs;

    public transient int flagHotbar;
    public transient int flagUse;
    public transient int flagAttack;
    public transient int flagKeybind;

    public List<String> opponents = new ArrayList<>();

    public List<JEvent> jumpEvents = new ArrayList<>();
    public List<CEvent> comboEvents = new ArrayList<>();
    public List<SEvent> swingEvents = new ArrayList<>();
    public transient List<FEvent> flagEvents = new ArrayList<>();

    public transient List<SourceEntry> flagSources = new ArrayList<>();

    public static final class SourceEntry {
        public String kind;
        public String type;
        public String modId;
        public String modName;
        public String modVersion;
        public String detail;
        public int count;
        public List<String> suspects = new ArrayList<>();

        public SourceEntry() {
        }

        public SourceEntry(String kind, String type, String modId, String modName,
                           String modVersion, String detail, int count, List<String> suspects) {
            this.kind = kind;
            this.type = type;
            this.modId = modId;
            this.modName = modName;
            this.modVersion = modVersion;
            this.detail = detail;
            this.count = count;
            this.suspects = suspects == null ? new ArrayList<>() : new ArrayList<>(suspects);
        }
    }

    public static final class ModEntry {
        public String name;
        public String version;

        public ModEntry() {
        }

        public ModEntry(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }

    public static final class JEvent {
        public long t;
        public long deltaMs;
        public String result;

        public JEvent() {
        }

        public JEvent(long t, long deltaMs, String result) {
            this.t = t;
            this.deltaMs = deltaMs;
            this.result = result;
        }
    }

    public static final class ShieldSwapEvent {
        public long t;
        public long deltaMs;

        public ShieldSwapEvent() {
        }

        public ShieldSwapEvent(long t, long deltaMs) {
            this.t = t;
            this.deltaMs = deltaMs;
        }
    }

    public static final class CEvent {
        public long t;
        public long intervalMs;
        public boolean newCombo;

        public CEvent() {
        }

        public CEvent(long t, long intervalMs, boolean newCombo) {
            this.t = t;
            this.intervalMs = intervalMs;
            this.newCombo = newCombo;
        }
    }

    public static final class FEvent {
        public long t;
        public int kind;

        public FEvent() {
        }

        public FEvent(long t, int kind) {
            this.t = t;
            this.kind = kind;
        }
    }

    public static final class SEvent {
        public long t;
        public double reach;
        public boolean hit;
        public double aimDeg;
        public double offX;
        public double offY;
        public int target = -1;

        public SEvent() {
        }

        public SEvent(long t, double reach, boolean hit, double aimDeg, double offX, double offY, int target) {
            this.t = t;
            this.reach = reach;
            this.hit = hit;
            this.aimDeg = aimDeg;
            this.offX = offX;
            this.offY = offY;
            this.target = target;
        }
    }
}
