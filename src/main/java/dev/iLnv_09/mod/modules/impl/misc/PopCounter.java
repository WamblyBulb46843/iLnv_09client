package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.mod.modules.impl.client.ClientSetting;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.DeathEvent;
import dev.iLnv_09.api.events.impl.TotemEvent;
import dev.iLnv_09.core.impl.CommandManager;
import dev.iLnv_09.mod.modules.Module;
import net.minecraft.entity.player.PlayerEntity;

public class PopCounter
        extends Module {

    public static PopCounter INSTANCE;
    public final BooleanSetting unPop =
            add(new BooleanSetting("死亡播报", true));
    public PopCounter() {
        super("PopCounter", "计数玩家图腾消耗", Category.Misc);
        setChinese("图腾计数器");
        INSTANCE = this;
    }

    @EventHandler
    public void onPlayerDeath(DeathEvent event) {
        PlayerEntity player = event.getPlayer();
        if (iLnv_09.POP.popContainer.containsKey(player.getName().getString())) {
            int l_Count = iLnv_09.POP.popContainer.get(player.getName().getString());
            if (l_Count == 1) {
                if (player.equals(mc.player)) {
                    sendMessage("§f你§r 在消耗 " + "§f" + l_Count + "§r 个图腾后死亡.", player.getId());
                } else {
                    sendMessage("§f" + player.getName().getString() + "§r 在消耗 " + "§f" + l_Count + "§r 个图腾后死亡.", player.getId());
                }
            } else {
                if (player.equals(mc.player)) {
                    sendMessage("§f你§r 在消耗 " + "§f" + l_Count + "§r 个图腾后死亡.", player.getId());
                } else {
                    sendMessage("§f" + player.getName().getString() + "§r 在消耗 " + "§f" + l_Count + "§r 个图腾后死亡.", player.getId());
                }
            }
        } else if (unPop.getValue()) {
            if (player.equals(mc.player)) {
                sendMessage("§f你§r 死亡了.", player.getId());
            } else {
                sendMessage("§f" + player.getName().getString() + "§r 死亡了.", player.getId());
            }
        }
    }

    @EventHandler
    public void onTotem(TotemEvent event) {
        PlayerEntity player = event.getPlayer();
        int l_Count = 1;
        if (iLnv_09.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = iLnv_09.POP.popContainer.get(player.getName().getString());
        }
        if (l_Count == 1) {
            if (player.equals(mc.player)) {
                sendMessage("§f你§r 消耗了 " + "§f" + l_Count + "§r 个图腾.", player.getId());
            } else {
                sendMessage("§f" + player.getName().getString() + " §r消耗了 " + "§f" + l_Count + "§r 个图腾.", player.getId());
            }
        } else {
            if (player.equals(mc.player)) {
                sendMessage("§f你§r 消耗了 " + "§f" + l_Count + "§r 个图腾.", player.getId());
            } else {
                sendMessage("§f" + player.getName().getString() + " §r已经消耗了 " + "§f" + l_Count + "§r 个图腾.", player.getId());
            }
        }
    }
    
    public void sendMessage(String message, int id) {
        if (!nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                CommandManager.sendChatMessageWidthId("§f[" + "§3" + getName() + "§f] " + message, id);
                return;
            }
            CommandManager.sendChatMessageWidthId(message, id);//"§6[!] " + message, id);
        }
    }
}