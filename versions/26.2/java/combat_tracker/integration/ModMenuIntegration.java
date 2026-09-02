package combat_tracker.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import combat_tracker.config.CtConfig;
import combat_tracker.hud.HudRenderer;
import combat_tracker.record.SessionRecorder;
import combat_tracker.screen.ButtonEntry;
import combat_tracker.screen.HudPositionScreen;
import combat_tracker.stats.ComboStatsTracker;
import combat_tracker.stats.StatsTracker;
import combat_tracker.record.SavedRecordings;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            CtConfig config = CtConfig.get();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Combat Tracker"))
                    .setSavingRunnable(() -> {
                        if (config.window.upperBoundMs < config.window.lowerBoundMs) {
                            config.window.upperBoundMs = config.window.lowerBoundMs;
                        }
                        CtConfig.save();
                        StatsTracker.get().save();
                    });

            ConfigEntryBuilder eb = builder.entryBuilder();

            buildGeneral(builder, eb, config);
            buildTiming(builder, eb, config);
            buildRecording(builder, eb);

            return builder.build();
        };
    }

    private static void buildGeneral(ConfigBuilder builder, ConfigEntryBuilder eb, CtConfig config) {
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(eb.startBooleanToggle(Component.literal("Show HUD"), config.hudEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Master switch for the overlay. Also bound to J by default."))
                .setSaveConsumer(v -> config.hudEnabled = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Chat messages"), config.chatEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Print one line per jump-reset attempt (green HIT / red MISS)."))
                .setSaveConsumer(v -> config.chatEnabled = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Compact layout"), config.hudCompact)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Condense the HUD to a few lines instead of the full panel."))
                .setSaveConsumer(v -> config.hudCompact = v)
                .build());

        general.addEntry(eb.startIntSlider(Component.literal("HUD scale"),
                        (int) Math.round(config.hudScale * 100), 50, 200)
                .setDefaultValue(100)
                .setTextGetter(v -> Component.literal(String.format("%.2fx", v / 100.0)))
                .setTooltip(Component.literal("Shrinks or enlarges the whole overlay."))
                .setSaveConsumer(v -> config.hudScale = v / 100.0)
                .build());

        general.addEntry(eb.startIntSlider(Component.literal("Background opacity"), config.hudBgOpacityPct, 0, 100)
                .setDefaultValue(56)
                .setTextGetter(v -> Component.literal(v + "%"))
                .setTooltip(Component.literal("Transparency of the box behind the HUD. 0 draws no box."))
                .setSaveConsumer(v -> config.hudBgOpacityPct = v)
                .build());

        general.addEntry(eb.startSelector(Component.literal("Theme"),
                        HudRenderer.THEME_NAMES, themeName(config.hudThemeIndex))
                .setDefaultValue(HudRenderer.THEME_NAMES[0])
                .setNameProvider(Component::literal)
                .setTooltip(Component.literal("Accent color for the title and combo line."))
                .setSaveConsumer(v -> config.hudThemeIndex = themeIndex(v))
                .build());

        general.addEntry(new ButtonEntry(Component.literal("Move HUD..."),
                Component.literal("Drag the overlay anywhere on screen."),
                b -> Minecraft.getInstance().gui.setScreen(
                        new HudPositionScreen(Minecraft.getInstance().gui.screen()))));
    }

    private static String themeName(int index) {
        if (index < 0 || index >= HudRenderer.THEME_NAMES.length) {
            return HudRenderer.THEME_NAMES[0];
        }
        return HudRenderer.THEME_NAMES[index];
    }

    private static int themeIndex(String name) {
        for (int i = 0; i < HudRenderer.THEME_NAMES.length; i++) {
            if (HudRenderer.THEME_NAMES[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }

    private static void buildTiming(ConfigBuilder builder, ConfigEntryBuilder eb, CtConfig config) {
        ConfigCategory timing = builder.getOrCreateCategory(Component.literal("Timing"));

        timing.addEntry(eb.startLongSlider(Component.literal("Success window min"),
                        config.window.lowerBoundMs, 0, 600)
                .setDefaultValue(0)
                .setTextGetter(v -> Component.literal(v + " ms"))
                .setTooltip(Component.literal("Deltas below this count as MISS - too early."))
                .setSaveConsumer(v -> config.window.lowerBoundMs = v)
                .build());

        timing.addEntry(eb.startLongSlider(Component.literal("Success window max"),
                        config.window.upperBoundMs, 0, 600)
                .setDefaultValue(80)
                .setTextGetter(v -> Component.literal(v + " ms"))
                .setTooltip(Component.literal("Deltas above this count as MISS - too late."))
                .setSaveConsumer(v -> config.window.upperBoundMs = v)
                .build());

        timing.addEntry(eb.startTextDescription(Component.literal(
                        "Detection tuning is fixed and not adjustable. Knockback threshold and the post-hit "
                                + "tick windows are constants, and ping compensation is measured automatically "
                                + "from your latency to the server. A report is only worth something if every "
                                + "copy of the mod measured the same way."))
                .build());
    }

    private static void buildRecording(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory recording = builder.getOrCreateCategory(Component.literal("Recording"));

        recording.addEntry(eb.startTextDescription(Component.literal(
                        "Records every jump-reset attempt and combo interval, then writes an HTML report "
                                + "plus its JSON data to config/combat_tracker/recordings/."))
                .build());

        recording.addEntry(new ButtonEntry(recordLabel(), b -> {
            SessionRecorder.get().toggle();
            b.setMessage(recordLabel());
        }));

        recording.addEntry(new ButtonEntry(Component.literal("Open recordings folder"), b -> openRecordings()));

        savedRecordings(eb, recording);

        recording.addEntry(new ButtonEntry(Component.literal("Reset stats"),
                Component.literal("Clears both jump-reset and combo statistics. Click twice to confirm."),
                new ResetHandler()));
    }

    private static void savedRecordings(ConfigEntryBuilder eb, ConfigCategory recording) {
        List<SavedRecordings.Entry> saved = SavedRecordings.list();
        if (saved.isEmpty()) {
            recording.addEntry(eb.startTextDescription(Component.literal(
                            "No saved recordings yet. Stop a recording and it will appear here."))
                    .build());
            return;
        }

        List<AbstractConfigListEntry> rows = new ArrayList<>();
        for (SavedRecordings.Entry e : saved) {
            rows.add(new ButtonEntry(Component.literal(e.label()),
                    Component.literal(e.tooltip() + "\n\nClick to open the local recordings folder."),
                    b -> openRecordings()));
        }
        recording.addEntry(eb.startSubCategory(
                        Component.literal("Saved recordings (" + saved.size() + ")"), rows)
                .setTooltip(Component.literal("Every session on disk. Click one to open the local recordings folder."))
                .build());
    }

    private static Component recordLabel() {
        return SessionRecorder.get().isRecording()
                ? Component.literal("Stop Recording").withStyle(ChatFormatting.RED)
                : Component.literal("Start Recording").withStyle(ChatFormatting.GREEN);
    }

    private static void openRecordings() {
        try {
            Files.createDirectories(SessionRecorder.dir());
            openInFileManager(SessionRecorder.dir());
        } catch (Exception ignored) {
        }
    }

    private static void openInFileManager(java.nio.file.Path dir) throws java.io.IOException {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> cmd;
        if (os.contains("win")) {
            cmd = java.util.List.of("rundll32", "url.dll,FileProtocolHandler", dir.toString());
        } else if (os.contains("mac")) {
            cmd = java.util.List.of("open", dir.toString());
        } else {
            cmd = java.util.List.of("xdg-open", dir.toString());
        }
        new ProcessBuilder(cmd).start();
    }

    private static final class ResetHandler implements java.util.function.Consumer<Button> {
        private boolean confirming = false;

        @Override
        public void accept(Button button) {
            if (!confirming) {
                confirming = true;
                button.setMessage(Component.literal("Click again to confirm").withStyle(ChatFormatting.RED));
                return;
            }
            StatsTracker.get().reset();
            StatsTracker.get().save();
            ComboStatsTracker.get().reset();
            ComboStatsTracker.get().save();
            confirming = false;
            button.setMessage(Component.literal("Stats cleared").withStyle(ChatFormatting.GREEN));
        }
    }
}
