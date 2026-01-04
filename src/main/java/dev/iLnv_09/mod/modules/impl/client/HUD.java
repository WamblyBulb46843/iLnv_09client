package dev.iLnv_09.mod.modules.impl.client;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.utils.render.TextUtil;
import dev.iLnv_09.mod.gui.font.FontRenderers;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.ColorSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.mod.modules.settings.impl.StringSetting;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.math.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class HUD extends Module {
    public static HUD INSTANCE;

    public final BooleanSetting armor = add(new BooleanSetting("Armor", true));
    public final BooleanSetting up = add(new BooleanSetting("Up", false));
    public final BooleanSetting customFont = add(new BooleanSetting("CustomFont", true));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0)));
    public final ColorSetting pulse = add(new ColorSetting("Pulse", new Color(79, 0, 0)).injectBoolean(true));
    public final BooleanSetting waterMark = add(new BooleanSetting("WaterMark", true).setParent());
    public final StringSetting waterMarkString = add(new StringSetting("Title", "%hackname% %version%",waterMark::isOpen));
    public final SliderSetting offset = add(new SliderSetting("Offset", 1, 0, 100, -1,waterMark::isOpen));
    private final BooleanSetting textRadar=  add((new BooleanSetting("TextRadar", false).setParent()));
    private final SliderSetting yOffset =
            add(new SliderSetting("YOffset", 150, 0, 1000, textRadar::isOpen));
    private final BooleanSetting health=add((new BooleanSetting("Health", false,textRadar::isOpen)));
    public final SliderSetting updateDelay =add(new SliderSetting("UpdateDelay", 5, 0, 1000,textRadar::isOpen));
    private final BooleanSetting textRadarPing = add(new BooleanSetting("Ping", true, textRadar::isOpen));
    private final BooleanSetting textRadarBackground = add(new BooleanSetting("Background", true, textRadar::isOpen));
    private final ColorSetting textRadarBgColor = add(new ColorSetting("BG Color", new Color(0, 0, 0, 80), textRadarBackground::getValue));
    public final BooleanSetting sync = add(new BooleanSetting("InfoColorSync", true));
    public final BooleanSetting lowerCase = add(new BooleanSetting("LowerCase", false));
    public final BooleanSetting fps = add(new BooleanSetting("FPS", true));
    public final BooleanSetting ping = add(new BooleanSetting("Ping", true));
    public final BooleanSetting tps = add(new BooleanSetting("TPS", true));
    public final BooleanSetting ip = add(new BooleanSetting("IP", false));
    public final BooleanSetting time = add(new BooleanSetting("Time", false));
    public final BooleanSetting speed = add(new BooleanSetting("Speed", true));
    public final BooleanSetting brand = add(new BooleanSetting("Brand", false));
    public final BooleanSetting potions = add(new BooleanSetting("Potions", true));
    public final BooleanSetting coords = add(new BooleanSetting("Coords", true));
    private final SliderSetting pulseSpeed = add(new SliderSetting("Speed", 1, 0, 5, 0.1));
    private final SliderSetting pulseCounter = add(new SliderSetting("Counter", 10, 1, 50));
    public Map<String, Integer> players = new HashMap<>();
    private final Timer timer = new Timer();
    public HUD() {
        super("HUD", Category.Client);
        setChinese("界面");
        INSTANCE = this;
    }

    private final DecimalFormat decimal = new DecimalFormat("0.0");
    @Override
    public void onUpdate() {
        if (nullCheck())return;
        if (timer.passed(updateDelay.getValue())) {
            players = getTextRadarMap();
            timer.reset();
        }
    }
    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (armor.getValue()) {
            iLnv_09.GUI.armorHud.draw(drawContext, tickDelta, null);
        }
        if (waterMark.getValue()) {
            if (pulse.booleanValue) {
                TextUtil.drawStringPulse(drawContext, waterMarkString.getValue().replaceAll("%version%", iLnv_09.VERSION).replaceAll("%hackname%", iLnv_09.NAME), offset.getValueInt(), offset.getValueInt(), color.getValue(), pulse.getValue(), pulseSpeed.getValue(), pulseCounter.getValueInt(), customFont.getValue());
            } else {
                TextUtil.drawString(drawContext, waterMarkString.getValue().replaceAll("%version%", iLnv_09.VERSION).replaceAll("%hackname%", iLnv_09.NAME), offset.getValueInt(), offset.getValueInt(), color.getValue().getRGB(), customFont.getValue());
            }
        }
        if(textRadar.getValue()){
            drawTextRadar(drawContext,(int)yOffset.getValue());
        }
        int fontHeight = getHeight();
        int height;
        int y;
        if (up.getValue()) {
            y = 1;
            height = -fontHeight;
        } else {
            y = mc.getWindow().getScaledHeight() - fontHeight;
            if (mc.currentScreen instanceof ChatScreen) {
                y -= 15;
            }
            height = fontHeight;
        }
        int windowWidth = mc.getWindow().getScaledWidth() - 1;
        if (potions.getValue()) {
            List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
            for (StatusEffectInstance potionEffect : effects) {
                StatusEffect potion = potionEffect.getEffectType();
                String power = "";
                switch (potionEffect.getAmplifier()) {
                    case 0 -> power = "I";
                    case 1 -> power = "II";
                    case 2 -> power = "III";
                    case 3 -> power = "IV";
                    case 4 -> power = "V";
                }
                String s = potion.getName().getString() + " " + power;
                String s2 = getDuration(potionEffect);
                String text = s + " " + s2;
                int x = getWidth(text);
                TextUtil.drawString(drawContext, text, windowWidth - x, y, potionEffect.getEffectType().getColor(), customFont.getValue());
                y -= height;
            }
        }
        if (brand.getValue()) {
            String brand = (mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", ""));
            int x = getWidth("ServerBrand " + brand);
            drawText(drawContext, "ServerBrand §f" + brand, windowWidth - x, y);
            y -= height;
        }
        if (time.getValue()) {
            String text = "Time §f" + (new SimpleDateFormat("h:mm a", Locale.ENGLISH)).format(new Date());
            int width = getWidth(text);
            drawText(drawContext, text, windowWidth - width, y);
            y -= height;
        }
        if (ip.getValue()) {
            int x = getWidth("Server " + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address));
            drawText(drawContext, "Server §f" + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address), windowWidth - x, y);
            y -= height;
        }
        if (tps.getValue()) {
            int x = getWidth("TPS " + iLnv_09.SERVER.getTPS() + " [" + iLnv_09.SERVER.getCurrentTPS() + "]");
            drawText(drawContext, "TPS §f" + iLnv_09.SERVER.getTPS() + " §7[§f" + iLnv_09.SERVER.getCurrentTPS() + "§7]", windowWidth - x, y);
            y -= height;
        }
        if (speed.getValue()) {
            double x = mc.player.getX() - mc.player.prevX;
            // double y = mc.player.getY() - mc.player.prevY;
            double z = mc.player.getZ() - mc.player.prevZ;
            double dist = Math.sqrt(x * x + z * z) / 1000.0;
            double div = 0.05 / 3600.0;
            float timer = iLnv_09.TIMER.get();
            final double speed = dist / div * timer;
            String text = String.format("Speed §f%skm/h",
                    decimal.format(speed));
            int width = getWidth(text);
            drawText(drawContext, text, windowWidth - width, y);
            y -= height;
        }
        if (fps.getValue()) {
            int x = getWidth("FPS " + iLnv_09.FPS.getFps());
            drawText(drawContext, "FPS §f" + iLnv_09.FPS.getFps(), windowWidth - x, y);
            y -= height;
        }
        if (ping.getValue()) {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            String ping;
            if (playerListEntry == null) {
                ping = "Unknown";
            } else {
                ping = String.valueOf(playerListEntry.getLatency());
            }
            int x = getWidth("Ping " + ping);
            drawText(drawContext, "Ping §f" + ping, windowWidth - x, y);
            y -= height;
        }

        if (coords.getValue()) {
            boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);

            int posX = mc.player.getBlockX();
            int posY = mc.player.getBlockY();
            int posZ = mc.player.getBlockZ();

            float factor = !inNether ? 0.125F : 8.0F;

            int anotherWorldX = (int) (mc.player.getX() * factor);
            int anotherWorldZ = (int) (mc.player.getZ() * factor);

            String coordsString = "XYZ §f" + (inNether ? (posX + ", " + posY + ", " + posZ + " §7[§f" + anotherWorldX + ", " + anotherWorldZ + "§7]§f") : (posX + ", " + posY + ", " + posZ + "§7 [§f" + anotherWorldX + ", " + anotherWorldZ + "§7]"));

            drawText(drawContext, coordsString, (int) 2.0F, mc.getWindow().getScaledHeight() - fontHeight - (mc.currentScreen instanceof ChatScreen ? 15 : 0));
        }
    }

    private int getWidth(String s) {
        if (customFont.getValue()) {
            return (int) FontRenderers.ui.getWidth(s);
        }
        return mc.textRenderer.getWidth(s);
    }

    private int getHeight() {
        if (customFont.getValue()) {
            return (int) FontRenderers.ui.getFontHeight();
        }
        return mc.textRenderer.fontHeight;
    }

    private void drawText(DrawContext drawContext, String s, int x, int y) {
        if (sync.getValue()) {
            ModuleList.INSTANCE.counter--;
            if (lowerCase.getValue()) {
                s = s.toLowerCase();
            }
            TextUtil.drawString(drawContext, s, x, y, ModuleList.INSTANCE.getColor(ModuleList.INSTANCE.counter), customFont.getValue());
            return;
        }
        if (pulse.booleanValue) {
            TextUtil.drawStringPulse(drawContext, s, x, y, color.getValue(), pulse.getValue(), pulseSpeed.getValue(), pulseCounter.getValueInt(), customFont.getValue());
        } else {
            TextUtil.drawString(drawContext, s, x, y, color.getValue().getRGB(), customFont.getValue());
        }
    }

    public static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) {
            return "*:*";
        } else {
            int var1 = pe.getDuration();
            int mins = var1 / 1200;
            int sec = (var1 % 1200) / 20;

            return mins + ":" + sec;
        }
    }
    private void drawTextRadar(DrawContext drawContext, int yOffset) {
        if (players.isEmpty()) {
            return;
        }

        int x = 2;
        int y = yOffset;
        int textHeight = getHeight() + 1;
        int maxWidth = 0;

        // First, find the widest string to determine background width
        for (String text : players.keySet()) {
            int width = getWidth(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        // Now, draw background and text
        for (String text : players.keySet()) {
            if (textRadarBackground.getValue()) {
                drawContext.fill(x - 1, y, x + maxWidth + 1, y + textHeight, textRadarBgColor.getValue().getRGB());
            }

            drawText(drawContext, text, x, y);
            y += textHeight;
        }
    }
    private Map<String, Integer> getTextRadarMap() {
        Map<String, Integer> retval = new HashMap<>();
        DecimalFormat dfDistance = new DecimalFormat("#.#");
        dfDistance.setRoundingMode(RoundingMode.CEILING);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.equals(mc.player)) {
                continue;
            }

            int distanceInt = (int) mc.player.distanceTo(player);
            StringBuilder distanceSB = new StringBuilder();
            if (distanceInt >= 25) {
                distanceSB.append(Formatting.GREEN);
            } else if (distanceInt > 10) {
                distanceSB.append(Formatting.YELLOW);
            } else {
                distanceSB.append(Formatting.RED);
            }
            distanceSB.append(dfDistance.format(distanceInt));

            StringBuilder playerInfo = new StringBuilder();

            if (health.getValue()) {
                playerInfo.append(getHealthColor(player)).append(round2((player.getAbsorptionAmount() + player.getHealth()))).append(" ");
            }

            playerInfo.append(iLnv_09.FRIEND.isFriend(player) ? Formatting.AQUA : Formatting.RESET).append(player.getName().getString());

            if (textRadarPing.getValue()) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                int ping = entry == null ? -1 : entry.getLatency();
                playerInfo.append(Formatting.WHITE).append(" [").append(ping).append("ms]");
            }

            playerInfo.append(" ").append(Formatting.WHITE).append("[").append(Formatting.RESET).append(distanceSB).append("m").append(Formatting.WHITE).append("]");

            retval.put(playerInfo.toString(), distanceInt);
        }

        if (!retval.isEmpty()) {
            retval = MathUtil.sortByValue(retval, false);
        }

        return retval;
    }
    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
    private Formatting getHealthColor(@NotNull PlayerEntity entity) {
        int health = (int)((float)((int)entity.getHealth()) + entity.getAbsorptionAmount());
        if (health <= 15 && health > 7) {
            return Formatting.YELLOW;
        } else {
            return health > 15 ? Formatting.GREEN : Formatting.RED;
        }
    }
}