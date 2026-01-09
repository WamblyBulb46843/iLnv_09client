package dev.iLnv_09.mod.modules.impl.combat;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.utils.combat.MeteorExplosionUtil;
import dev.iLnv_09.core.impl.BreakManager;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.api.utils.combat.CombatUtil;
import dev.iLnv_09.api.utils.entity.EntityUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.Wrapper;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.player.PacketMine;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BurrowAssist
        extends Module {
    public static BurrowAssist INSTANCE;
    public static Timer delay;
    private final SliderSetting Delay = this.add(new SliderSetting("Delay", 50, 0, 1000));
    public BooleanSetting pause = this.add(new BooleanSetting("PauseEat", true));
    public SliderSetting speed = this.add(new SliderSetting("MaxSpeed", 8, 0, 20));
    public BooleanSetting ccheck = this.add(new BooleanSetting("CheckCrystal", true).setParent());
    private final SliderSetting cRange = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, () -> this.ccheck.isOpen()));
    private final SliderSetting breakMinSelf = this.add(new SliderSetting("BreakSelf", 12.0, 0.0, 36.0, () -> this.ccheck.isOpen()));
    public BooleanSetting mcheck = this.add(new BooleanSetting("CheckMine", true).setParent());
    public BooleanSetting checkPos = this.add(new BooleanSetting("CheckPos", true, () -> this.mcheck.isOpen()));
    public BooleanSetting mself = this.add(new BooleanSetting("Self", true, () -> this.mcheck.isOpen()));
    private final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", 4, 0, 10));
    private final BooleanSetting terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true));

    public BurrowAssist() {
        super("BurrowAssist", Module.Category.Combat);
        setChinese("自动卡黑曜石");
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        if (BurrowAssist.nullCheck()) {
            return;
        }
        if (!delay.passed((long)this.Delay.getValue())) {
            return;
        }
        if (this.pause.getValue() && BurrowAssist.mc.player.isUsingItem()) {
            return;
        }
        if (BurrowAssist.mc.options.jumpKey.isPressed()) {
            return;
        }
        if (!BurrowAssist.canbur()) {
            return;
        }
        if (BurrowAssist.mc.player.isOnGround() && iLnv_09.PLAYER.getPlayerSpeed((PlayerEntity)BurrowAssist.mc.player) < (double)this.speed.getValueInt() && (this.ccheck.getValue() && this.mcheck.getValue() ? this.findcrystal() || this.checkmine(this.mself.getValue()) : !(this.ccheck.getValue() && !this.findcrystal() || this.mcheck.getValue() && !this.checkmine(this.mself.getValue())))) {
            if (Burrow.INSTANCE.isOn()) {
                return;
            }
            Burrow.INSTANCE.enable();
            delay.reset();
        }
    }

    public boolean findcrystal() {
        PlayerAndPredict self = new PlayerAndPredict((PlayerEntity)BurrowAssist.mc.player);
        for (Entity crystal : BurrowAssist.mc.world.getEntities()) {
            float selfDamage;
            if (!(crystal instanceof EndCrystalEntity) || EntityUtil.getEyesPos().distanceTo(crystal.getPos()) > this.cRange.getValue() || (double)(selfDamage = this.calculateDamage(crystal.getPos(), self.player, self.predict)) < this.breakMinSelf.getValue()) continue;
            return true;
        }
        return false;
    }

    public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        if (this.terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }
        float damage = 0.0f;
        damage = (float) MeteorExplosionUtil.crystalDamage(player, pos, predict);
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    public boolean checkmine(boolean self) {
        ArrayList<BlockPos> pos = new ArrayList<BlockPos>();
        pos.add(EntityUtil.getPlayerPos(true));

        // Add additional positions only if CheckPos is enabled
        if (this.checkPos.getValue()) {
            pos.add(new BlockPosX(BurrowAssist.mc.player.getX() + 0.4, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() + 0.4));
            pos.add(new BlockPosX(BurrowAssist.mc.player.getX() - 0.4, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() + 0.4));
            pos.add(new BlockPosX(BurrowAssist.mc.player.getX() + 0.4, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() - 0.4));
            pos.add(new BlockPosX(BurrowAssist.mc.player.getX() - 0.4, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() - 0.4));
        }

        for (BreakManager.BreakData breakData : new HashMap<Integer, BreakManager.BreakData>(iLnv_09.BREAK.breakMap).values()) {
            if (breakData == null || breakData.getEntity() == null) continue;
            for (BlockPos pos1 : pos) {
                if (!pos1.equals((Object)breakData.pos) || breakData.getEntity() == BurrowAssist.mc.player) continue;
                return true;
            }
        }
        if (!self) {
            return false;
        }
        for (BlockPos pos1 : pos) {
            if (!pos1.equals((Object)PacketMine.breakPos)) continue;
            return true;
        }
        return false;
    }

    private static boolean canbur() {
        BlockPosX pos1 = new BlockPosX(BurrowAssist.mc.player.getX() + 0.3, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() + 0.3);
        BlockPosX pos2 = new BlockPosX(BurrowAssist.mc.player.getX() - 0.3, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() + 0.3);
        BlockPosX pos3 = new BlockPosX(BurrowAssist.mc.player.getX() + 0.3, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() - 0.3);
        BlockPosX pos4 = new BlockPosX(BurrowAssist.mc.player.getX() - 0.3, BurrowAssist.mc.player.getY() + 0.5, BurrowAssist.mc.player.getZ() - 0.3);
        BlockPos playerPos = EntityUtil.getPlayerPos(true);
        return Burrow.INSTANCE.canPlace(pos1) || Burrow.INSTANCE.canPlace(pos2) || Burrow.INSTANCE.canPlace(pos3) || Burrow.INSTANCE.canPlace(pos4);
    }

    static {
        delay = new Timer();
    }

    public class PlayerAndPredict {
        PlayerEntity player;
        PlayerEntity predict;

        public PlayerAndPredict(PlayerEntity player) {
            this.player = player;
            if (BurrowAssist.this.predictTicks.getValueFloat() > 0.0f) {
                this.predict = new PlayerEntity((World)Wrapper.mc.world, player.getBlockPos(), player.getYaw(), new GameProfile(UUID.fromString("66123666-1234-5432-6666-667563866600"), "PredictEntity339")){

                    public boolean isSpectator() {
                        return false;
                    }

                    public boolean isCreative() {
                        return false;
                    }
                };
                this.predict.setPosition(player.getPos().add(CombatUtil.getMotionVec((Entity)player, BurrowAssist.INSTANCE.predictTicks.getValueInt(), true)));
                this.predict.setHealth(player.getHealth());
                this.predict.prevX = player.prevX;
                this.predict.prevZ = player.prevZ;
                this.predict.prevY = player.prevY;
                this.predict.setOnGround(player.isOnGround());
                this.predict.getInventory().clone(player.getInventory());
                this.predict.setPose(player.getPose());
                for (StatusEffectInstance se : player.getStatusEffects()) {
                    this.predict.addStatusEffect(se);
                }
            } else {
                this.predict = player;
            }
        }
    }
}