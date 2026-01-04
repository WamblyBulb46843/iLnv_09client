package dev.iLnv_09.mod.modules.impl.player;

import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.mod.modules.Module;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;

public class InteractTweaks extends Module {

    public static InteractTweaks INSTANCE;
    public final BooleanSetting noEntityTrace =
            add(new BooleanSetting("NoEntityTrace", true).setParent());
    public final BooleanSetting onlyPickaxe =
            add(new BooleanSetting("OnlyPickaxe", true, noEntityTrace::isOpen));
    public final BooleanSetting multiTask =
            add(new BooleanSetting("MultiTask", true));
    public final BooleanSetting respawn =
            add(new BooleanSetting("Respawn", true));
    private final BooleanSetting noAbort =
            add(new BooleanSetting("NoMineAbort", false));
    private final BooleanSetting noReset =
            add(new BooleanSetting("NoMineReset", false));
    private final BooleanSetting noDelay =
            add(new BooleanSetting("NoMineDelay", false));
    private final BooleanSetting pickaxeSwitch =
            add(new BooleanSetting("SwitchEat", false).setParent());
    private final BooleanSetting allowSword =
            add(new BooleanSetting("allowSword", true, pickaxeSwitch::isOpen));
    private final BooleanSetting switchOnCrystal =
            add(new BooleanSetting("OnCrystal", true, pickaxeSwitch::isOpen));
    private final BooleanSetting switchOnTotem =
            add(new BooleanSetting("OnTotem", true, pickaxeSwitch::isOpen));
    public final BooleanSetting ghostHand =
            add(new BooleanSetting("IgnoreBedrock", false));
    private final BooleanSetting reach =
            add(new BooleanSetting("Reach", false));
    public final SliderSetting distance = add(new SliderSetting("Distance", 5, 0, 15, 0.1, reach::getValue));
    private final SliderSetting delay = add(new SliderSetting("UseDelay", 4, 0, 4, 1));
    public InteractTweaks() {
        super("InteractTweaks", Category.Player);
        setChinese("交互调整");
        INSTANCE = this;
    }

    boolean swapped = false;
    int lastSlot = 0;

    @Override
    public void onUpdate() {
        if (respawn.getValue() && mc.currentScreen instanceof DeathScreen) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
        if (mc.itemUseCooldown <= 4 - delay.getValueInt()) {
            mc.itemUseCooldown = 0;
        }
        if (pickaxeSwitch.getValue()) {
            Item currentItem = mc.player.getMainHandStack().getItem();
            boolean isAllowedTool = currentItem instanceof PickaxeItem
                    || (currentItem instanceof SwordItem && allowSword.getValue())
                    || (currentItem == Items.END_CRYSTAL && switchOnCrystal.getValue())
                    || (currentItem == Items.TOTEM_OF_UNDYING && switchOnTotem.getValue());

            boolean isGapple = currentItem == Items.ENCHANTED_GOLDEN_APPLE || currentItem == Items.GOLDEN_APPLE;

            if (!isAllowedTool && !isGapple) {
                if (swapped) {
                    InventoryUtil.switchToSlot(lastSlot);
                    swapped = false;
                }
                return;
            }

            int gappleSlot = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
            if (gappleSlot == -1) {
                gappleSlot = InventoryUtil.findItem(Items.GOLDEN_APPLE);
            }

            if (gappleSlot == -1) {
                if (swapped) {
                    InventoryUtil.switchToSlot(lastSlot);
                    swapped = false;
                }
                return;
            }

            if (mc.options.useKey.isPressed()) {
                if (isAllowedTool && mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE && mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE) {
                    if (!swapped) {
                        lastSlot = mc.player.getInventory().selectedSlot;
                        swapped = true;
                    }
                    InventoryUtil.switchToSlot(gappleSlot);
                }
            } else if (swapped) {
                InventoryUtil.switchToSlot(lastSlot);
                swapped = false;
            }
        }
    }

    public boolean isActive;

    @Override
    public void onDisable() {
        isActive = false;
    }

    public boolean reach() {
        return isOn() && reach.getValue();
    }
    public boolean noAbort() {
        return isOn() && noAbort.getValue() && !mc.options.useKey.isPressed();
    }

    public boolean noReset() {
        return isOn() && noReset.getValue();
    }
    public boolean noDelay() {
        return isOn() && noDelay.getValue();
    }
    public boolean multiTask() {
        return isOn() && multiTask.getValue();
    }

    public boolean noEntityTrace() {
        if (isOff() || !noEntityTrace.getValue()) return false;

        if (onlyPickaxe.getValue()) {
            return mc.player.getMainHandStack().getItem() instanceof PickaxeItem || mc.player.isUsingItem() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem);
        }
        return true;
    }

    public boolean ghostHand() {
        return isOn() && ghostHand.getValue() && !mc.options.useKey.isPressed() && !mc.options.sneakKey.isPressed();
    }
}