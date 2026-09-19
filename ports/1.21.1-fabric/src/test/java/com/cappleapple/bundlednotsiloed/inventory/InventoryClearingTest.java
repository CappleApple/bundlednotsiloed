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

class InventoryClearingTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void unlimitedClearIncludesOnlyMatchingHiddenStacks() {
        DynamicCapacityInventory inventory = inventoryWithVisibleAndHiddenStacks();

        int removed = InventoryClearing.clearOrCountMatchingItems(
                inventory, stack -> stack.is(Items.STONE), -1, 36);

        assertEquals(7, removed);
        assertEquals(3, inventory.syntheticStack(0).getCount());
        assertTrue(inventory.syntheticStack(36).isEmpty());
        assertEquals(2, inventory.syntheticStack(37).getCount());
        assertTrue(inventory.validate());
    }

    @Test
    void countOnlyIncludesAllHiddenMatchesWithoutMutation() {
        DynamicCapacityInventory inventory = inventoryWithVisibleAndHiddenStacks();

        int counted = InventoryClearing.clearOrCountMatchingItems(
                inventory, stack -> stack.is(Items.STONE), 0, 36);

        assertEquals(7, counted);
        assertEquals(7, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.validate());
    }

    @Test
    void positiveLimitRemovesOnlyTheRequestedHiddenAmount() {
        DynamicCapacityInventory inventory = inventoryWithVisibleAndHiddenStacks();

        int removed = InventoryClearing.clearOrCountMatchingItems(
                inventory, stack -> stack.is(Items.STONE), 4, 36);

        assertEquals(4, removed);
        assertEquals(3, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.validate());
    }

    private static DynamicCapacityInventory inventoryWithVisibleAndHiddenStacks() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 512);
        inventory.replaceSyntheticSlotFromItemUse(0, new ItemStack(Items.STONE, 3));
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.STONE, 7));
        inventory.replaceSyntheticSlotFromItemUse(37, new ItemStack(Items.DIRT, 2));
        return inventory;
    }
}
