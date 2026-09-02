package combat_tracker;

import combat_tracker.config.CtConfig;
import combat_tracker.detection.ClickTimestamps;
import combat_tracker.detection.ComboTracker;
import combat_tracker.detection.InputContext;
import combat_tracker.detection.IntegrityMonitor;
import combat_tracker.detection.JumpResetTracker;
import combat_tracker.detection.LatencyEstimator;
import combat_tracker.record.SessionRecorder;
import combat_tracker.stats.ComboStatsTracker;
import combat_tracker.stats.StatsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatTrackerClient implements ClientModInitializer {
    public static final String MOD_ID = "combat_tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "keybinds"));

    public static volatile long jumpNano = 0L;

    public static volatile long hitNano = 0L;

    public static volatile long clickNano = 0L;

    public static long consumeJumpNano() {
        long v = jumpNano;
        jumpNano = 0L;
        return v;
    }

    private final JumpResetTracker tracker = new JumpResetTracker();
    private KeyMapping toggleHudKey;
    private KeyMapping toggleRecordKey;

    @Override
    public void onInitializeClient() {
        CtConfig.get();
        StatsTracker.get();
        ComboStatsTracker.get();

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.combat_tracker.toggle_hud",
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));
        toggleRecordKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.combat_tracker.toggle_record",
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SessionRecorder.get().startAuto());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SessionRecorder.get().onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SessionRecorder.get().onClientStopping());

        LOGGER.info("Combat Tracker initialized (client-side, observational only)");
    }

    private void onEndTick(Minecraft client) {
        while (toggleHudKey.consumeClick()) {
            CtConfig config = CtConfig.get();
            config.hudEnabled = !config.hudEnabled;
            CtConfig.save();
        }
        while (toggleRecordKey.consumeClick()) {
            SessionRecorder.get().toggle();
        }

        LocalPlayer player = client.player;
        if (player == null) {
            ClickTimestamps.clear();
        }
        if (player != null && client.level != null) {
            tracker.tick(client);
            ComboTracker.get().tick(player);
            IntegrityMonitor.get().tick(player);
            SessionRecorder.get().samplePing(LatencyEstimator.get().currentMs());
            SessionRecorder.get().noteIdentity(player.getName().getString(), player.getUUID().toString());
            SessionRecorder.get().noteHeldItem(player.getMainHandItem());
            SessionRecorder.get().captureServer();
        }

        InputContext.resetForTick();
    }
}
