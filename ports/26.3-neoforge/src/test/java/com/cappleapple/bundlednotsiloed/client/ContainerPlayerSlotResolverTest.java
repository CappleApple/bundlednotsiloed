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
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); com.cappleapple.bundlednotsiloed.TestBootstrap.bindComponents(); }

    @Test
    void resolvesDirectAndStandardNeoForgeWrappedPlayerSlots() {
        Inventory inventory = new Inventory(null, new net.minecraft.world.entity.EntityEquipment());

        assertEquals(23, ContainerPlayerSlotResolver.resolve(new Slot(inventory, 23, 0, 0), inventory));
        assertEquals(-1, ContainerPlayerSlotResolver.resolve(new Slot(new SimpleContainer(36), 23, 0, 0), inventory));
    }
    @Test
    void resolvesNativeTransactionalPlayerSlotsAndRangeOffsets() {
        Inventory inventory = new Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        var wrapper = net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper.of(inventory);
        var full = new net.neoforged.neoforge.transfer.item.ResourceHandlerSlot(wrapper, (i, r, n) -> {}, 23, 0, 0);
        assertEquals(23, ContainerPlayerSlotResolver.resolve(full, inventory));
        var range = net.neoforged.neoforge.transfer.RangedResourceHandler.of(wrapper, 9, 36);
        var ranged = new net.neoforged.neoforge.transfer.item.ResourceHandlerSlot(range, (i, r, n) -> {}, 5, 0, 0);
        assertEquals(14, ContainerPlayerSlotResolver.resolve(ranged, inventory));
        var other = net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper.of(new Inventory(null, new net.minecraft.world.entity.EntityEquipment()));
        assertEquals(-1, ContainerPlayerSlotResolver.resolve(new net.neoforged.neoforge.transfer.item.ResourceHandlerSlot(other, (i,r,n) -> {}, 23, 0, 0), inventory));
    }
}
