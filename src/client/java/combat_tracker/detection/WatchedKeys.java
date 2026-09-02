package combat_tracker.detection;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public final class WatchedKeys {

    private WatchedKeys() {
    }

    public static boolean watched(KeyMapping mapping) {
        if (mapping == null) {
            return false;
        }
        Options o = options();
        if (o == null) {
            return false;
        }
        if (mapping == o.keyAttack || mapping == o.keyUse
                || mapping == o.keyJump || mapping == o.keySwapOffhand) {
            return true;
        }
        for (KeyMapping slot : o.keyHotbarSlots) {
            if (mapping == slot) {
                return true;
            }
        }
        return false;
    }

    public static boolean boundToWatched(InputConstants.Key key) {
        if (key == null) {
            return false;
        }
        Options o = options();
        if (o == null) {
            return false;
        }
        String name = key.getName();
        if (matches(o.keyAttack, name) || matches(o.keyUse, name)
                || matches(o.keyJump, name) || matches(o.keySwapOffhand, name)) {
            return true;
        }
        for (KeyMapping slot : o.keyHotbarSlots) {
            if (matches(slot, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(KeyMapping mapping, String keyName) {
        return mapping != null && !mapping.isUnbound() && mapping.saveString().equals(keyName);
    }

    private static Options options() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.options;
    }
}
