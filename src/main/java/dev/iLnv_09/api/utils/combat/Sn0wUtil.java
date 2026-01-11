package dev.iLnv_09.api.utils.combat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 从CatAura移植的水晶计算工具类
 */
public class Sn0wUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * 计算水晶爆炸伤害（从CatAura移植的核心方法）
     * @param target 目标实体
     * @param explosionPos 爆炸位置
     * @param terrainIgnore 是否忽略地形
     * @param miningIgnore 是否忽略挖矿
     * @return 伤害值
     */
    public static float calculateDamage(Entity target, Vec3d explosionPos, boolean terrainIgnore, boolean miningIgnore) {
        if (target == null || mc.world == null) return 0.0f;

        double distance = target.getPos().distanceTo(explosionPos);
        if (distance > 12.0) {
            return 0.0f;
        }

        // 计算方块密度（阻挡程度）
        double density = 1.0;
        if (!terrainIgnore) {
            density = getBlockDensity(explosionPos, target, miningIgnore);
        }

        // 基础伤害公式
        double impact = (1.0 - distance / 12.0) * density;
        double damage = (impact * impact + impact) * 4.0 * 12.0 + 1.0;

        // 应用护甲减伤
        if (target instanceof LivingEntity livingEntity) {
            damage = getDamageForDifficulty(damage);
            damage = applyArmorReduction(damage, livingEntity);
            damage = applyResistanceReduction(damage, livingEntity);
        }

        // 爆炸保护附魔减伤
        damage = applyBlastProtectionReduction(damage, target);

        return Math.max(0.0f, (float) damage);
    }

    /**
     * 获取方块密度（CatAura移植）
     */
    private static double getBlockDensity(Vec3d explosionPos, Entity entity, boolean miningIgnore) {
        if (mc.world == null) return 1.0;

        Box entityBox = entity.getBoundingBox();
        Vec3d entityCenter = entityBox.getCenter();

        // 从爆炸点到实体的射线采样点
        List<Vec3d> samplePoints = new ArrayList<>();
        samplePoints.add(new Vec3d(entityBox.minX, entityBox.minY, entityBox.minZ));
        samplePoints.add(new Vec3d(entityBox.minX, entityBox.minY, entityBox.maxZ));
        samplePoints.add(new Vec3d(entityBox.maxX, entityBox.minY, entityBox.minZ));
        samplePoints.add(new Vec3d(entityBox.maxX, entityBox.minY, entityBox.maxZ));
        samplePoints.add(new Vec3d(entityBox.minX, entityBox.maxY, entityBox.minZ));
        samplePoints.add(new Vec3d(entityBox.minX, entityBox.maxY, entityBox.maxZ));
        samplePoints.add(new Vec3d(entityBox.maxX, entityBox.maxY, entityBox.minZ));
        samplePoints.add(new Vec3d(entityBox.maxX, entityBox.maxY, entityBox.maxZ));
        samplePoints.add(entityCenter);

        int unobstructedRays = 0;
        for (Vec3d samplePoint : samplePoints) {
            // 创建射线追踪上下文
            RaycastContext context = new RaycastContext(
                    explosionPos,
                    samplePoint,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            );

            HitResult result = mc.world.raycast(context);
            if (result.getType() == HitResult.Type.MISS) {
                unobstructedRays++;
                continue;
            }

            // 如果是方块，检查是否应该忽略
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) result;
                BlockPos blockPos = blockResult.getBlockPos();
                BlockState blockState = mc.world.getBlockState(blockPos);
                Block block = blockState.getBlock();

                // 忽略空气、可爆炸方块、部分透明方块
                if (block == Blocks.AIR ||
                        block == Blocks.FIRE ||
                        block == Blocks.WATER ||
                        block == Blocks.LAVA ||
                        block == Blocks.TALL_GRASS ||
                        block == Blocks.GRASS_BLOCK ||
                        block == Blocks.FERN) {
                    unobstructedRays++;
                }
                // 挖矿忽略：忽略正在挖掘的方块
                else if (miningIgnore && CombatUtil.isMiningBlock(blockPos)) {
                    unobstructedRays++;
                }
            }
        }

        return (double) unobstructedRays / (double) samplePoints.size();
    }

    /**
     * 根据游戏难度调整伤害
     */
    private static double getDamageForDifficulty(double damage) {
        // CatAura中使用的是简化计算，这里保持原样
        return damage;
    }

    /**
     * 应用护甲减伤
     */
    private static double applyArmorReduction(double damage, LivingEntity entity) {
        if (entity == null) return damage;

        // Minecraft护甲减伤公式
        int armor = entity.getArmor();
        double armorReduction = damage * armor * 0.04;
        damage -= armorReduction;
        damage = Math.max(damage, damage * 0.2); // 至少造成20%伤害

        // 护甲韧性（仅钻石/下界合金装备）
        int toughness = 0; // Minecraft中护甲韧性属性
        if (entity instanceof PlayerEntity player) {
            // 简化处理：检查是否有高级护甲
            for (int i = 0; i < 4; i++) {
                String armorName = player.getInventory().getArmorStack(i).getName().getString();
                if (armorName.contains("Diamond") || armorName.contains("Netherite")) {
                    toughness = 2;
                    break;
                }
            }
        }

        if (toughness > 0) {
            double toughnessReduction = damage * toughness * 0.04;
            damage -= toughnessReduction;
        }

        return Math.max(0, damage);
    }

    /**
     * 应用抗性效果减伤
     */
    private static double applyResistanceReduction(double damage, LivingEntity entity) {
        if (entity == null) return damage;

        StatusEffectInstance resistance = entity.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) {
            int amplifier = resistance.getAmplifier();
            double reduction = 0.2 * (amplifier + 1);
            damage *= (1.0 - reduction);
        }

        StatusEffectInstance weakness = entity.getStatusEffect(StatusEffects.WEAKNESS);
        if (weakness != null) {
            // 虚弱效果不影响爆炸伤害
        }

        return Math.max(0, damage);
    }

    /**
     * 应用爆炸保护附魔减伤
     */
    private static double applyBlastProtectionReduction(double damage, Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            int blastProtection = getBlastProtectionLevel(livingEntity);
            if (blastProtection > 0) {
                // 每级爆炸保护减少8%爆炸伤害
                double reduction = 0.08 * blastProtection;
                damage *= (1.0 - reduction);
            }
        }
        return Math.max(0, damage);
    }

    /**
     * 获取爆炸保护等级（简化实现）
     */
    private static int getBlastProtectionLevel(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) return 0;

        // 在实际Minecraft中需要检查装备的附魔
        // 这里简化处理，返回一个估计值
        boolean hasAdvancedArmor = false;
        for (int i = 0; i < 4; i++) {
            String armorName = player.getInventory().getArmorStack(i).getName().getString();
            if (armorName.contains("Diamond") || armorName.contains("Netherite")) {
                hasAdvancedArmor = true;
                break;
            }
        }
        return hasAdvancedArmor ? 2 : 0;
    }

    /**
     * 检查是否可以放置水晶（从CatAura移植）
     * @param world 世界实例
     * @param pos 位置
     * @param onePointTwelve 是否使用1.12模式
     * @return 是否可以放置
     */
    public static boolean canPlaceCrystal(World world, BlockPos pos, boolean onePointTwelve) {
        if (world == null) return false;

        BlockPos below = pos.down();
        Block blockBelow = world.getBlockState(below).getBlock();

        // 需要下方是黑曜石或基岩
        if (blockBelow != Blocks.OBSIDIAN && blockBelow != Blocks.BEDROCK) {
            return false;
        }

        // 检查位置是否为空
        if (!isAirOrReplaceable(world, pos)) {
            return false;
        }

        // 1.12版本需要额外检查上方的位置
        if (onePointTwelve) {
            if (!isAirOrReplaceable(world, pos.up())) {
                return false;
            }
        }

        // 检查是否有实体阻挡
        Box box = new Box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + (onePointTwelve ? 3 : 2), pos.getZ() + 1
        );

        // 使用正确的Predicate
        Predicate<Entity> predicate = e -> e.isAlive() && !(e instanceof EndCrystalEntity);
        List<Entity> entities = world.getEntitiesByClass(
                Entity.class,
                box,
                predicate
        );

        // 忽略物品实体和TNT
        entities.removeIf(e -> e instanceof TntEntity);

        return entities.isEmpty();
    }

    /**
     * 检查方块是否是空气或可替换的
     */
    private static boolean isAirOrReplaceable(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.AIR ||
                block == Blocks.FIRE ||
                block == Blocks.SNOW ||
                block == Blocks.SNOW_BLOCK ||
                block == Blocks.VINE ||
                block == Blocks.TALL_GRASS ||
                block == Blocks.GRASS_BLOCK ||
                block == Blocks.FERN ||
                block == Blocks.DEAD_BUSH ||
                block == Blocks.COBWEB;
    }

    /**
     * 获取水晶伤害向量（CatAura方法）
     */
    public static Vec3d crystalDamageVec(BlockPos pos) {
        return Vec3d.of(pos).add(0.5, 1.0, 0.5);
    }

    /**
     * 检查是否可以打破水晶（从CatAura移植）
     * @param world 世界实例
     * @param player 玩家实体
     * @param crystal 水晶实体
     * @param breakRange 打破范围
     * @param breakWallsRange 墙后打破范围
     * @param ticksExisted 水晶存在时间
     * @return 是否可以打破
     */
    public static boolean canBreakCrystal(World world, PlayerEntity player, EndCrystalEntity crystal,
                                          double breakRange, double breakWallsRange, int ticksExisted) {
        if (world == null || player == null || crystal == null) return false;

        // 检查水晶存在时间
        if (ticksExisted > 0 && crystal.age <= ticksExisted) {
            return false;
        }

        // 检查距离
        double distance = player.getEyePos().distanceTo(crystal.getPos().add(0, 1.7, 0));
        if (distance > breakRange) {
            return false;
        }

        // 检查视线
        if (distance > breakWallsRange) {
            // 需要射线追踪检查是否可见
            RaycastContext context = new RaycastContext(
                    player.getEyePos(),
                    crystal.getPos().add(0, 0.5, 0),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            );

            HitResult result = world.raycast(context);
            return result.getType() == HitResult.Type.MISS;
        }

        return true;
    }

    /**
     * 简化的放置检查（不需要World参数）
     */
    public static boolean canPlaceCrystalSimple(BlockPos pos) {
        if (mc.world == null) return false;
        return canPlaceCrystal(mc.world, pos, false);
    }

    /**
     * 简化的打破检查（不需要World和Player参数）
     */
    public static boolean canBreakCrystalSimple(EndCrystalEntity crystal, double breakRange,
                                                double breakWallsRange, int ticksExisted) {
        if (mc.world == null || mc.player == null || crystal == null) return false;
        return canBreakCrystal(mc.world, mc.player, crystal, breakRange, breakWallsRange, ticksExisted);
    }
}