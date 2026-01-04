package dev.iLnv_09.mod.modules.impl.player;

import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class AutoFuck extends Module {
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 500, 0, 2000));
    private long lastTime = 0L;
    private boolean sneaking = false;

    public AutoFuck() {
        super("AutoFuck", Module.Category.Player);
        setChinese("自动蹲起");
        this.setDescription("Automatically toggle sneak state with configurable delay");
    }

    @Override
    public void onEnable() {
        this.lastTime = 0L;
        this.sneaking = false;
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.getNetworkHandler() != null && this.sneaking) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((double) (now - this.lastTime) >= this.delay.getValue()) {
            this.sneaking = !this.sneaking;
            ClientCommandC2SPacket.Mode mode =
                    this.sneaking
                            ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
                            : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, mode));
            this.lastTime = now;
        }
    }
}