package combat_tracker.detection;

public final class AlertSchedule {
    static final long FIRST_DELAY_MS = 10_000L;

    static final long[] BACKOFF_MS = { 120_000L, 600_000L };

    public static final int MAX_MESSAGES = 1 + BACKOFF_MS.length;

    private AlertSchedule() {
    }

    public static boolean exhausted(int sent) {
        return sent >= MAX_MESSAGES;
    }

    public static long dueAfter(int sent) {
        if (exhausted(sent)) {
            throw new IllegalStateException("no alert due after " + sent + " sent");
        }
        return sent == 0 ? FIRST_DELAY_MS : BACKOFF_MS[sent - 1];
    }

    public static boolean due(int sent, long since, long now) {
        return due(sent, since, now, false);
    }

    public static boolean due(int sent, long since, long now, boolean newInformation) {
        if (exhausted(sent)) {
            return newInformation && now - since >= FIRST_DELAY_MS;
        }
        return now - since >= dueAfter(sent);
    }
}
