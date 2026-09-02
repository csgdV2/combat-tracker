package combat_tracker.detection;

public final class InputContext {

    public enum Source {
        KEYBIND,
        SCROLL,
        SERVER,
        NONE
    }

    private static int keybindDepth;
    private static int scrollDepth;
    private static int serverSlotDepth;
    private static int physicalDepth;
    private static int housekeepingDepth;

    private InputContext() {
    }

    public static void enterKeybinds() {
        keybindDepth++;
    }

    public static void exitKeybinds() {
        if (keybindDepth > 0) {
            keybindDepth--;
        }
    }

    public static void enterScroll() {
        scrollDepth++;
    }

    public static void exitScroll() {
        if (scrollDepth > 0) {
            scrollDepth--;
        }
    }

    public static void enterServerSlot() {
        serverSlotDepth++;
    }

    public static void exitServerSlot() {
        if (serverSlotDepth > 0) {
            serverSlotDepth--;
        }
    }

    public static void enterPhysicalInput() {
        physicalDepth++;
    }

    public static void exitPhysicalInput() {
        if (physicalDepth > 0) {
            physicalDepth--;
        }
    }

    public static void enterHousekeeping() {
        housekeepingDepth++;
    }

    public static void exitHousekeeping() {
        if (housekeepingDepth > 0) {
            housekeepingDepth--;
        }
    }

    public static boolean trustedInput() {
        return physicalDepth > 0 || housekeepingDepth > 0;
    }

    public static boolean inKeybinds() {
        return keybindDepth > 0;
    }

    public static Source currentSource() {
        if (keybindDepth > 0) {
            return Source.KEYBIND;
        }
        if (scrollDepth > 0) {
            return Source.SCROLL;
        }
        return serverSlotDepth > 0 ? Source.SERVER : Source.NONE;
    }

    public static void resetForTick() {
        keybindDepth = 0;
        scrollDepth = 0;
        serverSlotDepth = 0;
        physicalDepth = 0;
        housekeepingDepth = 0;
    }
}
