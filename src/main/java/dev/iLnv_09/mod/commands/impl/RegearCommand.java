package dev.iLnv_09.mod.commands.impl;

import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.mod.commands.Command;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtil;

import java.io.*;
import java.util.*;

public class RegearCommand extends Command {
    private static final File KITS_FILE = new File(System.getProperty("user.dir") + File.separator + "Luminous" + File.separator + "kits.yml");

    public RegearCommand() {
        super("regear", "Manage your regear kits");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length < 1) {
            sendUsage();
            return;
        }

        switch (parameters[0].toLowerCase()) {
            case "save":
            case "create":
                if (parameters.length == 2) {
                    saveKit(parameters[1]);
                } else {
                    CommandManager.sendChatMessage("§cUsage: .regear save <name>");
                }
                break;

            case "load":
            case "set":
                if (parameters.length == 2) {
                    setKit(parameters[1]);
                } else {
                    CommandManager.sendChatMessage("§cUsage: .regear load <name>");
                }
                break;

            case "delete":
            case "del":
                if (parameters.length == 2) {
                    deleteKit(parameters[1]);
                } else {
                    CommandManager.sendChatMessage("§cUsage: .regear delete <name>");
                }
                break;

            case "list":
                listKits();
                break;

            case "current":
                getCurrentKit();
                break;

            case "clear":
                clearCurrent();
                break;

            case "help":
            default:
                sendUsage();
                break;
        }
    }

    public void sendUsage() {
        CommandManager.sendChatMessage("§6§lRegear Commands:");
        CommandManager.sendChatMessage("§e;regear save <name> §7- Save current inventory as kit");
        CommandManager.sendChatMessage("§e;regear load <name> §7- Load a kit");
        CommandManager.sendChatMessage("§e;regear delete <name> §7- Delete a kit");
        CommandManager.sendChatMessage("§e;regear list §7- List all kits");
        CommandManager.sendChatMessage("§e;regear current §7- Show current kit");
        CommandManager.sendChatMessage("§e;regear clear §7- Clear current kit");
    }

    private void saveKit(String name) {
        if (name.equalsIgnoreCase("pointer")) {
            CommandManager.sendChatMessage("§c'pointer' is a reserved name!");
            return;
        }

        try {
            Map<String, Object> kits = loadKits();

            if (kits.containsKey(name)) {
                CommandManager.sendChatMessage("§cKit '" + name + "' already exists!");
                return;
            }

            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player == null) {
                CommandManager.sendChatMessage("§cNot in game!");
                return;
            }

            StringBuilder inventoryData = new StringBuilder();
            // 保存主物品栏（36格）
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().main.get(i);
                if (stack.isEmpty()) {
                    inventoryData.append("minecraft:air");
                } else {
                    String itemKey = stack.getItem().getTranslationKey();

                    // 对于药水，简化处理：只保存物品类型，不保存颜色
                    if (stack.getItem() instanceof net.minecraft.item.PotionItem) {
                        // 移除potion.前缀，转换为标准格式
                        String baseKey = itemKey.replace("item.minecraft.", "");
                        // 移除颜色信息，只保留基本类型
                        if (baseKey.contains("potion")) {
                            // 提取药水类型
                            if (stack.getItem() instanceof net.minecraft.item.SplashPotionItem) {
                                baseKey = "splash_potion";
                            } else {
                                baseKey = "potion";
                            }
                            inventoryData.append("item.minecraft.").append(baseKey);
                        } else {
                            inventoryData.append(itemKey);
                        }
                    } else {
                        inventoryData.append(itemKey);
                    }
                }

                if (i < 35) {
                    inventoryData.append(" ");
                }
            }

            kits.put(name, inventoryData.toString());
            saveKits(kits);
            CommandManager.sendChatMessage("§aKit '" + name + "' saved successfully!");

        } catch (Exception e) {
            CommandManager.sendChatMessage("§cError saving kit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setKit(String name) {
        try {
            Map<String, Object> kits = loadKits();

            if (!kits.containsKey(name) || name.equalsIgnoreCase("pointer")) {
                CommandManager.sendChatMessage("§cKit '" + name + "' not found!");
                return;
            }

            kits.put("pointer", name);
            saveKits(kits);
            CommandManager.sendChatMessage("§aKit '" + name + "' loaded! Enable ChestStealer in Regear mode to use it.");

        } catch (Exception e) {
            CommandManager.sendChatMessage("§cError loading kit: " + e.getMessage());
        }
    }

    private void deleteKit(String name) {
        try {
            Map<String, Object> kits = loadKits();

            if (!kits.containsKey(name) || name.equalsIgnoreCase("pointer")) {
                CommandManager.sendChatMessage("§cKit '" + name + "' not found!");
                return;
            }

            kits.remove(name);
            // 如果删除的是当前选中的kit，清除指针
            if (kits.containsKey("pointer") && kits.get("pointer").equals(name)) {
                kits.put("pointer", "none");
            }

            saveKits(kits);
            CommandManager.sendChatMessage("§aKit '" + name + "' deleted!");

        } catch (Exception e) {
            CommandManager.sendChatMessage("§cError deleting kit: " + e.getMessage());
        }
    }

    private void listKits() {
        try {
            Map<String, Object> kits = loadKits();

            if (kits.size() <= 1) { // 只有pointer
                CommandManager.sendChatMessage("§cNo kits saved!");
                return;
            }

            CommandManager.sendChatMessage("§6§lSaved Kits:");
            for (String key : kits.keySet()) {
                if (!key.equals("pointer")) {
                    CommandManager.sendChatMessage("§e- " + key);
                }
            }

            String current = (String) kits.getOrDefault("pointer", "none");
            CommandManager.sendChatMessage("§7Current kit: §a" + current);

        } catch (Exception e) {
            CommandManager.sendChatMessage("§cError listing kits: " + e.getMessage());
        }
    }

    private void getCurrentKit() {
        try {
            Map<String, Object> kits = loadKits();
            String current = (String) kits.getOrDefault("pointer", "none");
            CommandManager.sendChatMessage("§7Current kit: §a" + current);

        } catch (Exception e) {
            CommandManager.sendChatMessage("§cError getting current kit: " + e.getMessage());
        }
    }

    private void clearCurrent() {
        try {
            Map<String, Object> kits = loadKits();
            kits.put("pointer", "none");
            saveKits(kits);
            CommandManager.sendChatMessage("§aCurrent kit cleared!");

        } catch (Exception e) {
            CommandManager.sendChatMessage("§cError clearing current kit: " + e.getMessage());
        }
    }

    private Map<String, Object> loadKits() throws IOException {
        Map<String, Object> kits = new HashMap<>();

        if (!KITS_FILE.exists()) {
            // 创建目录和文件
            KITS_FILE.getParentFile().mkdirs();
            KITS_FILE.createNewFile();
            kits.put("pointer", "none");
            saveKits(kits);
            return kits;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(KITS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        kits.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }

        // 确保有pointer
        if (!kits.containsKey("pointer")) {
            kits.put("pointer", "none");
        }

        return kits;
    }

    private void saveKits(Map<String, Object> kits) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(KITS_FILE))) {
            writer.write("# Luminous Regear Kits");
            writer.newLine();
            writer.newLine();

            for (Map.Entry<String, Object> entry : kits.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue());
                writer.newLine();
            }
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        // 添加边界检查：确保列表长度足够
        if (seperated.size() <= count) {
            // 如果列表不够长，返回默认值
            if (count == 1) {
                return new String[]{"save", "load", "delete", "list", "current", "clear", "help"};
            } else if (count == 2) {
                return new String[]{"<kit_name>"};
            }
            return null;
        }

        if (count == 1) {
            return new String[]{"save", "load", "delete", "list", "current", "clear", "help"};
        } else if (count == 2) {
            String action = seperated.get(1).toLowerCase();
            switch (action) {
                case "save":
                    return new String[]{"<kit_name>"};
                case "load":
                case "delete":
                    try {
                        Map<String, Object> kits = loadKits();
                        List<String> kitNames = new ArrayList<>();
                        for (String key : kits.keySet()) {
                            if (!key.equals("pointer")) {
                                kitNames.add(key);
                            }
                        }
                        return kitNames.toArray(new String[0]);
                    } catch (Exception e) {
                        return new String[]{"<kit_name>"};
                    }
                default:
                    return null;
            }
        }
        return null;
    }
}