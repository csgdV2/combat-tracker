package combat_tracker.mixin;

import combat_tracker.CombatTrackerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "jumpFromGround()V", at = @At("HEAD"))
    private void jumpResetTracker$onJump(CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player) {
            CombatTrackerClient.jumpNano = System.nanoTime();
        }
    }

    @Inject(method = "handleDamageEvent(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void jumpResetTracker$onDamageEvent(DamageSource source, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player) {
            CombatTrackerClient.hitNano = System.nanoTime();
        }
    }
}
