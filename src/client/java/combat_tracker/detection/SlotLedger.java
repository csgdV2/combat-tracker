package combat_tracker.detection;

public final class SlotLedger {
    private int last = -1;
    private boolean seeded;

    public void reseed(int slot) {
        last = slot;
        seeded = true;
    }

    public boolean isSeeded() {
        return seeded;
    }

    public int last() {
        return last;
    }

    public void clear() {
        last = -1;
        seeded = false;
    }

    public boolean accountFor(int newSlot, boolean attributed) {
        if (!seeded) {
            reseed(newSlot);
            return false;
        }
        if (newSlot == last) {
            return false;
        }
        last = newSlot;
        return !attributed;
    }

    public boolean observe(int slot) {
        if (!seeded) {
            reseed(slot);
            return false;
        }
        if (slot == last) {
            return false;
        }
        last = slot;
        return true;
    }
}
