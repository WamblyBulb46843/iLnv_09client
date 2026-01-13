package dev.iLnv_09.mod.modules.impl.client;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.SendMessageEvent;
import dev.iLnv_09.irc.IRCClient;
import dev.iLnv_09.irc.IRCHandler;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.StringSetting;
import dev.iLnv_09.api.utils.world.IRCUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.io.IOException;

public class IRCModule extends Module implements IRCHandler {

    public IRCClient client;
    
    // 内置IRC服务器配置，不可修改
    private static final String DEFAULT_HOST = "59.110.167.55";
    private static final int DEFAULT_PORT = 11451;

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
        // No position sending logic
    }

    @Override
    public void onEnable() {
        try {
            client = new IRCClient(DEFAULT_HOST, DEFAULT_PORT, this);
            IRCUtil.setIRCModule(this); // 注册到IRCUtil
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
        IRCUtil.setIRCModule(null); // 取消注册
    }

    @Override
    public void onMessage(String message) {
        // 检查是否是Punctuation特殊消息（以[PUNCTUATION]开头）
        if (message.startsWith("[PUNCTUATION]")) {
            String encryptedContent = message.substring("[PUNCTUATION]".length());
            // 使用当前游戏用户名作为发送者
            String currentUsername = getInGameUsername();
            print("§b已接收Punctuation消息");
            
            // 通知其他模块有新的IRC消息，同时传递发送者信息
            notifyModulesOfIRCMessage(encryptedContent, currentUsername);
            return;
        }
        
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
            String formattedMessage = String.format("%s§b<%s>§r %s", prefix, sender, content.trim());
            print(formattedMessage);
            
            // 通知其他模块有新的IRC消息，同时传递发送者信息
            notifyModulesOfIRCMessage(content, sender);
        } else {
            print("§b已接收: " + message);
            // 通知其他模块有新的IRC消息，发送者未知
            notifyModulesOfIRCMessage(message, "Unknown");
        }
    }

    private void notifyModulesOfIRCMessage(String message, String sender) {
        // 调用Punctuation的静态方法处理IRC消息，传入发送者信息
        dev.iLnv_09.mod.modules.impl.misc.Punctuation.handleIRCMessageWithSender(message, sender);
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

    public boolean isConnected() {
        return client != null;
    }

    public void sendIRCMessage(String message) {
        if (client != null) {
            client.sendMessage(message);
        }
    }

    private void print(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[IRC] " + message), false);
        }
    }
}