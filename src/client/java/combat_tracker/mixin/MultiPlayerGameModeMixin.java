package combat_tracker.mixin;

import combat_tracker.detection.ComboTracker;
import combat_tracker.detection.IntegrityMonitor;
import combat_tracker.detection.OpponentTracker;
import combat_tracker.record.SessionRecorder;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void jumpResetTracker$onAttack(Player player, Entity target, CallbackInfo ci) {
        IntegrityMonitor.get().checkSlotNow();
        IntegrityMonitor.get().onAttack();
        if (target instanceof Player hit) {
            OpponentTracker.get().note(hit.getName().getString());
        }
        float charge = player.getAttackStrengthScale(0.5F);
        boolean sprint = player.isSprinting();
        boolean weak = charge < 0.9F;
        boolean critical = charge > 0.9F && player.fallDistance > 0.0F && !player.onGround();
        boolean sweep = charge > 0.9F && !sprint && player.onGround()
                && player.getMainHandItem().is(ItemTags.SWORDS);
        SessionRecorder.get().recordLandedHit(critical, sprint, sweep, weak);
        ComboTracker.get().onAttack(target);
    }
}
