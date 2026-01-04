package dev.iLnv_09.api.utils.combat;

import dev.iLnv_09.api.utils.Wrapper;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.asm.accessors.IExplosion;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.Explosion.DestructionType;

public class MeteorExplosionUtil implements Wrapper {
   public static final Explosion explosion;

   public static double crystalDamage(PlayerEntity player, BlockPos pos, PlayerEntity predict) {
      return explosionDamage(player, pos.toCenterPos().add(0.0, -0.5, 0.0), predict, 6.0F);
   }

   public static double crystalDamage(PlayerEntity player, Vec3d pos, PlayerEntity predict) {
      return explosionDamage(player, pos, predict, 6.0F);
   }

   public static double anchorDamage(PlayerEntity player, BlockPos pos, PlayerEntity predict) {
      if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
         CombatUtil.modifyPos = pos;
         CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
         double damage = explosionDamage(player, pos.toCenterPos(), predict, 5.0F);
         CombatUtil.modifyPos = null;
         return damage;
      } else {
         return explosionDamage(player, pos.toCenterPos(), predict, 5.0F);
      }
   }

   public static double explosionDamage(PlayerEntity player, Vec3d pos, PlayerEntity predict, float power) {
      if (player != null && player.getAbilities().creativeMode) {
         return 0.0;
      } else {
         if (predict == null) {
            predict = player;
         }

         double modDistance = Math.sqrt(predict.squaredDistanceTo(pos));
         if (modDistance > 10.0) {
            return 0.0;
         } else {
            double exposure = (double)Explosion.getExposure(pos, predict);
            double impact = (1.0 - modDistance / 10.0) * exposure;
            double damage = (impact * impact + impact) / 2.0 * 7.0 * 10.0 + 1.0;
            damage = getDamageForDifficulty(damage);
            damage = resistanceReduction(player, damage);
            damage = (double)DamageUtil.getDamageLeft((float)damage, (float)player.getArmor(), (float)player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
            ((IExplosion)explosion).setWorld(mc.world);
            ((IExplosion)explosion).setX(pos.x);
            ((IExplosion)explosion).setY(pos.y);
            ((IExplosion)explosion).setZ(pos.z);
            ((IExplosion)explosion).setPower(power);
            damage = blastProtReduction(player, damage, explosion);
            if (damage < 0.0) {
               damage = 0.0;
            }

            return damage;
         }
      }
   }

   private static double getDamageForDifficulty(double damage) {
      double var10000;
      switch (mc.world.getDifficulty()) {
         case PEACEFUL -> var10000 = 0.0;
         case EASY -> var10000 = Math.min(damage / 2.0 + 1.0, damage);
         case HARD -> var10000 = damage * 3.0 / 2.0;
         default -> var10000 = damage;
      }

      return var10000;
   }

   private static double blastProtReduction(Entity player, double damage, Explosion explosion) {
      int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), mc.world.getDamageSources().explosion(explosion));
      if (protLevel > 20) {
         protLevel = 20;
      }

      damage *= 1.0 - (double)protLevel / 25.0;
      return damage < 0.0 ? 0.0 : damage;
   }

   private static double resistanceReduction(LivingEntity player, double damage) {
      if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
         int lvl = player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
         damage *= 1.0 - (double)lvl * 0.2;
      }

      return damage < 0.0 ? 0.0 : damage;
   }

   static {
      explosion = new Explosion(mc.world, (Entity)null, 0.0, 0.0, 0.0, 6.0F, false, DestructionType.DESTROY);
   }
}
