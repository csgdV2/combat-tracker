package combat_tracker.record;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SessionRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger("combat_tracker/record");
    private static final Gson CANONICAL = new GsonBuilder().disableHtmlEscaping().create();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter HUMAN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));

    private static final SessionRecorder INSTANCE = new SessionRecorder();

    public static SessionRecorder get() {
        return INSTANCE;
    }

    private boolean recording = false;
    private long startEpochMs = 0;
    private final List<SessionData.JEvent> jumps = new ArrayList<>();
    private final List<SessionData.CEvent> combos = new ArrayList<>();
    private final List<SessionData.SEvent> swings = new ArrayList<>();
    private final List<String> opponents = new ArrayList<>();

    private long pingSumMs = 0;
    private int pingSamples = 0;

    private String playerName = null;
    private String playerUuid = null;

    public boolean isRecording() {
        return recording;
    }

    public long startEpochMs() {
        return startEpochMs;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("combat_tracker").resolve("recordings");
    }

    public void toggle() {
        if (recording) {
            stop();
        } else {
            start();
        }
    }

    public void start() {
        recording = true;
        startEpochMs = System.currentTimeMillis();
        jumps.clear();
        combos.clear();
        swings.clear();
        opponents.clear();
        pingSumMs = 0;
        pingSamples = 0;
        playerName = null;
        playerUuid = null;
        LocalPlayer self = Minecraft.getInstance().player;
        if (self != null) {
            playerName = self.getName().getString();
            playerUuid = self.getUUID().toString();
        }
        chat("Recording started", ChatFormatting.GREEN);
    }

    public Path stop() {
        if (!recording) {
            return null;
        }
        recording = false;
        long endEpochMs = System.currentTimeMillis();
        try {
            Path html = write(endEpochMs);
            chat("Recording saved: " + html, ChatFormatting.GREEN);
            openLocalReport(html);
            return html;
        } catch (Exception e) {
            LOGGER.error("Failed to write recording", e);
            chat("Failed to save recording: " + e.getMessage(), ChatFormatting.RED);
            return null;
        }
    }

    public void recordJump(long deltaMs, String result) {
        if (recording) {
            jumps.add(new SessionData.JEvent(System.currentTimeMillis(), deltaMs, result));
        }
    }

    public void recordCombo(long intervalMs, boolean newCombo) {
        if (recording) {
            combos.add(new SessionData.CEvent(System.currentTimeMillis(), intervalMs, newCombo));
        }
    }

    public void samplePing(int roundTripMs) {
        if (recording && roundTripMs > 0) {
            pingSumMs += roundTripMs;
            pingSamples++;
        }
    }

    public void noteIdentity(String name, String uuid) {
        if (recording && name != null && uuid != null) {
            playerName = name;
            playerUuid = uuid;
        }
    }

    public void recordSwing(double reach, boolean hit, double aimDeg, double offX, double offY, String targetName) {
        if (!recording) {
            return;
        }
        swings.add(new SessionData.SEvent(System.currentTimeMillis(), reach, hit, aimDeg, offX, offY,
                opponentIndex(targetName)));
    }

    static String stripFormatting(String in) {
        return in == null ? null : in.replaceAll("§.", "").trim();
    }

    private static String firstNonNull(String a, String b, String fallback) {
        if (a != null) {
            return a;
        }
        return b != null ? b : fallback;
    }

    private int opponentIndex(String rawName) {
        String name = stripFormatting(rawName);
        if (name == null || name.isEmpty()) {
            return -1;
        }
        int i = opponents.indexOf(name);
        if (i >= 0) {
            return i;
        }
        opponents.add(name);
        return opponents.size() - 1;
    }

    private Path write(long endEpochMs) throws Exception {
        Files.createDirectories(dir());

        SessionData data = buildData(endEpochMs);
        String canonical = CANONICAL.toJson(data);

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(startEpochMs));

        SessionFile fileObj = new SessionFile(canonical);
        Path jsonPath = dir().resolve("session-" + stamp + ".json");
        Files.writeString(jsonPath, PRETTY.toJson(fileObj));

        Path htmlPath = dir().resolve("session-" + stamp + ".html");
        Files.writeString(htmlPath, ReportBuilder.build(data));

        return htmlPath;
    }

    private SessionData buildData(long endEpochMs) {
        LocalPlayer p = Minecraft.getInstance().player;

        SessionData d = new SessionData();
        d.mod = "Combat Tracker";
        d.mcVersion = "1.21.11";
        d.player = firstNonNull(playerName, p != null ? p.getName().getString() : null, "unknown");
        d.playerUuid = firstNonNull(playerUuid, p != null ? p.getUUID().toString() : null, "unknown");
        d.startEpochMs = startEpochMs;
        d.endEpochMs = endEpochMs;
        d.startUtc = HUMAN.format(Instant.ofEpochMilli(startEpochMs));
        d.endUtc = HUMAN.format(Instant.ofEpochMilli(endEpochMs));
        d.jumpEvents = new ArrayList<>(jumps);
        d.comboEvents = new ArrayList<>(combos);
        d.swingEvents = new ArrayList<>(swings);
        d.opponents = new ArrayList<>(opponents);
        d.pingMs = pingSamples == 0 ? 0 : (double) pingSumMs / pingSamples;
        SessionStats.summarise(d);
        return d;
    }

    private void chat(String msg, ChatFormatting color) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.sendSystemMessage(Component.literal("[Combat Tracker] " + msg).withStyle(color));
        }
    }

    private static void openLocalReport(Path report) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
            java.util.List<String> command = os.contains("win") ? java.util.List.of("rundll32", "url.dll,FileProtocolHandler", report.toString()) : os.contains("mac") ? java.util.List.of("open", report.toString()) : java.util.List.of("xdg-open", report.toString());
            new ProcessBuilder(command).start();
        } catch (Exception e) { LOGGER.debug("Could not open local report", e); }
    }

    private record SessionFile(String canonicalData) {
    }
}
