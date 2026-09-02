package combat_tracker.mixin;

import combat_tracker.detection.InputContext;
import combat_tracker.detection.IntegrityMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Inject(method = "setSelectedSlot(I)V", at = @At("HEAD"))
    private void combatTracker$onSetSelectedSlot(int slot, CallbackInfo ci) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || self.getInventory() != (Object) this) {
            return;
        }
        IntegrityMonitor.get().noteSetterCall(slot, InputContext.currentSource());
    }
}
