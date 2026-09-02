package combat_tracker.mixin;

import combat_tracker.detection.InputContext;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToggleKeyMapping.class)
public class ToggleKeyMappingMixin {

    @Inject(method = "reset", at = @At("HEAD"))
    private void combatTracker$resetStart(CallbackInfo ci) {
        InputContext.enterHousekeeping();
    }

    @Inject(method = "reset", at = @At("RETURN"))
    private void combatTracker$resetEnd(CallbackInfo ci) {
        InputContext.exitHousekeeping();
    }
}
