package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;

public class AutoDM extends Module {
    private final StringSetting message = add(new StringSetting("Message", ""));
    private final StringSetting id = add(new StringSetting("ID", ""));
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting allowInOverworld = add(new BooleanSetting("Overworld", true));
    private final BooleanSetting allowInNether = add(new BooleanSetting("Nether", false));
    private final BooleanSetting allowInTheEnd = add(new BooleanSetting("The End", false));
    private final SliderSetting randomLength = add(new SliderSetting("Random", 0, 0, 10, 1));
    private final SliderSetting delay = add(new SliderSetting("Delay", 30, 0, 500, 1));
    private Timer timer = new Timer();
    private boolean hasSent = false;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private Random random = new Random();
    public AutoDM() {
        super("AutoDM", Category.Misc);
        setChinese("自动私聊");
    }
    @Override
    public void onEnable() {
        super.onEnable();
        timer.reset();
        hasSent = false;

        if (mc.player == null || mc.world == null || !isDimensionAllowed()) {
            disable();
            return;
        }

        String target = id.getValue().trim();
        if (target.isEmpty()) {
            disable();
            return;
        }
    }

    @Override
    public void onUpdate() {
        if (mc.world == null || !isDimensionAllowed()) {
            disable();
            return;
        }
        if (hasSent && autoDisable.getValue()) {
            disable();
            return;
        }

        if (!timer.passedMs(delay.getValue())) return;

        String target = id.getValue().trim();
        if (target.isEmpty()) {
            disable();
            return;
        }
        String msg = message.getValue();

        if (randomLength.getValue() > 0) {
            String randomStr = generateRandomString(randomLength.getValueInt());
            msg += " " + randomStr;
        }

        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendChatCommand("tell " + target + " " + msg);
            hasSent = true;

            if (autoDisable.getValue()) {
                disable();
                return;
            }
        }

        timer.reset();
    }
    @Override
    public void onDisable() {
        super.onDisable();
        hasSent = false;
    }

    private boolean isDimensionAllowed() {
        if (mc.world == null) return false;

        boolean overworld = allowInOverworld.getValue();
        boolean nether = allowInNether.getValue();
        boolean end = allowInTheEnd.getValue();

        if (!overworld && !nether && !end) {
            return true;
        }

        if (overworld && mc.world.getRegistryKey().equals(World.OVERWORLD)) return true;
        if (nether && mc.world.getRegistryKey().equals(World.NETHER)) return true;
        if (end && mc.world.getRegistryKey().equals(World.END)) return true;

        return false;
    }

    private String generateRandomString(int length) {
        if (length <= 0) return "";

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}