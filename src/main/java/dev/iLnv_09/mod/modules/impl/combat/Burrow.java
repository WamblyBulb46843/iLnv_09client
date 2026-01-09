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
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.client.AntiCheat;
import dev.iLnv_09.mod.modules.impl.exploit.Blink;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

import static dev.iLnv_09.api.utils.world.BlockUtil.canReplace;

public class Burrow extends Module {
    public static Burrow INSTANCE;
    private final Timer timer = new Timer();
    private final Timer webTimer = new Timer();

    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    private final BooleanSetting disable =
            add(new BooleanSetting("Disable", true, () -> page.is(Page.General)));
    private final BooleanSetting jumpDisable =
            add(new BooleanSetting("JumpDisable", true, () -> !disable.getValue() && page.is(Page.General)));
    private final SliderSetting delay =
            add(new SliderSetting("Delay", 500, 0, 1000, () -> !disable.getValue() && page.is(Page.General)));
    private final SliderSetting webTime =
            add(new SliderSetting("WebTime", 0, 0, 500, () -> page.is(Page.General)));
    private final BooleanSetting enderChest =
            add(new BooleanSetting("EnderChest", true, () -> page.is(Page.General)));
    private final BooleanSetting antiLag =
            add(new BooleanSetting("AntiLag", false, () -> page.is(Page.General)));
    private final BooleanSetting detectMine =
            add(new BooleanSetting("DetectMining", false, () -> page.is(Page.General)));
    private final BooleanSetting headFill =
            add(new BooleanSetting("HeadFill", false, () -> page.is(Page.General)));
    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true, () -> page.is(Page.General)));
    private final BooleanSetting movingPause =
            add(new BooleanSetting("MovingPause", false, () -> page.is(Page.General)));
    private final BooleanSetting down =
            add(new BooleanSetting("Down", true, () -> page.is(Page.General)));
    private final BooleanSetting noSelfPos =
            add(new BooleanSetting("NoSelfPos", false, () -> page.is(Page.General)));
    private final BooleanSetting packetPlace =
            add(new BooleanSetting("PacketPlace", true, () -> page.is(Page.General)));
    private final BooleanSetting sound =
            add(new BooleanSetting("Sound", true, () -> page.is(Page.General)));
    private final SliderSetting blocksPer =
            add(new SliderSetting("BlocksPer", 4, 1, 4, 1, () -> page.is(Page.General)));
    private final BooleanSetting breakCrystal =
            add(new BooleanSetting("Break", true, () -> page.is(Page.General)));
    private final BooleanSetting wait =
            add(new BooleanSetting("Wait", true, () -> !disable.getValue() && page.is(Page.General)));
    private final BooleanSetting onlyTick =
            add(new BooleanSetting("OnlyTick", true, () -> page.getValue() == Page.General));
    private final BooleanSetting inventory =
            add(new BooleanSetting("InventorySwap", true, () -> page.is(Page.General)));

    // 从 BurrowTwo 复制的设置
    private final BooleanSetting cancelBlink =
            add(new BooleanSetting("CancelBlink", true, () -> page.is(Page.General)));
    private final BooleanSetting smartActive =
            add(new BooleanSetting("SmartActive", false, () -> page.is(Page.General)));
    private final BooleanSetting onlyStatic =
            add(new BooleanSetting("OnlyStatic", false, () -> page.is(Page.General)));
    private final BooleanSetting webCheck =
            add(new BooleanSetting("WebCheck", true, () -> page.is(Page.General)));

    // 新增：添加方块内部检查选项
    private final BooleanSetting allowInsideBlocks =
            add(new BooleanSetting("AllowInsideBlocks", false, () -> page.is(Page.General)));

    private final BooleanSetting fakeMove =
            add(new BooleanSetting("FakeMove", true, () -> page.is(Page.Above)));
    private final BooleanSetting center =
            add(new BooleanSetting("AllowCenter", false, () -> fakeMove.getValue() && page.is(Page.Above)));

    public final EnumSetting<LagBackMode> lagMode =
            add(new EnumSetting<>("LagMode", LagBackMode.Smart, () -> page.is(Page.LagMode)));
    public final EnumSetting<LagBackMode> aboveLagMode =
            add(new EnumSetting<>("MoveLagMode", LagBackMode.Smart, () -> page.is(Page.LagMode)));
    private final SliderSetting smartX =
            add(new SliderSetting("SmartXZ", 3, 0, 10, 0.1, () -> page.is(Page.LagMode) && (lagMode.getValue() == LagBackMode.Smart || aboveLagMode.getValue() == LagBackMode.Smart)));
    private final SliderSetting smartUp =
            add(new SliderSetting("SmartUp", 3, 0, 10, 0.1, () -> page.is(Page.LagMode) && (lagMode.getValue() == LagBackMode.Smart || aboveLagMode.getValue() == LagBackMode.Smart)));
    private final SliderSetting smartDown =
            add(new SliderSetting("SmartDown", 3, 0, 10, 0.1, () -> page.is(Page.LagMode) && (lagMode.getValue() == LagBackMode.Smart || aboveLagMode.getValue() == LagBackMode.Smart)));
    private final SliderSetting smartDistance =
            add(new SliderSetting("SmartDistance", 2, 0, 10, 0.1, () -> page.is(Page.LagMode) && (lagMode.getValue() == LagBackMode.Smart || aboveLagMode.getValue() == LagBackMode.Smart)));

    private final BooleanSetting rotate =
            add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotate));
    private final BooleanSetting yawStep =
            add(new BooleanSetting("YawStep", false, () -> rotate.getValue() && page.getValue() == Page.Rotate));
    private final SliderSetting steps =
            add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> yawStep.getValue() && page.getValue() == Page.Rotate));
    private final BooleanSetting checkFov =
            add(new BooleanSetting("OnlyLooking", true, () -> yawStep.getValue() && page.getValue() == Page.Rotate));
    private final SliderSetting fov =
            add(new SliderSetting("Fov", 5f, 0f, 30f, () -> yawStep.getValue() && checkFov.getValue() && page.getValue() == Page.Rotate));
    private final SliderSetting priority =
            add(new SliderSetting("Priority", 10, 0, 100, () -> yawStep.getValue() && page.getValue() == Page.Rotate));


    private double startY = 0;
    private int progress = 0;
    final List<BlockPos> placePos = new ArrayList<>();
    public boolean hasBurrowItem;
    public Vec3d directionVec = null;

    public enum Page {
        General,
        LagMode,
        Above,
        Rotate
    }

    public Burrow() {
        super("Burrow", Category.Combat);
        setChinese("卡黑曜石");
        INSTANCE = this;
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (!onlyTick.getValue()) {
            onUpdate();
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!onlyTick.getValue()) {
            onUpdate();
        }
    }

    @EventHandler
    public void onRotate(LookAtEvent event) {
        if (directionVec != null && rotate.getValue() && yawStep.getValue()) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
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

    @Override
    public void onEnable() {
        if (nullCheck()) {
            if (!disable.getValue()) disable();
            return;
        }
        startY = mc.player.getY();
    }

    @Override
    public void onUpdate() {
        // 从 BurrowTwo 复制的 CancelBlink 逻辑
        if (cancelBlink.getValue() && Blink.INSTANCE.isOn()) {
            return;
        }

        // 从 BurrowTwo 复制的智能激活检查
        if (smartActive.getValue() && !shouldActivate()) {
            return;
        }

        // 从 BurrowTwo 复制的静态检查
        if (onlyStatic.getValue() && isMoving()) {
            return;
        }

        // 检查玩家是否在蜘蛛网中
        if (webCheck.getValue() && iLnv_09.PLAYER.isInWeb(mc.player)) {
            webTimer.reset();
            return;
        }

        // 检查玩家是否在使用物品（如吃东西）
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }

        // 检查玩家是否在移动
        if (movingPause.getValue() && (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0)) {
            return;
        }

        // 检查蜘蛛网计时器
        if (!webTimer.passed(webTime.getValue())) {
            return;
        }

        // 检查延迟
        if (!disable.getValue() && !timer.passed(delay.getValue())) {
            return;
        }

        // 检查是否跳跃过高
        if ((jumpDisable.getValue() && Math.abs(startY - mc.player.getY()) > 0.5)) {
            disable();
            return;
        }

        // 检查是否在地面上
        if (!mc.player.isOnGround()) {
            return;
        }

        // 检查抗延迟
        if (antiLag.getValue()) {
            if (!mc.world.getBlockState(EntityUtil.getPlayerPos(true).down()).blocksMovement()) return;
        }

        // 检查Blink模块
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;

        // 新增：检查玩家是否完全在实心方块内部且不允许内部放置
        if (!allowInsideBlocks.getValue() && isPlayerInsideSolidBlock()) {
            if (!wait.getValue() && disable.getValue()) {
                CommandManager.sendChatMessageWidthId("§c§o玩家完全在实心方块内部，无法放置！", hashCode());
                disable();
            }
            return;
        }

        directionVec = null;
        int oldSlot = mc.player.getInventory().selectedSlot;
        int block;
        if ((block = getBlock()) == -1) {
            CommandManager.sendChatMessageWidthId("§c§oObsidian/CryingObsidian" + (enderChest.getValue() ? "/EnderChest" : "") + "?", hashCode());
            hasBurrowItem = false;
            disable();
            return;
        } else {
            hasBurrowItem = true;
        }
        progress = 0;
        placePos.clear();
        double offset = AntiCheat.getOffset();
        BlockPos pos1 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 0.5, mc.player.getZ() + offset);
        BlockPos pos2 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 0.5, mc.player.getZ() + offset);
        BlockPos pos3 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 0.5, mc.player.getZ() - offset);
        BlockPos pos4 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 0.5, mc.player.getZ() - offset);
        BlockPos pos5 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.5, mc.player.getZ() + offset);
        BlockPos pos6 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 1.5, mc.player.getZ() + offset);
        BlockPos pos7 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.5, mc.player.getZ() - offset);
        BlockPos pos8 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 1.5, mc.player.getZ() - offset);
        BlockPos pos9 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - 1, mc.player.getZ() + offset);
        BlockPos pos10 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - 1, mc.player.getZ() + offset);
        BlockPos pos11 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - 1, mc.player.getZ() - offset);
        BlockPos pos12 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - 1, mc.player.getZ() - offset);
        BlockPos playerPos = EntityUtil.getPlayerPos(true);
        boolean headFill = false;

        // 检查是否至少有一个可放置的位置
        if (!canPlace(pos1) && !canPlace(pos2) && !canPlace(pos3) && !canPlace(pos4)) {
            boolean cantHeadFill = !this.headFill.getValue() || !canPlace(pos5) && !canPlace(pos6) && !canPlace(pos7) && !canPlace(pos8);
            boolean cantDown = !down.getValue() || !canPlace(pos9) && !canPlace(pos10) && !canPlace(pos11) && !canPlace(pos12);
            if (cantHeadFill) {
                if (cantDown) {
                    if (!wait.getValue() && disable.getValue()) {
                        CommandManager.sendChatMessageWidthId("§c§o没有可放置方块的位置！", hashCode());
                        disable();
                    }
                    return;
                }
            } else {
                headFill = true;
            }
        }

        boolean above = false;
        BlockPos headPos = EntityUtil.getPlayerPos(true).up(2);
        CombatUtil.attackCrystal(pos1, rotate.getValue(), usingPause.getValue());
        CombatUtil.attackCrystal(pos2, rotate.getValue(), usingPause.getValue());
        CombatUtil.attackCrystal(pos3, rotate.getValue(), usingPause.getValue());
        CombatUtil.attackCrystal(pos4, rotate.getValue(), usingPause.getValue());
        if (headFill || mc.player.isCrawling() || trapped(headPos) || trapped(headPos.add(1, 0, 0)) || trapped(headPos.add(-1, 0, 0)) || trapped(headPos.add(0, 0, 1)) || trapped(headPos.add(0, 0, -1)) || trapped(headPos.add(1, 0, -1)) || trapped(headPos.add(-1, 0, -1)) || trapped(headPos.add(1, 0, 1)) || trapped(headPos.add(-1, 0, 1))) {
            above = true;
            if (!fakeMove.getValue()) {
                if (!wait.getValue() && disable.getValue()) {
                    CommandManager.sendChatMessageWidthId("§c§o玩家被困且不允许移动！", hashCode());
                    disable();
                }
                return;
            }
            boolean moved = false;
            BlockPos offPos = playerPos;
            if (checkSelf(offPos) && !canReplace(offPos) && (!this.headFill.getValue() || !canReplace(offPos.up()))) {
                gotoPos(offPos);
            } else {
                for (final Direction facing : Direction.values()) {
                    if (facing == Direction.UP || facing == Direction.DOWN) continue;
                    offPos = playerPos.offset(facing);
                    if (checkSelf(offPos) && !canReplace(offPos) && (!this.headFill.getValue() || !canReplace(offPos.up()))) {
                        gotoPos(offPos);
                        moved = true;
                        break;
                    }
                }
                if (!moved) {
                    for (final Direction facing : Direction.values()) {
                        if (facing == Direction.UP || facing == Direction.DOWN) continue;
                        offPos = playerPos.offset(facing);
                        if (checkSelf(offPos)) {
                            gotoPos(offPos);
                            moved = true;
                            break;
                        }
                    }
                    if (!moved) {
                        if (!center.getValue()) {
                            return;
                        }
                        for (final Direction facing : Direction.values()) {
                            if (facing == Direction.UP || facing == Direction.DOWN) continue;
                            offPos = playerPos.offset(facing);
                            if (canMove(offPos)) {
                                gotoPos(offPos);
                                moved = true;
                                break;
                            }
                        }
                        if (!moved) {
                            if (!wait.getValue() && disable.getValue()) {
                                CommandManager.sendChatMessageWidthId("§c§o无法移动到可放置位置！", hashCode());
                                disable();
                            }
                            return;
                        }
                    }
                }
            }
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ(), false));
        }
        timer.reset();
        doSwap(block);
        placeBlock(playerPos);
        placeBlock(pos1);
        placeBlock(pos2);
        placeBlock(pos3);
        placeBlock(pos4);
        if (down.getValue()) {
            placeBlock(pos9);
            placeBlock(pos10);
            placeBlock(pos11);
            placeBlock(pos12);
        }
        if (this.headFill.getValue() && above) {
            placeBlock(pos5);
            placeBlock(pos6);
            placeBlock(pos7);
            placeBlock(pos8);
        }
        if (inventory.getValue()) {
            doSwap(block);
            EntityUtil.syncInventory();
        } else {
            doSwap(oldSlot);
        }
        if (directionVec == null) return;
        switch (above ? aboveLagMode.getValue() : lagMode.getValue()) {
            case Smart -> {
                ArrayList<BlockPos> list = new ArrayList<>();
                for (double x = mc.player.getPos().getX() - smartX.getValue(); x < mc.player.getPos().getX() + smartX.getValue(); ++x) {
                    for (double z = mc.player.getPos().getZ() - smartX.getValue(); z < mc.player.getPos().getZ() + smartX.getValue(); ++z) {
                        for (double y = mc.player.getPos().getY() - smartDown.getValue(); y < mc.player.getPos().getY() + smartUp.getValue(); ++y) {
                            list.add(new BlockPosX(x, y, z));
                        }
                    }
                }

                double distance = 0;
                BlockPos bestPos = null;
                for (BlockPos pos : list) {
                    if (!canMove(pos)) continue;
                    if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos().add(0, -0.5, 0))) < smartDistance.getValue())
                        continue;
                    if (bestPos == null || mc.player.squaredDistanceTo(pos.toCenterPos()) < distance) {
                        bestPos = pos;
                        distance = mc.player.squaredDistanceTo(pos.toCenterPos());
                    }
                }
                if (bestPos != null) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(bestPos.getX() + 0.5, bestPos.getY(), bestPos.getZ() + 0.5, false));
                }
            }
            case Invalid -> {
                for (int i = 0; i < 20; i++)
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1337, mc.player.getZ(), false));
            }
            case Fly -> {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16610926093821, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.170005801788139, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.2426308013947485, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.6400880035762786, mc.player.getZ(), false));
            }
            case Glide -> {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0001, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0405, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0802, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.1027, mc.player.getZ(), false));
            }
            case TrollHack ->
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false));
            case Normal ->
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.9, mc.player.getZ(), false));
            case ToVoid ->
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), -70, mc.player.getZ(), false));
            case ToVoid2 ->
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), -7, mc.player.getZ(), false));
            case Rotation -> {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(-180, -90, false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(180, 90, false));
            }
        }
        if (disable.getValue()) disable();
    }

    private void placeBlock(BlockPos pos) {
        if (canPlace(pos) && !placePos.contains(pos) && progress < blocksPer.getValueInt()) {
            Direction side;
            if ((side = BlockUtil.getPlaceSide(pos)) == null) return;
            directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
            if (this.rotate.getValue()) {
                if (!faceVector(directionVec)) return;
            }
            placePos.add(pos);
            if (BlockUtil.airPlace()) {
                progress++;
                BlockUtil.placedPos.add(pos);
                if (sound.getValue())
                    mc.world.playSound(mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
                BlockUtil.clickBlock(pos, Direction.DOWN, false, packetPlace.getValue());
            }
            progress++;
            BlockUtil.placedPos.add(pos);
            if (sound.getValue())
                mc.world.playSound(mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, packetPlace.getValue());
        }
    }

    private void doSwap(int slot) {
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private void gotoPos(BlockPos offPos) {
        //mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.2, mc.player.getZ(), false));
        if (rotate.getValue()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(offPos.getX() + 0.5, mc.player.getY() + 0.1, offPos.getZ() + 0.5, false));
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(offPos.getX() + 0.5, mc.player.getY() + 0.1, offPos.getZ() + 0.5, iLnv_09.ROTATION.rotationYaw, 90, false));
        }
    }

    private boolean canMove(BlockPos pos) {
        return mc.world.isAir(pos) && mc.world.isAir(pos.up());
    }

    public boolean canPlace(BlockPos pos) {
        if (noSelfPos.getValue() && pos.equals(EntityUtil.getPlayerPos(true))) {
            return false;
        }
        if (!BlockUtil.airPlace() && BlockUtil.getPlaceSide(pos) == null) {
            return false;
        }
        if (!BlockUtil.canReplace(pos)) {
            return false;
        }
        if (detectMine.getValue() && iLnv_09.BREAK.isMining(pos)) {
            return false;
        }
        return !hasEntity(pos);
    }

    private boolean hasEntity(BlockPos pos) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (entity == mc.player) continue;
            if (!entity.isAlive() || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof ExperienceBottleEntity || entity instanceof ArrowEntity || entity instanceof EndCrystalEntity && breakCrystal.getValue() || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue())
                continue;
            return true;
        }
        return false;
    }

    private boolean checkSelf(BlockPos pos) {
        return mc.player.getBoundingBox().intersects(new Box(pos));
    }

    private boolean trapped(BlockPos pos) {
        return (mc.world.canCollide(mc.player, new Box(pos)) || BlockUtil.getBlock(pos) == Blocks.COBWEB) && checkSelf(pos.down(2));
    }

    /**
     * 检查玩家是否完全在实心方块内部
     */
    private boolean isPlayerInsideSolidBlock() {
        BlockPos playerBlockPos = EntityUtil.getPlayerPos(true);
        Box playerBox = mc.player.getBoundingBox();

        // 检查玩家所在的方块及其周围的方块
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) { // 包含脚部到头部
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = playerBlockPos.add(x, y, z);
                    Box blockBox = new Box(checkPos);

                    // 如果方块是实心的且与玩家碰撞箱相交
                    if (mc.world.getBlockState(checkPos).blocksMovement() &&
                            playerBox.intersects(blockBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 从 BurrowTwo 复制的智能激活检查（需要自定义实现）
     */
    private boolean shouldActivate() {
        // 这里需要根据你的实际情况实现智能激活逻辑
        // 例如：检查周围是否有敌人、检查生命值等
        return true; // 默认返回true，需要根据实际情况修改
    }

    /**
     * 从 BurrowTwo 复制的移动检查
     */
    private boolean isMoving() {
        // 检查玩家是否在移动
        return mc.player.input.movementForward != 0 ||
                mc.player.input.movementSideways != 0 ||
                mc.player.isSprinting() ||
                mc.player.isSwimming();
    }

    public int getBlock() {
        if (inventory.getValue()) {
            // 首先尝试查找黑曜石
            int obsidianSlot = InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
            if (obsidianSlot != -1) {
                return obsidianSlot;
            }

            // 如果没找到黑曜石，尝试查找哭泣黑曜石
            int cryingObsidianSlot = InventoryUtil.findBlockInventorySlot(Blocks.CRYING_OBSIDIAN);
            if (cryingObsidianSlot != -1) {
                return cryingObsidianSlot;
            }

            // 如果都没找到且允许使用末影箱，则使用末影箱
            if (enderChest.getValue()) {
                return InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
            }

            return -1;
        } else {
            // 首先尝试查找黑曜石
            int obsidianSlot = InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (obsidianSlot != -1) {
                return obsidianSlot;
            }

            // 如果没找到黑曜石，尝试查找哭泣黑曜石
            int cryingObsidianSlot = InventoryUtil.findBlock(Blocks.CRYING_OBSIDIAN);
            if (cryingObsidianSlot != -1) {
                return cryingObsidianSlot;
            }

            // 如果都没找到且允许使用末影箱，则使用末影箱
            if (enderChest.getValue()) {
                return InventoryUtil.findBlock(Blocks.ENDER_CHEST);
            }

            return -1;
        }
    }

    public enum LagBackMode {
        Smart, Invalid, TrollHack, ToVoid, ToVoid2, Normal, Rotation, Fly, Glide
    }
}