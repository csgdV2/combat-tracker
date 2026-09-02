package combat_tracker.mixin;

import combat_tracker.detection.InputContext;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void combatTracker$keyPressStart(long window, int action, KeyEvent event, CallbackInfo ci) {
        InputContext.enterPhysicalInput();
    }

    @Inject(method = "keyPress", at = @At("RETURN"))
    private void combatTracker$keyPressEnd(long window, int action, KeyEvent event, CallbackInfo ci) {
        InputContext.exitPhysicalInput();
    }
}
