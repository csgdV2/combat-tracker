package combat_tracker.detection;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class SourceAttribution {

    private static final String SELF_ID = "combat_tracker";

    private static final int MAX_FRAMES = 48;

    private static final int MAX_CACHED_CLASSES = 4096;

    private static final Set<String> NOT_A_MOD = Set.of("minecraft", "java", "fabricloader");

    private static final String[] INFRA = {
            "java.", "javax.", "jdk.", "sun.",
            "org.spongepowered.asm.", "org.objectweb.asm.", "net.fabricmc.loader.",
    };

    private static final String[] REFLECTION = {
            "java.lang.reflect.", "java.lang.invoke.", "jdk.internal.reflect.", "sun.reflect.",
    };

    private static final String[] GAME = {"net.minecraft.", "com.mojang.", "org.lwjgl."};

    private static final String[] GAME_THREADS = {
            "Render thread", "Server thread", "main", "Netty", "Worker-", "IO-Worker-",
            "Downloader", "Realms", "Sound", "Chunk",
    };

    private static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static final Map<String, String> CLASS_TO_MOD = new ConcurrentHashMap<>();

    private static volatile List<Mod> mods;
    private static volatile Map<String, String> idByToken;

    private record Mod(String id, String name, String version, List<Path> roots) {
    }

    private SourceAttribution() {
    }

    public static FlagOrigin attribute() {
        try {
            return WALKER.walk(SourceAttribution::scan);
        } catch (Throwable t) {
            return FlagOrigin.unknown("attribution failed: " + t.getClass().getSimpleName());
        }
    }

    private static FlagOrigin scan(Stream<StackWalker.StackFrame> frames) {
        boolean sawGame = false;
        boolean sawReflection = false;
        String foreign = null;

        for (StackWalker.StackFrame f : frames.limit(MAX_FRAMES).toList()) {
            String cls = f.getDeclaringClass().getName();
            String method = f.getMethodName();

            if (cls.startsWith("combat_tracker.") || method.startsWith("combatTracker$")) {
                continue;
            }
            if (startsWithAny(cls, REFLECTION)) {
                sawReflection = true;
                continue;
            }
            if (startsWithAny(cls, INFRA)) {
                continue;
            }

            Mod byName = modFromMethodName(method);
            if (byName != null) {
                return named(byName, "mixin handler in " + at(cls, method));
            }
            if (startsWithAny(cls, GAME)) {
                sawGame = true;
                continue;
            }

            Mod owner = modOf(cls);
            if (owner != null) {
                return named(owner, at(cls, method));
            }

            if (foreign == null) {
                foreign = at(cls, method);
            }
        }
        return characterise(foreign, sawGame, sawReflection);
    }

    private static FlagOrigin characterise(String foreign, boolean sawGame, boolean sawReflection) {
        if (foreign != null) {
            return FlagOrigin.unknown("not part of any loaded mod: " + foreign);
        }
        String thread = Thread.currentThread().getName();
        if (!startsWithAny(thread, GAME_THREADS)) {
            return FlagOrigin.external("called from thread \"" + thread + "\"",
                    ExternalSuspects.current());
        }
        if (!sawGame) {
            return FlagOrigin.external("no game frame on the stack", ExternalSuspects.current());
        }
        if (sawReflection) {
            return FlagOrigin.unknown("reached by reflection; the caller is not on the stack");
        }
        return FlagOrigin.unknown("nothing outside the game on the stack");
    }

    private static Mod modFromMethodName(String method) {
        if (method.indexOf('$') < 0) {
            return null;
        }
        Map<String, String> tokens = tokens();
        if (tokens.isEmpty()) {
            return null;
        }
        for (String segment : method.split("\\$")) {
            String token = normalise(segment);
            if (token.length() < 3) {
                continue;
            }
            String id = tokens.get(token);
            if (id != null) {
                Mod m = byId(id);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }

    private static Mod modOf(String cls) {
        String cached = CLASS_TO_MOD.get(cls);
        if (cached != null) {
            return cached.isEmpty() ? null : byId(cached);
        }
        String resource = resourceName(cls);
        String found = "";
        for (Mod m : mods()) {
            if (owns(m, resource)) {
                found = m.id();
                break;
            }
        }
        if (CLASS_TO_MOD.size() < MAX_CACHED_CLASSES) {
            CLASS_TO_MOD.put(cls, found);
        }
        return found.isEmpty() ? null : byId(found);
    }

    private static boolean owns(Mod m, String resource) {
        for (Path root : m.roots()) {
            try {
                if (Files.exists(root.resolve(resource))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static String resourceName(String cls) {
        String name = cls;
        int lambda = name.indexOf("$$Lambda");
        if (lambda > 0) {
            name = name.substring(0, lambda);
        }
        int slash = name.indexOf('/');
        if (slash > 0) {
            name = name.substring(0, slash);
        }
        return name.replace('.', '/') + ".class";
    }

    private static FlagOrigin named(Mod m, String detail) {
        return FlagOrigin.mod(m.id(), m.name(), m.version(), detail);
    }

    private static Mod byId(String id) {
        for (Mod m : mods()) {
            if (m.id().equals(id)) {
                return m;
            }
        }
        return null;
    }

    private static List<Mod> mods() {
        List<Mod> m = mods;
        return m != null ? m : load();
    }

    private static Map<String, String> tokens() {
        Map<String, String> t = idByToken;
        if (t == null) {
            load();
            t = idByToken;
        }
        return t == null ? Map.of() : t;
    }

    private static synchronized List<Mod> load() {
        if (mods != null) {
            return mods;
        }
        List<Mod> found = new ArrayList<>();
        Map<String, String> byToken = new HashMap<>();
        try {
            for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
                var meta = c.getMetadata();
                if (meta == null) {
                    continue;
                }
                String id = meta.getId();
                if (id == null || id.isEmpty() || SELF_ID.equals(id) || NOT_A_MOD.contains(id)) {
                    continue;
                }
                String name = meta.getName() == null || meta.getName().isEmpty() ? id : meta.getName();
                String version = meta.getVersion() == null ? "" : meta.getVersion().getFriendlyString();
                found.add(new Mod(id, name, version, c.getRootPaths()));
                byToken.putIfAbsent(normalise(id), id);
            }
        } catch (Throwable t) {
        }
        idByToken = Map.copyOf(byToken);
        mods = List.copyOf(found);
        return mods;
    }

    private static String normalise(String in) {
        StringBuilder out = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); i++) {
            char c = Character.toLowerCase(in.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String at(String cls, String method) {
        String where = cls + "." + method;
        return where.length() <= 120 ? where : where.substring(0, 119) + "…";
    }

    private static boolean startsWithAny(String s, String[] prefixes) {
        for (String p : prefixes) {
            if (s.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
