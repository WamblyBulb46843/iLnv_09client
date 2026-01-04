package dev.iLnv_09.mod.modules.impl.client;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.Render3DEvent;
import dev.iLnv_09.api.events.impl.SendMessageEvent;
import dev.iLnv_09.api.utils.render.Render3DUtil;
import dev.iLnv_09.irc.IRCClient;
import dev.iLnv_09.irc.IRCHandler;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BindSetting;
import dev.iLnv_09.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class IRCModule extends Module implements IRCHandler {

    private final StringSetting host = add(new StringSetting("Host", "0.0.0.0"));
    private final StringSetting port = add(new StringSetting("Port", "11451"));

    private final dev.iLnv_09.mod.modules.settings.impl.BooleanSetting showOwnBeacon = add(new dev.iLnv_09.mod.modules.settings.impl.BooleanSetting("Show Own Beacon", false));
    private final BindSetting posBind = add(new BindSetting("Send Position", -1));

    private IRCClient client;
    private boolean posSent = false;

    private final List<PositionBeacon> beacons = new CopyOnWriteArrayList<>();

    private static class PositionBeacon {
        final String sender;
        final Vec3d position;
        final long createdAt;

        PositionBeacon(String sender, Vec3d position) {
            this.sender = sender;
            this.position = position;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public IRCModule() {
        super("IRC", Category.Client);
        setChinese("网络聊天");
        enable();
    }

    @EventHandler
    public void onChat(SendMessageEvent e) {
        String message = e.message;
        if (message.startsWith(".irc")) {
            e.cancel();
            String content = message.substring(4);
            String senderName = getInGameUsername();
            // 发送包含发送者信息的消息，格式为 "sender:content"
            client.sendMessage(senderName + ":" + content);
            print("§b已发送: " + content);
        }
    }

    @Override
    public void onUpdate() {
        if (posBind.isPressed()) {
            if (!posSent) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                String senderName = getInGameUsername();
                String posMessage = String.format(Locale.US, "pos:%s:%.2f:%.2f:%.2f", senderName, x, y, z);
                client.sendMessage(posMessage);
                print("§b坐标已发送");
                posSent = true;
            }
        } else {
            posSent = false;
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        beacons.removeIf(b -> System.currentTimeMillis() - b.createdAt > 30000);

        for (PositionBeacon beacon : beacons) {
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(beacon.position.x - 0.5, -64, beacon.position.z - 0.5, beacon.position.x + 0.5, 320, beacon.position.z + 0.5);
            Render3DUtil.drawFill(event.getMatrixStack(), box, new Color(0, 255, 255, 100));

            Render3DUtil.drawText3D(beacon.sender, beacon.position.add(0, 2.2, 0), Color.WHITE);
        }
    }

    @Override
    public void onEnable() {
        try {
            client = new IRCClient(host.getValue(), Integer.parseInt(port.getValue()), this);
        } catch (IOException e) {
            print("IRC 连接失败: " + e.getMessage());
            this.disable();
        }
    }

    @Override
    public void onDisable() {
        if (client != null) {
            client.close();
        }
        beacons.clear();
    }

    @Override
    public void onMessage(String message) {
        String[] parts = message.split(":");

        // New format: pos:sender:x:y:z
        if (parts.length == 5 && parts[0].equals("pos")) {
            print("§b已接收: " + message);

            try {
                String sender = parts[1];

                // Don't show beacon for own messages if showOwnBeacon is false
                if (!showOwnBeacon.getValue() && sender.equals(getInGameUsername())) {
                    return;
                }

                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                double z = Double.parseDouble(parts[4]);
                Vec3d position = new Vec3d(x, y, z);

                beacons.removeIf(b -> b.sender.equals(sender));
                beacons.add(new PositionBeacon(sender, position));
                print(String.format("§a坐标光柱已在 [%.1f, %.1f, %.1f] 为 %s 创建", x, y, z, sender));
            } catch (NumberFormatException e) {
                print("§c坐标信息解析失败: " + message);
            }
        } else {
            int idx = message.indexOf(':');
            if (idx > 0) {
                String sender = message.substring(0, idx);
                String content = message.substring(idx + 1);
                String prefix = "";
                if (sender.equals("5mi1e")) {
                    prefix = "§c[Dev] ";
                }
                if (sender.equals("WamblyBulb46843")) {
                    prefix = "§d[Dev] ";
                }
                print(String.format("%s§b<%s>§r %s", prefix, sender, content.trim()));
            } else {
                print("§b已接收: " + message);
            }
        }
    }

    @Override
    public void onDisconnected(String message) {
        print("IRC 已断开: " + message);
        this.disable();
    }

    @Override
    public void onConnected() {
        print("IRC 已连接。");
    }

    @Override
    public String getInGameUsername() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    @Override
    public String getUsername() {
        return getInGameUsername();
    }


    private void print(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[IRC] " + message), false);
        }
    }
}