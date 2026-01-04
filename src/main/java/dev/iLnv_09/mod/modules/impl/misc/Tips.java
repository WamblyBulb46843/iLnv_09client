package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.mod.modules.impl.combat.AutoCrystal;
import dev.iLnv_09.mod.modules.impl.player.AutoPot;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.DeathEvent;
import dev.iLnv_09.api.events.impl.PacketEvent;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.awt.*;
import java.text.DecimalFormat;

public class Tips extends Module {
    public static Tips INSTANCE;
    public Tips() {
        super("Tips", Category.Misc);
        setChinese("提示");
        INSTANCE = this;
    }
    private final BooleanSetting alphaOS= add(new BooleanSetting("AlphaOS", true));

    public final BooleanSetting deathCoords =
            add(new BooleanSetting("DeathCoords", true));
    public final BooleanSetting serverLag =
            add(new BooleanSetting("ServerLag", true));
    public final BooleanSetting lagBack =
            add(new BooleanSetting("LagBack", true));
    public final BooleanSetting turtle =
            add(new BooleanSetting("Turtle", true).setParent());
    private final SliderSetting yOffset = add(new SliderSetting("YOffset", 0, -200, 200, () -> turtle.isOpen()));
    public final BooleanSetting shulkerViewer =
            add(new BooleanSetting("ShulkerViewer", true));

    int turtles = 0;

    @Override
    public void onUpdate() {
        if (turtle.getValue()) {
            turtles = InventoryUtil.getPotionCount(StatusEffects.RESISTANCE);
        }
    }
    private final Timer lagTimer = new Timer();
    private final Timer lagBackTimer = new Timer();
    @EventHandler
    public void onPacketEvent(PacketEvent.Receive event) {
        lagTimer.reset();
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            lagBackTimer.reset();
        }
    }
    DecimalFormat df = new DecimalFormat("0.0");
    int color = new Color(190, 0, 0).getRGB();
    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (serverLag.getValue() && lagTimer.passedS(1.4)) {
            String text = "服务器无响应 (" + df.format(lagTimer.getPassedTimeMs() / 1000d) + "s)";
            drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 10 + mc.textRenderer.fontHeight, color, true);
        }
        if (lagBack.getValue() && !lagBackTimer.passedS(1.5)) {
            String text = "Lagback (" + df.format((1500 - lagBackTimer.getPassedTimeMs()) / 1000d) + "s)";
            drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 10 + mc.textRenderer.fontHeight * 2, color, true);
        }
        if (turtle.getValue()) {
            String text = "§e" + turtles;
            if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE) && mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() >= 2) {
                text += " §f" + (mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
            }
            drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, mc.getWindow().getScaledHeight() / 2 + mc.textRenderer.fontHeight - yOffset.getValueInt(), -1, true);
        }
        if(alphaOS.getValue())
        {
            String text;
            if((AutoPot.INSTANCE.isOn()) && AutoCrystal.INSTANCE.isOn()){
                text = "[§b§l禁忌的真理，正强行灌入你的脑海§f][§a§l√§f]";
            } else {
                text = "[§7§l你切断了连接，但回响已在灵魂中扎根§f][§c§lX§f]";
            }
            int textWidth = mc.textRenderer.getWidth(text);
            int x = mc.getWindow().getScaledWidth() / 2 - textWidth / 2;
            int y = mc.getWindow().getScaledHeight() / 2 + mc.textRenderer.fontHeight + 33;
            drawContext.drawText(mc.textRenderer, text, x, y, -1, true);
        }
    }

    @EventHandler
    public void onPlayerDeath(DeathEvent event) {
        PlayerEntity player = event.getPlayer();
        if (deathCoords.getValue() && player == mc.player) {
            CommandManager.sendChatMessage("§4你死在 " + (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ());
        }
    }


}