package combat_tracker.detection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class LatencyEstimator {
    private static final LatencyEstimator INSTANCE = new LatencyEstimator();

    public static LatencyEstimator get() {
        return INSTANCE;
    }

    private int lastRaw = -1;

    public void sample(Minecraft client) {
        if (client.getConnection() == null || client.player == null) {
            return;
        }
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        if (info == null) {
            return;
        }
        int raw = info.getLatency();
        if (raw > 0) {
            lastRaw = raw;
        }
    }

    public int currentMs() {
        return lastRaw;
    }

    public void reset() {
        lastRaw = -1;
    }
}
