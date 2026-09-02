package combat_tracker.detection;

public final class ClickTimestamps {
    private static final long MAX_AGE_NANO = 200L * 1_000_000L;

    private static volatile long pending = 0L;

    private ClickTimestamps() {
    }

    public static void record(long nano) {
        pending = nano;
    }

    public static long claim() {
        long v = pending;
        if (v == 0L) {
            return 0L;
        }
        pending = 0L;
        return (System.nanoTime() - v) > MAX_AGE_NANO ? 0L : v;
    }

    public static void clear() {
        pending = 0L;
    }
}
