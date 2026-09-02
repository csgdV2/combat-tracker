package combat_tracker.mixin;

import combat_tracker.detection.InputContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSetHeldSlot", at = @At("HEAD"))
    private void combatTracker$onHeldSlotStart(ClientboundSetHeldSlotPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            InputContext.enterServerSlot();
        }
    }

    @Inject(method = "handleSetHeldSlot", at = @At("RETURN"))
    private void combatTracker$onHeldSlotEnd(ClientboundSetHeldSlotPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            InputContext.exitServerSlot();
        }
    }
}
