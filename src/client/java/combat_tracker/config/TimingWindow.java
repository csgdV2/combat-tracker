package combat_tracker.config;

public class TimingWindow {
    public long lowerBoundMs = 0L;
    public long upperBoundMs = 80L;

    public enum Result {
        SUCCESS,
        TOO_EARLY,
        TOO_LATE
    }

    public Result classify(long deltaMs) {
        if (deltaMs < lowerBoundMs) {
            return Result.TOO_EARLY;
        }
        if (deltaMs > upperBoundMs) {
            return Result.TOO_LATE;
        }
        return Result.SUCCESS;
    }
}
