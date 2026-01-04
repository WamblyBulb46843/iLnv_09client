package dev.iLnv_09.api.events.impl;

import dev.iLnv_09.api.events.Event;

public class GameLeftEvent extends Event {
    public GameLeftEvent() {
        super(Stage.Post);
    }
}
