package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.utils.math.MathUtil;
import dev.iLnv_09.api.utils.render.Render3DUtil;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.List;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class findbase extends Module {
    // 设置项
    private final SliderSetting chestCount = add(new SliderSetting("ChestCount", 10, 0, 100, 1));
    private final SliderSetting enderChestCount = add(new SliderSetting("EnderChestCount", 0, 0, 100, 1));
    private final SliderSetting hopperCount = add(new SliderSetting("HopperCount", 0, 0, 200, 1));
    private final SliderSetting shulkerCount = add(new SliderSetting("ShulkerCount", 5, 0, 100, 1));
    private final SliderSetting barrelCount = add(new SliderSetting("BarrelCount", 5, 0, 100, 1));
    private final SliderSetting netherPortalCount = add(new SliderSetting("NetherPortalCount", 0, 0, 100, 1));
    private final SliderSetting renderDistance = add(new SliderSetting("RenderDistance", 128, 32, 512, 1));
    private final SliderSetting detectionDelay = add(new SliderSetting("DetectionDelay", 20, 1, 100, 1));
    private final BooleanSetting render = add(new BooleanSetting("Render", true));
    private final BooleanSetting notify = add(new BooleanSetting("Notify", true));
    private final BooleanSetting logToFile = add(new BooleanSetting("LogToFile", true));
    private final BooleanSetting showHUD = add(new BooleanSetting("ShowHUD", true));

    // 存储已发现的基地
    private final java.util.List<BaseLocation> foundBases = new ArrayList<>();
    private final Set<ChunkPos> detectedChunks = new HashSet<>();
    private final File logFile;
    private int tickCounter = 0;

    // 内部类，用于存储基地信息
    private static class BaseLocation {
        final ChunkPos chunkPos;
        final ArrayList<BlockPos> chestPositions = new ArrayList<>();
        final ArrayList<BlockPos> enderChestPositions = new ArrayList<>();
        final ArrayList<BlockPos> hopperPositions = new ArrayList<>();
        final ArrayList<BlockPos> shulkerPositions = new ArrayList<>();
        final ArrayList<BlockPos> barrelPositions = new ArrayList<>();
        final ArrayList<BlockPos> portalPositions = new ArrayList<>();

        BaseLocation(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
        }
    }

    public findbase() {
        super("FindBase", Category.Misc);
        setChinese("基地探测");

        File dir = new File(mc.runDirectory, "iLnv_09");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        logFile = new File(dir, "findbase.txt");
    }

    @Override
    public void onEnable() {
        foundBases.clear();
        detectedChunks.clear();
        tickCounter = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        foundBases.clear();
        detectedChunks.clear();
        super.onDisable();
    }

    @Override
    public void onUpdate() {
        if (mc.world == null || mc.player == null) return;

        tickCounter++;
        if (tickCounter < detectionDelay.getValueInt()) {
            return;
        }
        tickCounter = 0;

        // 获取当前加载的所有区块
        for (WorldChunk chunk : BlockUtil.getLoadedChunks().collect(Collectors.toList())) {
            if (chunk == null || chunk.isEmpty() || detectedChunks.contains(chunk.getPos())) continue;

            BaseLocation potentialBase = new BaseLocation(chunk.getPos());
            int chests = 0;
            int enderChests = 0;
            int hoppers = 0;
            int shulkers = 0;
            int barrels = 0;
            int netherPortals = 0;

            // 遍历区块内的所有方块实体
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                BlockPos pos = blockEntity.getPos();
                Block block = chunk.getBlockState(pos).getBlock();

                if (blockEntity instanceof ChestBlockEntity) {
                    chests++;
                    potentialBase.chestPositions.add(pos);
                } else if (blockEntity instanceof EnderChestBlockEntity) {
                    enderChests++;
                    potentialBase.enderChestPositions.add(pos);
                } else if (block == Blocks.HOPPER) {
                    hoppers++;
                    potentialBase.hopperPositions.add(pos);
                } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
                    shulkers++;
                    potentialBase.shulkerPositions.add(pos);
                } else if (blockEntity instanceof BarrelBlockEntity) {
                    barrels++;
                    potentialBase.barrelPositions.add(pos);
                }
            }

            // 遍历区块寻找地狱门
            for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                        if (chunk.getBlockState(pos).getBlock() == Blocks.NETHER_PORTAL) {
                            netherPortals++;
                            potentialBase.portalPositions.add(pos);
                        }
                    }
                }
            }

            // 检查是否满足基地条件
            boolean isBase = chests >= chestCount.getValueInt() &&
                    enderChests >= enderChestCount.getValueInt() &&
                    hoppers >= hopperCount.getValueInt() &&
                    shulkers >= shulkerCount.getValueInt() &&
                    barrels >= barrelCount.getValueInt() &&
                    netherPortals >= netherPortalCount.getValueInt();

            if (isBase) {
                detectedChunks.add(chunk.getPos());
                foundBases.add(potentialBase);

                int baseX = (chunk.getPos().x << 4) + 8;
                int baseZ = (chunk.getPos().z << 4) + 8;
                int baseY = 64;

                String reason = String.format("Chests: %d, E-Chests: %d, Hoppers: %d, Shulkers: %d, Barrels: %d, Portals: %d",
                        chests, enderChests, hoppers, shulkers, barrels, netherPortals);

                if (notify.getValue()) {
                    String message = "发现基地! 坐标: " + baseX + ", " + baseY + ", " + baseZ + " - " + reason;
                    sendNotify(message);
                }

                if (logToFile.getValue()) {
                    logBaseToFile(baseX, baseY, baseZ, reason);
                }
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (mc.world == null || mc.player == null || !render.getValue()) return;

        for (BaseLocation base : foundBases) {
            BlockPos center = base.chunkPos.getCenterAtY(64);
            if (mc.player.getBlockPos().getSquaredDistance(center) > renderDistance.getValue() * renderDistance.getValue()) {
                continue;
            }

            renderBlocks(matrixStack, base.chestPositions, new Color(255, 165, 0, 150)); // 橙色
            renderBlocks(matrixStack, base.enderChestPositions, new Color(138, 43, 226, 150)); // 紫色
            renderBlocks(matrixStack, base.hopperPositions, new Color(169, 169, 169, 150)); // 灰色
            renderBlocks(matrixStack, base.shulkerPositions, new Color(255, 0, 255, 150)); // 紫色
            renderBlocks(matrixStack, base.barrelPositions, new Color(139, 69, 19, 150)); // 棕色
            renderBlocks(matrixStack, base.portalPositions, new Color(255, 0, 0, 150)); // 红色
        }

    }

    private void renderBlocks(MatrixStack matrixStack, ArrayList<BlockPos> positions, Color color) {
        for (BlockPos pos : positions) {
            Box box = new Box(pos);
            Render3DUtil.draw3DBox(matrixStack, box, color);
        }
    }

    private void logBaseToFile(int x, int y, int z, String reason) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logEntry = timestamp + " - 基地坐标: " + x + ", " + y + ", " + z + " - 原因: " + reason + "\n";
            writer.write(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRender2D(net.minecraft.client.gui.DrawContext drawContext, float tickDelta) {
        if (mc.player == null || !showHUD.getValue() || foundBases.isEmpty()) {
            return;
        }

        int yOffset = 2;
        final int xOffset = 2;
        final int color = 0xFFFFFFFF; // 白色

        drawContext.drawTextWithShadow(mc.textRenderer, "Found Bases:", xOffset, yOffset, color);
        yOffset += 12;

        // 排序并限制显示数量
        List<BaseLocation> sortedBases = new ArrayList<>(foundBases);
        sortedBases.sort(Comparator.comparingDouble(base -> mc.player.getBlockPos().getSquaredDistance(base.chunkPos.getCenterAtY(mc.player.getBlockY()))));

        int count = 0;
        for (BaseLocation base : sortedBases) {
            if (count >= 5) break;

            BlockPos center = base.chunkPos.getCenterAtY(base.chunkPos.getCenterAtY(64).getY());
            double distance = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(center));

            String info = String.format("Pos: %d, %d, %d (%.1fm)", center.getX(), center.getY(), center.getZ(), distance);
            drawContext.drawTextWithShadow(mc.textRenderer, info, xOffset, yOffset, color);
            yOffset += 10;

            StringBuilder details = new StringBuilder("  ");
            if (!base.chestPositions.isEmpty()) details.append(String.format("C: %d ", base.chestPositions.size()));
            if (!base.shulkerPositions.isEmpty()) details.append(String.format("S: %d ", base.shulkerPositions.size()));
            if (!base.barrelPositions.isEmpty()) details.append(String.format("B: %d ", base.barrelPositions.size()));
            if (!base.enderChestPositions.isEmpty()) details.append(String.format("E: %d ", base.enderChestPositions.size()));
            if (!base.hopperPositions.isEmpty()) details.append(String.format("H: %d ", base.hopperPositions.size()));
            if (!base.portalPositions.isEmpty()) details.append(String.format("P: %d", base.portalPositions.size()));

            drawContext.drawTextWithShadow(mc.textRenderer, details.toString(), xOffset, yOffset, 0xFFCCCCCC); // 浅灰色
            yOffset += 12;
            count++;
        }
    }
}