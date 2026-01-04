package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.events.impl.DeathEvent;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.UpdateWalkingPlayerEvent;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.EnumSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KillEffects extends Module {
    public static KillEffects INSTANCE;
    public final SliderSetting gethealth = this.add(new SliderSetting("Health", 0, 0, 36));
    public final SliderSetting staytime = this.add(new SliderSetting("Time", 0.0, 0.0, 5000.0, 1.0));

    private final BooleanSetting remove = this.add(new BooleanSetting("Remove", true));
    private final BooleanSetting lowHealthAlert = this.add(new BooleanSetting("LowHealthAlert", true));
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Lightning));
    private final Map<PlayerEntity, Long> lowHealthPlayers = new ConcurrentHashMap<>();
    private final List<LightningEffect> activeLightningEffects = new ArrayList<>();

    public KillEffects() {
        super("KillEffects", Module.Category.Misc);
        setChinese("杀死效果");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lowHealthPlayers.clear();
        activeLightningEffects.clear();
    }

    @EventHandler
    public void onPlayerDeath(DeathEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player == mc.player) {
            return;
        }
        doKillEffect(player);
    }

    @EventHandler
    public void onUpdate(UpdateWalkingPlayerEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!activeLightningEffects.isEmpty()) {
            Iterator<LightningEffect> iterator = activeLightningEffects.iterator();
            while (iterator.hasNext()) {
                LightningEffect effect = iterator.next();
                if (effect.isExpired()) {
                    iterator.remove();
                } else {
                    if (effect.shouldSpawn()) {
                        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
                        lightning.setPos(effect.getPosition().getX(), effect.getPosition().getY(), effect.getPosition().getZ());
                        mc.world.addEntity(lightning);
                        mc.world.playSound(mc.player, effect.getPosition().getX(), effect.getPosition().getY(), effect.getPosition().getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 1.0F, 1.0F);
                        effect.onStrike();
                    }
                }
            }
        }

        if (lowHealthAlert.getValue()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || player.isRemoved()) {
                    continue;
                }

                boolean isLowHealth = player.getHealth() <= this.gethealth.getValueFloat() && player.getHealth() > 0;
                boolean isTracked = this.lowHealthPlayers.containsKey(player);

                if (isLowHealth && !isTracked) {
                    doEffect(player);
                    this.lowHealthPlayers.put(player, System.currentTimeMillis());
                } else if (!isLowHealth && isTracked) {
                    this.lowHealthPlayers.remove(player);
                }
            }
        }

        if (!this.lowHealthPlayers.isEmpty()) {
            this.lowHealthPlayers.entrySet().removeIf(entry -> entry.getKey().isRemoved() || !lowHealthAlert.getValue());
        }
    }

    private void doKillEffect(PlayerEntity player) {
        if (mode.getValue() == Mode.Lightning || mode.getValue() == Mode.Both) {
            activeLightningEffects.add(new LightningEffect(player));
            mc.world.playSound(mc.player, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0F, 1.0F);
        }
        if (mode.getValue() == Mode.Particle || mode.getValue() == Mode.Both) {
            for (int i = 0; i < 360; i += 5) {
                double x = player.getX() + Math.sin(i) * 2;
                double y = player.getY() + 1;
                double z = player.getZ() + Math.cos(i) * 2;
                mc.world.addParticle(net.minecraft.particle.ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
            }
        }
    }

    private void doEffect(PlayerEntity player) {
        if (mode.getValue() == Mode.Lightning || mode.getValue() == Mode.Both) {
            activeLightningEffects.add(new LightningEffect(player));
            mc.world.playSound(mc.player, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0F, 1.0F);
        }
        if (mode.getValue() == Mode.Particle || mode.getValue() == Mode.Both) {
            for (int i = 0; i < 15; i++) {
                mc.world.addParticle(net.minecraft.particle.ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), (Math.random() - 0.5) * 2, Math.random(), (Math.random() - 0.5) * 2);
            }
        }
    }

    private static class LightningEffect {
        private final Vec3d position;
        private int strikesRemaining;
        private long lastSpawnTime;

        public LightningEffect(PlayerEntity player) {
            this.position = player.getPos();
            this.strikesRemaining = 5;
            this.lastSpawnTime = 0;
        }

        public Vec3d getPosition() {
            return position;
        }

        public boolean isExpired() {
            return strikesRemaining <= 0;
        }

        public void onStrike() {
            strikesRemaining--;
        }

        public boolean shouldSpawn() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSpawnTime > 200) {
                lastSpawnTime = currentTime;
                return true;
            }
            return false;
        }
    }

    public enum Mode {
        Lightning,
        Particle,
        Both,
        None
    }
}