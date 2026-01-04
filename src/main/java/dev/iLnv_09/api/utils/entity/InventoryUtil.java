package dev.iLnv_09.api.utils.entity;

import dev.iLnv_09.mod.modules.impl.client.AntiCheat;
import dev.iLnv_09.api.utils.Wrapper;
import dev.iLnv_09.api.utils.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.potion.PotionUtil;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

public class InventoryUtil implements Wrapper {

    static int lastSlot = -1;
    static int lastSelect = -1;

    public static void inventorySwap(int slot, int selectedSlot) {
        if (slot == lastSlot) {
            switchToSlot(lastSelect);
            lastSlot = -1;
            lastSelect = -1;
            return;
        }

        if (slot - 36 == selectedSlot) return;

        if (AntiCheat.INSTANCE.invSwapBypass.getValue()) {
            if (slot - 36 >= 0) {
                lastSlot = slot;
                lastSelect = selectedSlot;
                switchToSlot(slot - 36);
                return;
            }
            mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(slot));
            return;
        }

        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, mc.player);
    }

    public static void switchToSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private static Map<Class, Integer> classCache = new HashMap<>();
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION = 1000;

    public static int findClass(Class clazz) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheTime > CACHE_DURATION) {
            classCache.clear();
            lastCacheTime = currentTime;
        }

        if (classCache.containsKey(clazz)) {
            Integer cached = classCache.get(clazz);
            if (cached != null && cached >= 0) {
                ItemStack stack = getStackInSlot(cached);
                if (isInstanceOf(stack, clazz)) {
                    return cached;
                }
            }
        }

        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (clazz.isInstance(stack.getItem()) ||
                    (stack.getItem() instanceof BlockItem &&
                            clazz.isInstance(((BlockItem) stack.getItem()).getBlock()))) {
                classCache.put(clazz, i);
                return i;
            }
        }

        classCache.put(clazz, -1);
        return -1;
    }

    public static int getItemCount(Item item) {
        int count = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int getPotionCount(StatusEffect targetEffect) {
        int count = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.getItem() == Items.SPLASH_POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(itemStack);
                for (StatusEffectInstance effect : effects) {
                    if (effect.getEffectType() == targetEffect) {
                        count += itemStack.getCount();
                        break;
                    }
                }
            }
        }
        return count;
    }


    public static int findItemInventorySlot(Item item) {
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i + 36;
            }
        }

        for (int i = 9; i < 36; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    public static void quickSwapToItem(Class clazz) {
        int slot = findClass(clazz);
        if (slot != -1) {
            switchToSlot(slot);
        }
    }

    public static void quickSwapToItem(Item item) {
        int slot = findItem(item);
        if (slot != -1) {
            switchToSlot(slot);
        }
    }

    public static boolean holdingItem(Class clazz) {
        ItemStack stack = mc.player.getMainHandStack();
        return isInstanceOf(stack, clazz);
    }

    public static boolean isInstanceOf(ItemStack stack, Class clazz) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (clazz.isInstance(item)) {
            return true;
        }
        if (item instanceof BlockItem) {
            Block block = Block.getBlockFromItem(item);
            return clazz.isInstance(block);
        }
        return false;
    }

    public static ItemStack getStackInSlot(int i) {
        if (i < 0 || i >= 45) return ItemStack.EMPTY;
        return mc.player.getInventory().getStack(i);
    }

    public static int findItem(Item input) {
        for (int i = 0; i < 9; ++i) {
            if (getStackInSlot(i).getItem() == input) {
                return i;
            }
        }
        return -1;
    }


    public static int getItemCount(Class clazz) {
        int count = 0;
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (entry.getValue().getItem() instanceof BlockItem && clazz.isInstance(((BlockItem) entry.getValue().getItem()).getBlock())) {
                count = count + entry.getValue().getCount();
            }
        }
        return count;
    }

    public static int findClassInventorySlot(Class clazz) {
        for (int i = 0; i < 45; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack == ItemStack.EMPTY) continue;
            if (clazz.isInstance(stack.getItem())) {
                return i < 9 ? i + 36 : i;
            }
            if (!(stack.getItem() instanceof BlockItem) || !clazz.isInstance(((BlockItem) stack.getItem()).getBlock()))
                continue;
            return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    public static int findBlock(Block blockIn) {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof BlockItem) || ((BlockItem) stack.getItem()).getBlock() != blockIn)
                continue;
            return i;
        }
        return -1;
    }

    public static int findUnBlock() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem)
                continue;
            return i;
        }
        return -1;
    }

    public static int findBlock() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem && !BlockUtil.shiftBlocks.contains(Block.getBlockFromItem(stack.getItem())) && ((BlockItem) stack.getItem()).getBlock() != Blocks.COBWEB)
                return i;
        }
        return -1;
    }
    public static int findBlockInventorySlot(Block block) {
        return findItemInventorySlot(block.asItem());
    }

    public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
        HashMap<Integer, ItemStack> fullInventorySlots = new HashMap<>();

        for (int current = 0; current <= 44; ++current) {
            fullInventorySlots.put(current, mc.player.getInventory().getStack(current));
        }

        return fullInventorySlots;
    }
    public static int getPotCount(StatusEffect potion) {
        int count = 0;
        Iterator var2 = getInventoryAndHotbarSlots().entrySet().iterator();

        while(true) {
            while(true) {
                Map.Entry entry;
                do {
                    if (!var2.hasNext()) {
                        return count;
                    }

                    entry = (Map.Entry)var2.next();
                } while(!(((ItemStack)entry.getValue()).getItem() instanceof SplashPotionItem));

                List effects = new ArrayList(PotionUtil.getPotionEffects((ItemStack)entry.getValue()));
                Iterator var5 = effects.iterator();

                while(var5.hasNext()) {
                    StatusEffectInstance potionEffect = (StatusEffectInstance)var5.next();
                    if (potionEffect.getEffectType() == potion) {
                        count += ((ItemStack)entry.getValue()).getCount();
                        break;
                    }
                }
            }
        }
    }
    public static int findPotInventorySlot(StatusEffect potion) {
        for(int i = 0; i < 45; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != ItemStack.EMPTY && stack.getItem() instanceof SplashPotionItem) {
                List effects = new ArrayList(PotionUtil.getPotionEffects(stack));
                Iterator var4 = effects.iterator();

                while(var4.hasNext()) {
                    StatusEffectInstance potionEffect = (StatusEffectInstance)var4.next();
                    if (potionEffect.getEffectType() == potion) {
                        return i < 9 ? i + 36 : i;
                    }
                }
            }
        }

        return -1;
    }
    public static int findPot(StatusEffect potion) {
        for(int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack != ItemStack.EMPTY && stack.getItem() instanceof SplashPotionItem) {
                List effects = new ArrayList(PotionUtil.getPotionEffects(stack));
                Iterator var4 = effects.iterator();

                while(var4.hasNext()) {
                    StatusEffectInstance potionEffect = (StatusEffectInstance)var4.next();
                    if (potionEffect.getEffectType() == potion) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }
    public static int getArmorCount(ArmorItem.Type type) {
        int count = 0;

        for (Map.Entry<Integer, ItemStack> integerItemStackEntry : getInventoryAndHotbarSlots().entrySet()) {
            if (((ItemStack) ((Map.Entry) integerItemStackEntry).getValue()).getItem() instanceof ArmorItem && ((ArmorItem) ((ItemStack) ((Map.Entry) integerItemStackEntry).getValue()).getItem()).getType() == type) {
                count += ((ItemStack) ((Map.Entry) integerItemStackEntry).getValue()).getCount();
            }
        }

        return count;
    }
    public static int getClassCount(Class clazz) {
        int count = 0;

        for (Map.Entry<Integer, ItemStack> integerItemStackEntry : getInventoryAndHotbarSlots().entrySet()) {
            if (( integerItemStackEntry).getValue() != ItemStack.EMPTY) {
                if (clazz.isInstance((( integerItemStackEntry).getValue()).getItem())) {
                    count += ( ( integerItemStackEntry).getValue()).getCount();
                }

                if ((( integerItemStackEntry).getValue()).getItem() instanceof BlockItem && clazz.isInstance(((BlockItem) (( integerItemStackEntry).getValue()).getItem()).getBlock())) {
                    count += (( integerItemStackEntry).getValue()).getCount();
                }
            }
        }

        return count;
    }
    public static boolean CheckArmorType(Item item, ArmorItem.Type type) {
        return item instanceof ArmorItem && ((ArmorItem)item).getType() == type;
    }

    public static void doSwap(int slot) {
    }
}

