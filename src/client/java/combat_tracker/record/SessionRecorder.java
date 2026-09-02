package combat_tracker.record;

import combat_tracker.detection.FlagOrigin;
import combat_tracker.detection.IntegrityMonitor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SessionRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger("combat_tracker/record");
    private static final Gson CANONICAL = new GsonBuilder().disableHtmlEscaping().create();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter HUMAN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));

    private static final int MAX_EVENTS = 20_000;

    private static final SessionRecorder INSTANCE = new SessionRecorder();

    public static SessionRecorder get() {
        return INSTANCE;
    }

    private boolean recording = false;
    private boolean playerInitiated = false;
    private long startEpochMs = 0;
    private final List<SessionData.JEvent> jumps = new ArrayList<>();
    private final List<SessionData.CEvent> combos = new ArrayList<>();
    private final List<SessionData.SEvent> swings = new ArrayList<>();
    private final List<SessionData.ShieldSwapEvent> shieldSwapEvents = new ArrayList<>();
    private final List<String> opponents = new ArrayList<>();
    private int shieldBreaks;
    private int shieldMisses;
    private Path lastReportPath;
    private static final long SHIELD_SWAP_TIMEOUT_MS = 2_000L;
    private boolean heldSword;
    private long shieldSwapStartMs = -1;
    private boolean shieldBreakDuringSwap;
    private int hits;
    private int criticalHits;
    private int sprintHits;
    private int sweepHits;
    private int weakHits;

    private int baseHotbar;
    private int baseUse;
    private int baseAttack;
    private int baseKeybind;

    private Map<String, Integer> baseSources = Map.of();

    private String serverAddress = null;

    private long pingSumMs = 0;
    private int pingSamples = 0;

    private String playerName = null;
    private String playerUuid = null;

    public boolean isRecording() {
        return recording;
    }

    public boolean isPlayerRecording() {
        return recording && playerInitiated;
    }

    public long startEpochMs() {
        return startEpochMs;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("combat_tracker").resolve("recordings");
    }

    public void toggle() {
        if (isPlayerRecording()) {
            stop();
        } else {
            start();
        }
    }

    public void startAuto() {
        if (recording) {
            return;
        }
        recording = true;
        playerInitiated = false;
        resetSessionState();
    }

    public void start() {
        if (recording) {
            finalizeSession(false, false);
        }
        recording = true;
        playerInitiated = true;
        resetSessionState();
        chat("Recording started", ChatFormatting.GREEN);
    }

    public Path stop() {
        if (!isPlayerRecording()) {
            return null;
        }
        Path html = finalizeSession(true, false);
        recording = false;
        playerInitiated = false;
        if (html != null) {
            lastReportPath = html;
            recordingSavedChat(html);
        } else {
            chat("Failed to save recording", ChatFormatting.RED);
        }
        startAuto();
        return html;
    }

    public void onDisconnect() {
        if (!recording) {
            return;
        }
        finalizeSession(isPlayerRecording(), false);
        recording = false;
        playerInitiated = false;
    }

    public void onClientStopping() {
        if (!recording) {
            return;
        }
        finalizeSession(isPlayerRecording(), true);
        recording = false;
        playerInitiated = false;
    }

    public void recordJump(long deltaMs, String result) {
        if (recording) {
            jumps.add(new SessionData.JEvent(System.currentTimeMillis(), deltaMs, result));
            trim(jumps);
        }
    }

    public void recordCombo(long intervalMs, boolean newCombo) {
        if (recording) {
            combos.add(new SessionData.CEvent(System.currentTimeMillis(), intervalMs, newCombo));
            trim(combos);
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

    public void captureServer() {
        if (!recording) {
            return;
        }
        try {
            var sd = Minecraft.getInstance().getCurrentServer();
            if (sd != null && sd.ip != null && !sd.ip.isEmpty()) {
                serverAddress = sd.ip;
            }
        } catch (Exception ignored) {
        }
    }

    public void recordSwing(double reach, boolean hit, double aimDeg, double offX, double offY, String targetName) {
        if (!recording) {
            return;
        }
        swings.add(new SessionData.SEvent(System.currentTimeMillis(), reach, hit, aimDeg, offX, offY,
                opponentIndex(targetName)));
        trim(swings);
    }

    public void recordShieldBreakerAttempt(boolean brokeShield) {
        if (!recording) {
            return;
        }
        if (brokeShield) {
            shieldBreaks++;
            shieldBreakDuringSwap |= shieldSwapStartMs >= 0;
        } else {
            shieldMisses++;
        }
    }

    public void recordLandedHit(boolean critical, boolean sprint, boolean sweep, boolean weak) {
        if (!recording) {
            return;
        }
        hits++;
        if (critical) criticalHits++;
        if (sprint) sprintHits++;
        if (sweep) sweepHits++;
        if (weak) weakHits++;
    }

    public void noteHeldItem(ItemStack held) {
        if (!recording) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean sword = held.is(ItemTags.SWORDS);
        boolean axe = held.is(ItemTags.AXES);
        if (axe && heldSword) {
            shieldSwapStartMs = now;
            shieldBreakDuringSwap = false;
        } else if (sword && !heldSword && shieldSwapStartMs >= 0) {
            if (shieldBreakDuringSwap && now - shieldSwapStartMs <= SHIELD_SWAP_TIMEOUT_MS) {
                shieldSwapEvents.add(new SessionData.ShieldSwapEvent(now, now - shieldSwapStartMs));
            }
            shieldSwapStartMs = -1;
            shieldBreakDuringSwap = false;
        } else if (!sword && !axe) {
            shieldSwapStartMs = -1;
            shieldBreakDuringSwap = false;
        }
        heldSword = sword;
    }

    private static void trim(List<?> list) {
        while (list.size() > MAX_EVENTS) {
            list.remove(0);
        }
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

    private void resetSessionState() {
        startEpochMs = System.currentTimeMillis();
        jumps.clear();
        combos.clear();
        swings.clear();
        shieldSwapEvents.clear();
        opponents.clear();
        shieldBreaks = 0;
        shieldMisses = 0;
        heldSword = false;
        shieldSwapStartMs = -1;
        shieldBreakDuringSwap = false;
        hits = 0;
        criticalHits = 0;
        sprintHits = 0;
        sweepHits = 0;
        weakHits = 0;
        pingSumMs = 0;
        pingSamples = 0;
        playerName = null;
        playerUuid = null;
        serverAddress = null;
        IntegrityMonitor im = IntegrityMonitor.get();
        baseHotbar = im.hotbarFlags();
        baseUse = im.useFlags();
        baseAttack = im.attackFlags();
        baseKeybind = im.keybindFlags();
        baseSources = snapshotSources(im);
        LocalPlayer self = Minecraft.getInstance().player;
        if (self != null) {
            playerName = self.getName().getString();
            playerUuid = self.getUUID().toString();
        }
        captureServer();
    }

    private Path finalizeSession(boolean keepFiles, boolean blockingSend) {
        if (!recording) {
            return null;
        }
        long endEpochMs = System.currentTimeMillis();
        SessionData data;
        String canonical;
        String html;
        String server = serverAddress;
        try {
            data = buildData(endEpochMs);
            canonical = CANONICAL.toJson(data);
            html = ReportBuilder.build(data);
        } catch (Exception e) {
            LOGGER.error("Failed to build session report", e);
            return null;
        }

        Path htmlPath = null;
        if (keepFiles) {
            try {
                htmlPath = writeFiles(data, canonical, html);
            } catch (Exception e) {
                LOGGER.error("Failed to write recording", e);
            }
        }

        boolean flagged = (data.flagHotbar + data.flagUse + data.flagAttack + data.flagKeybind) > 0;
        if (flagged) {
            ReportUploader.report(data, server, html, canonical, blockingSend);
        }
        return htmlPath;
    }

    private Path writeFiles(SessionData data, String canonical, String html)
            throws Exception {
        Files.createDirectories(dir());

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.of("UTC"))
                .format(Instant.ofEpochMilli(startEpochMs));

        SessionFile fileObj = new SessionFile(canonical);
        Path jsonPath = dir().resolve("session-" + stamp + ".json");
        Files.writeString(jsonPath, PRETTY.toJson(fileObj));

        Path htmlPath = dir().resolve("session-" + stamp + ".html");
        Files.writeString(htmlPath, html);

        return htmlPath;
    }

    private static String detectAccountType() {
        try {
            User user = Minecraft.getInstance().getUser();
            if (user == null) {
                return "Unknown";
            }
            UUID id = user.getProfileId();
            if (id == null) {
                return "Unknown";
            }
            String name = user.getName();
            if (name != null && !name.isEmpty()) {
                UUID offline = UUID.nameUUIDFromBytes(
                        ("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
                if (offline.equals(id)) {
                    return "Cracked";
                }
            }
            return id.version() == 4 ? "Premium" : "Cracked";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static final java.util.Set<String> SYSTEM_MOD_IDS = java.util.Set.of("minecraft", "java", "fabricloader");

    private static List<SessionData.ModEntry> collectMods() {
        List<SessionData.ModEntry> out = new ArrayList<>();
        try {
            for (var container : FabricLoader.getInstance().getAllMods()) {
                var meta = container.getMetadata();
                if (meta == null || SYSTEM_MOD_IDS.contains(meta.getId())) {
                    continue;
                }
                String version = meta.getVersion() == null ? "" : meta.getVersion().getFriendlyString();
                out.add(new SessionData.ModEntry(meta.getName(), version));
            }
            out.sort(java.util.Comparator.comparing(
                    (SessionData.ModEntry m) -> m.name == null ? "" : m.name.toLowerCase(java.util.Locale.ROOT)));
        } catch (Exception e) {
            LOGGER.debug("Could not enumerate mods", e);
        }
        return out;
    }

    private SessionData buildData(long endEpochMs) {
        LocalPlayer p = Minecraft.getInstance().player;

        SessionData d = new SessionData();
        d.mod = "Combat Tracker";
        d.mcVersion = "1.21.11";
        d.mods = collectMods();
        d.player = firstNonNull(playerName, p != null ? p.getName().getString() : null, "unknown");
        d.playerUuid = firstNonNull(playerUuid, p != null ? p.getUUID().toString() : null, "unknown");
        d.startEpochMs = startEpochMs;
        d.endEpochMs = endEpochMs;
        d.startUtc = HUMAN.format(Instant.ofEpochMilli(startEpochMs));
        d.endUtc = HUMAN.format(Instant.ofEpochMilli(endEpochMs));
        d.jumpEvents = new ArrayList<>(jumps);
        d.comboEvents = new ArrayList<>(combos);
        d.swingEvents = new ArrayList<>(swings);
        d.shieldSwapEvents = new ArrayList<>(shieldSwapEvents);
        d.hits = hits;
        d.criticalHits = criticalHits;
        d.sprintHits = sprintHits;
        d.sweepHits = sweepHits;
        d.weakHits = weakHits;
        d.opponents = new ArrayList<>(opponents);
        d.shieldBreaks = shieldBreaks;
        d.shieldMisses = shieldMisses;
        d.pingMs = pingSamples == 0 ? 0 : (double) pingSumMs / pingSamples;
        d.accountType = detectAccountType();

        IntegrityMonitor im = IntegrityMonitor.get();
        d.flagHotbar = sessionDelta(im.hotbarFlags(), baseHotbar);
        d.flagUse = sessionDelta(im.useFlags(), baseUse);
        d.flagAttack = sessionDelta(im.attackFlags(), baseAttack);
        d.flagKeybind = sessionDelta(im.keybindFlags(), baseKeybind);
        d.flagSources = sessionSources(im);
        SessionStats.summarise(d);
        return d;
    }

    private static int sessionDelta(int current, int base) {
        return current >= base ? current - base : Math.max(0, current);
    }

    private static Map<String, Integer> snapshotSources(IntegrityMonitor im) {
        Map<String, Integer> out = new HashMap<>();
        for (IntegrityMonitor.SourceCount c : im.sourceCounts()) {
            out.put(c.key(), c.count());
        }
        return out;
    }

    private List<SessionData.SourceEntry> sessionSources(IntegrityMonitor im) {
        List<SessionData.SourceEntry> out = new ArrayList<>();
        for (IntegrityMonitor.SourceCount c : im.sourceCounts()) {
            int n = sessionDelta(c.count(), baseSources.getOrDefault(c.key(), 0));
            if (n <= 0) {
                continue;
            }
            FlagOrigin o = c.origin();
            out.add(new SessionData.SourceEntry(c.kind(), o.type().name(), o.modId(), o.modName(),
                    o.modVersion(), o.detail(), n, o.suspects()));
        }
        out.sort(Comparator.comparingInt((SessionData.SourceEntry e) -> -e.count)
                .thenComparing(e -> e.kind == null ? "" : e.kind));
        return out;
    }

    private void chat(String msg, ChatFormatting color) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.displayClientMessage(Component.literal("[Combat Tracker] " + msg).withStyle(color), false);
        }
    }

    public boolean openLatestReport() {
        return lastReportPath != null && Files.exists(lastReportPath) && openLocalReport(lastReportPath);
    }

    private static boolean openLocalReport(Path report) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
            java.util.List<String> command = os.contains("win")
                    ? java.util.List.of("rundll32", "url.dll,FileProtocolHandler", report.toString())
                    : os.contains("mac") ? java.util.List.of("open", report.toString())
                    : java.util.List.of("xdg-open", report.toString());
            new ProcessBuilder(command).start();
            return true;
        } catch (Exception e) {
            LOGGER.debug("Could not open local report", e);
            return false;
        }
    }

    private void recordingSavedChat(Path html) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            return;
        }
        Component msg = Component.literal("[Combat Tracker] Recording saved ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("[Open report]").withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenFile(html.toString()))));
        p.displayClientMessage(msg, false);
    }

    private record SessionFile(String canonicalData) {
    }
}
