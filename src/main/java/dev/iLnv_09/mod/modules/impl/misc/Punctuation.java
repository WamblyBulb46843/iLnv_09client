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
import java.awt.Color;
import java.lang.reflect.Method;
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

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;

public class Punctuation
extends Module {
    private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
    private final SliderSetting clearTime = this.add(new SliderSetting("ClearTime", 10.0, 0.0, 100.0, 0.1).setSuffix("s"));
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
    private final BooleanSetting showOwnBeacon = this.add(new BooleanSetting("Show Own Beacon", false));
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
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, "AES");
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onDisable() {
        this.waypoint.clear();
    }

    @Override
    public void onUpdate() {
        this.waypoint.values().removeIf(t -> t.timer.passedS(this.clearTime.getValue()));
        if (this.enemySpot.isPressed()) {
            HitResult hitResult;
            if (!this.pressed && (hitResult = mc.player.raycast(256.0, 0.0f, false)) instanceof BlockHitResult) {
                BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                BlockPos pos = blockHitResult.getBlockPos();
                
                // 发送加密的坐标消息
                Punctuation.mc.player.networkHandler.sendChatMessage(this.Encrypt("EnemyHere{" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + this.color.getValue().getRGB() + "}"));
                
                // 同时在本地直接添加标记，确保自己发送的坐标也能显示光柱
                if (showOwnBeacon.getValue()) {
                    String sender = mc.getSession().getUsername();
                    this.waypoint.put(sender, new Spot(sender, new BlockPosX(pos.getX(), pos.getY(), pos.getZ()), this.color.getValue(), new Timer()));
                    CommandManager.sendChatMessage(sender + " marked at §r(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
                }
            }
            this.pressed = true;
        } else {
            this.pressed = false;
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        for (Spot spot : this.waypoint.values()) {
            Vec3d vector = TextUtil.worldSpaceToScreenSpace(spot.pos.toCenterPos().add(0.0, 1.0, 0.0));
            String text = "§a" + spot.name + " §f(" + spot.pos.getX() + ", " + spot.pos.getY() + ", " + spot.pos.getZ() + ")";
            if (!(vector.z > 0.0) || !(vector.z < 1.0)) continue;
            double posX = vector.x;
            double posY = vector.y;
            double endPosX = Math.max(vector.x, vector.z);
            float diff = (float)(endPosX - posX) / 2.0f;
            float textWidth = Punctuation.mc.textRenderer.getWidth(text);
            float tagX = (float)((posX + (double)diff - (double)(textWidth / 4.0f)) * 1.0);
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 0);
            context.getMatrices().scale(0.5f, 0.5f, 1.0f);
            int n = (int)tagX * 2;
            context.drawTextWithShadow(Punctuation.mc.textRenderer, text, n, (int)(posY - 11.0 + 9.0 * 1.2) * 2, -1);
            context.getMatrices().pop();
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        for (Spot spot : this.waypoint.values()) {
            Render3DUtil.drawFill(matrixStack, new Box((double)spot.pos.getX() + 0.25, -60.0, (double)spot.pos.getZ() + 0.25, (double)spot.pos.getX() + 0.75, 320.0, (double)spot.pos.getZ() + 0.75), spot.color);
        }
    }

    @EventHandler
    private void PacketReceive(PacketEvent.Receive event) {
        if (Punctuation.nullCheck()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        
        // 增加更明显的日志标记
        System.out.println("=== PacketReceive Method Called ===");
        System.out.println("PacketReceive: Received packet of type: " + packet.getClass().getName());
        System.out.println("PacketReceive: Packet toString: " + packet.toString());
        
        // 处理服务器发送的聊天消息
        if (packet instanceof ChatMessageS2CPacket) {
            ChatMessageS2CPacket s2cPacket = (ChatMessageS2CPacket) packet;
            try {
                // 使用反射获取消息内容，增加更多的错误处理
                Object messageObject = null;
                String messageStr = null;
                
                System.out.println("PacketReceive: Processing ChatMessageS2CPacket");
                
                // 尝试使用不同的方式获取消息内容
                try {
                    // 直接使用getClass获取所有方法，确保我们能看到所有可用方法
                    Class<?> packetClass = s2cPacket.getClass();
                    Method[] allMethods = packetClass.getDeclaredMethods();
                    System.out.println("PacketReceive: Declared methods:");
                    for (Method method : allMethods) {
                        if (method.getParameterCount() == 0) {
                            System.out.println("  - " + method.getName() + "()");
                        }
                    }
                    
                    // 尝试获取public方法
                    allMethods = packetClass.getMethods();
                    System.out.println("PacketReceive: Public methods:");
                    for (Method method : allMethods) {
                        if (method.getParameterCount() == 0 && !method.getName().startsWith("getClass")) {
                            System.out.println("  - " + method.getName() + "()");
                        }
                    }
                    
                    // 尝试不同的方法名获取消息内容
                    String[] contentMethods = {"content", "getContent", "getChatMessage", "getMessage", "getRawMessage", "getText"};
                    for (String methodName : contentMethods) {
                        try {
                            Method method = packetClass.getMethod(methodName);
                            messageObject = method.invoke(s2cPacket);
                            System.out.println("PacketReceive: Got message using " + methodName + "() method");
                            break;
                        } catch (Exception e) {
                            System.out.println("PacketReceive: Failed to use " + methodName + "() method: " + e.getMessage());
                        }
                    }
                    
                    // 如果还是没有获取到消息内容，尝试直接使用toString()
                    if (messageObject == null) {
                        messageObject = s2cPacket.toString();
                        System.out.println("PacketReceive: Using packet toString() as fallback: " + messageObject);
                    }
                    
                    // 尝试将获取到的对象转换为字符串
                    if (messageObject != null) {
                        System.out.println("PacketReceive: Message object type: " + messageObject.getClass().getName());
                        System.out.println("PacketReceive: Message object toString: " + messageObject.toString());
                        
                        if (messageObject instanceof String) {
                            messageStr = (String) messageObject;
                            System.out.println("PacketReceive: Direct string conversion successful");
                        } else {
                            // 尝试调用不同的方法将消息对象转换为字符串
                            String[] stringMethods = {"getString", "getUnformattedText", "getLiteralText", "getContents", "asString"};
                            for (String methodName : stringMethods) {
                                try {
                                    Method method = messageObject.getClass().getMethod(methodName);
                                    messageStr = (String) method.invoke(messageObject);
                                    System.out.println("PacketReceive: Converted message using " + methodName + "() method");
                                    break;
                                } catch (Exception e) {
                                    System.out.println("PacketReceive: Failed to use " + methodName + "() method: " + e.getMessage());
                                }
                            }
                            
                            // 如果还是无法转换，使用toString()作为最后手段
                            if (messageStr == null) {
                                messageStr = messageObject.toString();
                                System.out.println("PacketReceive: Converted message using toString() as fallback");
                            }
                        }
                    }
                    
                    if (messageStr != null && !messageStr.isEmpty()) {
                        System.out.println("PacketReceive: Final message string: " + messageStr);
                        final String finalMessage = messageStr;
                        System.out.println("PacketReceive: Calling receive method with message...");
                        mc.execute(() -> this.receive(finalMessage));
                    } else {
                        System.err.println("PacketReceive: Message string is null or empty after all attempts");
                        if (messageObject != null) {
                            System.err.println("PacketReceive: Message object: " + messageObject.toString());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("PacketReceive: Error processing chat packet: " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.err.println("Failed to process S2C chat packet: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("PacketReceive: Not a ChatMessageS2CPacket, skipping");
        }
    }

    @EventListener
    private void PacketSend(PacketEvent.Send event) {
        if (Punctuation.nullCheck()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        
        // 不再处理客户端发送的聊天消息
        // 因为自己发送的坐标已经在onUpdate方法中处理了
        // 捕获发送的消息会导致对加密字符串再次解密，造成解密失败
    }

    private void receive(String s) {
        try {
            System.out.println("=== Receive Method Called ===");
            if (s == null || s.isEmpty()) {
                System.out.println("Receive: Message is null or empty");
                return;
            }
            
            System.out.println("Receive: Raw message: " + s);
            
            // 移除颜色代码
            String messageWithoutColors = s.replaceAll("§[a-zA-Z0-9]", "");
            System.out.println("Message without colors: " + messageWithoutColors);
            
            // 提取玩家名称和加密内容 - 改进正则表达式，使其更健壮
            String sender = null;
            String encryptedContent = messageWithoutColors;
            
            // 尝试匹配不同格式的聊天消息
            Pattern pattern = Pattern.compile("<([^>]+)>\s*(.+)");
            Matcher matcher = pattern.matcher(messageWithoutColors);
            if (matcher.find()) {
                sender = matcher.group(1);
                encryptedContent = matcher.group(2).trim();
                System.out.println("Matched <player> message format");
            } else {
                // 尝试其他可能的格式
                pattern = Pattern.compile("([^:]+):\s*(.+)");
                matcher = pattern.matcher(messageWithoutColors);
                if (matcher.find()) {
                    sender = matcher.group(1);
                    encryptedContent = matcher.group(2).trim();
                    System.out.println("Matched player: message format");
                } else {
                    // 尝试匹配可能的其他格式
                        pattern = Pattern.compile("([^ ]+)\s+(.+)");
                        matcher = pattern.matcher(messageWithoutColors);
                        if (matcher.find()) {
                            sender = matcher.group(1);
                            encryptedContent = matcher.group(2).trim();
                            System.out.println("Matched player message format");
                        } else {
                            System.out.println("Could not extract sender from message, using entire message as content");
                        }
                }
            }
            
            System.out.println("Sender: " + sender + ", Encrypted content: " + encryptedContent);
            
            // 只解密加密部分
            String decrypt = this.Decrypt(encryptedContent);
            if (decrypt == null) {
                System.out.println("Decryption failed");
                return;
            }
            
            System.out.println("Decrypted content: " + decrypt);
            
            if (decrypt.contains("EnemyHere")) {
                matcher = Pattern.compile("\\{(.*?)}").matcher(decrypt);
                if (matcher.find()) {
                    String pos = matcher.group(1);
                    String[] posSplit = pos.split(",");
                    
                    if (posSplit.length >= 3) {
                        try {
                            // 解析坐标
                            double x = Double.parseDouble(posSplit[0].trim());
                            double y = Double.parseDouble(posSplit[1].trim());
                            double z = Double.parseDouble(posSplit[2].trim());
                            Color spotColor = this.color.getValue();
                            
                            // 解析颜色（如果提供）
                            if (posSplit.length >= 4) {
                                try {
                                    int colorInt = Integer.parseInt(posSplit[3].trim());
                                    spotColor = new Color(colorInt, true);
                                } catch (NumberFormatException e) {
                                    System.err.println("Failed to parse color: " + e.getMessage());
                                }
                            }
                            
                            // 播放音效
                            if (this.sound.getValue()) {
                                Punctuation.mc.world.playSound(
                                    (PlayerEntity)Punctuation.mc.player, 
                                    Punctuation.mc.player.getBlockPos(), 
                                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 
                                    SoundCategory.MASTER, 
                                    100.0f, 
                                    1.9f
                                );
                            }
                            
                            // 不再过滤自己发送的消息，始终显示所有标记
                            
                            // 创建并添加标记
                            String displayName = sender != null ? sender : "Unknown";
                            Spot newSpot = new Spot(displayName, new BlockPosX(x, y, z), spotColor, new Timer());
                            
                            // 使用唯一键存储标记
                            String key = sender != null ? sender : "unknown_" + System.currentTimeMillis();
                            this.waypoint.put(key, newSpot);
                            
                            // 发送聊天消息通知
                            CommandManager.sendChatMessage(displayName + " marked at §r(" + (int)x + ", " + (int)y + ", " + (int)z + ")");
                            
                        } catch (NumberFormatException e) {
                            System.err.println("Failed to parse coordinates: " + e.getMessage());
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error processing received message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public String Decrypt(String strToDecrypt) {
        try {
            if (strToDecrypt == null || strToDecrypt.isEmpty()) {
                System.err.println("Decrypt: Input string is null or empty");
                return null;
            }
            
            // 检查密钥
            String keyValue = this.key.getValue();
            if (keyValue == null || keyValue.isEmpty()) {
                System.err.println("Decrypt: Key value is null or empty");
                return null;
            }
            
            // 去除密钥中的空格
            keyValue = keyValue.trim();
            System.out.println("Decrypt: Using trimmed key value: " + keyValue);
            
            // 生成密钥
            SecretKeySpec secretKey = Punctuation.getKey(keyValue);
            if (secretKey == null) {
                System.err.println("Decrypt: Failed to generate secret key");
                return null;
            }
            
            System.out.println("Decrypt: Input string: " + strToDecrypt);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16]; // 使用空IV（与加密一致）
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            
            // 尝试解码Base64
            byte[] encryptedBytes;
            try {
                encryptedBytes = Base64.getDecoder().decode(strToDecrypt);
                System.out.println("Decrypt: Base64 decoding successful, length: " + encryptedBytes.length);
            } catch (IllegalArgumentException e) {
                System.err.println("Decrypt: Base64 decoding failed: " + e.getMessage());
                System.err.println("Decrypt: Input string: " + strToDecrypt);
                return null;
            }
            
            // 尝试解密
            byte[] original;
            try {
                original = cipher.doFinal(encryptedBytes);
                System.out.println("Decrypt: AES decryption successful, length: " + original.length);
            } catch (Exception e) {
                System.err.println("Decrypt: AES decryption failed: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
            
            String result = new String(original, StandardCharsets.UTF_8);
            System.out.println("Decrypt: Final result: " + result);
            return result;
        }
        catch (Exception e) {
            System.err.println("Decrypt: Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String Encrypt(String strToEncrypt) {
        try {
            if (strToEncrypt == null || strToEncrypt.isEmpty()) {
                System.err.println("Encrypt: Input string is null or empty");
                return null;
            }
            
            // 检查密钥
            String keyValue = this.key.getValue();
            if (keyValue == null || keyValue.isEmpty()) {
                System.err.println("Encrypt: Key value is null or empty");
                return null;
            }
            
            // 去除密钥中的空格
            keyValue = keyValue.trim();
            System.out.println("Encrypt: Using trimmed key value: " + keyValue);
            
            // 生成密钥
            SecretKeySpec secretKey = Punctuation.getKey(keyValue);
            if (secretKey == null) {
                System.err.println("Encrypt: Failed to generate secret key");
                return null;
            }
            
            System.out.println("Encrypt: Input string: " + strToEncrypt);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16]; // 使用空IV（与解密一致）
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
            
            byte[] encryptedBytes = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            String result = Base64.getEncoder().encodeToString(encryptedBytes);
            
            System.out.println("Encrypt: AES encryption successful");
            System.out.println("Encrypt: Base64 encoded result: " + result);
            
            return result;
        }
        catch (Exception e) {
            System.err.println("Encrypt: Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }



    private record Spot(String name, BlockPosX pos, Color color, Timer timer) {
    }
}