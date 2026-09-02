package combat_tracker.stats;

public class Attempt {
    public long deltaMs;
    public boolean success;
    public long timestamp;

    public Attempt() {
    }

    public Attempt(long deltaMs, boolean success, long timestamp) {
        this.deltaMs = deltaMs;
        this.success = success;
        this.timestamp = timestamp;
    }
}
