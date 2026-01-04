package dev.iLnv_09.api.events.impl;

import dev.iLnv_09.api.events.Event;

public class EntityVelocityUpdateEvent extends Event {
    public EntityVelocityUpdateEvent() {
        super(Stage.Pre);
    }
}
