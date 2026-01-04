package dev.iLnv_09.mod.modules.impl.movement;

import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class VClip extends Module {

    public VClip() {
        super("VClip", Category.Movement);
        setChinese("纵向穿墙");
    }

    final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Jump));
    final SliderSetting blocks = add(new SliderSetting("Blocks", 1, 1, 3));
    final BooleanSetting noBlack = add(new BooleanSetting("NoBlack", true));

    public enum Mode {
        Glitch,
        Teleport,
        Jump
    }

    @Override
    public void onUpdate() {
        disable();
        switch (mode.getValue()) {
            case Teleport -> {
                // 计算传送高度: blocks值 + 2 (玩家自身占1格)
                int teleportHeight = blocks.getValueInt() + 2;
                
                // 检测是否需要执行noBlack逻辑
                boolean shouldTeleport = true;
                if (noBlack.getValue()) {
                    // 检测玩家头上方块
                    double playerY = mc.player.getY();
                    int checkY = (int) Math.ceil(playerY) + 1; // 玩家头顶位置
                    
                    // 如果头顶没有方块，则不执行传送
                    if (mc.world.getBlockState(mc.player.getBlockPos().up(checkY - (int)playerY)).isAir()) {
                        shouldTeleport = false;
                    }
                }
                
                if (shouldTeleport) {
                    mc.player.setPosition(mc.player.getX(), mc.player.getY() + teleportHeight, mc.player.getZ());
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
                }
            }
            case Jump -> {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
                //mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false));
                mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            }
            case Glitch -> {
                double posX = mc.player.getX();
                double posY = Math.round(mc.player.getY());
                double posZ = mc.player.getZ();
                boolean onGround = mc.player.isOnGround();

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX,
                        posY,
                        posZ,
                        onGround));

                double halfY = 2 / 400.0;
                posY -= halfY;

                mc.player.setPosition(posX, posY, posZ);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX,
                        posY,
                        posZ,
                        onGround));

                posY -= halfY * 300.0;
                mc.player.setPosition(posX, posY, posZ);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX,
                        posY,
                        posZ,
                        onGround));
            }
        }
    }
}
