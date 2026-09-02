package combat_tracker.record;

import combat_tracker.detection.ClickTimestamps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

public final class ReportPreview {
    private static final DateTimeFormatter HUMAN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));

    private ReportPreview() {
    }

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of(args.length > 0 ? args[0] : "build/preview");
        int events = args.length > 1 ? Integer.parseInt(args[1]) : 120;
        Files.createDirectories(outDir);

        SessionData d = synthesise(events, 20250726_000000L, 38.0);

        Path html = outDir.resolve("preview-report.html");
        Files.writeString(html, ReportBuilder.build(d));

        System.out.println("report   : " + html.toAbsolutePath());
        System.out.println("swings=" + d.swings + " jumps=" + d.jumpAttempts + " combos=" + d.comboIntervals);

        SessionData tight = synthesise(events, 777L, 6.0);
        Path tightHtml = outDir.resolve("preview-report-tight.html");
        Files.writeString(tightHtml, ReportBuilder.build(tight));
        System.out.println("tight    : " + tightHtml.toAbsolutePath()
                + "  hits " + tight.hitMinMs + "-" + tight.hitMaxMs + " ms, sd " + fmt(tight.hitSdMs));

        SessionData flagged = synthesise(events, 4242L, 22.0);
        flagged.flagAttack = 14;
        flagged.flagKeybind = 7;
        flagged.flagHotbar = 6;
        flagged.flagUse = 3;
        flagged.flagSources = List.of(
                new SessionData.SourceEntry("attack", "MOD", "turboclicker", "Turbo Clicker",
                        "2.4.1", "mixin handler in net.minecraft.client.Minecraft."
                        + "handler$zzz000$turboclicker$onStartAttack", 9, List.of()),
                new SessionData.SourceEntry("keybind", "EXTERNAL", null, null, null,
                        "called from thread \"macro-worker-1\"", 7,
                        List.of("AutoHotkey", "Logitech G HUB")),
                new SessionData.SourceEntry("hotbar", "UNKNOWN", null, null, null,
                        "direct field write (no call on the stack)", 6, List.of()),
                new SessionData.SourceEntry("attack", "EXTERNAL", null, null, null,
                        "no game frame on the stack", 5, List.of("AutoHotkey")),
                new SessionData.SourceEntry("use", "MOD", "turboclicker", "Turbo Clicker",
                        "2.4.1", "com.example.turbo.ClickLoop.tick", 3, List.of()));
        Path flaggedHtml = outDir.resolve("preview-report-flagged.html");
        Files.writeString(flaggedHtml, ReportBuilder.build(flagged));
        System.out.println("flagged  : " + flaggedHtml.toAbsolutePath()
                + "  " + flagged.flagSources.size() + " source rows");

        clickTimestampSelfTest();
    }

    private static void clickTimestampSelfTest() {
        ClickTimestamps.clear();
        check(ClickTimestamps.claim() == 0L, "empty slot yields no click");

        long now = System.nanoTime();
        ClickTimestamps.record(now);
        check(ClickTimestamps.claim() == now, "a fresh press is returned");
        check(ClickTimestamps.claim() == 0L, "a press is claimed at most once");

        ClickTimestamps.record(now - 5_000L * 1_000_000L);
        check(ClickTimestamps.claim() == 0L, "a stale press is refused");

        ClickTimestamps.record(System.nanoTime());
        ClickTimestamps.clear();
        check(ClickTimestamps.claim() == 0L, "clear drops a pending press");

        System.out.println("click pairing: 5 checks passed");
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError("click pairing broken: " + what);
        }
    }

    static String fmt(double v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }

    static SessionData synthesise(int events, long seed, double jumpSpreadMs) {
        Random rng = new Random(seed);
        long start = Instant.parse("2026-07-26T02:00:00Z").toEpochMilli();
        long end = start + events * 900L;

        SessionData d = new SessionData();
        d.mod = "Combat Tracker";
        d.mcVersion = "1.21.11";
        d.player = "csgd";
        d.playerUuid = "6ce8b1a1-0000-4000-8000-0000deadbeef";
        d.startEpochMs = start;
        d.endEpochMs = end;
        d.startUtc = HUMAN.format(Instant.ofEpochMilli(start));
        d.endUtc = HUMAN.format(Instant.ofEpochMilli(end));
        d.opponents = List.of("Notch", "jeb_");
        d.pingMs = 118;
        d.accountType = "Premium";
        d.mods = List.of(
                new SessionData.ModEntry("Cloth Config API", "15.0.140"),
                new SessionData.ModEntry("Combat Tracker", "1.0.0"),
                new SessionData.ModEntry("Fabric API", "0.116.0+1.21.11"),
                new SessionData.ModEntry("Iris Shaders", "1.8.1"),
                new SessionData.ModEntry("Lithium", "0.14.7"),
                new SessionData.ModEntry("Mod Menu", "11.0.3"),
                new SessionData.ModEntry("Sodium", "0.6.9"));

        for (int i = 0; i < events; i++) {
            long t = start + i * 900L + rng.nextInt(120);

            long delta = Math.round(45 + rng.nextGaussian() * jumpSpreadMs);
            if (Math.abs(delta) <= 200) {
                String result = delta < 0 ? "TOO_EARLY" : (delta > 80 ? "TOO_LATE" : "SUCCESS");
                d.jumpEvents.add(new SessionData.JEvent(t, delta, result));
            }

            if (i % 2 == 0) {
                long interval = Math.round(640 + rng.nextGaussian() * 55);
                d.comboEvents.add(new SessionData.CEvent(t, Math.max(80, interval), i % 8 == 0));
            }

            boolean hit = rng.nextDouble() < 0.72;
            double reach = hit
                    ? Math.min(3.0, 2.2 + rng.nextGaussian() * 0.35)
                    : 3.0 + Math.abs(rng.nextGaussian()) * 0.6;
            reach = Math.max(0.4, Math.min(5.5, reach));
            double offX = rng.nextGaussian() * 0.16;
            double offY = rng.nextGaussian() * 0.42;
            double aimDeg = Math.toDegrees(Math.atan2(Math.hypot(offX, offY), Math.max(0.5, reach)));
            d.swingEvents.add(new SessionData.SEvent(
                    t, reach, hit, aimDeg, offX, offY, rng.nextInt(d.opponents.size())));
        }

        SessionStats.summarise(d);
        return d;
    }
}
