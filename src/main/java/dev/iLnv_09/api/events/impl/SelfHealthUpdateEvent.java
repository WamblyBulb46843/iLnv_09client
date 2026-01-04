package dev.iLnv_09.api.events.impl;

import net.minecraft.client.network.ClientPlayerEntity;

public record SelfHealthUpdateEvent(ClientPlayerEntity player, float health) {
}
