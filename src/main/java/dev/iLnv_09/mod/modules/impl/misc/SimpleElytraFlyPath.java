package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.api.utils.render.TextUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.BlockPos;

public class SimpleElytraFlyPath extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean isArrive = false;

    public final SliderSetting globalX = add(new SliderSetting("TargetX", 0, -30000000, 30000000, 1));
    public final SliderSetting globalZ = add(new SliderSetting("TargetZ", 0, -30000000, 30000000, 1));
    public final BooleanSetting autoStop = add(new BooleanSetting("AutoStop", true));
    public final SliderSetting speed = add(new SliderSetting("Speed", 0.5, 0.1, 3, 0.1));
    public final BooleanSetting autoQuitServer = add(new BooleanSetting("AutoQuit", false));
    public final BooleanSetting autoTakeoff = add(new BooleanSetting("AutoTakeoff", true));
    public final SliderSetting arrivalDistance2D = add(new SliderSetting("ArrivalDistance", 1, 1, 256, 0.1));
    public final BooleanSetting hud = add(new BooleanSetting("HUD", true));
    public final SliderSetting hudX = add(new SliderSetting("HUD X", 5, 0, 1000, 1));
    public final SliderSetting hudY = add(new SliderSetting("HUD Y", 30, 0, 1000, 1));

    private BlockPos target;

    public SimpleElytraFlyPath() {
        super("SimpleElytraFlyPath", "Elytra auto pilot.", Category.Misc);
        setChinese("简单鞘翅飞行路径");
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null || !checkElytra()) {
            toggle();
            return;
        }

        if (!checkValidHeight()) {
            mc.player.sendMessage(Text.literal("此模块只能在每个维度指定的高度之上使用."));
            toggle();
            return;
        }

        if (!mc.player.isCreative()) mc.player.getAbilities().allowFlying = false;
        mc.player.getAbilities().flying = false;

        if (autoTakeoff.getValue() && !mc.player.isFallFlying()) {
            recastElytra(mc.player);
        }

        mc.player.sendMessage(Text.literal("Navigating to: X=" + globalX.getValueInt() + ", Z=" + globalZ.getValueInt()));
    }

    @Override
    public void onDisable() {
        target = null;
        isArrive = false;

        if (mc.player != null) {
            if (!mc.player.isCreative()) mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
        }
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isFallFlying() && checkElytra()) {
            target = new BlockPos(globalX.getValueInt(), (int) mc.player.getY(), globalZ.getValueInt());

            double deltaX = target.getX() - mc.player.getX();
            double deltaZ = target.getZ() - mc.player.getZ();
            double distance2D = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            if (distance2D > arrivalDistance2D.getValue()) {
                // Calculate yaw and pitch for navigation
                double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
                mc.player.setYaw((float) yaw);

            } else {
                // Arrived at destination
                mc.player.setVelocity(0, 0, 0); // Stop the player
                isArrive = true;
            }
        }

        if (autoStop.getValue()) {
            int chunkX = (int) (mc.player.getX() / 16);
            int chunkZ = (int) (mc.player.getZ() / 16);
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                mc.player.setVelocity(0, 0, 0);
            }
        }

        if (autoTakeoff.getValue() && !mc.player.isFallFlying()) {
            recastElytra(mc.player);
        }

        if (isArrive && autoQuitServer.getValue()) {
            mc.world.disconnect();
            toggle();
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (mc.player == null || mc.world == null || !hud.getValue() || target == null) return;

        String targetCoords = "目标: " + target.getX() + ", " + target.getZ();
        double deltaX = target.getX() - mc.player.getX();
        double deltaZ = target.getZ() - mc.player.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        String distanceString = String.format("距离: %.1f", distance);

        float x = hudX.getValueFloat();
        float y = hudY.getValueFloat();
        TextUtil.drawString(drawContext, targetCoords, (int) x, (int) y, -1, true);
        y += 10;
        TextUtil.drawString(drawContext, distanceString, (int) x, (int) y, -1, true);
    }

    // ==================== 工具方法 ====================

    /**
     * 检查鞘翅装备状态（改进版本，包含耐久度检查）
     */
    private boolean checkElytra() {
        // 检测玩家是否装备了鞘翅并且可用
        for (ItemStack is : mc.player.getArmorItems()) {
            if (is.getItem() instanceof ElytraItem && ElytraItem.isUsable(is)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否在允许的高度范围内
     * 必须在各维度高度上限之上
     * 地狱: Y > 128 (基岩层之上)
     * 主世界: Y > 320 (建筑高度上限之上)
     * 末地: Y > 256 (高度上限之上)
     */
    private boolean checkValidHeight() {
        if (mc.player == null || mc.world == null) return false;

        double playerY = mc.player.getY();
        String dimensionName = mc.world.getRegistryKey().getValue().toString();

        switch (dimensionName) {
            case "minecraft:the_nether":
                // 地狱顶部基岩层之上 (Y > 128)
                return playerY > 128;
            case "minecraft:overworld":
                // 主世界建筑高度上限之上 (Y > 320)
                return playerY > 320;
            case "minecraft:the_end":
                // 末地高度上限之上 (Y > 256)
                return playerY > 256;
            default:
                return false;
        }
    }

    /**
     * 重新激活鞘翅飞行
     * 发送网络包给服务器，请求开始鞘翅飞行
     */
    public static boolean recastElytra(ClientPlayerEntity player) {
        if (checkConditions(player) && ignoreGround(player)) {
            // 发送开始鞘翅飞行的网络包
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return true;
        }
        return false;
    }

    /**
     * 检查鞘翅飞行的前置条件
     * 确保玩家可以安全地开始鞘翅飞行
     */
    public static boolean checkConditions(ClientPlayerEntity player) {
        ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
        return (!player.getAbilities().flying &&    // 不在创造模式飞行
                !player.hasVehicle() &&                 // 不在载具中
                !player.isClimbing() &&                 // 不在攀爬
                itemStack.isOf(Items.ELYTRA) &&         // 装备了鞘翅
                ElytraItem.isUsable(itemStack));        // 鞘翅可用（有耐久度）
    }

    /**
     * 忽略地面检测，强制启动鞘翅飞行
     * 即使在地面上也可以启动鞘翅飞行
     */
    private static boolean ignoreGround(ClientPlayerEntity player) {
        if (!player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.LEVITATION)) {
            ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
            if (itemStack.isOf(Items.ELYTRA) && ElytraItem.isUsable(itemStack)) {
                return true;
            }
        }
        return false;
    }


    // ==================== 速度获取和设置方法 ====================

    /**
     * 获取玩家当前的X轴速度
     */
    private double getX() {
        return mc.player.getVelocity().x;
    }

    /**
     * 获取玩家当前的Y轴速度
     */
    private double getY() {
        return mc.player.getVelocity().y;
    }

    /**
     * 获取玩家当前的Z轴速度
     */
    private double getZ() {
        return mc.player.getVelocity().z;
    }

    /**
     * 设置玩家的X轴速度
     * 保持Y和Z轴速度不变
     */
    private void setX(double f) {
        Vec3d currentVel = mc.player.getVelocity();
        Vec3d newVel = new Vec3d(f, currentVel.y, currentVel.z);
        mc.player.setVelocity(newVel);
    }

    /**
     * 设置玩家的Y轴速度
     * 保持X和Z轴速度不变
     */
    private void setY(double f) {
        Vec3d currentVel = mc.player.getVelocity();
        Vec3d newVel = new Vec3d(currentVel.x, f, currentVel.z);
        mc.player.setVelocity(newVel);
    }

    /**
     * 设置玩家的Z轴速度
     * 保持X和Y轴速度不变
     */
    private void setZ(double f) {
        Vec3d currentVel = mc.player.getVelocity();
        Vec3d newVel = new Vec3d(currentVel.x, currentVel.y, f);
        mc.player.setVelocity(newVel);
    }


}