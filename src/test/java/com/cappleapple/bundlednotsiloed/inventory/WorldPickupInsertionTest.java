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
    void brandNewIdentityUsesMainGridInDisplayOrder() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.STONE));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT), true, false);

        assertEquals(Items.DIRT, inventory.syntheticStack(10).getItem());
        assertTrue(inventory.syntheticStack(0).isEmpty());
    }

    @Test
    void existingStowedIdentityStaysStowedInsteadOfOccupyingAnEmptyVisibleSlot() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.DIRT, 10));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT, 3), true, false);

        assertEquals(13, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.syntheticStack(9).isEmpty());
    }

    @Test
    void visibleHotbarIdentityIsFilledThenOverflowIsStowed() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 63));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT, 3), true, false);

        assertEquals(64, inventory.syntheticStack(0).getCount());
        assertEquals(2, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.syntheticStack(9).isEmpty());
    }

    @Test
    void hotbarIsOnlyUsedForNewIdentityAfterTheMainGridIsFull() {
        DynamicCapacityInventory inventory = inventory();
        for (int slot = 9; slot < 36; slot++) inventory.replaceSyntheticSlot(slot, new ItemStack(Items.STONE));

        WorldPickupInsertion.insert(inventory, new ItemStack(Items.DIRT), true, false);

        assertEquals(Items.DIRT, inventory.syntheticStack(0).getItem());
    }

    private static DynamicCapacityInventory inventory() {
        return new DynamicCapacityInventory(() -> 10_000);
    }
}
