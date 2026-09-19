package com.cappleapple.bundlednotsiloed.compat;

import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class PlayerItemHandlerProvider {
    private PlayerItemHandlerProvider() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(Capabilities.Item.ENTITY, net.minecraft.world.entity.EntityTypes.PLAYER,
                (player, ignored) -> new DynamicPlayerResourceHandler(player));
        event.registerEntity(Capabilities.Item.ENTITY_AUTOMATION, net.minecraft.world.entity.EntityTypes.PLAYER,
                (player, ignored) -> new DynamicPlayerResourceHandler(player));
    }
}
