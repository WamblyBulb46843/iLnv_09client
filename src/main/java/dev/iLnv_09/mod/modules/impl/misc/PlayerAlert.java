package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class PlayerAlert extends Module {

    private final SliderSetting range = new SliderSetting("Range", 50, 1, 1000, 1);
    private final BooleanSetting sound = new BooleanSetting("Sound", true);
    private final BooleanSetting onlyHostile = new BooleanSetting("Only Hostile", true);
    private final BooleanSetting showCoords = new BooleanSetting("Show Coords", true);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", true);
    private final BooleanSetting notifyJoinLeave = new BooleanSetting("Notify Join/Leave", true);


    private final Set<UUID> playersInRange = new HashSet<>();
    private Map<UUID, String> lastPlayers = new HashMap<>();
    private int timer = 0;

    public PlayerAlert() {
        super("PlayerAlert", Category.Misc);
        add(range);
        add(sound);
        add(onlyHostile);
        add(showCoords);
        add(showDistance);
        add(notifyJoinLeave);
        setChinese("玩家提醒");
    }

    @Override
    public void onEnable() {
        playersInRange.clear();
        lastPlayers.clear();
        timer = 0;

        if (mc.player != null && mc.player.networkHandler != null) {
            for (PlayerListEntry entry : mc.player.networkHandler.getPlayerList()) {
                lastPlayers.put(entry.getProfile().getId(), entry.getProfile().getName());
            }
        }
    }

    @Override
    public void onDisable() {
        playersInRange.clear();
        lastPlayers.clear();
    }

    @Override
    public void onUpdate() {
        if (mc.world == null || mc.player == null) {
            return;
        }

        // Range check logic
        Set<UUID> currentPlayersInRange = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.equals(mc.player) || player.isCreative() || player.isSpectator()) {
                continue;
            }

            if (onlyHostile.getValue() && iLnv_09.FRIEND.isFriend(player.getName().getString())) {
                continue;
            }

            double distance = mc.player.distanceTo(player);
            if (distance <= range.getValue()) {
                currentPlayersInRange.add(player.getUuid());
                if (!playersInRange.contains(player.getUuid())) {
                    alert(player, distance);
                }
            }
        }

        playersInRange.clear();
        playersInRange.addAll(currentPlayersInRange);


        // Join/Leave notification logic
        timer++;
        if (notifyJoinLeave.getValue() && timer >= 20) {
            timer = 0;

            if (mc.player.networkHandler == null) return;

            Map<UUID, String> currentPlayers = new HashMap<>();
            for (PlayerListEntry entry : mc.player.networkHandler.getPlayerList()) {
                currentPlayers.put(entry.getProfile().getId(), entry.getProfile().getName());
            }

            // Check for new players
            for (Map.Entry<UUID, String> entry : currentPlayers.entrySet()) {
                if (!lastPlayers.containsKey(entry.getKey()) && !entry.getKey().equals(mc.player.getUuid())) {
                    String name = entry.getValue();
                    mc.player.sendMessage(Text.literal("§8[§a+§8] §7" + name), false);
                    if (sound.getValue()) {
                        mc.player.playSound(iLnv_09.CUSTOM_SOUND_EVENT, 1.0F, 1.0F);
                    }
                }
            }

            // Check for players who left
            for (Map.Entry<UUID, String> entry : lastPlayers.entrySet()) {
                if (!currentPlayers.containsKey(entry.getKey()) && !entry.getKey().equals(mc.player.getUuid())) {
                    String name = entry.getValue();
                    mc.player.sendMessage(Text.literal("§8[§c-§8] §7" + name), false);
                    if (sound.getValue()) {
                        mc.player.playSound(iLnv_09.CUSTOM_SOUND_EVENT, 1.0F, 1.0F);
                    }
                }
            }

            lastPlayers = currentPlayers;
        }
    }

    private void alert(PlayerEntity player, double distance) {
        StringBuilder message = new StringBuilder("§b[玩家提醒] §e" + player.getName().getString());

        if (showDistance.getValue()) {
            message.append(String.format(" §7(§a%.1f§7m)", distance));
        }

        if (showCoords.getValue()) {
            message.append(String.format(" §7[§b%.0f, %.0f, %.0f§7]", player.getX(), player.getY(), player.getZ()));
        }

        mc.player.sendMessage(Text.literal(message.toString()), false);

        if (sound.getValue()) {
            mc.player.playSound(iLnv_09.CUSTOM_SOUND_EVENT, 1.0F, 1.0F);
        }
    }
}