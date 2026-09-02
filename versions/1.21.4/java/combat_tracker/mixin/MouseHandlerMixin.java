package combat_tracker.mixin;

import combat_tracker.detection.ClickTimestamps;
import combat_tracker.detection.InputContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void combatTracker$scrollStart(long window, double xOffset, double yOffset, CallbackInfo ci) {
        InputContext.enterScroll();
    }

    @Inject(method = "onScroll", at = @At("RETURN"))
    private void combatTracker$scrollEnd(long window, double xOffset, double yOffset, CallbackInfo ci) {
        InputContext.exitScroll();
    }

    @Inject(method = "grabMouse", at = @At("HEAD"))
    private void combatTracker$grabStart(CallbackInfo ci) {
        InputContext.enterHousekeeping();
    }

    @Inject(method = "grabMouse", at = @At("RETURN"))
    private void combatTracker$grabEnd(CallbackInfo ci) {
        InputContext.exitHousekeeping();
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void combatTracker$physicalStart(long window, int button, int action, int mods, CallbackInfo ci) {
        InputContext.enterPhysicalInput();
    }

    @Inject(method = "onPress", at = @At("RETURN"))
    private void combatTracker$physicalEnd(long window, int button, int action, int mods, CallbackInfo ci) {
        InputContext.exitPhysicalInput();
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void combatTracker$onButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || action != GLFW.GLFW_PRESS) {
            return;
        }
        MouseHandler self = (MouseHandler) (Object) this;
        if (!self.isMouseGrabbed() || Minecraft.getInstance().screen != null) {
            return;
        }
        ClickTimestamps.record(System.nanoTime());
    }
}
