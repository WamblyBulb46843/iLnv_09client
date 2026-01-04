package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.PacketEvent;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.combat.Criticals;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class TrueAttackCooldown extends Module {

    public TrueAttackCooldown() {
        super("TrueAttackCD", Category.Misc);
        setChinese("攻击冷却修正");
    }

    @EventHandler
    public void onPacket(PacketEvent.Send event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof HandSwingC2SPacket || packet instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType((PlayerInteractEntityC2SPacket) packet) == Criticals.InteractType.ATTACK) {
            mc.player.resetLastAttackedTicks();
        }
    }
}
