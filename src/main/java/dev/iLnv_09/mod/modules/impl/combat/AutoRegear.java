package dev.iLnv_09.mod.modules.impl.combat;

import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.impl.player.PacketMine;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoRegear extends Module {
    // ==================== 通用设置 ====================
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 6.0));
    private final SliderSetting minRange = this.add(new SliderSetting("MinRange", 1.0, 0.0, 3.0));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting place = this.add(new BooleanSetting("Place", true));
    private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));

    // ==================== REARGEAR专用设置 ====================
    private final SliderSetting speed = this.add(new SliderSetting("Delay", 50, 1, 1000));
    private final SliderSetting clicks = this.add(new SliderSetting("Clicks", 1, 1, 36));
    private final BooleanSetting autoSetup = this.add(new BooleanSetting("AutoSetup", true));

    // ==================== REARGEAR新增设置 ====================
    private final BooleanSetting autoOpen = this.add(new BooleanSetting("AutoOpen", true));
    private final SliderSetting maxWaitTime = this.add(new SliderSetting("MaxWaitTime", 3000, 0, 10000));

    // 新增：优化拿取逻辑的设置
    private final BooleanSetting mergeStacks = this.add(new BooleanSetting("MergeStacks", true));
    private final BooleanSetting prioritizeFullStacks = this.add(new BooleanSetting("PrioritizeFull", true));

    // ==================== 内部变量 ====================
    private HashMap<Integer, String> expectedInv = new HashMap<>();
    private final Timer timer = new Timer();
    private final Timer regearTimer = new Timer();
    private BlockPos placePos = null;
    private final Timer disableTimer = new Timer();
    private BlockPos openPos;
    private boolean opend = false;
    private boolean setupDone = false;
    private boolean regearActive = false;
    private long regearDelay = 0;
    private final Timer regearWaitTimer = new Timer();
    private boolean waitingForShulker = false;
    private BlockPos targetShulkerPos = null;
    private boolean needPlaceForRegear = false;
    private boolean regearCompleted = false;
    private final Timer completionTimer = new Timer();
    private final Set<Integer> processedSlots = new HashSet<>();
    private final Timer actionCooldown = new Timer();
    private boolean hasAttemptedPlacement = false;
    private boolean wasInShulkerScreen = false;
    private boolean shouldDisableOnScreenClose = false; // 新增：标记是否应该在界面关闭时禁用
    private int currentActionCount = 0; // 当前已执行的操作数
    private final Timer microDelay = new Timer(); // 微延迟计时器

    // ==================== 预设装备配置 ====================
    private final Map<Integer, String[]> presetKits = new HashMap<Integer, String[]>() {{
        put(1, new String[]{
                "minecraft:netherite_sword", "minecraft:totem_of_undying", "minecraft:totem_of_undying", "minecraft:end_crystal",
                "minecraft:enchanted_golden_apple", "minecraft:enchanted_golden_apple", "minecraft:ender_pearl", "minecraft:ender_pearl",
                "minecraft:ender_chest", "minecraft:obsidian", "minecraft:obsidian", "minecraft:obsidian",
                "minecraft:redstone_block", "minecraft:glowstone", "minecraft:cobweb", "minecraft:cobweb",
                "minecraft:experience_bottle", "minecraft:experience_bottle", "minecraft:experience_bottle", "minecraft:piston",
                "minecraft:piston", "minecraft:chorus_fruit", "minecraft:chorus_fruit", "minecraft:chorus_fruit",
                "minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air",
                "minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air",
                "minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air"
        });
        put(2, new String[]{
                "minecraft:netherite_helmet", "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots",
                "minecraft:elytra", "minecraft:elytra", "minecraft:netherite_pickaxe", "minecraft:netherite_sword",
                "minecraft:totem_of_undying", "minecraft:totem_of_undying", "minecraft:totem_of_undying", "minecraft:totem_of_undying",
                "minecraft:end_crystal", "minecraft:end_crystal", "minecraft:end_crystal", "minecraft:end_crystal",
                "minecraft:enchanted_golden_apple", "minecraft:enchanted_golden_apple", "minecraft:ender_pearl", "minecraft:ender_pearl",
                "minecraft:ender_pearl", "minecraft:ender_pearl", "minecraft:ender_chest", "minecraft:obsidian",
                "minecraft:obsidian", "minecraft:obsidian", "minecraft:obsidian", "minecraft:respawn_anchor",
                "minecraft:experience_bottle", "minecraft:experience_bottle", "minecraft:chorus_fruit", "minecraft:chorus_fruit",
                "minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air"
        });
    }};

    // ==================== 配置文件路径 ====================
    private static final File KITS_FILE = new File(System.getProperty("user.dir") + File.separator + "Luminous" + File.separator + "kits.yml");

    public AutoRegear() {
        super("AutoRegear", "Auto regear from shulker boxes", Category.Combat);
        setChinese("自动装备");
    }

    public int findShulker() {
        AtomicInteger atomicInteger = new AtomicInteger(-1);
        if (this.findClass(ShulkerBoxBlock.class) != -1) {
            atomicInteger.set(this.findClass(ShulkerBoxBlock.class));
        }
        return atomicInteger.get();
    }

    public int findClass(Class clazz) {
        return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
    }

    @Override
    public void onEnable() {
        // 重置所有状态
        this.openPos = null;
        this.disableTimer.reset();
        this.placePos = null;
        this.setupDone = false;
        this.regearActive = false;
        this.regearDelay = (long) speed.getValue();
        this.waitingForShulker = false;
        this.targetShulkerPos = null;
        this.regearWaitTimer.reset();
        this.needPlaceForRegear = false;
        this.regearCompleted = false;
        this.completionTimer.reset();
        this.processedSlots.clear();
        this.actionCooldown.reset();
        this.hasAttemptedPlacement = false;
        this.opend = false;
        this.wasInShulkerScreen = false;
        this.shouldDisableOnScreenClose = false; // 重置禁用标记

        if (nullCheck()) {
            return;
        }

        // 检查是否需要放置潜影盒
        if (this.place.getValue()) {
            // 使用放置逻辑
            handlePlaceLogic();
        } else if (autoOpen.getValue() && !regearCompleted) {
            // 如果不放置，直接寻找已有的潜影盒
            findAndOpenShulkerForRegear();
        }
    }

    private void handlePlaceLogic() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        double distance = 100.0;
        BlockPos bestPos = null;

        Iterator<BlockPos> var5 = BlockUtil.getSphere((float)this.range.getValue()).iterator();

        while(true) {
            BlockPos pos;
            do {
                do {
                    do {
                        do {
                            do {
                                do {
                                    if (!var5.hasNext()) {
                                        if (bestPos != null) {
                                            int slot = this.findShulker();
                                            if (slot == -1) {
                                                return;
                                            }
                                            this.doSwap(slot);
                                            this.placeBlock(bestPos);
                                            this.placePos = bestPos;
                                            if (this.inventory.getValue()) {
                                                this.doSwap(slot);
                                            } else {
                                                this.doSwap(oldSlot);
                                            }
                                            this.timer.reset();

                                            // 标记需要打开这个潜影盒
                                            needPlaceForRegear = true;
                                        } else {
                                            if (autoOpen.getValue() && !regearCompleted) {
                                                // 如果找不到放置位置，寻找已有的潜影盒
                                                findAndOpenShulkerForRegear();
                                            }
                                        }
                                        return;
                                    }
                                    pos = var5.next();
                                } while(!BlockUtil.isAir(pos.up()));
                            } while((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < this.minRange.getValue());
                        } while(!BlockUtil.clientCanPlace(pos, false));
                    } while(!BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP));
                } while(!BlockUtil.canClick(pos.offset(Direction.DOWN)));
                break;
            } while(bestPos != null && !((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance));

            distance = (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
            bestPos = pos;
        }
    }

    private void findAndOpenShulkerForRegear() {
        BlockPos nearestShulker = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockUtil.getSphere((float)range.getValue())) {
            if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                double distance = mc.player.squaredDistanceTo(pos.toCenterPos());
                if (distance >= minRange.getValue() * minRange.getValue() && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestShulker = pos;
                }
            }
        }

        if (nearestShulker != null) {
            targetShulkerPos = nearestShulker;
            if (rotate.getValue()) {
                BlockUtil.clickBlock(nearestShulker, BlockUtil.getClickSide(nearestShulker), true);
            } else {
                BlockUtil.clickBlock(nearestShulker, BlockUtil.getClickSide(nearestShulker), false);
            }
            waitingForShulker = true;
            regearWaitTimer.reset();
        }
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    @Override
    public void onDisable() {
        this.opend = false;
        // 检查是否需要挖矿
        if (this.mine.getValue() && this.placePos != null) {
            PacketMine.INSTANCE.mine(this.placePos);
        }

        this.regearActive = false;
        this.waitingForShulker = false;
        this.targetShulkerPos = null;
        this.needPlaceForRegear = false;
        this.processedSlots.clear();
        this.hasAttemptedPlacement = false;
        this.wasInShulkerScreen = false;
        this.shouldDisableOnScreenClose = false;
    }

    @Override
    public void onUpdate() {
        handleRegearMode();
        checkScreenChange();
    }

    private void checkScreenChange() {
        // 检查屏幕状态变化
        boolean isInShulkerScreen = mc.currentScreen instanceof ShulkerBoxScreen;

        // 如果之前是在潜影盒界面，但现在不是了
        if (wasInShulkerScreen && !isInShulkerScreen) {
            // 界面被关闭，检查是否需要禁用模块
            if (shouldDisableOnScreenClose) {
                this.disable();
                return; // 禁用后直接返回，避免后续处理
            }
        }

        // 更新状态标记
        wasInShulkerScreen = isInShulkerScreen;
    }

    private void handleRegearMode() {
        // 如果刚放置了潜影盒，需要打开它
        if (needPlaceForRegear && placePos != null && mc.world.getBlockState(placePos).getBlock() instanceof ShulkerBoxBlock) {
            if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
                openPos = placePos;
                BlockUtil.clickBlock(placePos, BlockUtil.getClickSide(placePos), rotate.getValue());
                waitingForShulker = true;
                regearWaitTimer.reset();
                needPlaceForRegear = false;
            }
        }

        // 如果在等待打开潜影盒
        if (waitingForShulker && !(mc.currentScreen instanceof ShulkerBoxScreen)) {
            if (regearWaitTimer.passedMs((long)maxWaitTime.getValue())) {
                waitingForShulker = false;
            }
            return;
        }

        // 如果潜影盒已打开
        if (mc.currentScreen instanceof ShulkerBoxScreen) {
            waitingForShulker = false;
            needPlaceForRegear = false;

            // 标记应该在界面关闭时禁用模块
            shouldDisableOnScreenClose = true;

            if (!regearActive) {
                setupRegear();
                regearActive = true;
                regearTimer.reset();
                regearDelay = (long) speed.getValue();
                processedSlots.clear();
            }

            ScreenHandler handler = mc.player.currentScreenHandler;
            if (handler.slots.size() != 63 && handler.slots.size() != 90) {
                return;
            }

            if (expectedInv.isEmpty()) {
                return;
            }

            // 检查延迟是否已过 - 使用更小的延迟
            if (regearTimer.passedMs(regearDelay)) {
                boolean allItemsCorrect = true;
                int actionsTaken = 0;

                // 获取所有需要处理的物品
                List<Map.Entry<Integer, String>> itemsToProcess = new ArrayList<>(expectedInv.entrySet());

                // 按优先级排序：先处理空槽位，再处理物品不正确的槽位
                itemsToProcess.sort((a, b) -> {
                    int slotA = a.getKey();
                    int slotB = b.getKey();
                    ItemStack stackA = getItemInTargetSlot(slotA, handler);
                    ItemStack stackB = getItemInTargetSlot(slotB, handler);

                    boolean isEmptyA = stackA.isEmpty();
                    boolean isEmptyB = stackB.isEmpty();
                    boolean isCorrectA = isItemCorrect(stackA, a.getValue());
                    boolean isCorrectB = isItemCorrect(stackB, b.getValue());

                    if (isEmptyA && !isEmptyB) return -1;
                    if (!isEmptyA && isEmptyB) return 1;
                    if (!isCorrectA && isCorrectB) return -1;
                    if (isCorrectA && !isCorrectB) return 1;
                    return 0;
                });

                for (Map.Entry<Integer, String> entry : itemsToProcess) {
                    if (actionsTaken >= clicks.getValue()) {
                        break;
                    }

                    int slotIndex = entry.getKey();
                    String expectedItem = entry.getValue();

                    // 获取目标槽位
                    int targetSlot = getTargetSlot(slotIndex, handler);
                    if (targetSlot < 0 || targetSlot >= handler.slots.size()) {
                        continue;
                    }

                    ItemStack targetStack = handler.slots.get(targetSlot).getStack();

                    // 检查是否需要拿取
                    if (needToTakeItem(targetStack, expectedItem)) {
                        // 查找潜影盒中是否有这个物品
                        int sourceSlot = findItemInShulker(expectedItem, handler, targetSlot);
                        if (sourceSlot != -1) {
                            // 执行拿取操作（移除了 Thread.sleep）
                            performPickupAction(handler, sourceSlot, targetSlot);
                            actionsTaken++;
                            allItemsCorrect = false;
                        } else {
                            // 如果找不到物品，但这个槽位不是空的，可能需要清空它
                            if (!targetStack.isEmpty() && !isItemCorrect(targetStack, expectedItem)) {
                                // 尝试把错误的物品放回潜影盒
                                int emptySlot = findEmptySlotInShulker(handler);
                                if (emptySlot != -1) {
                                    mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                                    mc.interactionManager.clickSlot(handler.syncId, emptySlot, 0, SlotActionType.PICKUP, mc.player);
                                    actionsTaken++;
                                }
                            }
                        }
                    } else {
                        // 如果物品正确但需要合并堆叠
                        if (mergeStacks.getValue() && targetStack.getCount() < targetStack.getMaxCount()) {
                            int mergeSlot = findMergeableStack(expectedItem, handler, targetSlot);
                            if (mergeSlot != -1) {
                                performMergeAction(handler, mergeSlot, targetSlot);
                                actionsTaken++;
                            }
                        }
                    }
                }

                regearTimer.reset();

                // 检查是否所有物品都正确
                if (allItemsCorrect || checkAllItemsCorrect(handler)) {
                    regearCompleted = true;
                    processedSlots.clear();

                    // 检查是否需要挖矿
                    if (mine.getValue() && placePos != null) {
                        // 执行挖矿
                        PacketMine.INSTANCE.mine(placePos);
                    }

                    // 重置状态以便下一次重新开始
                    resetRegearState();
                }
            }
        } else {
            // 当界面关闭时，重置 regearActive 但保持其他状态
            regearActive = false;
            processedSlots.clear();

            // 注意：移除了自动重新打开的代码，因为现在应该在界面关闭时禁用模块
            // 不再自动重新打开潜影盒
        }
    }

    // ==================== 优化的辅助方法 ====================

    // 获取目标槽位
    private int getTargetSlot(int playerSlot, ScreenHandler handler) {
        if (handler.slots.size() == 63) { // 潜影盒界面
            return playerSlot < 9 ? playerSlot + 54 : playerSlot + 18;
        } else if (handler.slots.size() == 90) { // 潜影盒界面（大）
            return playerSlot < 9 ? playerSlot + 81 : playerSlot + 45;
        }
        return -1;
    }

    // 获取目标槽位的物品
    private ItemStack getItemInTargetSlot(int playerSlot, ScreenHandler handler) {
        int targetSlot = getTargetSlot(playerSlot, handler);
        if (targetSlot >= 0 && targetSlot < handler.slots.size()) {
            return handler.slots.get(targetSlot).getStack();
        }
        return ItemStack.EMPTY;
    }

    // 判断物品是否正确
    private boolean isItemCorrect(ItemStack stack, String expectedItem) {
        if (stack.isEmpty()) {
            return false;
        }

        String itemKey = stack.getItem().getTranslationKey();

        // 对于药水特殊处理
        if (stack.getItem() instanceof PotionItem) {
            String basePotionId = itemKey.replaceAll("\\d+$", "");
            String expectedBase = expectedItem.replaceAll("\\d+$", "");
            return basePotionId.equals(expectedBase);
        }

        return itemKey.equals(expectedItem);
    }

    // 判断是否需要拿取物品
    private boolean needToTakeItem(ItemStack currentStack, String expectedItem) {
        if (currentStack.isEmpty()) {
            return true; // 空槽位，需要拿取
        }

        return !isItemCorrect(currentStack, expectedItem);
    }

    // 在潜影盒中查找物品
    private int findItemInShulker(String itemName, ScreenHandler handler, int excludeSlot) {
        int maxSearch = handler.slots.size() == 63 ? 27 : 54;

        List<Integer> candidateSlots = new ArrayList<>();
        int bestSlot = -1;
        int bestCount = 0;

        for (int i = 0; i < maxSearch; i++) {
            if (i == excludeSlot) continue;

            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) {
                String stackKey = stack.getItem().getTranslationKey();

                // 对于药水，需要特殊处理
                if (stack.getItem() instanceof PotionItem) {
                    String basePotionId = stackKey.replaceAll("\\d+$", "");
                    String expectedBase = itemName.replaceAll("\\d+$", "");
                    if (basePotionId.equals(expectedBase)) {
                        candidateSlots.add(i);
                        if (prioritizeFullStacks.getValue() && stack.getCount() == stack.getMaxCount()) {
                            return i;
                        }
                        if (stack.getCount() > bestCount) {
                            bestCount = stack.getCount();
                            bestSlot = i;
                        }
                    }
                } else if (stackKey.equals(itemName)) {
                    candidateSlots.add(i);
                    if (prioritizeFullStacks.getValue() && stack.getCount() == stack.getMaxCount()) {
                        return i;
                    }
                    if (stack.getCount() > bestCount) {
                        bestCount = stack.getCount();
                        bestSlot = i;
                    }
                }
            }
        }

        return bestSlot;
    }

    // 查找可合并的堆叠
    private int findMergeableStack(String itemName, ScreenHandler handler, int targetSlot) {
        int maxSearch = handler.slots.size() == 63 ? 27 : 54;
        ItemStack targetStack = handler.slots.get(targetSlot).getStack();

        if (targetStack.isEmpty() || targetStack.getCount() >= targetStack.getMaxCount()) {
            return -1;
        }

        for (int i = 0; i < maxSearch; i++) {
            if (i == targetSlot) continue;

            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == targetStack.getItem()) {
                if (stack.getItem() instanceof PotionItem) {
                    String stackKey = stack.getItem().getTranslationKey();
                    String targetKey = targetStack.getItem().getTranslationKey();
                    String baseStackId = stackKey.replaceAll("\\d+$", "");
                    String baseTargetId = targetKey.replaceAll("\\d+$", "");
                    if (baseStackId.equals(baseTargetId)) {
                        return i;
                    }
                } else if (stack.getItem().getTranslationKey().equals(targetStack.getItem().getTranslationKey())) {
                    return i;
                }
            }
        }

        return -1;
    }

    // 查找潜影盒中的空槽位
    private int findEmptySlotInShulker(ScreenHandler handler) {
        int maxSearch = handler.slots.size() == 63 ? 27 : 54;

        for (int i = 0; i < maxSearch; i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (stack.isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    // 执行拾取操作（优化版：移除阻塞延迟）
    private void performPickupAction(ScreenHandler handler, int sourceSlot, int targetSlot) {
        mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);

        ItemStack cursorStack = handler.getCursorStack();
        if (!cursorStack.isEmpty()) {
            mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    // 执行合并操作（优化版：移除阻塞延迟）
    private void performMergeAction(ScreenHandler handler, int sourceSlot, int targetSlot) {
        ItemStack sourceStack = handler.slots.get(sourceSlot).getStack();
        ItemStack targetStack = handler.slots.get(targetSlot).getStack();

        if (sourceStack.isEmpty() || targetStack.isEmpty()) {
            return;
        }

        int spaceLeft = targetStack.getMaxCount() - targetStack.getCount();
        int available = Math.min(spaceLeft, sourceStack.getCount());

        if (available > 0) {
            if (sourceStack.getCount() == available) {
                mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            } else {
                mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);

                ItemStack cursorStack = handler.getCursorStack();
                if (!cursorStack.isEmpty()) {
                    mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
                }
            }
        }
    }

    // 简化物品检查逻辑
    private boolean checkAllItemsCorrect(ScreenHandler handler) {
        for (Map.Entry<Integer, String> entry : expectedInv.entrySet()) {
            int targetSlot = getTargetSlot(entry.getKey(), handler);
            if (targetSlot < 0 || targetSlot >= handler.slots.size()) {
                continue;
            }

            ItemStack itemInSlot = handler.slots.get(targetSlot).getStack();
            String expected = entry.getValue();

            if (itemInSlot.isEmpty()) {
                return false;
            }

            if (!isItemCorrect(itemInSlot, expected)) {
                return false;
            }
        }
        return true;
    }

    private void setupRegear() {
        if (!setupDone && autoSetup.getValue()) {
            try {
                loadCustomKit();
                setupDone = true;
            } catch (Exception e) {
                // 静默处理异常
            }
        }
    }

    private void loadCustomKit() throws IOException {
        if (KITS_FILE.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(KITS_FILE));
            String line;
            String selectedKit = "";
            Map<String, String> kits = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        if (key.equals("pointer")) {
                            selectedKit = value;
                        } else {
                            kits.put(key, value);
                        }
                    }
                }
            }
            reader.close();

            if (!selectedKit.isEmpty() && kits.containsKey(selectedKit)) {
                String kitItems = kits.get(selectedKit);
                String[] items = kitItems.split(" ");
                expectedInv = new HashMap<>();

                for (int i = 0; i < 36 && i < items.length; i++) {
                    if (!items[i].equals("minecraft:air")) {
                        if (items[i].startsWith("item.minecraft.potion") || items[i].startsWith("item.minecraft.splash_potion")) {
                            String basePotionId = items[i].replaceAll("\\d+$", "");
                            expectedInv.put(i, basePotionId);
                        } else {
                            expectedInv.put(i, items[i]);
                        }
                    }
                }
            }
        }
    }

    private void resetRegearState() {
        // 重置状态以便重新开始
        regearActive = false;
        regearCompleted = false;
        processedSlots.clear();
        waitingForShulker = false;
        needPlaceForRegear = false;
        regearTimer.reset();
        regearWaitTimer.reset();
        // 注意：不重置 shouldDisableOnScreenClose，因为界面关闭时应该禁用
    }

    private void placeBlock(BlockPos pos) {
        BlockUtil.clickBlock(pos.offset(Direction.DOWN), Direction.UP, this.rotate.getValue());
    }
}