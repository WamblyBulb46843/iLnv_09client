package dev.iLnv_09.mod.modules.impl.player;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.utils.combat.CombatUtil;
import dev.iLnv_09.api.utils.entity.EntityUtil;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.math.MathUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.render.Render3DUtil;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.asm.accessors.IEntity;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.lang.Math;

public class AutoPot extends Module {
    public static AutoPot INSTANCE;

    private final BooleanSetting rotate = this.add((new BooleanSetting("Rotate", true)).setParent());
    private final BooleanSetting snapBack = this.add(new BooleanSetting("SnapBack", true, rotate::isOpen));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    public final SliderSetting delay = this.add(new SliderSetting("Delay", 1050, 0, 2000));
    private final SliderSetting earlyThrow = this.add(new SliderSetting("EarlyThrow", 3, 1, 10).setSuffix("s"));

    // 新增边缘检测设置
    private final BooleanSetting edgeCheck = this.add(new BooleanSetting("EdgeCheck", false).setParent());
    private final SliderSetting edgeDistance = this.add(new SliderSetting("EdgeDistance", 0.3, 0.1, 0.5, 0.05,
            () -> edgeCheck.getValue()).setSuffix("m"));

    private final BooleanSetting checkPlayer = add(new BooleanSetting("PlayerCheck", true).setParent());
    private final SliderSetting range = add(new SliderSetting("Range", 15, 0, 20, checkPlayer::isOpen).setSuffix("m"));
    private final BooleanSetting hcehck = this.add((new BooleanSetting("HealthCheck", false)).setParent());
    public final SliderSetting health = this.add((new SliderSetting("Health", 20, 0, 36, hcehck::isOpen)).setSuffix("HP"));
    public final SliderSetting effectRange = this.add(new SliderSetting("EffectRange", 3.0, 0.0, 6.0, 0.1));
    private final SliderSetting predictTicks = this.add((new SliderSetting("Predict", 2, 0, 10)).setSuffix("ticks"));
    public final BooleanSetting debug = this.add(new BooleanSetting("debug", false));

    private final BooleanSetting speed = add(new BooleanSetting("Speed", true));
    private final BooleanSetting resistance = add(new BooleanSetting("Resistance", true));
    private final BooleanSetting slowFalling = add(new BooleanSetting("SlowFalling", true));
    private final BooleanSetting usingPause = add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround = add(new BooleanSetting("OnlyGround", true));

    private final Timer timer = new Timer();
    private boolean rotating = false;
    private float oldYaw, oldPitch;

    public AutoPot() {
        super("AutoPot", Category.Player);
        setChinese("自动药水");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return "R:" + InventoryUtil.getPotCount(StatusEffects.RESISTANCE) + " S:" + InventoryUtil.getPotCount(StatusEffects.SPEED) + " SF:" + InventoryUtil.getPotCount(StatusEffects.SLOW_FALLING);
    }

    @Override
    public void onUpdate() {
        if (this.rotating && snapBack.getValue()) {
            mc.player.setYaw(oldYaw);
            mc.player.setPitch(oldPitch);
            this.rotating = false;
        }

        if (!shouldExecute()) {
            return;
        }

        int earlyThrowTicks = (int) (earlyThrow.getValue() * 20);

        StatusEffectInstance resistanceEffect = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance.getValue() && (resistanceEffect == null || resistanceEffect.getDuration() < earlyThrowTicks || resistanceEffect.getAmplifier() < 2)) {
            if (!hcehck.getValue() || (mc.player.getHealth() + mc.player.getAbsorptionAmount()) < health.getValue()) {
                if (findPot(StatusEffects.RESISTANCE) != -1) {
                    // 添加边缘检测
                    if (edgeCheck.getValue() && isPlayerOnEdge()) {
                        return;
                    }
                    this.doPot(StatusEffects.RESISTANCE);
                    return;
                }
            }
        } else {
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            if (speed.getValue() && (speedEffect == null || speedEffect.getDuration() < earlyThrowTicks)) {
                if (findPot(StatusEffects.SPEED) != -1) {
                    // 添加边缘检测
                    if (edgeCheck.getValue() && isPlayerOnEdge()) {
                        return;
                    }
                    this.doPot(StatusEffects.SPEED);
                    return;
                }
            } else {
                StatusEffectInstance slowFallingEffect = mc.player.getStatusEffect(StatusEffects.SLOW_FALLING);
                if (slowFalling.getValue() && (slowFallingEffect == null || slowFallingEffect.getDuration() < earlyThrowTicks)) {
                    if (findPot(StatusEffects.SLOW_FALLING) != -1) {
                        // 添加边缘检测
                        if (edgeCheck.getValue() && isPlayerOnEdge()) {
                            return;
                        }
                        this.doPot(StatusEffects.SLOW_FALLING);
                    }
                }
            }
        }
    }

    private boolean shouldExecute() {
        if (checkPlayer.getValue()) {
            PlayerEntity player = CombatUtil.getClosestEnemy(range.getValue());
            if (player == null) {
                return false;
            }
        }

        if (onlyGround.getValue() && !mc.player.isOnGround()) {
            return false;
        }

        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return false;
        }

        // 添加边缘检测
        if (edgeCheck.getValue() && isPlayerOnEdge()) {
            return false;
        }

        return this.timer.passedMs((long) this.delay.getValueInt());
    }

    /**
     * 检测玩家是否站在方块边缘
     */
    private boolean isPlayerOnEdge() {
        if (mc.player == null || mc.world == null) return false;

        Vec3d playerPos = mc.player.getPos();
        double playerX = playerPos.x;
        double playerY = playerPos.y;
        double playerZ = playerPos.z;

        // 获取玩家脚下的方块位置
        BlockPos blockPos = new BlockPos(
                (int) Math.floor(playerX),
                (int) Math.floor(playerY - 0.5), // 减去0.5以获得脚下的方块
                (int) Math.floor(playerZ)
        );

        // 检查玩家是否站在一个实体方块上
        if (mc.world.getBlockState(blockPos).getBlock() == Blocks.AIR) {
            return true; // 如果没有方块支撑，也认为是边缘
        }

        // 获取玩家脚部相对于方块的位置（0-1）
        double offsetX = playerX - blockPos.getX();
        double offsetZ = playerZ - blockPos.getZ();

        // 获取配置的边缘距离阈值
        double edgeThreshold = edgeDistance.getValue();

        // 检查是否接近任何边缘
        boolean nearXEdge = offsetX < edgeThreshold || offsetX > (1.0 - edgeThreshold);
        boolean nearZEdge = offsetZ < edgeThreshold || offsetZ > (1.0 - edgeThreshold);

        // 如果玩家接近任何一个边缘，返回true
        return nearXEdge || nearZEdge;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.debug.getValue()) {
            Render3DUtil.draw3DBox(matrixStack, ((IEntity) mc.player).getDimensions().getBoxAt(calculatePredictedPosition()).expand(0.0, 0.1, 0.0), new Color(0, 255, 255, 80), false, true);
        }
    }

    private void doPot(StatusEffect effect) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int slot = this.findPot(effect);
        if (slot == -1) {
            return;
        }

        this.timer.reset();
        this.doSwap(slot);
        if (this.rotate.getValue()) {
            this.oldYaw = mc.player.getYaw();
            this.oldPitch = mc.player.getPitch();
            float newPitch = this.calculateOptimalPitch();
            if (Float.isNaN(newPitch)) {
                return;
            }
            this.rotating = true;
            EntityUtil.sendYawAndPitch(iLnv_09.ROTATION.rotationYaw, newPitch);
        }

        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, EntityUtil.getWorldActionId(mc.world)));
        if (this.inventory.getValue()) {
            this.doSwap(slot);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(oldSlot);
        }
    }

    public int findPot(StatusEffect statusEffect) {
        return this.inventory.getValue() ? InventoryUtil.findPotInventorySlot(statusEffect) : InventoryUtil.findPot(statusEffect);
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private Vec3d calculatePredictedPosition() {
        double ticks = this.predictTicks.getValue() / 2.0;
        Vec3d playerPos = mc.player.getPos();
        Vec3d playerVelocity = mc.player.getVelocity();
        return new Vec3d(playerPos.x + playerVelocity.x * ticks, playerPos.y, playerPos.z + playerVelocity.z * ticks);
    }

    private float calculateOptimalPitch() {
        double gravity = 0.03;
        double velocity = 0.5;
        Vec3d predictedPos = calculatePredictedPosition();
        double dX = mc.player.getX() - predictedPos.x;
        double dZ = mc.player.getZ() - predictedPos.z;
        double dH = mc.player.getEyeY() - predictedPos.y;
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        if (horizontalDistance == 0) {
            return -90.0F;
        } else {
            return (float) Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(Math.pow(velocity, 4.0) - gravity * (gravity * horizontalDistance * horizontalDistance + 2.0 * dH * velocity * velocity))) / (gravity * horizontalDistance)));
        }
    }
}
