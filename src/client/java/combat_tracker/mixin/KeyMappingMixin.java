package combat_tracker.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import combat_tracker.detection.InputContext;
import combat_tracker.detection.IntegrityMonitor;
import combat_tracker.detection.WatchedKeys;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "setDown(Z)V", at = @At("HEAD"))
    private void combatTracker$onSetDown(boolean down, CallbackInfo ci) {
        if (down && !InputContext.trustedInput()
                && WatchedKeys.watched((KeyMapping) (Object) this)) {
            IntegrityMonitor.get().onSyntheticKeybind();
        }
    }

    @Inject(method = "click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V", at = @At("HEAD"))
    private static void combatTracker$onClick(InputConstants.Key key, CallbackInfo ci) {
        if (!InputContext.trustedInput() && WatchedKeys.boundToWatched(key)) {
            IntegrityMonitor.get().onSyntheticKeybind();
        }
    }
}
