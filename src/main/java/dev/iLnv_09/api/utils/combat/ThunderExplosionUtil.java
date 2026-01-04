package dev.iLnv_09.api.utils.combat;

import java.util.Objects;
import dev.iLnv_09.api.utils.Wrapper;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.asm.accessors.IExplosion;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
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

public class ThunderExplosionUtil implements Wrapper {
   public static final Explosion explosion;

   public static float anchorDamage(BlockPos pos, PlayerEntity target, PlayerEntity predict) {
      if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
         CombatUtil.modifyPos = pos;
         CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
         float damage = calculateDamage(pos.toCenterPos(), target, predict, 5.0F);
         CombatUtil.modifyPos = null;
         return damage;
      } else {
         return calculateDamage(pos.toCenterPos(), target, predict, 5.0F);
      }
   }

   public static float calculateDamage(BlockPos pos, PlayerEntity target, PlayerEntity predict, float power) {
      return calculateDamage(pos.toCenterPos().add(0.0, -0.5, 0.0), target, predict, power);
   }

   public static float calculateDamage(Vec3d explosionPos, PlayerEntity target, PlayerEntity predict, float power) {
      if (mc.world.getDifficulty() == Difficulty.PEACEFUL) {
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
      explosion = new Explosion(mc.world, (Entity)null, 0.0, 0.0, 0.0, 6.0F, false, DestructionType.DESTROY);
   }
}
