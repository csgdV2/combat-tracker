package combat_tracker.hud;

import combat_tracker.config.CtConfig;
import combat_tracker.detection.ComboTracker;
import combat_tracker.record.SessionRecorder;
import combat_tracker.stats.ComboStatsTracker;
import combat_tracker.stats.StatsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public class HudRenderer {
    private static final int PADDING = 2;

    public static final int[] THEME_COLORS = {
            0xFFFFFF55, 0xFF55FFFF, 0xFF55FF55, 0xFFFF77FF, 0xFFFFAA00, 0xFFFFFFFF
    };
    public static final String[] THEME_NAMES = {
            "Yellow", "Aqua", "Green", "Pink", "Orange", "White"
    };

    private HudRenderer() {
    }

    private record Line(String text, int color) {
    }

    public static int accentColor() {
        int i = CtConfig.get().hudThemeIndex;
        if (i < 0 || i >= THEME_COLORS.length) {
            i = 0;
        }
        return THEME_COLORS[i];
    }

    private static int bgColor() {
        int alpha = Math.max(0, Math.min(100, CtConfig.get().hudBgOpacityPct)) * 255 / 100;
        return (alpha << 24);
    }

    private static double scale() {
        return Math.max(0.5, Math.min(2.0, CtConfig.get().hudScale));
    }

    public static void render(GuiGraphicsExtractor graphics) {
        CtConfig config = CtConfig.get();
        if (!config.hudEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int w = scaledWidth();
        int h = scaledHeight();
        int x = Math.max(1, Math.min(config.hudX, graphics.guiWidth() - w));
        int y = Math.max(1, Math.min(config.hudY, graphics.guiHeight() - h));
        renderScaledAt(graphics, x, y);
    }

    public static void renderScaledAt(GuiGraphicsExtractor graphics, int x, int y) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale((float) scale());
        drawContent(graphics);
        pose.popMatrix();
    }

    private static void drawContent(GuiGraphicsExtractor graphics) {
        Font font = Minecraft.getInstance().font;
        List<Line> lines = buildLines();
        int lineStep = font.lineHeight + 1;

        graphics.fill(0, 0, unscaledWidth(lines, font), unscaledHeight(lines, font), bgColor());

        int textY = PADDING;
        for (Line line : lines) {
            graphics.text(font, line.text(), PADDING, textY, line.color());
            textY += lineStep;
        }
    }

    public static int scaledWidth() {
        Font font = Minecraft.getInstance().font;
        List<Line> lines = buildLines();
        return (int) Math.ceil(unscaledWidth(lines, font) * scale());
    }

    public static int scaledHeight() {
        Font font = Minecraft.getInstance().font;
        List<Line> lines = buildLines();
        return (int) Math.ceil(unscaledHeight(lines, font) * scale());
    }

    private static int unscaledWidth(List<Line> lines, Font font) {
        int max = 0;
        for (Line line : lines) {
            max = Math.max(max, font.width(line.text()));
        }
        return max + PADDING * 2;
    }

    private static int unscaledHeight(List<Line> lines, Font font) {
        return lines.size() * (font.lineHeight + 1) + PADDING * 2;
    }

    private static List<Line> buildLines() {
        CtConfig cfg = CtConfig.get();
        StatsTracker jr = StatsTracker.get();
        ComboStatsTracker combo = ComboStatsTracker.get();
        int accent = accentColor();
        List<Line> lines = new ArrayList<>();

        SessionRecorder rec = SessionRecorder.get();
        if (rec.isRecording()) {
            long s = Math.max(0, (System.currentTimeMillis() - rec.startEpochMs()) / 1000);
            lines.add(new Line(String.format("● REC %d:%02d", s / 60, s % 60), 0xFFFF5555));
        }

        int comboCount = ComboTracker.get().currentCombo();
        long lastHit = combo.lastInterval();
        double variance = combo.jitter();
        int varColor = varianceColor(variance, combo.count());

        if (cfg.hudCompact) {
            lines.add(new Line(String.format("CT  JR %d/%d  %.0f%%", jr.hits(), jr.misses(), jr.successRate()), accent));
            lines.add(new Line(String.format("Combo %d   Last %dms", comboCount, lastHit), 0xFFFFFFFF));
            lines.add(new Line(String.format("Variance %.0fms", variance), varColor));
        } else {
            lines.add(new Line("Combat Tracker", accent));
            lines.add(new Line(String.format("Jump: %d hit / %d miss", jr.hits(), jr.misses()), 0xFFFFFFFF));
            lines.add(new Line(String.format("Rate %.1f%%  Avg %.0f SD %.0f", jr.successRate(), jr.averageDelta(), jr.stdDev()), 0xFFAAAAAA));
            lines.add(new Line(String.format("Last JR: %s", jr.lastResult()), jr.lastResultColor()));
            lines.add(new Line(String.format("Combo: %d   Last %dms", comboCount, lastHit), accent));
            lines.add(new Line(String.format("Variance %.0fms", variance), varColor));
        }

        return lines;
    }

    private static int varianceColor(double varianceMs, int sampleCount) {
        if (sampleCount < 5) {
            return 0xFFAAAAAA;
        }
        double t = Math.max(0.0, Math.min(1.0, varianceMs / 50.0));
        int gb = (int) Math.round(60 + t * (255 - 60));
        return 0xFF000000 | (255 << 16) | (gb << 8) | gb;
    }
}
