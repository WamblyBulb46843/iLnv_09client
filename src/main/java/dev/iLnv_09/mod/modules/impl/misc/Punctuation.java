package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.eventbus.EventListener;
import dev.iLnv_09.api.events.impl.PacketEvent;
import dev.iLnv_09.api.events.impl.UpdateEvent;
import dev.iLnv_09.api.utils.math.MathUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.render.Render3DUtil;
import dev.iLnv_09.api.utils.render.TextUtil;
import dev.iLnv_09.api.utils.world.BlockPosX;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BindSetting;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.ColorSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.mod.modules.settings.impl.StringSetting;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.lang.reflect.Method;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Punctuation
        extends Module {
    private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
    private final SliderSetting clearTime = this.add(new SliderSetting("ClearTime", 10.0, 0.0, 100.0, 0.1).setSuffix("s"));
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
    private final BindSetting enemySpot = this.add(new BindSetting("EnemySpot", -1));
    private final StringSetting key = this.add(new StringSetting("EncryptKey", "IDKWTFTHIS"));
    private final BooleanSetting useIRC = this.add(new BooleanSetting("UseIRC", false));
    private final ConcurrentHashMap<String, Spot> waypoint = new ConcurrentHashMap();
    private boolean pressed = false;

    public Punctuation() {
        super("Punctuation", Module.Category.Misc);
        this.setChinese("标点");
    }

    public static SecretKeySpec getKey(String myKey) {
        try {
            if (myKey == null || myKey.isEmpty()) {
                return null;
            }
            
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            
            key = Arrays.copyOf(key, 16); // 取前16字节作为AES密钥
            
            return new SecretKeySpec(key, "AES");
        }
        catch (Exception e) {
            return null;
        }
    }

    // 静态方法，供IRCModule调用处理IRC消息
    public static void handleIRCMessage(String message) {
        // 通过模块管理器获取Punctuation实例
        Module module = dev.iLnv_09.iLnv_09.MODULE.getModuleByName("Punctuation");
        if (module instanceof Punctuation) {
            ((Punctuation) module).receive(message);
        }
    }
    
    // 静态方法，供IRCModule调用处理带有发送者信息的IRC消息
    public static void handleIRCMessageWithSender(String message, String sender) {
        // 通过模块管理器获取Punctuation实例
        Module module = dev.iLnv_09.iLnv_09.MODULE.getModuleByName("Punctuation");
        if (module instanceof Punctuation) {
            ((Punctuation) module).receiveWithSender(message, sender);
        }
    }

    @Override
    public void onDisable() {
        this.waypoint.clear();
    }

    @EventListener
    public void onUpdate() {
        this.waypoint.values().removeIf(t -> t.timer.passedS(this.clearTime.getValue()));
        if (this.enemySpot.isPressed()) {
            HitResult hitResult;
            if (!this.pressed && (hitResult = mc.player.raycast(256.0, 0.0f, false)) instanceof BlockHitResult) {
                BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                BlockPos pos = blockHitResult.getBlockPos();
                String encryptedMessage = this.Encrypt("EnemyHere{" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + this.color.getValue().getRGB() + "}");
                
                if (this.useIRC.getValue()) {
                    // 通过IRC发送加密消息，格式为 "[PUNCTUATION]encryptedMessage"
                    if (dev.iLnv_09.api.utils.world.IRCUtil.isIRCConnected()) {
                        String prefixedMessage = "[PUNCTUATION]" + encryptedMessage;
                        dev.iLnv_09.api.utils.world.IRCUtil.sendIRCMessage(prefixedMessage);
                        CommandManager.sendChatMessage("[Punctuation] Enemy position sent via IRC");
                    } else {
                        CommandManager.sendChatMessage("[Punctuation] IRC not connected, unable to send message");
                    }
                } else {
                    // 通过游戏聊天发送加密消息
                    Punctuation.mc.player.networkHandler.sendChatMessage(encryptedMessage);
                }
            }
            this.pressed = true;
        } else {
            this.pressed = false;
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        try {
            for (Spot spot : this.waypoint.values()) {
                if (spot == null || spot.pos == null) {
                    continue;
                }
                
                Vec3d vector = TextUtil.worldSpaceToScreenSpace(spot.pos.toCenterPos().add(0.0, 1.0, 0.0));
                if (vector == null) {
                    continue;
                }
                
                String text = "\u00a7a" + spot.name + " \u00a7f(" + spot.pos.getX() + ", " + spot.pos.getY() + ", " + spot.pos.getZ() + ")";
                if (!(vector.z > 0.0) || !(vector.z < 1.0)) continue;
                
                double posX = vector.x;
                double posY = vector.y;
                double endPosX = Math.max(vector.x, vector.z);
                float diff = (float)(endPosX - posX) / 2.0f;
                float textWidth = Punctuation.mc.textRenderer.getWidth(text);
                float tagX = (float)((posX + (double)diff - (double)(textWidth / 4.0f)) * 1.0);
                
                context.getMatrices().push();
                context.getMatrices().scale(0.5f, 0.5f, 1.0f);
                TextRenderer PackResourceMetadata = Punctuation.mc.textRenderer;
                int n = (int)tagX * 2;
                Objects.requireNonNull(Punctuation.mc.textRenderer);
                context.drawText(PackResourceMetadata, text, n, (int)(posY - 11.0 + 9.0 * 1.2) * 2, -1, true);
                context.getMatrices().pop();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // 2D渲染数组越界异常处理
        } catch (Exception e) {
            // 2D渲染异常处理
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        try {
            for (Spot spot : this.waypoint.values()) {
                if (spot == null || spot.pos == null || spot.color == null) {
                    continue;
                }
                Render3DUtil.drawFill(matrixStack, new Box((double)spot.pos.getX() + 0.25, -60.0, (double)spot.pos.getZ() + 0.25, (double)spot.pos.getX() + 0.75, 320.0, (double)spot.pos.getZ() + 0.75), spot.color);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // 3D渲染数组越界异常处理
        } catch (Exception e) {
            // 3D渲染异常处理
        }
    }

    @EventHandler
    private void PacketReceive(PacketEvent.Receive event) {
        if (Punctuation.nullCheck()) {
            return;
        }
        
        Packet<?> packet = event.getPacket();
        
        // 只处理聊天相关的数据包，减少日志输出
        if (packet instanceof GameMessageS2CPacket gamePacket) {
            String message = gamePacket.content().getString();
            if (message != null && !message.isEmpty()) {
                mc.execute(() -> this.receive(message));
            }
        }
        
        if (packet instanceof ChatMessageS2CPacket chatPacket) {
            try {
                // 尝试使用content()方法获取消息内容
                Object messageContent = null;
                
                try {
                    // 首先尝试使用content()方法（适用于新版本）
                    Method contentMethod = chatPacket.getClass().getMethod("content");
                    messageContent = contentMethod.invoke(chatPacket);
                } catch (NoSuchMethodException e) {
                    // 如果content()方法不存在，尝试使用unsignedContent()方法
                    try {
                        Method unsignedContentMethod = chatPacket.getClass().getMethod("unsignedContent");
                        messageContent = unsignedContentMethod.invoke(chatPacket);
                    } catch (NoSuchMethodException ex) {
                        // 如果以上方法都不存在，尝试使用其他可能的方法
                        String[] otherMethods = {"getMessage", "getRawMessage", "getText"};
                        for (String methodName : otherMethods) {
                            try {
                                Method method = chatPacket.getClass().getMethod(methodName);
                                messageContent = method.invoke(chatPacket);
                                break;
                            } catch (Exception ignored) {
                                // 忽略异常，继续尝试下一个方法
                            }
                        }
                    }
                }
                
                // 如果获取到了消息内容，尝试转换为字符串
                if (messageContent != null) {
                    String finalMessage = null;
                    
                    // 如果消息内容本身就是String类型
                    if (messageContent instanceof String) {
                        finalMessage = (String) messageContent;
                    } else {
                        // 否则尝试调用getString()方法获取字符串
                        try {
                            Method getStringMethod = messageContent.getClass().getMethod("getString");
                            finalMessage = (String) getStringMethod.invoke(messageContent);
                        } catch (Exception ignored) {
                            // 如果失败，使用toString()作为最后手段
                            finalMessage = messageContent.toString();
                        }
                    }
                    
                    // 如果成功获取到消息字符串，处理它
                    if (finalMessage != null && !finalMessage.isEmpty()) {
                        String tempMessage = finalMessage; // 创建临时变量确保是实际上的最终变量
                        mc.execute(() -> this.receive(tempMessage));
                    }
                }
            } catch (Exception e) {
                // 处理聊天数据包异常
            }
        }
    }

    private void receive(String s) {
        try {
            this.receiveWithSender(s, "Unknown");
        } catch (Exception e) {
            // 接收消息异常处理
        }
    }
    
    private void receiveWithSender(String s, String senderName) {
        try {
            Pattern pattern;
            Matcher matcher;
            if (s == null) {
                return;
            }
            
            // 清理消息格式
            String cleanedMessage = s.replaceAll("\u00a7[a-zA-Z0-9]", "").replaceAll("<[^>]*> ", "");
            
            // 解密消息
            String decrypt = this.Decrypt(cleanedMessage);
            
            if (decrypt == null) {
                return;
            }
            
            if (decrypt.contains("EnemyHere") && (matcher = (pattern = Pattern.compile("\\{(.*?)}")).matcher(decrypt)).find()) {
                String pos = matcher.group(1);
                
                String[] posSplit = pos.split(",");
                
                // 检查数组长度，避免ArrayIndexOutOfBoundsException
                if (posSplit.length < 3) {
                    return;
                }
                
                if (posSplit.length == 3) {
                    try {
                        if (this.sound.getValue()) {
                            Punctuation.mc.world.playSound((PlayerEntity)Punctuation.mc.player, Punctuation.mc.player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.BLOCKS, 100.0f, 1.9f);
                        }
                        String xString = posSplit[0];
                        String yString = posSplit[1];
                        String zString = posSplit[2];
                        pattern = Pattern.compile("<(.*?)>");
                        matcher = pattern.matcher(s);
                        if (!this.isNumeric(xString)) {
                            return;
                        }
                        double x = Double.parseDouble(xString);
                        if (!this.isNumeric(yString)) {
                            return;
                        }
                        double y = Double.parseDouble(yString);
                        if (!this.isNumeric(zString)) {
                            return;
                        }
                        double z = Double.parseDouble(zString);
                        if (!"Unknown".equals(senderName)) {
                            this.waypoint.put(senderName, new Spot(senderName, new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                            CommandManager.sendChatMessage(senderName + " marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        } else if (matcher.find()) {
                            String sender = matcher.group(1);
                            this.waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                            CommandManager.sendChatMessage(sender + " marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        } else {
                            this.waypoint.put("" + MathUtil.random(0.0f, 1.0E9f), new Spot("Unknown", new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                            CommandManager.sendChatMessage("Unknown marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        }
                    } catch (Exception e) {
                        // 处理异常
                    }
                } else if (posSplit.length == 4) {
                    try {
                        if (this.sound.getValue()) {
                            Punctuation.mc.world.playSound((PlayerEntity)Punctuation.mc.player, Punctuation.mc.player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.BLOCKS, 100.0f, 1.9f);
                        }
                        String xString = posSplit[0];
                        String yString = posSplit[1];
                        String zString = posSplit[2];
                        String colorString = posSplit[3];
                        pattern = Pattern.compile("<(.*?)>");
                        matcher = pattern.matcher(s);
                        if (!this.isNumeric(xString)) {
                            return;
                        }
                        double x = Double.parseDouble(xString);
                        if (!this.isNumeric(yString)) {
                            return;
                        }
                        double y = Double.parseDouble(yString);
                        if (!this.isNumeric(zString)) {
                            return;
                        }
                        double z = Double.parseDouble(zString);
                        if (!this.isNumeric(colorString)) {
                            return;
                        }
                        double color = Double.parseDouble(colorString);
                        if (!"Unknown".equals(senderName)) {
                            this.waypoint.put(senderName, new Spot(senderName, new BlockPosX(x, y, z), new Color((int)color, true), new Timer()));
                            CommandManager.sendChatMessage(senderName + " marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        } else if (matcher.find()) {
                            String sender = matcher.group(1);
                            this.waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), new Color((int)color, true), new Timer()));
                            CommandManager.sendChatMessage(sender + " marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        } else {
                            this.waypoint.put("" + MathUtil.random(0.0f, 1.0E9f), new Spot("Unknown", new BlockPosX(x, y, z), new Color((int)color, true), new Timer()));
                            CommandManager.sendChatMessage("Unknown marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        }
                    } catch (Exception e) {
                        // 处理异常
                    }
                } else {
                    // 位置信息格式错误
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // 数组越界异常处理
        } catch (Exception e) {
            // 接收消息异常处理
        }
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public String Decrypt(String strToDecrypt) {
        try {
            // 检查输入是否为空
            if (strToDecrypt == null || strToDecrypt.isEmpty()) {
                return null;
            }
            
            // 检查密钥是否有效
            String keyValue = this.key.getValue();
            if (keyValue == null || keyValue.isEmpty()) {
                return null;
            }
            
            // 获取密钥
            SecretKeySpec secretKey = Punctuation.getKey(keyValue);
            if (secretKey == null) {
                return null;
            }
            
            // 初始化AES解密
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16]; // 使用全零IV
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            
            // 解密数据
            byte[] decodedBytes = Base64.getDecoder().decode(strToDecrypt);
            byte[] original = cipher.doFinal(decodedBytes);
            String decryptedText = new String(original, StandardCharsets.UTF_8);
            
            return decryptedText;
        }
        catch (IllegalArgumentException e) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    public String Encrypt(String strToEncrypt) {
        try {
            // 检查输入是否为空
            if (strToEncrypt == null || strToEncrypt.isEmpty()) {
                return null;
            }
            
            // 获取密钥
            SecretKeySpec secretKey = Punctuation.getKey(this.key.getValue());
            if (secretKey == null) {
                return null;
            }
            
            // 初始化AES加密
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16]; // 使用全零IV
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
            
            // 加密数据
            byte[] encryptedBytes = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
            
            return encryptedBase64;
        }
        catch (Exception e) {
            return null;
        }
    }



    private record Spot(String name, BlockPos pos, Color color, Timer timer) {
    }
}