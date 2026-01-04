package dev.iLnv_09.mod.modules.impl.combat;

import dev.iLnv_09.api.utils.entity.EntityUtil;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AntiPush extends Module {
    public static AntiPush INSTANCE;
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    public final BooleanSetting packet = this.add(new BooleanSetting("Packet", true));
    public final BooleanSetting ncp = this.add(new BooleanSetting("NCP", false));
    public final BooleanSetting helper = this.add(new BooleanSetting("Helper", true));
    public final BooleanSetting trap = this.add((new BooleanSetting("Trap", true)).setParent());
    private final BooleanSetting onlyBurrow = this.add((new BooleanSetting("OnlyBurrow", true, () -> {
        return this.trap.isOpen();
    })).setParent());
    private final BooleanSetting whenDouble = this.add(new BooleanSetting("WhenDouble", true, () -> {
        return this.trap.isOpen() && this.onlyBurrow.isOpen();
    }));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));

    public AntiPush() {
        super("AntiPush", "Trap self when piston kick", Module.Category.Combat);
        INSTANCE = this;
        setChinese("反活塞");
    }

    public static boolean canPlace(BlockPos pos) {
        if (!BlockUtil.canBlockFacing(pos)) {
            return false;
        } else if (!BlockUtil.canReplace(pos)) {
            return false;
        } else {
            return !BlockUtil.hasEntity(pos, false);
        }
    }

    public void onUpdate() {
        if (mc.player.isOnGround()) {
            if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                this.block();
            }
        }
    }

    private void block() {
        BlockPos pos = EntityUtil.getPlayerPos();
        if (this.getBlock(pos.up(2)) != Blocks.OBSIDIAN && this.getBlock(pos.up(2)) != Blocks.BEDROCK) {
            int progress = 0;
            Direction[] var3;
            int var4;
            int var5;
            Direction i;
            if (this.whenDouble.getValue()) {
                var3 = Direction.values();
                var4 = var3.length;

                for (var5 = 0; var5 < var4; ++var5) {
                    i = var3[var5];
                    if (i != Direction.DOWN && i != Direction.UP && this.getBlock(pos.offset(i).up()) instanceof PistonBlock && ((Direction) mc.world.getBlockState(pos.offset(i).up()).get(FacingBlock.FACING)).getOpposite() == i) {
                        ++progress;
                    }
                }
            }

            var3 = Direction.values();
            var4 = var3.length;

            for (var5 = 0; var5 < var4; ++var5) {
                i = var3[var5];
                if (i != Direction.DOWN && i != Direction.UP && this.getBlock(pos.offset(i).up()) instanceof PistonBlock && ((Direction) mc.world.getBlockState(pos.offset(i).up()).get(FacingBlock.FACING)).getOpposite() == i) {
                    this.placeBlock(pos.up().offset(i, -1));
                    if (this.trap.getValue() && (this.getBlock(pos) != Blocks.AIR || !this.onlyBurrow.getValue() || progress >= 2)) {
                        this.placeBlock(pos.up(2));
                        if (!BlockUtil.canPlace(pos.up(2))) {
                            Direction[] var7 = Direction.values();

                            for (Direction i2 : var7) {
                                if (canPlace(pos.offset(i2).up(2))) {
                                    this.placeBlock(pos.offset(i2).up(2));
                                    break;
                                }
                            }
                        }
                    }

                    if (!BlockUtil.canPlace(pos.up().offset(i, -1)) && this.helper.getValue()) {
                        if (BlockUtil.canPlace(pos.offset(i, -1))) {
                            this.placeBlock(pos.offset(i, -1));
                        } else {
                            this.placeBlock(pos.offset(i, -1).down());
                        }
                    }
                }
            }

        }
    }

    private Block getBlock(BlockPos block) {
        return mc.world.getBlockState(block).getBlock();
    }

    private void placeBlock(BlockPos pos) {
        if (canPlace(pos)) {
            int old = mc.player.getInventory().selectedSlot;
            int block = this.findBlock(Blocks.OBSIDIAN);
            if (block != -1) {
                this.doSwap(block);
                if (ncp.getValue()) {
                    BlockUtil.placeBlock2(pos, rotate.getValue(), packet.getValue(), true);
                } else {
                    BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packet.getValue());
                }
                if (this.inventory.getValue()) {
                    this.doSwap(block);
                    EntityUtil.syncInventory();
                } else {
                    this.doSwap(old);
                }

            }
        }
    }

    public int findBlock(Block blockIn) {
        return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }

    }
}