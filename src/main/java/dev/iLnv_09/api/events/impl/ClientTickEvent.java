package dev.iLnv_09.api.events.impl;

import dev.iLnv_09.api.events.Event;
import dev.iLnv_09.api.events.Event.Stage;

public class ClientTickEvent
extends Event {
    private ClientTickEvent(Stage stage) {
        super(stage);
    }

    public static ClientTickEvent get(Event.Stage stage) {
        return new ClientTickEvent(stage);
    }
}