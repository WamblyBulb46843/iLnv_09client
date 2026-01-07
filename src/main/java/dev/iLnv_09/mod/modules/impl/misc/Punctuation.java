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
    private final ConcurrentHashMap<String, Spot> waypoint = new ConcurrentHashMap();
    private boolean pressed = false;

    public Punctuation() {
        super("Punctuation", Module.Category.Misc);
        this.setChinese("标点");
    }

    public static SecretKeySpec getKey(String myKey) {
        try {
            if (myKey == null || myKey.isEmpty()) {
                System.out.println("Punctuation: 密钥为空");
                return null;
            }
            
            // 检查输入密钥长度
            System.out.println("Punctuation: 原始密钥: " + myKey);
            
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            
            // 检查哈希后的密钥长度
            System.out.println("Punctuation: 哈希后密钥长度: " + key.length);
            
            key = Arrays.copyOf(key, 16); // 取前16字节作为AES密钥
            
            return new SecretKeySpec(key, "AES");
        }
        catch (Exception e) {
            System.out.println("Punctuation: 生成密钥异常: " + e.getMessage());
            e.printStackTrace();
            return null;
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
                Punctuation.mc.player.networkHandler.sendChatMessage(this.Encrypt("EnemyHere{" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + this.color.getValue().getRGB() + "}"));
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
            System.out.println("Punctuation: onRender2D 数组越界异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Punctuation: onRender2D 异常: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("Punctuation: onRender3D 数组越界异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Punctuation: onRender3D 异常: " + e.getMessage());
            e.printStackTrace();
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
                System.err.println("Failed to process chat packet: " + e.getMessage());
            }
        }
    }

    private void receive(String s) {
        try {
            Pattern pattern;
            Matcher matcher;
            if (s == null) {
                return;
            }
            
            System.out.println("Punctuation: 收到消息: " + s);
            
            // 清理消息格式
            String cleanedMessage = s.replaceAll("\u00a7[a-zA-Z0-9]", "").replaceAll("<[^>]*> ", "");
            System.out.println("Punctuation: 清理后的消息: " + cleanedMessage);
            
            // 解密消息
            String decrypt = this.Decrypt(cleanedMessage);
            
            // 添加调试信息
            if (decrypt == null) {
                System.out.println("Punctuation: 解密失败，原始消息: " + s);
                System.out.println("Punctuation: 清理后的消息: " + cleanedMessage);
                return;
            }
            
            System.out.println("Punctuation: 解密成功，解密后消息: " + decrypt);
            
            if (decrypt.contains("EnemyHere") && (matcher = (pattern = Pattern.compile("\\{(.*?)}")).matcher(decrypt)).find()) {
                String pos = matcher.group(1);
                System.out.println("Punctuation: 提取到位置信息: " + pos);
                
                String[] posSplit = pos.split(",");
                System.out.println("Punctuation: 位置信息分割后长度: " + posSplit.length);
                
                // 检查数组长度，避免ArrayIndexOutOfBoundsException
                if (posSplit.length < 3) {
                    System.out.println("Punctuation: 位置信息格式错误，分割后长度不足3: " + pos);
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
                            System.out.println("Punctuation: X坐标不是数字: " + xString);
                            return;
                        }
                        double x = Double.parseDouble(xString);
                        if (!this.isNumeric(yString)) {
                            System.out.println("Punctuation: Y坐标不是数字: " + yString);
                            return;
                        }
                        double y = Double.parseDouble(yString);
                        if (!this.isNumeric(zString)) {
                            System.out.println("Punctuation: Z坐标不是数字: " + zString);
                            return;
                        }
                        double z = Double.parseDouble(zString);
                        if (matcher.find()) {
                            String sender = matcher.group(1);
                            this.waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                            CommandManager.sendChatMessage(sender + " marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        } else {
                            this.waypoint.put("" + MathUtil.random(0.0f, 1.0E9f), new Spot("Unknown", new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                            CommandManager.sendChatMessage("Unknown marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("Punctuation: 处理3坐标位置信息时出错: " + e.getMessage());
                        e.printStackTrace();
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
                            System.out.println("Punctuation: X坐标不是数字: " + xString);
                            return;
                        }
                        double x = Double.parseDouble(xString);
                        if (!this.isNumeric(yString)) {
                            System.out.println("Punctuation: Y坐标不是数字: " + yString);
                            return;
                        }
                        double y = Double.parseDouble(yString);
                        if (!this.isNumeric(zString)) {
                            System.out.println("Punctuation: Z坐标不是数字: " + zString);
                            return;
                        }
                        double z = Double.parseDouble(zString);
                        if (!this.isNumeric(colorString)) {
                            System.out.println("Punctuation: 颜色值不是数字: " + colorString);
                            return;
                        }
                        double color = Double.parseDouble(colorString);
                        if (matcher.find()) {
                            String sender = matcher.group(1);
                            this.waypoint.put(sender, new Spot(sender, new BlockPosX(x, y, z), new Color((int)color, true), new Timer()));
                            CommandManager.sendChatMessage(sender + " marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        } else {
                            this.waypoint.put("" + MathUtil.random(0.0f, 1.0E9f), new Spot("Unknown", new BlockPosX(x, y, z), new Color((int)color, true), new Timer()));
                            CommandManager.sendChatMessage("Unknown marked at \u00a7r(" + xString + ", " + yString + ", " + zString + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("Punctuation: 处理4坐标位置信息时出错: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Punctuation: 位置信息格式错误，分割后长度为: " + posSplit.length + "，内容: " + pos);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Punctuation: 数组越界异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Punctuation: 接收消息时发生异常: " + e.getMessage());
            e.printStackTrace();
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
                System.out.println("Punctuation: 密钥为空");
                return null;
            }
            
            // 获取密钥
            SecretKeySpec secretKey = Punctuation.getKey(keyValue);
            if (secretKey == null) {
                System.out.println("Punctuation: 获取密钥失败");
                return null;
            }
            
            // 初始化AES解密
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16]; // 使用全零IV
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            
            // 解密数据
            System.out.println("Punctuation: 解密前Base64字符串: " + strToDecrypt);
            byte[] decodedBytes = Base64.getDecoder().decode(strToDecrypt);
            System.out.println("Punctuation: Base64解码后字节长度: " + decodedBytes.length);
            
            byte[] original = cipher.doFinal(decodedBytes);
            String decryptedText = new String(original, StandardCharsets.UTF_8);
            System.out.println("Punctuation: 解密后文本: " + decryptedText);
            
            return decryptedText;
        }
        catch (IllegalArgumentException e) {
            System.out.println("Punctuation: Base64解码失败: " + strToDecrypt);
            e.printStackTrace();
            return null;
        }
        catch (Exception e) {
            System.out.println("Punctuation: 解密异常: " + strToDecrypt + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String Encrypt(String strToEncrypt) {
        try {
            // 检查输入是否为空
            if (strToEncrypt == null || strToEncrypt.isEmpty()) {
                System.out.println("Punctuation: 加密输入为空");
                return null;
            }
            
            System.out.println("Punctuation: 加密前文本: " + strToEncrypt);
            
            // 获取密钥
            SecretKeySpec secretKey = Punctuation.getKey(this.key.getValue());
            if (secretKey == null) {
                System.out.println("Punctuation: 获取密钥失败");
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
            
            System.out.println("Punctuation: 加密后Base64字符串: " + encryptedBase64);
            
            return encryptedBase64;
        }
        catch (Exception e) {
            System.out.println("Punctuation: 加密异常: " + strToEncrypt + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }



    private record Spot(String name, BlockPos pos, Color color, Timer timer) {
    }
}