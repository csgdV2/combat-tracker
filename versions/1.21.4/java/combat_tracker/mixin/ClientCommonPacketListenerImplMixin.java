package combat_tracker.mixin;

import combat_tracker.detection.IntegrityMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {

    @Inject(method = "send", at = @At("HEAD"))
    private void combatTracker$onSend(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ServerboundSetCarriedItemPacket carried)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (carried.getSlot() != player.getInventory().selected) {
            IntegrityMonitor.get().onSilentSlotPacket();
        }
    }
}
