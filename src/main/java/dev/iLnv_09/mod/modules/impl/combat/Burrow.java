package dev.iLnv_09.mod.modules.impl.combat;

import dev.iLnv_09.iLnv_09;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.Packet;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

public class Burrow
        extends Module {
    public static Burrow INSTANCE;
    public boolean hasBurrowItem = false;
    private final Timer timer = new Timer();
    private final Timer webTimer = new Timer();
    private final BooleanSetting disable = this.add(new BooleanSetting("Disable", true));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 500, 0, 1000, () -> !this.disable.getValue()));
    private final SliderSetting webTime = this.add(new SliderSetting("WebTime", 0, 0, 500));
    private final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", true));
    private final BooleanSetting antiLag = this.add(new BooleanSetting("AntiLag", false));
    private final BooleanSetting detectMine = this.add(new BooleanSetting("DetectMining", false));
    private final BooleanSetting headFill = this.add(new BooleanSetting("HeadFill", false));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
    private final BooleanSetting down = this.add(new BooleanSetting("Down", true));
    private final BooleanSetting noSelfPos = this.add(new BooleanSetting("NoSelfPos", false));
    private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
    private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
    private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 4.0, 1.0, 4.0, 1.0));
    private final EnumSetting<RotateMode> rotate = this.add(new EnumSetting<RotateMode>("RotateMode", RotateMode.Bypass));
    private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true));
    private final BooleanSetting wait = this.add(new BooleanSetting("Wait", true, () -> !this.disable.getValue()));
    private final BooleanSetting fakeMove = this.add(new BooleanSetting("FakeMove", true).setParent());
    private final BooleanSetting center = this.add(new BooleanSetting("AllowCenter", false, () -> this.fakeMove.isOpen()));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final EnumSetting<LagBackMode> lagMode = this.add(new EnumSetting<LagBackMode>("LagMode", LagBackMode.TrollHack));
    private final EnumSetting<LagBackMode> aboveLagMode = this.add(new EnumSetting<LagBackMode>("MoveLagMode", LagBackMode.Smart));
    private final SliderSetting smartX = this.add(new SliderSetting("SmartXZ", 3.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == LagBackMode.Smart || this.aboveLagMode.getValue() == LagBackMode.Smart));
    private final SliderSetting smartUp = this.add(new SliderSetting("SmartUp", 3.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == LagBackMode.Smart || this.aboveLagMode.getValue() == LagBackMode.Smart));
    private final SliderSetting smartDown = this.add(new SliderSetting("SmartDown", 3.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == LagBackMode.Smart || this.aboveLagMode.getValue() == LagBackMode.Smart));
    private final SliderSetting smartDistance = this.add(new SliderSetting("SmartDistance", 2.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == LagBackMode.Smart || this.aboveLagMode.getValue() == LagBackMode.Smart));
    private int progress = 0;
    public final List<BlockPos> placePos = new ArrayList<BlockPos>();

    public Burrow() {
        super("Burrow", Module.Category.Combat);
        this.setChinese("卡黑曜石");
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        boolean bl = this.hasBurrowItem = this.getBlock() != -1;
        if (iLnv_09.PLAYER.isInWeb((PlayerEntity)Burrow.mc.player)) {
            this.webTimer.reset();
            return;
        }
        if (this.usingPause.getValue() && Burrow.mc.player.isUsingItem()) {
            return;
        }
        if (!this.webTimer.passed(this.webTime.getValue())) {
            return;
        }
        if (!this.disable.getValue() && !this.timer.passed(this.delay.getValue())) {
            return;
        }
        if (!Burrow.mc.player.isOnGround()) {
            return;
        }
        if (this.antiLag.getValue() && !Burrow.mc.world.getBlockState(EntityUtil.getPlayerPos().down()).blocksMovement()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        int oldSlot = Burrow.mc.player.getInventory().selectedSlot;
        int block = this.getBlock();
        if (block == -1) {
            CommandManager.sendChatMessageWidthId("§c§oObsidian" + (this.enderChest.getValue() ? "/EnderChest" : "") + "?", this.hashCode());
            this.disable();
            return;
        }
        this.progress = 0;
        this.placePos.clear();
        double offset = AntiCheat.getOffset();
        BlockPosX pos1 = new BlockPosX(Burrow.mc.player.getX() + offset, Burrow.mc.player.getY() + 0.5, Burrow.mc.player.getZ() + offset);
        BlockPosX pos2 = new BlockPosX(Burrow.mc.player.getX() - offset, Burrow.mc.player.getY() + 0.5, Burrow.mc.player.getZ() + offset);
        BlockPosX pos3 = new BlockPosX(Burrow.mc.player.getX() + offset, Burrow.mc.player.getY() + 0.5, Burrow.mc.player.getZ() - offset);
        BlockPosX pos4 = new BlockPosX(Burrow.mc.player.getX() - offset, Burrow.mc.player.getY() + 0.5, Burrow.mc.player.getZ() - offset);
        BlockPosX pos5 = new BlockPosX(Burrow.mc.player.getX() + offset, Burrow.mc.player.getY() + 1.5, Burrow.mc.player.getZ() + offset);
        BlockPosX pos6 = new BlockPosX(Burrow.mc.player.getX() - offset, Burrow.mc.player.getY() + 1.5, Burrow.mc.player.getZ() + offset);
        BlockPosX pos7 = new BlockPosX(Burrow.mc.player.getX() + offset, Burrow.mc.player.getY() + 1.5, Burrow.mc.player.getZ() - offset);
        BlockPosX pos8 = new BlockPosX(Burrow.mc.player.getX() - offset, Burrow.mc.player.getY() + 1.5, Burrow.mc.player.getZ() - offset);
        BlockPosX pos9 = new BlockPosX(Burrow.mc.player.getX() + offset, Burrow.mc.player.getY() - 1.0, Burrow.mc.player.getZ() + offset);
        BlockPosX pos10 = new BlockPosX(Burrow.mc.player.getX() - offset, Burrow.mc.player.getY() - 1.0, Burrow.mc.player.getZ() + offset);
        BlockPosX pos11 = new BlockPosX(Burrow.mc.player.getX() + offset, Burrow.mc.player.getY() - 1.0, Burrow.mc.player.getZ() - offset);
        BlockPosX pos12 = new BlockPosX(Burrow.mc.player.getX() - offset, Burrow.mc.player.getY() - 1.0, Burrow.mc.player.getZ() - offset);
        BlockPos playerPos = EntityUtil.getPlayerPos();
        boolean headFill = false;
        if (!(this.canPlace(pos1) || this.canPlace(pos2) || this.canPlace(pos3) || this.canPlace(pos4))) {
            boolean cantDown;
            boolean cantHeadFill = !this.headFill.getValue() || !this.canPlace(pos5) && !this.canPlace(pos6) && !this.canPlace(pos7) && !this.canPlace(pos8);
            boolean bl2 = cantDown = !this.down.getValue() || !this.canPlace(pos9) && !this.canPlace(pos10) && !this.canPlace(pos11) && !this.canPlace(pos12);
            if (cantHeadFill) {
                if (cantDown) {
                    if (!this.wait.getValue() && this.disable.getValue()) {
                        this.disable();
                    }
                    return;
                }
            } else {
                headFill = true;
            }
        }
        boolean above = false;
        BlockPos headPos = EntityUtil.getPlayerPos().up(2);
        boolean rotate = this.rotate.getValue() == RotateMode.Normal;
        CombatUtil.attackCrystal(pos1, rotate, false);
        CombatUtil.attackCrystal(pos2, rotate, false);
        CombatUtil.attackCrystal(pos3, rotate, false);
        CombatUtil.attackCrystal(pos4, rotate, false);
        if (headFill || Burrow.mc.player.isCrawling() || this.trapped(headPos) || this.trapped(headPos.add(1, 0, 0)) || this.trapped(headPos.add(-1, 0, 0)) || this.trapped(headPos.add(0, 0, 1)) || this.trapped(headPos.add(0, 0, -1)) || this.trapped(headPos.add(1, 0, -1)) || this.trapped(headPos.add(-1, 0, -1)) || this.trapped(headPos.add(1, 0, 1)) || this.trapped(headPos.add(-1, 0, 1))) {
            above = true;
            if (!this.fakeMove.getValue()) {
                if (!this.wait.getValue() && this.disable.getValue()) {
                    this.disable();
                }
                return;
            }
            boolean moved = false;
            BlockPos offPos = playerPos;
            if (!(!this.checkSelf(offPos) || BlockUtil.canReplace(offPos) || this.headFill.getValue() && BlockUtil.canReplace(offPos.up()))) {
                this.gotoPos(offPos);
            } else {
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.UP || direction == Direction.DOWN || !this.checkSelf(offPos = playerPos.offset(direction)) || BlockUtil.canReplace(offPos) || this.headFill.getValue() && BlockUtil.canReplace(offPos.up())) continue;
                    this.gotoPos(offPos);
                    moved = true;
                    break;
                }
                if (!moved) {
                    for (Direction direction : Direction.values()) {
                        if (direction == Direction.UP || direction == Direction.DOWN || !this.checkSelf(offPos = playerPos.offset(direction))) continue;
                        this.gotoPos(offPos);
                        moved = true;
                        break;
                    }
                    if (!moved) {
                        if (!this.center.getValue()) {
                            return;
                        }
                        for (Direction direction : Direction.values()) {
                            if (direction == Direction.UP || direction == Direction.DOWN || !this.canMove(offPos = playerPos.offset(direction))) continue;
                            this.gotoPos(offPos);
                            moved = true;
                            break;
                        }
                        if (!moved) {
                            if (!this.wait.getValue() && this.disable.getValue()) {
                                this.disable();
                            }
                            return;
                        }
                    }
                }
            }
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 0.4199999868869781, Burrow.mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 0.7531999805212017, Burrow.mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 0.9999957640154541, Burrow.mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.1661092609382138, Burrow.mc.player.getZ(), false));
        }
        this.timer.reset();
        this.doSwap(block);
        if (this.rotate.getValue() == RotateMode.Bypass) {
            iLnv_09.ROTATION.snapAt(iLnv_09.ROTATION.rotationYaw, 90.0f);
        }
        this.placeBlock(playerPos, rotate);
        this.placeBlock(pos1, rotate);
        this.placeBlock(pos2, rotate);
        this.placeBlock(pos3, rotate);
        this.placeBlock(pos4, rotate);
        if (this.down.getValue()) {
            this.placeBlock(pos9, rotate);
            this.placeBlock(pos10, rotate);
            this.placeBlock(pos11, rotate);
            this.placeBlock(pos12, rotate);
        }
        if (this.headFill.getValue() && above) {
            this.placeBlock(pos5, rotate);
            this.placeBlock(pos6, rotate);
            this.placeBlock(pos7, rotate);
            this.placeBlock(pos8, rotate);
        }
        if (this.inventory.getValue()) {
            this.doSwap(block);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(oldSlot);
        }
        switch (above ? this.aboveLagMode.getValue() : this.lagMode.getValue()) {
            case Smart: {
                ArrayList<BlockPosX> list = new ArrayList<BlockPosX>();
                for (double x = Burrow.mc.player.getPos().getX() - this.smartX.getValue(); x < Burrow.mc.player.getPos().getX() + this.smartX.getValue(); x += 1.0) {
                    for (double z = Burrow.mc.player.getPos().getZ() - this.smartX.getValue(); z < Burrow.mc.player.getPos().getZ() + this.smartX.getValue(); z += 1.0) {
                        for (double d = Burrow.mc.player.getPos().getY() - this.smartDown.getValue(); d < Burrow.mc.player.getPos().getY() + this.smartUp.getValue(); d += 1.0) {
                            list.add(new BlockPosX(x, d, z));
                        }
                    }
                }
                double distance = 0.0;
                BlockPos bestPos = null;
                for (BlockPos blockPos : list) {
                    if (!this.canMove(blockPos) || (double) MathHelper.sqrt((float)((float)Burrow.mc.player.squaredDistanceTo(blockPos.toCenterPos().add(0.0, -0.5, 0.0)))) < this.smartDistance.getValue() || bestPos != null && !(Burrow.mc.player.squaredDistanceTo(blockPos.toCenterPos()) < distance)) continue;
                    bestPos = blockPos;
                    distance = Burrow.mc.player.squaredDistanceTo(blockPos.toCenterPos());
                }
                if (bestPos == null) break;
                mc.getNetworkHandler().sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround((double)bestPos.getX() + 0.5, (double)bestPos.getY(), (double)bestPos.getZ() + 0.5, false));
                break;
            }
            case Invalid: {
                for (int i = 0; i < 20; ++i) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1337.0, Burrow.mc.player.getZ(), false));
                }
                break;
            }
            case Fly: {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.16610926093821, Burrow.mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.170005801788139, Burrow.mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.2426308013947485, Burrow.mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 2.3400880035762786, Burrow.mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 2.640088003576279, Burrow.mc.player.getZ(), false));
                break;
            }
            case Glide: {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.0001, Burrow.mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.0405, Burrow.mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.0802, Burrow.mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.1027, Burrow.mc.player.getZ(), false));
                break;
            }
            case TrollHack: {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 2.3400880035762786, Burrow.mc.player.getZ(), false));
                break;
            }
            case Normal: {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), Burrow.mc.player.getY() + 1.9, Burrow.mc.player.getZ(), false));
                break;
            }
            case ToVoid: {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), -70.0, Burrow.mc.player.getZ(), false));
                break;
            }
            case ToVoid2: {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Burrow.mc.player.getX(), -7.0, Burrow.mc.player.getZ(), false));
                break;
            }
            case Rotation: {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(-180.0f, -90.0f, false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(180.0f, 90.0f, false));
            }
        }
        if (this.disable.getValue()) {
            this.disable();
        }
    }

    private void placeBlock(BlockPos pos, boolean rotate) {
        if (this.canPlace(pos) && !this.placePos.contains(pos) && this.progress < this.blocksPer.getValueInt()) {
            Direction side;
            this.placePos.add(pos);
            if (BlockUtil.airPlace()) {
                ++this.progress;
                BlockUtil.placedPos.add(pos);
                if (this.sound.getValue()) {
                    Burrow.mc.world.playSound((PlayerEntity)Burrow.mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
                }
                BlockUtil.clickBlock(pos, Direction.DOWN, rotate, this.packetPlace.getValue());
            }
            if ((side = BlockUtil.getPlaceSide(pos)) == null) {
                return;
            }
            ++this.progress;
            BlockUtil.placedPos.add(pos);
            if (this.sound.getValue()) {
                Burrow.mc.world.playSound((PlayerEntity)Burrow.mc.player, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
            }
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), rotate, this.packetPlace.getValue());
        }
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, Burrow.mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private void gotoPos(BlockPos offPos) {
        if (this.rotate.getValue() == RotateMode.None) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround((double)offPos.getX() + 0.5, Burrow.mc.player.getY() + 0.1, (double)offPos.getZ() + 0.5, false));
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full((double)offPos.getX() + 0.5, Burrow.mc.player.getY() + 0.1, (double)offPos.getZ() + 0.5, iLnv_09.ROTATION.rotationYaw, 90.0f, false));
        }
    }

    private boolean canMove(BlockPos pos) {
        return Burrow.mc.world.isAir(pos) && Burrow.mc.world.isAir(pos.up());
    }

    public boolean canPlace(BlockPos pos) {
        if (this.noSelfPos.getValue() && pos.equals((Object)EntityUtil.getPlayerPos())) {
            return false;
        }
        if (!BlockUtil.airPlace() && BlockUtil.getPlaceSide(pos) == null) {
            return false;
        }
        if (!BlockUtil.canReplace(pos)) {
            return false;
        }
        if (this.detectMine.getValue() && iLnv_09.BREAK.isMining(pos)) {
            return false;
        }
        return !this.hasEntity(pos);
    }

    private boolean hasEntity(BlockPos pos) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (entity == Burrow.mc.player || !entity.isAlive() || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof ExperienceBottleEntity || entity instanceof ArrowEntity || entity instanceof EndCrystalEntity && this.breakCrystal.getValue() || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue()) continue;
            return true;
        }
        return false;
    }

    private boolean checkSelf(BlockPos pos) {
        return Burrow.mc.player.getBoundingBox().intersects(new Box(pos));
    }

    private boolean trapped(BlockPos pos) {
        return (Burrow.mc.world.canCollide((Entity)Burrow.mc.player, new Box(pos)) || BlockUtil.getBlock(pos) == Blocks.COBWEB) && this.checkSelf(pos.down(2));
    }

    public int getBlock() {
        if (this.inventory.getValue()) {
            if (InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) != -1 || !this.enderChest.getValue()) {
                return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
            }
            return InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
        }
        if (InventoryUtil.findBlock(Blocks.OBSIDIAN) != -1 || !this.enderChest.getValue()) {
            return InventoryUtil.findBlock(Blocks.OBSIDIAN);
        }
        return InventoryUtil.findBlock(Blocks.ENDER_CHEST);
    }

    private static enum RotateMode {
        Bypass,
        Normal,
        None;

    }

    private static enum LagBackMode {
        Smart,
        Invalid,
        TrollHack,
        ToVoid,
        ToVoid2,
        Normal,
        Rotation,
        Fly,
        Glide;

    }
}