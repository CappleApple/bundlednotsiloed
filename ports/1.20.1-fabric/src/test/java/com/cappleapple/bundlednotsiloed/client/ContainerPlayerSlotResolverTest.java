package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ContainerPlayerSlotResolverTest {
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void resolvesPlayerSlotsAndRejectsEquipmentAndExternalContainers() {
        Inventory inventory = new Inventory(null);

        assertEquals(23, ContainerPlayerSlotResolver.resolve(new Slot(inventory, 23, 0, 0), inventory));
        assertEquals(-1, ContainerPlayerSlotResolver.resolve(
                new Slot(new SimpleContainer(36), 23, 0, 0), inventory));
        assertEquals(-1, ContainerPlayerSlotResolver.resolve(new Slot(inventory, 40, 0, 0), inventory));
    }
}
