package dev.iLnv_09.core.impl;

import dev.iLnv_09.api.utils.world.BlockUtil;
import dev.iLnv_09.iLnv_09;
import dev.iLnv_09.api.events.eventbus.EventHandler;
import dev.iLnv_09.api.events.eventbus.EventPriority;
import dev.iLnv_09.api.events.impl.TickEvent;
import dev.iLnv_09.mod.modules.impl.render.PlaceRender;

public class ThreadManager {
    public static ClientService clientService;

    public ThreadManager() {
        iLnv_09.EVENT_BUS.subscribe(this);
        clientService = new ClientService();
        clientService.setName("iLnv_09ClientService");
        clientService.setDaemon(true);
        clientService.start();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEvent(TickEvent event) {
        if (event.isPre()) {
            if (!clientService.isAlive()) {
                clientService = new ClientService();
                clientService.setName("iLnv_09ClientService");
                clientService.setDaemon(true);
                clientService.start();
            }
            BlockUtil.placedPos.forEach(pos -> PlaceRender.renderMap.put(pos, PlaceRender.INSTANCE.create(pos)));
            BlockUtil.placedPos.clear();
            iLnv_09.SERVER.onUpdate();
            iLnv_09.PLAYER.onUpdate();
            iLnv_09.MODULE.onUpdate();
            iLnv_09.GUI.onUpdate();
            iLnv_09.POP.onUpdate();
        }
    }

    public static class ClientService extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (iLnv_09.MODULE != null) {
                        iLnv_09.MODULE.onThread();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
