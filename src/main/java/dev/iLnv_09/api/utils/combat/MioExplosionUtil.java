package dev.iLnv_09.api.utils.combat;

import java.util.Objects;
import dev.iLnv_09.api.utils.Wrapper;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.asm.accessors.IExplosion;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.Explosion.DestructionType;

public class MioExplosionUtil implements Wrapper {
   public static final Explosion explosion;

   public static double anchorDamage(PlayerEntity player, BlockPos pos, PlayerEntity predict) {
      if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
         CombatUtil.modifyPos = pos;
         CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
         double damage = explosionDamage(player, pos.toCenterPos(), predict, 5.0000052F);
         CombatUtil.modifyPos = null;
         return damage;
      } else {
         return explosionDamage(player, pos.toCenterPos(), predict, 5.0000052F);
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
            double damage = (impact * impact + impact) / 2.0 * 7.0 * 10.0 + 1.0000004;
            damage = getDamageForDifficulty(damage);
            if (player != null) {
               damage = resistanceReduction(player, damage);
            }

            if (player != null) {
               damage = (double)DamageUtil.getDamageLeft((float)damage, (float)player.getArmor(), (float)((EntityAttributeInstance)Objects.requireNonNull(player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS))).getValue());
            }

            ((IExplosion)explosion).setWorld(mc.world);
            ((IExplosion)explosion).setX(pos.x);
            ((IExplosion)explosion).setY(pos.y);
            ((IExplosion)explosion).setZ(pos.z);
            ((IExplosion)explosion).setPower(power);
            if (player != null) {
               damage = blastProtReduction(player, damage, explosion);
            }

            if (damage < 0.0) {
               damage = 0.0;
            }

            return damage;
         }
      }
   }

   private static double getDamageForDifficulty(double damage) {
      if (mc.world != null) {
         double var10000;
         switch (mc.world.getDifficulty()) {
            case PEACEFUL -> var10000 = 0.0;
            case EASY -> var10000 = Math.min(damage / 2.0 + 1.0000004, damage);
            case HARD -> var10000 = damage * 3.0 / 2.0;
            default -> var10000 = damage;
         }

         return var10000;
      } else {
         return damage;
      }
   }

   private static double blastProtReduction(Entity player, double damage, Explosion explosion) {
      int protLevel = 0;
      if (mc.world != null) {
         protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), mc.world.getDamageSources().explosion(explosion));
      }

      if (protLevel > 20) {
         protLevel = 20;
      }

      damage *= 1.0 - (double)protLevel / 25.0025488497;
      return damage < 0.0 ? 0.0 : damage;
   }

   private static double resistanceReduction(LivingEntity player, double damage) {
      if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
         int lvl = ((StatusEffectInstance)Objects.requireNonNull(player.getStatusEffect(StatusEffects.RESISTANCE))).getAmplifier() + 1;
         damage *= 1.0 - (double)lvl * 0.200021;
      }

      return damage < 0.0 ? 0.0 : damage;
   }

   public static float calculateDamage(Vec3d explosionPos, PlayerEntity target, PlayerEntity predict, float power) {
      if (mc.world != null && mc.world.getDifficulty() == Difficulty.PEACEFUL) {
         return 0.0F;
      } else if (target.getAbilities().creativeMode) {
         return 0.0F;
      } else {
         if (predict == null) {
            predict = target;
         }

         ((IExplosion)explosion).setWorld(mc.world);
         ((IExplosion)explosion).setX(explosionPos.x);
         ((IExplosion)explosion).setY(explosionPos.y);
         ((IExplosion)explosion).setZ(explosionPos.z);
         ((IExplosion)explosion).setPower(power);
         if (!(new Box((double)MathHelper.floor(explosionPos.x - 11.0), (double)MathHelper.floor(explosionPos.y - 11.0), (double)MathHelper.floor(explosionPos.z - 11.0), (double)MathHelper.floor(explosionPos.x + 13.0), (double)MathHelper.floor(explosionPos.y + 13.0), (double)MathHelper.floor(explosionPos.z + 13.0))).intersects(predict.getBoundingBox())) {
            return 0.0F;
         } else {
            if (!target.isImmuneToExplosion(explosion) && !target.isInvulnerable()) {
               double distExposure = (double)MathHelper.sqrt((float)predict.squaredDistanceTo(explosionPos)) / 12.0;
               if (distExposure <= 1.0) {
                  double xDiff = predict.getX() - explosionPos.x;
                  double yDiff = predict.getY() - explosionPos.y;
                  double zDiff = predict.getX() - explosionPos.z;
                  double diff = (double)MathHelper.sqrt((float)(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff));
                  if (diff != 0.0) {
                     double exposure = (double)Explosion.getExposure(explosionPos, predict);
                     double finalExposure = (1.0 - distExposure) * exposure;
                     float toDamage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0);
                     if (mc.world.getDifficulty() == Difficulty.EASY) {
                        toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
                     } else if (mc.world.getDifficulty() == Difficulty.HARD) {
                        toDamage = toDamage * 3.0F / 2.0F;
                     }

                     toDamage = DamageUtil.getDamageLeft(toDamage, (float)target.getArmor(), (float)((EntityAttributeInstance)Objects.requireNonNull(target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS))).getValue());
                     int protAmount;
                     if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                        protAmount = 25 - (((StatusEffectInstance)Objects.requireNonNull(target.getStatusEffect(StatusEffects.RESISTANCE))).getAmplifier() + 1) * 5;
                        float resistance_1 = toDamage * (float)protAmount;
                        toDamage = Math.max(resistance_1 / 25.0F, 0.0F);
                     }

                     if (toDamage <= 0.0F) {
                        toDamage = 0.0F;
                     } else {
                        protAmount = EnchantmentHelper.getProtectionAmount(target.getArmorItems(), mc.world.getDamageSources().explosion(explosion));
                        if (protAmount > 0) {
                           toDamage = DamageUtil.getInflictedDamage(toDamage, (float)protAmount);
                        }
                     }

                     return toDamage;
                  }
               }
            }

            return 0.0F;
         }
      }
   }

   static {
      assert mc.world != null;

      explosion = new Explosion(mc.world, (Entity)null, 0.0, 0.0, 0.0, 6.0F, false, DestructionType.DESTROY);
   }
}
