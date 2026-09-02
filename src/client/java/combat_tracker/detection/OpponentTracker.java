package combat_tracker.detection;

public final class OpponentTracker {
    private static final long FRESH_MS = 15_000L;

    private static final OpponentTracker INSTANCE = new OpponentTracker();

    public static OpponentTracker get() {
        return INSTANCE;
    }

    private String lastName;
    private long lastAtMs;

    private OpponentTracker() {
    }

    public void note(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        lastName = name;
        lastAtMs = System.currentTimeMillis();
    }

    public String recent() {
        if (lastName == null || System.currentTimeMillis() - lastAtMs > FRESH_MS) {
            return null;
        }
        return lastName;
    }

    public void clear() {
        lastName = null;
        lastAtMs = 0L;
    }
}
