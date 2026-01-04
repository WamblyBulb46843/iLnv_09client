package dev.iLnv_09.mod.modules.impl.misc;

import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.impl.ServerConnectBeginEvent;
import dev.iLnv_09.api.utils.entity.InventoryUtil;
import dev.iLnv_09.api.utils.math.Timer;
import dev.iLnv_09.mod.modules.Module;
import dev.iLnv_09.mod.modules.settings.impl.BooleanSetting;
import dev.iLnv_09.mod.modules.settings.impl.SliderSetting;
import dev.iLnv_09.mod.modules.settings.impl.StringSetting;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoReconnect extends Module {
    public final BooleanSetting rejoin = add(new BooleanSetting("Rejoin", true));
    public final SliderSetting delay =
            add(new SliderSetting("Delay", 5, 0, 20,.1).setSuffix("s"));
    public final BooleanSetting autoLogin = add(new BooleanSetting("AutoAuth", true));
    public final SliderSetting afterLoginTime =
            add(new SliderSetting("AfterLoginTime", 3, 0, 10,.1).setSuffix("s"));
    private final StringSetting password = add(new StringSetting("password", "123456"));
    public final BooleanSetting autoQueue = add(new BooleanSetting("AutoQueue", true));
    public final SliderSetting joinQueueDelay =
            add(new SliderSetting("JoinQueueDelay", 3, 0, 10,.1).setSuffix("s"));

    public final BooleanSetting autoJoin = add(new BooleanSetting("AutoJoin", true));
    public final SliderSetting joinDelay = add(new SliderSetting("JoinDelay", 3, 0, 10, .1).setSuffix("s"));
    public final SliderSetting containerClickDelay =
            add(new SliderSetting("ContainerClickDelay", 2, 0, 10, .1).setSuffix("s"));
    private final String[] keywords = {"Game", "戏", "队", "入"};
    private final Timer joinTimer = new Timer();

    public Pair<ServerAddress, ServerInfo> lastServerConnection;

    public static AutoReconnect INSTANCE;
    public AutoReconnect() {
        super("AutoReconnect", Category.Misc);
        setChinese("自动重连");
        INSTANCE = this;
        iLnv_09.EVENT_BUS.subscribe(new StaticListener());
    }
    private final Timer queueTimer = new Timer();
    private final Timer timer = new Timer();
    private final Timer containerTimer = new Timer();
    private boolean login = false;
    @Override
    public void onUpdate() {
        if (login && timer.passedS(afterLoginTime.getValue())) {
            mc.getNetworkHandler().sendChatCommand("login " + password.getValue());
            login = false;
        }
        if (autoQueue.getValue() && InventoryUtil.findItem(Items.COMPASS) != -1 && queueTimer.passedS(joinQueueDelay.getValue())) {
            InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.COMPASS));
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id));
            queueTimer.reset();
        }

        // 处理容器界面中的指南针点击
        if (isInLoginLobby() && autoJoin.getValue() && mc.currentScreen instanceof GenericContainerScreen screen && containerTimer.passedS(containerClickDelay.getValue())) {
            GenericContainerScreen containerScreen = (GenericContainerScreen) mc.currentScreen;
            var handler = containerScreen.getScreenHandler();

            // 查找容器中的指南针
            for (int i = 0; i < handler.slots.size(); i++) {
                var slot = handler.slots.get(i);
                if (slot.hasStack() && slot.getStack().getItem() == Items.COMPASS) {
                    // 点击指南针
                    mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                    containerTimer.reset();
                    break;
                }
            }
        }

        if (autoJoin.getValue() && mc.currentScreen instanceof GenericContainerScreen screen && joinTimer.passedS(joinDelay.getValue())) {
            String title = screen.getTitle().getString();
            boolean titleMatch = false;
            for (String keyword : keywords) {
                if (title.contains(keyword)) {
                    titleMatch = true;
                    break;
                }
            }

            if (titleMatch) {
                for (int i = 0; i < screen.getScreenHandler().getStacks().size(); i++) {
                    ItemStack itemStack = screen.getScreenHandler().getStacks().get(i);
                    if (itemStack.isEmpty()) continue;

                    String itemName = itemStack.getName().getString();
                    boolean itemMatch = false;
                    for (String keyword : keywords) {
                        if (itemName.contains(keyword)) {
                            itemMatch = true;
                            break;
                        }
                    }

                    if (itemMatch) {
                        mc.interactionManager.clickSlot(
                                screen.getScreenHandler().syncId,
                                i,
                                0,
                                SlotActionType.PICKUP,
                                mc.player
                        );
                        joinTimer.reset();
                        break;
                    }
                }
            }
        }
     }

    @Override
    public void onLogin() {
        if (autoLogin.getValue()) {
            login = true;
            timer.reset();
            containerTimer.reset();
        }
    }

    private boolean isInLoginLobby() {
        if (mc.player == null) return false;
        var pos = mc.player.getBlockPos();
        return pos.getX() == 8 && pos.getY() == 5 && pos.getZ() == 8;
    }

    public boolean rejoin() {
        return isOn() && rejoin.getValue();
    }
    private class StaticListener {
        @EventHandler
        private void onGameJoined(ServerConnectBeginEvent event) {
            lastServerConnection = new ObjectObjectImmutablePair<>(event.address, event.info);
        }
    }
}