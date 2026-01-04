package dev.iLnv_09.mod.modules.impl.combat;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.LookAtEvent;
import dev.iLnv_09.api.events.impl.UpdateWalkingPlayerEvent;
import dev.iLnv_09.api.utils.combat.CombatUtil;
import dev.iLnv_09.api.utils.entity.EntityUtil;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.client.AntiCheat;
import dev.iLnv_09.mod.modules.impl.exploit.Blink;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static dev.iLnv_09.api.utils.world.BlockUtil.*;

public class AutoWeb extends Module {
    public static AutoWeb INSTANCE;
    public AutoWeb() {
        super("AutoWeb", Category.Combat);
        setChinese("蜘蛛网光环");
        INSTANCE = this;
    }

    // 优化默认设置
    public final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));
    public final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 35, 0, 500, () -> page.getValue() == Page.General)); // 降低延迟
    public final SliderSetting blocksPer = add(new SliderSetting("BlocksPer", 3, 1, 10, () -> page.getValue() == Page.General)); // 增加默认值
    public final SliderSetting predictTicks = add(new SliderSetting("PredictTicks", 3, 0.0, 50, 1, () -> page.getValue() == Page.General)); // 优化预测
    private final BooleanSetting preferAnchor = add(new BooleanSetting("PreferAnchor", true, () -> page.getValue() == Page.General));
    private final BooleanSetting detectMining =
            add(new BooleanSetting("DetectMining", true, () -> page.getValue() == Page.General));
    private final BooleanSetting onlyTick =
            add(new BooleanSetting("OnlyTick", false, () -> page.getValue() == Page.General));
    private final BooleanSetting feet =
            add(new BooleanSetting("Feet", true, () -> page.getValue() == Page.General));
    private final BooleanSetting face =
            add(new BooleanSetting("Face", true, () -> page.getValue() == Page.General));
    public final SliderSetting maxWebs =
            add(new SliderSetting("MaxWebs", 2, 1, 8, 1, () -> page.getValue() == Page.General));
    private final BooleanSetting down =
            add(new BooleanSetting("Down", true, () -> page.getValue() == Page.General));
    private final BooleanSetting inventorySwap =
            add(new BooleanSetting("InventorySwap", true, () -> page.getValue() == Page.General));
    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true, () -> page.getValue() == Page.General));
    public final SliderSetting offset =
            add(new SliderSetting("Offset", 0.25, 0.0, 0.3, 0.01, () -> page.getValue() == Page.General));
    public final SliderSetting placeRange =
            add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, 0.1, () -> page.getValue() == Page.General));
    public final SliderSetting targetRange =
            add(new SliderSetting("TargetRange", 8.0, 0.0, 8.0, 0.1, () -> page.getValue() == Page.General));

    private final BooleanSetting rotate =
            add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotate).setParent());
    private final BooleanSetting yawStep =
            add(new BooleanSetting("YawStep", false, () -> rotate.isOpen() && page.getValue() == Page.Rotate));
    private final SliderSetting steps =
            add(new SliderSetting("Steps", 0.3, 0.1, 1.0, 0.01, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotate));
    private final BooleanSetting checkFov =
            add(new BooleanSetting("OnlyLooking", true, () -> rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotate));
    private final SliderSetting fov =
            add(new SliderSetting("Fov", 30, 0, 50, () -> rotate.isOpen() && yawStep.getValue() && checkFov.getValue() && page.getValue() == Page.Rotate));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10,0 ,100, () ->rotate.isOpen() && yawStep.getValue() && page.getValue() == Page.Rotate));
    public final BooleanSetting crystalSync = add(new BooleanSetting("CrystalSync", true, () -> page.getValue() == Page.General));
    public final SliderSetting syncDistance = add(new SliderSetting("SyncDistance", 3.0, 1.0, 6.0, () -> page.getValue() == Page.General && crystalSync.getValue()).setSuffix("m"));
    private final Timer timer = new Timer();
    public Vec3d directionVec = null;

    @Override
    public String getInfo() {
        if (pos.isEmpty()) return null;
        return "Working";
    }
    // 新增同步方法
    private boolean shouldPlaceWebForCrystalSync(PlayerEntity target) {
        if (!crystalSync.getValue() || !AutoCrystal.INSTANCE.isOff()) return false;

        // 检查目标是否在自动水晶的范围内
        if (AutoCrystal.crystalPos != null && AutoCrystal.INSTANCE.displayTarget == target) {
            double distance = target.getPos().distanceTo(AutoCrystal.crystalPos.toCenterPos());
            return distance <= syncDistance.getValue();
        }

        return false;
    }

    public static boolean force = false;
    public static boolean ignore = false;
    @EventHandler
    public void onRotate(LookAtEvent event) {
        if (rotate.getValue() && yawStep.getValue() && directionVec != null) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (!onlyTick.getValue()) {
            onUpdate();
        }
    }
    @Override
    public void onDisable() {
        force = false;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!onlyTick.getValue()) {
            onUpdate();
        }
    }

    int progress = 0;

    final ArrayList<BlockPos> pos = new ArrayList<>();
    public void onUpdate() {
        if (force) {
            ignore = true;
            processForceMode();
        }
        update();
        ignore = false;
    }
    // 新增强制模式处理
    private void processForceMode() {
        if (!timer.passedMs(placeDelay.getValueInt())) return;

        PlayerEntity nearestTarget = CombatUtil.getNearestEnemy(targetRange.getValue());
        if (nearestTarget != null) {
            placeEmergencyWebs(nearestTarget);
        }

        // 重置强制模式
        if (timer.passedMs(2000)) { // 2秒后重置
            force = false;
        }
    }
    // 紧急放置蜘蛛网
    private void placeEmergencyWebs(PlayerEntity target) {
        Vec3d targetPos = predictTicks.getValue() > 0 ?
                CombatUtil.getEntityPosVec(target, predictTicks.getValueInt()) : target.getPos();

        // 优先放置脚部蜘蛛网
        BlockPosX feetPos = new BlockPosX(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        if (placeWeb(feetPos)) return;

        // 其次放置头部蜘蛛网
        BlockPosX headPos = new BlockPosX(targetPos.getX(), targetPos.getY() + 1.0, targetPos.getZ());
        placeWeb(headPos);
    }
    private boolean shouldSkipUpdate() {
        return (preferAnchor.getValue() && AutoAnchor.INSTANCE.currentPos != null) ||
                (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) ||
                (usingPause.getValue() && mc.player.isUsingItem()) ||
                (getWebSlot() == -1);
    }
    private void processTargets() {
        for (PlayerEntity player : CombatUtil.getEnemies(targetRange.getValue())) {
            // 新增：水晶同步检查
            boolean crystalSyncPlacement = shouldPlaceWebForCrystalSync(player);

            Vec3d playerPos = predictTicks.getValue() > 0 ?
                    CombatUtil.getEntityPosVec(player, predictTicks.getValueInt()) : player.getPos();

            int webs = countExistingWebs(player, playerPos);

            if (webs >= maxWebs.getValueFloat() && !ignore && !crystalSyncPlacement) {
                continue;
            }

            // 处理蜘蛛网放置
            if (feet.getValue()) {
                if (placeFeetWebs(player, playerPos, webs, crystalSyncPlacement)) {
                    continue;
                }
            }

            if (face.getValue()) {
                placeFaceWebs(player, playerPos, webs, crystalSyncPlacement);
            }
        }
    }
    private void placeFaceWebs(PlayerEntity player, Vec3d playerPos, int webs, boolean crystalSyncPlacement) {
        for (float x : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
            for (float z : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                BlockPosX pos = new BlockPosX(playerPos.getX() + x, playerPos.getY() + 1.1, playerPos.getZ() + z);
                if (isTargetHere(pos, player)) {
                    if (placeWeb(pos)) {
                        webs++;
                        if (webs >= maxWebs.getValueFloat() && !crystalSyncPlacement) {
                            return; // 达到最大数量且不是同步模式时停止
                        }
                    }
                }
            }
        }
    }
    private boolean placeFeetWebs(PlayerEntity player, Vec3d playerPos, int webs, boolean crystalSyncPlacement) {
        for (float x : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
            for (float z : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                BlockPosX pos = new BlockPosX(playerPos.getX() + x, playerPos.getY(), playerPos.getZ() + z);
                if (isTargetHere(pos, player)) {
                    if (placeWeb(pos)) {
                        webs++;
                        if (webs >= maxWebs.getValueFloat() && !crystalSyncPlacement) {
                            return true; // 达到最大数量且不是同步模式时停止
                        }
                    }
                }
            }
        }
        return false;
    }
    private int countExistingWebs(PlayerEntity player, Vec3d playerPos) {
        int webs = 0;
        List<BlockPos> checkedPositions = new ArrayList<>();

        for (float x : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
            for (float z : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                for (float y : new float[]{0, 1, -1}) {
                    BlockPosX pos = new BlockPosX(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    if (!checkedPositions.contains(pos)) {
                        checkedPositions.add(pos);
                        if (isTargetHere(pos, player) &&
                                mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB &&
                                !iLnv_09.BREAK.isMining(pos)) {
                            webs++;
                        }
                    }
                }
            }
        }
        return webs;
    }
    private void update() {
        if (!timer.passedMs(placeDelay.getValueInt())) {
            return;
        }
        pos.clear();
        progress = 0;
        directionVec = null;
        if (shouldSkipUpdate()) return;

        processTargets();
        if (preferAnchor.getValue() && AutoAnchor.INSTANCE.currentPos != null) {
            return;
        }
        if (getWebSlot() == -1) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }
        for (PlayerEntity player : CombatUtil.getEnemies(targetRange.getValue())) {
            Vec3d playerPos = predictTicks.getValue() > 0 ? CombatUtil.getEntityPosVec(player, predictTicks.getValueInt()) : player.getPos();
            int webs = 0;
            if (down.getValue()) {
                placeWeb(new BlockPosX(playerPos.getX(), playerPos.getY() - 0.8, playerPos.getZ()));
            }
            List<BlockPos> list = new ArrayList<>();
            for (float x : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                for (float z : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                    for (float y : new float[]{0, 1, -1}) {
                        BlockPosX pos = new BlockPosX(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                        if (!list.contains(pos)) {
                            list.add(pos);
                            if (isTargetHere(pos, player) && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB && !iLnv_09.BREAK.isMining(pos)) {
                                webs++;
                            }
                        }
                    }
                }
            }
            if (webs >= maxWebs.getValueFloat() && !ignore) {
                continue;
            }
            boolean skip = false;
            if (feet.getValue()) {
                start:
                for (float x : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                    for (float z : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                        BlockPosX pos = new BlockPosX(playerPos.getX() + x, playerPos.getY(), playerPos.getZ() + z);
                        if (isTargetHere(pos, player)) {
                            if (placeWeb(pos)) {
                                webs++;
                                if (webs >= maxWebs.getValueFloat()) {
                                    skip = true;
                                    break start;
                                }
                            }
                        }
                    }
                }
            }
            if (skip) continue;
            if (face.getValue()) {
                start:
                for (float x : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                    for (float z : new float[]{0, offset.getValueFloat(), -offset.getValueFloat()}) {
                        BlockPosX pos = new BlockPosX(playerPos.getX() + x, playerPos.getY() + 1.1, playerPos.getZ() + z);
                        if (isTargetHere(pos, player)) {
                            if (placeWeb(pos)) {
                                webs++;
                                if (webs >= maxWebs.getValueFloat()) {
                                    break start;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private boolean isTargetHere(BlockPos pos, PlayerEntity target) {
        return new Box(pos).intersects(target.getBoundingBox());
    }
    private boolean placeWeb(BlockPos pos) {
        if (this.pos.contains(pos)) return false;
        if (progress >= blocksPer.getValueInt()) return false;
        if (getWebSlot() == -1) return false;

        if (isValidWebPosition(pos)) {
            this.pos.add(pos);
            return executeWebPlacement(pos);
        }
        return false;
    }
    private boolean isValidWebPosition(BlockPos pos) {
        return !(detectMining.getValue() && iLnv_09.BREAK.isMining(pos)) &&
                BlockUtil.getPlaceSide(pos, placeRange.getValue()) != null &&
                (mc.world.isAir(pos) || (ignore && getBlock(pos) == Blocks.COBWEB)) &&
                pos.getY() < 320;
    }

    private boolean executeWebPlacement(BlockPos pos) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int webSlot = getWebSlot();

        if (!placeBlock(pos, rotate.getValue(), webSlot)) return false;

        BlockUtil.placedPos.add(pos);
        progress++;

        // 优化交换逻辑
        handleSlotSwapping(oldSlot, webSlot);

        force = false;
        timer.reset();
        return true;
    }

    private void handleSlotSwapping(int oldSlot, int webSlot) {
        if (inventorySwap.getValue()) {
            doSwap(webSlot);
            EntityUtil.syncInventory();
        } else {
            doSwap(oldSlot);
        }
    }

    public boolean placeBlock(BlockPos pos, boolean rotate, int slot) {
        Direction side = getPlaceSide(pos);
        if (side == null) {
            if (airPlace()) {
                return clickBlock(pos, Direction.DOWN, rotate, slot);
            }
            return false;
        }
        return clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
    }

    public boolean clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (rotate) {
            if (!faceVector(directionVec)) return false;
        }
        doSwap(slot);
        EntityUtil.swingHand(Hand.MAIN_HAND, AntiCheat.INSTANCE.swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        if (rotate && !yawStep.getValue() && AntiCheat.INSTANCE.snapBack.getValue()) {
            iLnv_09.ROTATION.snapBack();
        }
        return true;
    }

    private boolean faceVector(Vec3d directionVec) {
        if (!yawStep.getValue()) {
            iLnv_09.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.directionVec = directionVec;
            if (iLnv_09.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }

    private void doSwap(int slot) {
        if (inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getWebSlot() {
        if (inventorySwap.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.COBWEB);
        } else {
            return InventoryUtil.findBlock(Blocks.COBWEB);
        }
    }

    public enum Page {
        General,
        Rotate
    }
}