package combat_tracker.detection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ExternalSuspects {

    private static final long TTL_MS = 5 * 60 * 1000L;

    private static final int MAX_NAMES = 6;

    private static volatile List<String> found = List.of();

    private static volatile long scannedAtMs;

    private static final AtomicBoolean scanning = new AtomicBoolean();

    private ExternalSuspects() {
    }

    public static List<String> current() {
        long now = System.currentTimeMillis();
        boolean stale = scannedAtMs == 0 || now - scannedAtMs > TTL_MS;
        if (stale && scanning.compareAndSet(false, true)) {
            Thread t = new Thread(ExternalSuspects::scan, "combat-tracker-suspects");
            t.setDaemon(true);
            t.start();
        }
        return found;
    }

    private static void scan() {
        try {
            List<String> macros = new ArrayList<>();
            List<String> peripherals = new ArrayList<>();
            ProcessHandle.allProcesses().forEach(p -> match(p, macros, peripherals));
            for (String p : peripherals) {
                if (macros.size() >= MAX_NAMES) {
                    break;
                }
                addOnce(macros, p);
            }
            found = List.copyOf(macros);
        } catch (Throwable t) {
            found = List.of();
        } finally {
            scannedAtMs = System.currentTimeMillis();
            scanning.set(false);
        }
    }

    private static void match(ProcessHandle p, List<String> macros, List<String> peripherals) {
        String command;
        try {
            command = p.info().command().orElse(null);
        } catch (Throwable t) {
            return;
        }
        if (command == null) {
            return;
        }
        String exe = basename(command);
        if (exe.isEmpty()) {
            return;
        }
        String macro = lookup(MACRO_TOOLS, exe);
        if (macro != null) {
            addOnce(macros, macro);
            return;
        }
        String peripheral = lookup(PERIPHERAL_SOFTWARE, exe);
        if (peripheral != null) {
            addOnce(peripherals, peripheral);
        }
    }

    private static String lookup(Map<String, String> table, String exe) {
        for (Map.Entry<String, String> e : table.entrySet()) {
            if (exe.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private static void addOnce(List<String> into, String name) {
        if (into.size() < MAX_NAMES && !into.contains(name)) {
            into.add(name);
        }
    }

    private static String basename(String command) {
        int cut = Math.max(command.lastIndexOf('\\'), command.lastIndexOf('/'));
        String name = (cut < 0 ? command : command.substring(cut + 1)).toLowerCase(Locale.ROOT);
        return name.endsWith(".exe") ? name.substring(0, name.length() - 4) : name;
    }

    private static Map<String, String> table(String... pairs) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            out.put(pairs[i], pairs[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    private static final Map<String, String> MACRO_TOOLS = table(
            "autohotkey", "AutoHotkey",
            "autoit", "AutoIt",
            "autoclick", "an auto-clicker",
            "clickermann", "Clickermann",
            "mouseclick", "a mouse clicker",
            "tinytask", "TinyTask",
            "macrorecorder", "Macro Recorder",
            "macrocreator", "Pulover's Macro Creator",
            "pulovers", "Pulover's Macro Creator",
            "jitbit", "Jitbit Macro Recorder",
            "mouserecorder", "Mouse Recorder",
            "keyboard maestro", "Keyboard Maestro",
            "karabiner", "Karabiner-Elements",
            "xdotool", "xdotool",
            "ydotool", "ydotool",
            "xmousebutton", "X-Mouse Button Control");

    private static final Map<String, String> PERIPHERAL_SOFTWARE = table(
            "lghub", "Logitech G HUB",
            "lcore", "Logitech Gaming Software",
            "logitech", "Logitech software",
            "synapse", "Razer Synapse",
            "icue", "Corsair iCUE",
            "ngenuity", "HyperX NGENUITY",
            "steelseries", "SteelSeries GG",
            "armoury", "ASUS Armoury Crate",
            "gloriouscore", "Glorious CORE",
            "wootility", "Wootility",
            "msicenter", "MSI Center",
            "bloody", "Bloody mouse software",
            "a4tech", "A4Tech software",
            "redragon", "Redragon software");
}
