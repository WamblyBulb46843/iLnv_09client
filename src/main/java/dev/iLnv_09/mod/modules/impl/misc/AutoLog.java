package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.SelfHealthUpdateEvent;
import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class AutoLog extends Module {

    public AutoLog() {
        super("AutoLog", Category.Misc);
        setChinese("自动退出");
    }

    public final SliderSetting totemThreshold = add(new SliderSetting("TotemThreshold", 5, 0, 64));
    public final BooleanSetting enableTotemCheck = add(new BooleanSetting("EnableTotemCheck", true));
    public final BooleanSetting checkNearbyPlayers = add(new BooleanSetting("CheckNearbyPlayers", true));
    public final SliderSetting checkRange = add(new SliderSetting("CheckRange", 10, 1, 50, checkNearbyPlayers::getValue));

    @Override
    public void onDisable() {
        // 禁用时逻辑（可选）
    }

    @EventHandler
    void onHealthUpdate(SelfHealthUpdateEvent event) {
        ClientPlayerEntity player = event.player();

        if (enableTotemCheck.getValue() && getTotemCount(player) == totemThreshold.getValueInt()) {
            if (checkNearbyPlayers.getValue()) {
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p != player && player.distanceTo(p) <= checkRange.getValue() && !iLnv_09.FRIEND.isFriend(p)) {
                        disconnect(player);
                        return;
                    }
                }
            } else {
                disconnect(player);
            }
        }
    }

    private void disconnect(ClientPlayerEntity player) {
        player.networkHandler.getConnection().disconnect(
                Text.literal("你携带的图腾数量等于限制，已自动退出游戏。")
        );
        this.disable();
    }

    /**
     * 统计玩家背包中图腾的数量
     * @param player 玩家实体
     * @return 图腾数量
     */
    private int getTotemCount(PlayerEntity player) {
        int count = 0;
        if (isTotem(player.getMainHandStack())) count += player.getMainHandStack().getCount();
        if (isTotem(player.getOffHandStack())) count += player.getOffHandStack().getCount();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isTotem(stack)) count += stack.getCount();
        }
        return count;
    }

    private boolean isTotem(ItemStack stack) {
        return stack.getItem() == Items.TOTEM_OF_UNDYING;
    }
}