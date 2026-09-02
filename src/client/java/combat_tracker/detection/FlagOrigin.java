package combat_tracker.detection;

import java.util.List;

public record FlagOrigin(Type type, String modId, String modName, String modVersion,
                         String detail, List<String> suspects) {

    public enum Type {
        MOD,
        EXTERNAL,
        UNKNOWN
    }

    public static FlagOrigin mod(String id, String name, String version, String detail) {
        return new FlagOrigin(Type.MOD, id, name, version, detail, List.of());
    }

    public static FlagOrigin external(String detail, List<String> suspects) {
        return new FlagOrigin(Type.EXTERNAL, null, null, null, detail,
                suspects == null ? List.of() : List.copyOf(suspects));
    }

    public static FlagOrigin unknown(String detail) {
        return new FlagOrigin(Type.UNKNOWN, null, null, null, detail, List.of());
    }

    public FlagOrigin withSuspects(List<String> replacement) {
        if (replacement == null || replacement.isEmpty() || replacement.equals(suspects)) {
            return this;
        }
        return new FlagOrigin(type, modId, modName, modVersion, detail, List.copyOf(replacement));
    }

    public String key() {
        return type + "|" + (modId == null ? "" : modId) + "|" + (detail == null ? "" : detail);
    }
}
