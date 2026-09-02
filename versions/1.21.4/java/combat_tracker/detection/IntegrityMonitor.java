package combat_tracker.detection;

import combat_tracker.record.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.lang.ref.WeakReference;

public final class IntegrityMonitor {
    private static final int MAX_EVENTS = 200;

    public enum Kind {
        HOTBAR,
        USE,
        ATTACK,
        KEYBIND
    }

    private static final IntegrityMonitor INSTANCE = new IntegrityMonitor();

    public static IntegrityMonitor get() {
        return INSTANCE;
    }

    private final SlotLedger slots = new SlotLedger();

    private WeakReference<LocalPlayer> lastPlayer;

    private int hotbarFlags;
    private int useFlags;
    private int attackFlags;
    private int keybindFlags;

    private IntegrityMonitor() {
    }

    public int hotbarFlags() {
        return hotbarFlags;
    }

    public int useFlags() {
        return useFlags;
    }

    public int attackFlags() {
        return attackFlags;
    }

    public int keybindFlags() {
        return keybindFlags;
    }

    public int totalFlags() {
        return hotbarFlags + useFlags + attackFlags + keybindFlags;
    }

    public void reset() {
        hotbarFlags = 0;
        useFlags = 0;
        attackFlags = 0;
        keybindFlags = 0;
        slots.clear();
        lastPlayer = null;
    }

    public void onSyntheticKeybind() {
        keybindFlags++;
        record(Kind.KEYBIND);
    }

    public void noteSetterCall(int newSlot, InputContext.Source source) {
    }

    public void onSilentSlotPacket() {
        hotbarFlags++;
        record(Kind.HOTBAR);
    }

    public void onUseItem() {
        if (!InputContext.inKeybinds()) {
            useFlags++;
            record(Kind.USE);
        }
    }

    public void onAttack() {
        if (!InputContext.inKeybinds()) {
            attackFlags++;
            record(Kind.ATTACK);
        }
    }

    public void tick(LocalPlayer player) {
    }

    public void checkSlotNow() {
    }

    private void record(Kind kind) {
    }
}
