package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class ChestClearer extends Module {

    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));

    public ChestClearer() {
        super("ChestClearer", Category.Misc);
        setChinese("自动清空");
    }

    @Override
    public void onUpdate() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
            int containerSlots = ((GenericContainerScreenHandler) screen.getScreenHandler()).getRows() * 9;
            for (int i = 0; i < containerSlots; i++) {
                ItemStack itemStack = screen.getScreenHandler().getSlot(i).getStack();
                if (!itemStack.isEmpty()) {
                    mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, i, 0, SlotActionType.THROW, mc.player);
                }
            }
            if (autoDisable.getValue()) {
                disable();
            }
        }
    }
}