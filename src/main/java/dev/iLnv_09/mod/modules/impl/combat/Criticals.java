package dev.iLnv_09.mod.modules.impl.combat;

import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import io.netty.buffer.Unpooled;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.PacketEvent;
import dev.iLnv_09.api.events.impl.UpdateEvent;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.exploit.Blink;
import dev.iLnv_09.mod.modules.impl.exploit.BowBomb;
import dev.iLnv_09.mod.modules.impl.exploit.PearlPhase;
import dev.iLnv_09.mod.modules.impl.player.AutoPearl;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;

public class Criticals extends Module {
    public static Criticals INSTANCE;

    public Criticals() {
        super("Criticals", Category.Combat);
        setChinese("刀刀暴击");
        INSTANCE = this;
    }

    // 模式设置
    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.OldNCP));

    // 通用设置
    private final BooleanSetting onlyGround = add(new BooleanSetting("OnlyGround", true, () -> !mode.is(Mode.Ground)));

    // Ground模式专用设置
    private final BooleanSetting setOnGround = add(new BooleanSetting("SetNoGround", false, () -> mode.is(Mode.Ground)));
    private final BooleanSetting blockCheck = add(new BooleanSetting("BlockCheck", true, () -> mode.is(Mode.Ground)));
    private final BooleanSetting autoJump = add(new BooleanSetting("AutoJump", true, () -> mode.is(Mode.Ground)).setParent());
    private final BooleanSetting mini = add(new BooleanSetting("Mini", true, () -> mode.is(Mode.Ground) && autoJump.isOpen()));
    private final SliderSetting y = add(new SliderSetting("MotionY", 0.05, 0.0, 1.0, 1.0E-10, () -> mode.is(Mode.Ground) && autoJump.isOpen()));
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true, () -> mode.is(Mode.Ground)));
    private final BooleanSetting crawlingDisable = add(new BooleanSetting("CrawlingDisable", true, () -> mode.is(Mode.Ground)));
    private final BooleanSetting flight = add(new BooleanSetting("Flight", false, () -> mode.is(Mode.Ground)));

    private boolean requireJump = false;

    public enum Mode {
        NewNCP, Strict, NCP, OldNCP, Hypixel2K22, Packet, Ground, BBTT
    }

    @Override
    public String getInfo() {
        return mode.getValue().name();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.isCancelled()) return;

        // 如果Blink模块开启并暂停其他模块，则不处理
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }

        // Ground模式处理
        if (mode.is(Mode.Ground)) {
            // 如果BowBomb在发送数据包，则不处理
            if (BowBomb.send) return;

            // 如果AutoPearl在投掷或Phase模块开启，则不处理
            if (AutoPearl.throwing || PearlPhase.INSTANCE.isOn()) return;

            // 如果设置了SetNoGround，则将发送的移动数据包onGround设为false
            if (setOnGround.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
                ((dev.iLnv_09.asm.accessors.IPlayerMoveC2SPacket) event.getPacket()).setOnGround(false);
            }
            return; // Ground模式不执行常规暴击逻辑
        }

        // 其他模式的处理
        Entity entity;
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet &&
                getInteractType(packet) == InteractType.ATTACK &&
                !((entity = getEntity(packet)) instanceof EndCrystalEntity)) {

            if ((!onlyGround.getValue() || mc.player.isOnGround() || mc.player.getAbilities().flying) &&
                    !mc.player.isInLava() &&
                    !mc.player.isSubmergedInWater() &&
                    entity != null) {

                mc.player.addCritParticles(entity);
                doCrit();
            }
        }
    }

    @Override
    public void onEnable() {
        // 如果Blink模块开启并暂停其他模块，则禁用
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            disable();
            return;
        }

        if (mode.is(Mode.Ground)) {
            requireJump = true;

            // 检查null
            if (mc.player == null || mc.world == null) {
                if (autoDisable.getValue()) {
                    disable();
                }
                return;
            }

            // 如果移动时自动禁用
            if (isMoving() && autoDisable.getValue()) {
                disable();
                return;
            }

            // 如果爬行时禁用
            if (crawlingDisable.getValue() && mc.player.isCrawling()) {
                disable();
                return;
            }

            // 如果在地面上且开启自动跳跃
            if (mc.player.isOnGround() && autoJump.getValue()) {
                // 检查是否需要检查方块
                if (!blockCheck.getValue() || canCollideAbove(2)) {
                    jump();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        requireJump = false;
    }

    @Override
    public void onLogout() {
        if (mode.is(Mode.Ground) && autoDisable.getValue()) {
            disable();
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }

        if (mode.is(Mode.Ground)) {
            // 如果爬行时禁用
            if (crawlingDisable.getValue() && mc.player.isCrawling()) {
                disable();
                return;
            }

            // 如果移动时自动禁用
            if (isMoving() && autoDisable.getValue()) {
                disable();
                return;
            }

            // 飞行模式处理
            if (flight.getValue() && mc.player.fallDistance > 0.0F) {
                setMotion(0.0, 0.0, 0.0);
                requireJump = false;
                return;
            }

            // 方块检查
            if (blockCheck.getValue() && !canCollideAbove(2)) {
                requireJump = true;
                return;
            }

            // 自动跳跃
            if (mc.player.isOnGround() && autoJump.getValue() && (flight.getValue() || requireJump)) {
                jump();
                requireJump = false;
            }
        }
    }

    private boolean isMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }

    private boolean canCollideAbove(int blocksAbove) {
        if (mc.player == null || mc.world == null) return false;

        Box box = new Box(
                mc.player.getX() - 0.3, mc.player.getY() + 1.8, mc.player.getZ() - 0.3,
                mc.player.getX() + 0.3, mc.player.getY() + blocksAbove, mc.player.getZ() + 0.3
        );

        return !mc.world.getBlockCollisions(mc.player, box).iterator().hasNext();
    }

    private void setMotion(double x, double y, double z) {
        if (mc.player != null) {
            mc.player.setVelocity(x, y, z);
        }
    }

    private void jump() {
        if (mini.getValue()) {
            setMotion(mc.player.getVelocity().x, y.getValue(), mc.player.getVelocity().z);
        } else {
            mc.player.jump();
        }
    }

    public void doCrit() {
        switch (mode.getValue()) {
            case Strict:
                if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() != Blocks.COBWEB) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.062600301692775, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.07260029960661, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                }
                break;
            case NCP:
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625D, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                break;
            case OldNCP:
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.00001058293536, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.00000916580235, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.00000010371854, mc.player.getZ(), false));
                break;
            case NewNCP:
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000271875, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                break;
            case Hypixel2K22:
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0045, mc.player.getZ(), true));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000152121, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.025, mc.player.getZ(), false));
                break;
            case Packet:
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0005, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0001, mc.player.getZ(), false));
                break;
            case BBTT:
                if (isMoving()) {
                    return;
                }
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.045, mc.player.getZ(), false));
                break;
            case Ground:
                // Ground模式不在这里处理
                break;
        }
    }

    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
        packet.write(packetBuf);
        return mc.world == null ? null : mc.world.getEntityById(packetBuf.readVarInt());
    }

    public static InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
        packet.write(packetBuf);

        packetBuf.readVarInt();
        return packetBuf.readEnumConstant(InteractType.class);
    }

    public enum InteractType {
        INTERACT,
        ATTACK,
        INTERACT_AT
    }
}