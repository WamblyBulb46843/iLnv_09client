package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.ColorSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.awt.*;

public class ExtraTab extends Module {
    public ExtraTab() {
        super("ExtraTab", Category.Misc);
        setChinese("更多信息");
        INSTANCE = this;
    }

    // General Settings
    public final SliderSetting tabSize = add(new SliderSetting("TabSize", 80, 50, 1000));
    public final SliderSetting tabHeight = add(new SliderSetting("TabHeight", 20, 20, 100));

    // Feature Settings
    private final BooleanSetting self = add(new BooleanSetting("Self", true));
    private final ColorSetting selfColor = add(new ColorSetting("SelfColor", new Color(255, 255, 255, 100), self::getValue));
    private final BooleanSetting friends = add(new BooleanSetting("Friends", true));
    private final ColorSetting friendColor = add(new ColorSetting("FriendColor", new Color(0, 255, 0), friends::getValue));
    private final BooleanSetting gamemode = add(new BooleanSetting("GameMode", true));
    private final BooleanSetting pingColor = add(new BooleanSetting("PingColor", true));
    private final BooleanSetting health = add(new BooleanSetting("Health", true));
    public final BooleanSetting accurateLatency = add(new BooleanSetting("AccurateLatency", false));

    public static ExtraTab INSTANCE;

    public Text getPlayerName(PlayerListEntry playerListEntry) {
        Text baseName = playerListEntry.getDisplayName();
        if (baseName == null) {
            baseName = Text.literal(playerListEntry.getProfile().getName());
        }

        TextColor finalColor = null;
        // 首先检查是否是自己（优先于Friend渲染）
        if (playerListEntry.getProfile().getId().equals(mc.player.getGameProfile().getId()) && self.getValue()) {
            finalColor = TextColor.fromRgb(selfColor.getValue().getRGB());
        }
        // 然后检查是否是好友（排除自己）
        else if (friends.getValue() && iLnv_09.FRIEND != null && iLnv_09.FRIEND.isFriend(playerListEntry.getProfile().getName())) {
            finalColor = TextColor.fromRgb(friendColor.getValue().getRGB());
        }
        // 最后使用ping颜色
        else if (pingColor.getValue()) {
            int latency = playerListEntry.getLatency();
            Color c;
            if (latency < 0) c = new Color(170, 170, 170);
            else if (latency < 150) c = new Color(0, 255, 0);
            else if (latency < 300) c = new Color(255, 255, 0);
            else c = new Color(255, 0, 0);
            finalColor = TextColor.fromRgb(c.getRGB());
        }

        String nameString = baseName.getString();
        for (Formatting format : Formatting.values()) {
            if (format.isColor()) {
                nameString = nameString.replace(format.toString(), "");
            }
        }
        MutableText finalDisplayName = Text.literal(nameString);
        if (finalColor != null) {
            finalDisplayName.setStyle(baseName.getStyle().withColor(finalColor));
        } else {
            finalDisplayName.setStyle(baseName.getStyle());
        }

        MutableText suffix = Text.literal("");

        if (gamemode.getValue()) {
            GameMode gm = playerListEntry.getGameMode();
            if (gm != null) {
                switch (gm) {
                    case CREATIVE -> suffix.append(Text.literal(" [C]").formatted(Formatting.AQUA));
                    case SURVIVAL -> suffix.append(Text.literal(" [S]").formatted(Formatting.GOLD));
                    case ADVENTURE -> suffix.append(Text.literal(" [A]").formatted(Formatting.GREEN));
                    case SPECTATOR -> suffix.append(Text.literal(" [SP]").formatted(Formatting.GRAY));
                    default -> {}
                }
            }
        }

        if (health.getValue()) {
            PlayerEntity playerEntity = mc.world.getPlayerByUuid(playerListEntry.getProfile().getId());
            if (playerEntity != null && !playerEntity.getUuid().equals(mc.player.getUuid())) {
                float hp = playerEntity.getHealth() + playerEntity.getAbsorptionAmount();

                Formatting healthFormat;
                if (hp > 18) healthFormat = Formatting.GREEN;
                else if (hp > 10) healthFormat = Formatting.YELLOW;
                else if (hp > 5) healthFormat = Formatting.GOLD;
                else healthFormat = Formatting.RED;

                suffix.append(Text.literal(" " + (int) hp).formatted(healthFormat));
            }
        }

        finalDisplayName.append(suffix);
        return finalDisplayName;
    }
}