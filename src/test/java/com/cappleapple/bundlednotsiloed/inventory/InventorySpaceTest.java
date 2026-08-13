package com.cappleapple.bundlednotsiloed.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class InventorySpaceTest {
    @Test
    void reportsNoSpaceWhenNotEvenOneItemCanBeAccepted() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 64);
        assertTrue(inventory.insert(new ItemStack(Items.STONE, 64), false).acceptedAll());

        assertFalse(InventorySpace.canAcceptAny(inventory, new ItemStack(Items.DIRT, 64)));
    }

    @Test
    void partialCapacityStillCountsAsUsableSpaceWithoutMutatingInventory() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 65);
        assertTrue(inventory.insert(new ItemStack(Items.STONE, 64), false).acceptedAll());
        long revision = inventory.revision();

        assertTrue(InventorySpace.canAcceptAny(inventory, new ItemStack(Items.DIRT, 64)));
        assertEquals(revision, inventory.revision());
        assertEquals(64, inventory.entries().getFirst().quantity());
    }

    @Test
    void honorsNonSixtyFourStackCapacityCosts() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 64);
        assertTrue(inventory.insert(new ItemStack(Items.ENDER_PEARL, 16), false).acceptedAll());

        assertFalse(InventorySpace.canAcceptAny(inventory, new ItemStack(Items.ENDER_PEARL)));
    }
}
