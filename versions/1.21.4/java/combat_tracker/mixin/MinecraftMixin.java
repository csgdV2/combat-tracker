package combat_tracker.mixin;

import combat_tracker.CombatTrackerClient;
import combat_tracker.detection.ClickTimestamps;
import combat_tracker.detection.InputContext;
import combat_tracker.detection.IntegrityMonitor;
import combat_tracker.detection.SwingTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    private static final String START_ATTACK = "Lnet/minecraft/client/Minecraft;startAttack()Z";
    private static final String START_USE = "Lnet/minecraft/client/Minecraft;startUseItem()V";

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = START_ATTACK))
    private void combatTracker$attackKeyStart(CallbackInfo ci) {
        InputContext.enterKeybinds();
    }

    @Inject(method = "handleKeybinds",
            at = @At(value = "INVOKE", target = START_ATTACK, shift = At.Shift.AFTER))
    private void combatTracker$attackKeyEnd(CallbackInfo ci) {
        InputContext.exitKeybinds();
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = START_USE))
    private void combatTracker$useKeyStart(CallbackInfo ci) {
        InputContext.enterKeybinds();
    }

    @Inject(method = "handleKeybinds",
            at = @At(value = "INVOKE", target = START_USE, shift = At.Shift.AFTER))
    private void combatTracker$useKeyEnd(CallbackInfo ci) {
        InputContext.exitKeybinds();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void combatTracker$setScreenStart(Screen screen, CallbackInfo ci) {
        InputContext.enterHousekeeping();
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void combatTracker$setScreenEnd(Screen screen, CallbackInfo ci) {
        InputContext.exitHousekeeping();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void combatTracker$onStartUseItem(CallbackInfo ci) {
        IntegrityMonitor.get().onUseItem();
    }

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void combatTracker$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = (Minecraft) (Object) this;

        IntegrityMonitor.get().onAttack();

        CombatTrackerClient.clickNano = ClickTimestamps.claim();

        LocalPlayer player = client.player;
        if (player == null || client.hitResult == null) {
            return;
        }
        if (client.missTime > 0 || player.isHandsBusy()) {
            return;
        }
        SwingTracker.get().onSwing(client, client.hitResult);
    }
}
