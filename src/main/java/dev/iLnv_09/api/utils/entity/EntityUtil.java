package dev.iLnv_09.api.utils.entity;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.utils.Wrapper;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.asm.accessors.IClientWorld;
import dev.iLnv_09.core.impl.RotationManager;
import dev.iLnv_09.mod.modules.impl.client.AntiCheat;
import dev.iLnv_09.mod.modules.settings.SwingSide;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class EntityUtil implements Wrapper {
    public static boolean isUsing() {
        if (mc.player == null) return false;
        return mc.player.isUsingItem();
    }

    public static boolean isHoldingWeapon(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof SwordItem || player.getMainHandStack().getItem() instanceof AxeItem;
    }
    public static boolean isInsideBlock() {
        if (BlockUtil.getBlock(EntityUtil.getPlayerPos(true)) == Blocks.ENDER_CHEST) return true;
        return mc.world.canCollide(mc.player, mc.player.getBoundingBox());
    }
    public static int getDamagePercent(ItemStack stack) {
        if (stack.getDamage() == stack.getMaxDamage()) return 100;
        return (int) ((stack.getMaxDamage() - stack.getDamage()) / Math.max(0.1, stack.getMaxDamage()) * 100.0f);
    }
    public static boolean isArmorLow(PlayerEntity player, int durability) {
        for (ItemStack piece : player.getArmorItems()) {
            if (piece == null || piece.isEmpty()) {
                return true;
            }

            if (getDamagePercent(piece) >= durability) continue;
            return true;
        }
        return false;
    }

    public static float getHealth(Entity entity) {
        if (entity.isLiving()) {
            LivingEntity livingBase = (LivingEntity) entity;
            return livingBase.getHealth() + livingBase.getAbsorptionAmount();
        }
        return 0.0f;
    }

    public static BlockPos getEntityPos(Entity entity) {
        return new BlockPosX(entity.getPos());
    }

    public static BlockPos getPlayerPos(boolean fix) {
        return new BlockPosX(mc.player.getPos(), fix);
    }
    public static BlockPos getPlayerPos() {
        return new BlockPosX(mc.player.getPos());
    }
    public static BlockPos getEntityPos(Entity entity, boolean fix) {
        return new BlockPosX(entity.getPos(), fix);
    }

    public static Vec3d getEyesPos() {
        return mc.player.getEyePos();
    }

    public static boolean canSee(BlockPos pos, Direction side) {
        Vec3d testVec = pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
        HitResult result = mc.world.raycast(new RaycastContext(getEyesPos(), testVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    public static void swingHand(Hand hand, SwingSide side) {
        switch (side) {
            case All -> mc.player.swingHand(hand);
            case Client -> mc.player.swingHand(hand, false);
            case Server -> mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    public static void syncInventory() {
        if (AntiCheat.INSTANCE.inventorySync.getValue()) mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }
    public static void sendYawAndPitch(float yaw, float pitch) {
        sendLook(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
    }
    public static boolean rotating = false;
    public static void sendLook(PlayerMoveC2SPacket packet) {
        if (packet.changesLook() && (packet.getYaw(114514.0F) != iLnv_09.ROTATION.lastYaw || packet.getPitch(114514.0F) != iLnv_09.ROTATION.lastPitch)) {
            rotating = true;
            iLnv_09.ROTATION.setRenderRotation(packet.getYaw(0.0F), packet.getPitch(0.0F), true);
            mc.player.networkHandler.sendPacket(packet);
            rotating = false;
        }
    }
    public static int getWorldActionId(ClientWorld world) {
        PendingUpdateManager pum = getUpdateManager(world);
        int p = pum.getSequence();
        pum.close();
        return p;
    }

    static PendingUpdateManager getUpdateManager(ClientWorld world) {
        return ((IClientWorld) world).acquirePendingUpdateManager();
    }
    public static float[] getLegitRotations(Vec3d vec) {
        Vec3d eyesPos = getEyesPos();
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{mc.player.getYaw() + MathHelper.wrapDegrees(yaw - mc.player.getYaw()), mc.player.getPitch() + MathHelper.wrapDegrees(pitch - mc.player.getPitch())};
    }
    public static void facePosSide(BlockPos pos, Direction side) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d((double)side.getVector().getX() * 0.5, (double)side.getVector().getY() * 0.5, (double)side.getVector().getZ() * 0.5));
        faceVector(hitVec);
    }
    public static void faceVector(Vec3d directionVec) {
        RotationManager.ROTATE_TIMER.reset();
        RotationManager.directionVec = directionVec;
        float[] angle = getLegitRotations(directionVec);
        if (angle[0] != iLnv_09.ROTATION.lastYaw || angle[1] != iLnv_09.ROTATION.lastPitch) {
            sendLook(new PlayerMoveC2SPacket.LookAndOnGround(angle[0], angle[1], mc.player.isOnGround()));
        }
    }

    public static void sync() {
    }
}
