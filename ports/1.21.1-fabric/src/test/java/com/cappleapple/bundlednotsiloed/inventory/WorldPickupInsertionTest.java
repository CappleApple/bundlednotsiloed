package com.cappleapple.bundlednotsiloed.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldPickupInsertionTest {
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void brandNewIdentityUsesFirstAvailableMainGridSlot() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlot(11, new ItemStack(Items.STONE));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT), NewItemDestination.INVENTORY_FIRST, false);

        assertEquals(Items.DIRT, inventory.syntheticStack(10).getItem());
        assertEquals(Items.STONE, inventory.syntheticStack(11).getItem());
        assertTrue(inventory.syntheticStack(0).isEmpty());
    }

    @Test
    void existingStowedIdentityStaysStowedInsteadOfOccupyingAnEmptyVisibleSlot() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.DIRT, 10));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT, 3), NewItemDestination.HOTBAR_FIRST, false);

        assertEquals(13, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.syntheticStack(9).isEmpty());
    }

    @Test
    void visibleHotbarIdentityIsFilledThenOverflowIsStowed() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 63));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT, 3), NewItemDestination.STOWED_FIRST, false);

        assertEquals(64, inventory.syntheticStack(0).getCount());
        assertEquals(2, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.syntheticStack(9).isEmpty());
    }

    @Test
    void inventoryFirstUsesHotbarOnlyAfterTheMainGridIsFull() {
        DynamicCapacityInventory inventory = inventory();
        for (int slot = 9; slot < 36; slot++) inventory.replaceSyntheticSlot(slot, new ItemStack(Items.STONE));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT), NewItemDestination.INVENTORY_FIRST, false);

        assertEquals(Items.DIRT, inventory.syntheticStack(0).getItem());
    }

    @Test
    void hotbarFirstUsesTheFirstEmptyHotbarSlot() {
        DynamicCapacityInventory inventory = inventory();

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT), NewItemDestination.HOTBAR_FIRST, false);

        assertEquals(Items.DIRT, inventory.syntheticStack(0).getItem());
        assertTrue(inventory.syntheticStack(9).isEmpty());
    }

    @Test
    void stowedFirstBypassesEmptyVisibleSlots() {
        DynamicCapacityInventory inventory = inventory();

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT), NewItemDestination.STOWED_FIRST, false);

        assertEquals(Items.DIRT, inventory.syntheticStack(36).getItem());
        assertTrue(inventory.syntheticStack(0).isEmpty());
        assertTrue(inventory.syntheticStack(9).isEmpty());
    }

    @Test
    void customMenuRelocationUsesMainGridWhenHotbarFirstHasNoRoom() {
        DynamicCapacityInventory inventory = inventory();
        for (int slot = 0; slot < 9; slot++) inventory.replaceSyntheticSlot(slot, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlot(11, new ItemStack(Items.STONE));
        var before = inventory.visibleCompatibilitySnapshot();
        inventory.replaceSyntheticSlot(35, new ItemStack(Items.DIRT, 4));

        assertTrue(WorldPickupInsertion.relocateReceivedVisibleStacks(
                inventory, before, NewItemDestination.HOTBAR_FIRST));

        assertEquals(4, inventory.syntheticStack(10).getCount());
        assertTrue(inventory.syntheticStack(10).is(Items.DIRT));
        assertTrue(inventory.syntheticStack(35).isEmpty());
    }

    @Test
    void customMenuRelocationHonorsStowedFirst() {
        DynamicCapacityInventory inventory = inventory();
        var before = inventory.visibleCompatibilitySnapshot();
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.DIRT, 4));

        assertTrue(WorldPickupInsertion.relocateReceivedVisibleStacks(
                inventory, before, NewItemDestination.STOWED_FIRST));

        assertTrue(inventory.syntheticStack(9).isEmpty());
        assertEquals(4, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.syntheticStack(36).is(Items.DIRT));
    }

    private static DynamicCapacityInventory inventory() {
        return new DynamicCapacityInventory(() -> 10_000);
    }
}
