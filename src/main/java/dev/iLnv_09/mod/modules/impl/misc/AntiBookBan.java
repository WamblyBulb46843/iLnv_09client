package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.PacketEvent;
import dev.iLnv_09.mod.modules.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;

public class AntiBookBan extends Module {
    public static AntiBookBan INSTANCE;

    public AntiBookBan() {
        super("AntiBookBan", Category.Misc);
        setChinese("反书封禁");
        INSTANCE = this;
    }

    @EventHandler
    public void onReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            checkAndCancel(packet.getStack(), event);
        }

        if (event.getPacket() instanceof InventoryS2CPacket packet) {
            for (ItemStack itemStack : packet.getContents()) {
                checkAndCancel(itemStack, event);
                if (event.isCancelled()) break;
            }
        }
    }

    private boolean checkAndCancel(ItemStack stack, PacketEvent event) {
        if (stack != null && (stack.getItem() == Items.WRITTEN_BOOK || stack.getItem() == Items.WRITABLE_BOOK)) {
            if (isBookBanned(stack)) {
                event.setCancelled(true);
                return true;
            }
        }
        return false;
    }

    private boolean isBookBanned(ItemStack stack) {
        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                // Check for an excessive number of pages
                if (nbt.contains("pages", 9)) { // 9 is the NBT type for List
                    if (nbt.getList("pages", 8).size() > 50) { // 8 is the NBT type for String
                        return true;
                    }
                }

                // Check for oversized NBT data which can cause crashes
                if (nbt.toString().length() > 32000) {
                    return true;
                }
            }
        }
        return false;
    }
}
