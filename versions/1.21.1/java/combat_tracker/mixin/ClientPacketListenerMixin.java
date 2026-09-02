package combat_tracker.mixin;

import combat_tracker.detection.InputContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
    private void combatTracker$onHeldSlotStart(ClientboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            InputContext.enterServerSlot();
        }
    }

    @Inject(method = "handleSetCarriedItem", at = @At("RETURN"))
    private void combatTracker$onHeldSlotEnd(ClientboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            InputContext.exitServerSlot();
        }
    }
}
