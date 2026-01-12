package dev.iLnv_09.mod.modules.impl.combat;

import dev.iLnv_09.api.utils.combat.CombatUtil;
import dev.iLnv_09.api.utils.entity.EntityUtil;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.entity.MovementUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.exploit.Blink;
import dev.iLnv_09.mod.modules.settings.CombatSetting;
import dev.iLnv_09.mod.modules.impl.player.PacketMine;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public class Autopiston
        extends Module {
    public static Autopiston INSTANCE;
    public static boolean dopush;
    public final EnumSetting<Enum_EeQOXZQmWkBIGBYWBifQ> page = this.add(new EnumSetting<Enum_EeQOXZQmWkBIGBYWBifQ>("Page", Enum_EeQOXZQmWkBIGBYWBifQ.General));
    public final BooleanSetting syncCrystal = this.add(new BooleanSetting("SyncCrystal", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.Sync));
    public final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    public final BooleanSetting allowWeb = this.add(new BooleanSetting("AllowWeb", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    public final SliderSetting updateDelay = this.add(new SliderSetting("UpdateDelay", 100, 0, 500, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    public final SliderSetting surroundCheck = this.add(new SliderSetting("SurroundCheck", 2, 0, 4, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final Timer timer = new Timer();
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotation", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting preferAnchor = this.add(new BooleanSetting("PreferAnchor", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting waitBurrow = this.add(new BooleanSetting("WaitBurrow", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting cancelBurrow = this.add(new BooleanSetting("CancelBurrow", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting cancelBlink = this.add(new BooleanSetting("CancelBlink", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting nomove = this.add(new BooleanSetting("MovePause", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.Sync));
    private final BooleanSetting syncweb = this.add(new BooleanSetting("SyncWeb[!Test]", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.Sync));
    private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting redStonePacket = this.add(new BooleanSetting("RedStonePacket", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting noEating = this.add(new BooleanSetting("NoEating", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting attackCrystal = this.add(new BooleanSetting("BreakCrystal", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting mine = this.add(new BooleanSetting("Mine", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting checkPiston = this.add(new BooleanSetting("CheckPiston", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting pullBack = this.add(new BooleanSetting("PullBack", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General).setParent());
    private final BooleanSetting onlyBurrow = this.add(new BooleanSetting("OnlyBurrow", true, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", false, () -> this.page.getValue() == Enum_EeQOXZQmWkBIGBYWBifQ.General));
    public PlayerEntity displayTarget = null;
    public Vec3d directionVec = null;
    private Enum_EeQOXZQmWkBIGBYWBifQ Page;

    public Autopiston() {
        super("Autopiston", Category.Combat);
        setChinese("自动活塞");
        INSTANCE = this;
    }

    public static void pistonFacing(Direction i) {
        if (i == Direction.EAST) {
            EntityUtil.sendYawAndPitch(-90.0f, 5.0f);
        } else if (i == Direction.WEST) {
            EntityUtil.sendYawAndPitch(90.0f, 5.0f);
        } else if (i == Direction.NORTH) {
            EntityUtil.sendYawAndPitch(180.0f, 5.0f);
        } else if (i == Direction.SOUTH) {
            EntityUtil.sendYawAndPitch(0.0f, 5.0f);
        }
    }

    public static boolean isTargetHere(BlockPos pos, Entity target) {
        return new Box(pos).intersects(target.getBoundingBox());
    }

    public static boolean isInWeb(PlayerEntity player) {
        Vec3d playerPos = player.getPos();
        for (float x : new float[]{0.0f, 0.3f, -0.3f}) {
            for (float z : new float[]{0.0f, 0.3f, -0.3f}) {
                for (float y : new float[]{0.0f, 1.0f, -1.0f}) {
                    BlockPosX pos = new BlockPosX(playerPos.getX() + (double) x, playerPos.getY() + (double) y, playerPos.getZ() + (double) z);
                    if (!Autopiston.isTargetHere(pos, player) || Autopiston.mc.world.getBlockState(pos).getBlock() != Blocks.COBWEB)
                        continue;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onEnable() {
        AutoCrystal.INSTANCE.lastBreakTimer.reset();
    }

    public boolean check(boolean onlyStatic) {
        return MovementUtil.isMoving() && onlyStatic;
    }

    @Override
    public void onUpdate() {
        if (!this.timer.passedMs(this.updateDelay.getValue())) {
            return;
        }
        if (Autopiston.mc.player != null && this.selfGround.getValue() && !Autopiston.mc.player.isOnGround()) {
            if (this.autoDisable.getValue()) {
                this.disable();
            }
            return;
        }
        if (Autopiston.mc.player != null && this.check(this.nomove.getValue())) {
            return;
        }
        if (this.waitBurrow.getValue() && Burrow.INSTANCE.placePos == null) {
            return;
        }
        if (this.cancelBurrow.getValue() && Burrow.INSTANCE.isOn()) {
            return;
        }
        if (this.cancelBlink.getValue() && Blink.INSTANCE.isOn()) {
            return;
        }
        if (this.syncCrystal.getValue() && AutoCrystal.crystalPos != null) {
            return;
        }
        if (this.syncweb.getValue() && AutoWeb.INSTANCE.pos == null) {
            return;
        }
        if (this.findBlock(Blocks.REDSTONE_BLOCK) == -1 || this.findClass(PistonBlock.class) == -1) {
            if (this.autoDisable.getValue()) {
                this.disable();
            }
            return;
        }
        if (this.noEating.getValue() && Autopiston.mc.player.isUsingItem()) {
            return;
        }
        this.timer.reset();
        for (PlayerEntity target : CombatUtil.getEnemies(this.range.getValue())) {
            if (!this.canPush(target).booleanValue() || !target.isOnGround() && this.onlyGround.getValue()) continue;
            this.displayTarget = target;
            if (!this.doPush(EntityUtil.getEntityPos(target), target)) continue;
            return;
        }
        if (this.autoDisable.getValue()) {
            this.disable();
        }
        this.displayTarget = null;
    }

    private boolean checkPiston(BlockPos targetPos) {
        for (Direction i : Direction.values()) {
            BlockPos pos;
            if (i == Direction.DOWN || i == Direction.UP || !(this.getBlock((pos = targetPos.up()).offset(i)) instanceof PistonBlock) || ((Direction) this.getBlockState(pos.offset(i)).get((Property) FacingBlock.FACING)).getOpposite() != i)
                continue;
            for (Direction i2 : Direction.values()) {
                if (this.getBlock(pos.offset(i).offset(i2)) != Blocks.REDSTONE_BLOCK || !this.mine.getValue()) continue;
                this.mine(pos.offset(i).offset(i2));
                if (this.autoDisable.getValue()) {
                    this.disable();
                }
                return true;
            }
        }
        return false;
    }

    public boolean doPush(BlockPos targetPos, PlayerEntity target) {
        if (this.checkPiston.getValue() && this.checkPiston(targetPos)) {
            return true;
        }
        if (!Autopiston.mc.world.getBlockState(targetPos.up(2)).blocksMovement()) {
            BlockPos pos;
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP || !(this.getBlock(pos = targetPos.offset(i).up()) instanceof PistonBlock) || this.getBlockState(pos.offset(i, -2)).blocksMovement() || this.getBlock(pos.offset(i, -2).up()) != Blocks.AIR && this.getBlock(pos.offset(i, -2).up()) != Blocks.REDSTONE_BLOCK || ((Direction) this.getBlockState(pos).get((Property) FacingBlock.FACING)).getOpposite() != i)
                    continue;
                for (Direction i2 : Direction.values()) {
                    if (this.getBlock(pos.offset(i2)) != Blocks.REDSTONE_BLOCK) continue;
                    if (this.mine.getValue()) {
                        this.mine(pos.offset(i2));
                    }
                    if (this.autoDisable.getValue()) {
                        this.disable();
                    }
                    return true;
                }
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP || !(this.getBlock(pos = targetPos.offset(i).up()) instanceof PistonBlock) || this.getBlockState(pos.offset(i, -2)).blocksMovement() || this.getBlock(pos.offset(i, -2).up()) != Blocks.AIR && this.getBlock(pos.offset(i, -2).up()) != Blocks.REDSTONE_BLOCK || ((Direction) this.getBlockState(pos).get((Property) FacingBlock.FACING)).getOpposite() != i || !this.doPower(pos))
                    continue;
                return true;
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP) continue;
                pos = targetPos.offset(i).up();
                if ((Autopiston.mc.player.getY() - target.getY() <= -1.0 || Autopiston.mc.player.getY() - target.getY() >= 2.0) && BlockUtil.distanceToXZ((double) pos.getX() + 0.5, (double) pos.getZ() + 0.5) < 2.6)
                    continue;
                this.attackCrystal(pos);
                if (!this.isTrueFacing(pos, i) || !BlockUtil.clientCanPlace(pos, false) || this.getBlockState(pos.offset(i, -2)).blocksMovement() || this.getBlockState(pos.offset(i, -2).up()).blocksMovement())
                    continue;
                if (BlockUtil.getPlaceSide(pos) == null && this.downPower(pos)) break;
                this.doPiston(i, pos, this.mine.getValue());
                return true;
            }
            if (this.getBlock(targetPos) == Blocks.AIR && this.onlyBurrow.getValue() || !this.pullBack.getValue()) {
                if (this.autoDisable.getValue()) {
                    this.disable();
                }
                return true;
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP) continue;
                pos = targetPos.offset(i).up();
                for (Direction i2 : Direction.values()) {
                    if (!(this.getBlock(pos) instanceof PistonBlock) || this.getBlock(pos.offset(i2)) != Blocks.REDSTONE_BLOCK || ((Direction) this.getBlockState(pos).get((Property) FacingBlock.FACING)).getOpposite() != i)
                        continue;
                    this.mine(pos.offset(i2));
                    if (this.autoDisable.getValue()) {
                        this.disable();
                    }
                    return true;
                }
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP) continue;
                pos = targetPos.offset(i).up();
                for (Direction i2 : Direction.values()) {
                    if (!(this.getBlock(pos) instanceof PistonBlock) || this.getBlock(pos.offset(i2)) != Blocks.AIR || ((Direction) this.getBlockState(pos).get((Property) FacingBlock.FACING)).getOpposite() != i)
                        continue;
                    this.attackCrystal(pos.offset(i2));
                    if (this.doPower(pos, i2)) continue;
                    this.mine(pos.offset(i2));
                    return true;
                }
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP) continue;
                pos = targetPos.offset(i).up();
                if (Autopiston.mc.player != null && (Autopiston.mc.player.getY() - target.getY() <= -1.0 || Autopiston.mc.player.getY() - target.getY() >= 2.0) && BlockUtil.distanceToXZ((double) pos.getX() + 0.5, (double) pos.getZ() + 0.5) < 2.6)
                    continue;
                this.attackCrystal(pos);
                if (!this.isTrueFacing(pos, i) || !BlockUtil.clientCanPlace(pos, false) || this.downPower(pos))
                    continue;
                this.doPiston(i, pos, true);
                return true;
            }
        } else {
            BlockPos pos;
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP || !(this.getBlock(pos = targetPos.offset(i).up()) instanceof PistonBlock) || (!Autopiston.mc.world.isAir(pos.offset(i, -2)) || !Autopiston.mc.world.isAir(pos.offset(i, -2).down())) && !Autopiston.isTargetHere(pos.offset(i, 2), target) || ((Direction) this.getBlockState(pos).get((Property) FacingBlock.FACING)).getOpposite() != i)
                    continue;
                for (Direction i2 : Direction.values()) {
                    if (this.getBlock(pos.offset(i2)) != Blocks.REDSTONE_BLOCK) continue;
                    if (this.mine.getValue()) {
                        this.mine(pos.offset(i2));
                    }
                    if (this.autoDisable.getValue()) {
                        this.disable();
                    }
                    return true;
                }
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP || !(this.getBlock(pos = targetPos.offset(i).up()) instanceof PistonBlock) || (!Autopiston.mc.world.isAir(pos.offset(i, -2)) || !Autopiston.mc.world.isAir(pos.offset(i, -2).down())) && !Autopiston.isTargetHere(pos.offset(i, 2), target) || ((Direction) this.getBlockState(pos).get((Property) FacingBlock.FACING)).getOpposite() != i || !this.doPower(pos))
                    continue;
                return true;
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP) continue;
                pos = targetPos.offset(i).up();
                if (Autopiston.mc.player != null && (Autopiston.mc.player.getY() - target.getY() <= -1.0 || Autopiston.mc.player.getY() - target.getY() >= 2.0) && BlockUtil.distanceToXZ((double) pos.getX() + 0.5, (double) pos.getZ() + 0.5) < 2.6)
                    continue;
                this.attackCrystal(pos);
                if (!this.isTrueFacing(pos, i) || !BlockUtil.clientCanPlace(pos, false) || (!Autopiston.mc.world.isAir(pos.offset(i, -2)) || !Autopiston.mc.world.isAir(pos.offset(i, -2).down())) && !Autopiston.isTargetHere(pos.offset(i, 2), target) || this.getBlockState(pos.offset(i, -2).up()).blocksMovement())
                    continue;
                if (BlockUtil.getPlaceSide(pos) != null || !this.downPower(pos)) {
                    dopush = true;
                    this.doPiston(i, pos, this.mine.getValue());
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private boolean isTrueFacing(BlockPos pos, Direction facing) {
        Vec3d hitVec;
        if (this.yawDeceive.getValue()) {
            return true;
        }
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) {
            side = Direction.UP;
        }
        return Direction.fromRotation(EntityUtil.getLegitRotations(hitVec = pos.offset((side = side.getOpposite()).getOpposite()).toCenterPos().add(new Vec3d((double) side.getVector().getX() * 0.5, (double) side.getVector().getY() * 0.5, (double) side.getVector().getZ() * 0.5)))[0]) == facing;
    }

    private boolean doPower(BlockPos pos, Direction i2) {
        if (!BlockUtil.canPlace(pos.offset(i2), this.placeRange.getValue())) {
            return true;
        }
        int old = 0;
        if (Autopiston.mc.player != null) {
            old = Autopiston.mc.player.getInventory().selectedSlot;
        }
        int power = this.findBlock(Blocks.REDSTONE_BLOCK);
        this.doSwap(power);
        BlockUtil.placeBlock(pos.offset(i2), this.rotate.getValue(), this.redStonePacket.getValue());
        if (this.inventory.getValue()) {
            this.doSwap(power);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(old);
        }
        return false;
    }

    private boolean doPower(BlockPos pos) {
        Direction facing = BlockUtil.getBestNeighboring(pos, null);
        if (facing != null) {
            this.attackCrystal(pos.offset(facing));
            if (!this.doPower(pos, facing)) {
                return true;
            }
        }
        for (Direction i2 : Direction.values()) {
            this.attackCrystal(pos.offset(i2));
            if (this.doPower(pos, i2)) continue;
            return true;
        }
        return false;
    }

    private boolean downPower(BlockPos pos) {
        if (BlockUtil.getPlaceSide(pos) == null) {
            boolean noPower = true;
            for (Direction i2 : Direction.values()) {
                if (this.getBlock(pos.offset(i2)) != Blocks.REDSTONE_BLOCK) continue;
                noPower = false;
                break;
            }
            if (noPower) {
                if (!BlockUtil.canPlace(pos.add(0, -1, 0), this.placeRange.getValue())) {
                    return true;
                }
                int old = 0;
                if (Autopiston.mc.player != null) {
                    old = Autopiston.mc.player.getInventory().selectedSlot;
                }
                int power = this.findBlock(Blocks.REDSTONE_BLOCK);
                this.doSwap(power);
                BlockUtil.placeBlock(pos.add(0, -1, 0), this.rotate.getValue(), this.redStonePacket.getValue());
                if (this.inventory.getValue()) {
                    this.doSwap(power);
                    EntityUtil.syncInventory();
                } else {
                    this.doSwap(old);
                }
            }
        }
        return false;
    }

    private void doPiston(Direction i, BlockPos pos, boolean mine) {
        if (BlockUtil.canPlace(pos, this.placeRange.getValue())) {
            int piston = this.findClass(PistonBlock.class);
            Direction side = BlockUtil.getPlaceSide(pos);
            if (this.rotate.getValue()) {
                EntityUtil.facePosSide(pos.offset(side), side.getOpposite());
            }
            if (this.yawDeceive.getValue()) {
                Autopiston.pistonFacing(i);
            }
            int old = 0;
            if (Autopiston.mc.player != null) {
                old = Autopiston.mc.player.getInventory().selectedSlot;
            }
            this.doSwap(piston);
            BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
            if (this.inventory.getValue()) {
                this.doSwap(piston);
                EntityUtil.syncInventory();
            } else {
                this.doSwap(old);
            }
            if (this.rotate.getValue()) {
                EntityUtil.facePosSide(pos.offset(side), side.getOpposite());
            }
            for (Direction i2 : Direction.values()) {
                if (this.getBlock(pos.offset(i2)) != Blocks.REDSTONE_BLOCK) continue;
                if (mine) {
                    this.mine(pos.offset(i2));
                }
                if (this.autoDisable.getValue()) {
                    this.disable();
                }
                return;
            }
            this.doPower(pos);
        }
    }

    @Override
    public String getInfo() {
        if (this.displayTarget != null) {
            return this.displayTarget.getName().getString();
        }
        return null;
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            if (Autopiston.mc.player != null) {
                InventoryUtil.inventorySwap(slot, Autopiston.mc.player.getInventory().selectedSlot);
            }
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    public int findBlock(Block blockIn) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(blockIn);
        }
        return InventoryUtil.findBlock(blockIn);
    }

    public int findClass(Class clazz) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findClassInventorySlot(clazz);
        }
        return InventoryUtil.findClass(clazz);
    }

    private void attackCrystal(BlockPos pos) {
        if (!this.attackCrystal.getValue()) {
            return;
        }
        if (Autopiston.mc.world != null) {
            for (Entity crystal : Autopiston.mc.world.getEntities()) {
                if (!(crystal instanceof EndCrystalEntity) || (double) MathHelper.sqrt((float) crystal.squaredDistanceTo((double) pos.getX() + 0.5, pos.getY(), (double) pos.getZ() + 0.5)) > 2.0)
                    continue;
                CombatUtil.attackCrystal(crystal, this.rotate.getValue(), false);
                return;
            }
        }
    }

    private void mine(BlockPos pos) {
        PacketMine.INSTANCE.mine(pos);
    }

    private Block getBlock(BlockPos pos) {
        return Autopiston.mc.world.getBlockState(pos).getBlock();
    }

    private BlockState getBlockState(BlockPos pos) {
        if (Autopiston.mc.world != null) {
            return Autopiston.mc.world.getBlockState(pos);
        }
        return null;
    }

    private Boolean canPush(PlayerEntity player) {
        if (this.onlyGround.getValue() && !player.isOnGround()) {
            return false;
        }
        if (!this.allowWeb.getValue() && Autopiston.isInWeb(player)) {
            return false;
        }
        int progress = 0;
        if (!Autopiston.mc.world.isAir(new BlockPosX(player.getX() + 1.0, player.getY() + 0.5, player.getZ()))) {
            ++progress;
        }
        if (!Autopiston.mc.world.isAir(new BlockPosX(player.getX() - 1.0, player.getY() + 0.5, player.getZ()))) {
            ++progress;
        }
        if (!Autopiston.mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() + 1.0))) {
            ++progress;
        }
        if (!Autopiston.mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() - 1.0))) {
            ++progress;
        }
        if (!Autopiston.mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ()))) {
            for (Direction i : Direction.values()) {
                BlockPos pos;
                if (i == Direction.UP || i == Direction.DOWN || (!Autopiston.mc.world.isAir(pos = EntityUtil.getEntityPos(player).offset(i)) || !Autopiston.mc.world.isAir(pos.up())) && !Autopiston.isTargetHere(pos, player))
                    continue;
                if (!Autopiston.mc.world.isAir(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()))) {
                    return true;
                }
                return (double) progress > this.surroundCheck.getValue() - 1.0;
            }
            return false;
        }
        if (!Autopiston.mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                BlockPos pos = EntityUtil.getEntityPos(player).offset(i);
                Box box = player.getBoundingBox().offset(new Vec3d(i.getOffsetX(), i.getOffsetY(), i.getOffsetZ()));
                if (this.getBlock(pos.up()) == Blocks.PISTON_HEAD || Autopiston.mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0)) || Autopiston.isTargetHere(pos, player) || !Autopiston.mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()))))
                    continue;
                return true;
            }
        }
        return (double) progress > this.surroundCheck.getValue() - 1.0 || CombatUtil.isHard(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()));
    }

    public void placeBlock(BlockPos pos, boolean rotate, boolean bypass) {
        Direction side;
        if (BlockUtil.airPlace()) {
            for (Direction i : Direction.values()) {
                if (Autopiston.mc.world == null || !Autopiston.mc.world.isAir(pos.offset(i))) continue;
                BlockUtil.clickBlock(pos, i, rotate);
                return;
            }
        }
        if ((side = BlockUtil.getPlaceSide(pos)) == null) {
            return;
        }
        Vec3d directionVec = new Vec3d((double) pos.getX() + 0.5 + (double) side.getVector().getX() * 0.5, (double) pos.getY() + 0.5 + (double) side.getVector().getY() * 0.5, (double) pos.getZ() + 0.5 + (double) side.getVector().getZ() * 0.5);
        EntityUtil.swingHand(Hand.MAIN_HAND, CombatSetting.INSTANCE.swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        BlockUtil.placedPos.add(pos);
        boolean sprint = false;
        if (Autopiston.mc.player != null) {
            sprint = Autopiston.mc.player.isSprinting();
        }
        boolean sneak = false;
        if (sprint) {
            Autopiston.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(Autopiston.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        if (sneak) {
            Autopiston.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(Autopiston.mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
        BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), rotate);
        if (sneak) {
            Autopiston.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(Autopiston.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
        if (sprint) {
            Autopiston.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(Autopiston.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
        if (bypass) {
            EntityUtil.swingHand(Hand.MAIN_HAND, CombatSetting.INSTANCE.swingMode.getValue());
        }
    }

    /*
     * Exception performing whole class analysis ignored.
     */
    public enum Enum_EeQOXZQmWkBIGBYWBifQ {
        General,
        Sync

    }
}
