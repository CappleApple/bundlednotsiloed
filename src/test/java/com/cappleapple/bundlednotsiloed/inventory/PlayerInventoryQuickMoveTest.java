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

class PlayerInventoryQuickMoveTest {
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void mergesThenFillsTheHotbar() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 63));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.DIRT, 3));

        assertTrue(PlayerInventoryQuickMove.move(inventory, 9));

        assertEquals(64, inventory.syntheticStack(0).getCount());
        assertEquals(2, inventory.syntheticStack(1).getCount());
        assertTrue(inventory.syntheticStack(9).isEmpty());
        assertTrue(inventory.validate());
    }

    @Test
    void appendsRemainderAfterNecessarySlotsWhenTheHotbarIsFull() {
        DynamicCapacityInventory inventory = inventory();
        for (int slot = 0; slot < 9; slot++) inventory.replaceSyntheticSlot(slot, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.DIRT, 4));
        inventory.replaceSyntheticSlot(11, new ItemStack(Items.APPLE));
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.COBBLESTONE));
        inventory.replaceSyntheticSlot(38, new ItemStack(Items.GRANITE));

        assertTrue(PlayerInventoryQuickMove.move(inventory, 9));

        assertTrue(inventory.syntheticStack(9).isEmpty());
        assertTrue(inventory.syntheticStack(10).isEmpty());
        assertTrue(inventory.syntheticStack(37).isEmpty());
        assertEquals(4, inventory.syntheticStack(39).getCount());
        assertTrue(inventory.syntheticStack(39).is(Items.DIRT));
        assertTrue(inventory.validate());
    }

    @Test
    void hotbarSourceUsesFirstAvailableMainInventorySlot() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 4));
        for (int slot = 1; slot < 9; slot++) inventory.replaceSyntheticSlot(slot, new ItemStack(Items.STONE));

        assertTrue(PlayerInventoryQuickMove.move(inventory, 0));

        assertTrue(inventory.syntheticStack(0).isEmpty());
        assertEquals(4, inventory.syntheticStack(9).getCount());
        assertTrue(inventory.syntheticStack(9).is(Items.DIRT));
        assertTrue(inventory.syntheticStack(36).isEmpty());
        assertTrue(inventory.validate());
    }

    @Test
    void hotbarSourceMergesThenFillsTheMainInventory() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 4));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.DIRT, 63));

        assertTrue(PlayerInventoryQuickMove.move(inventory, 0));

        assertTrue(inventory.syntheticStack(0).isEmpty());
        assertEquals(64, inventory.syntheticStack(9).getCount());
        assertEquals(3, inventory.syntheticStack(10).getCount());
        assertTrue(inventory.syntheticStack(10).is(Items.DIRT));
        assertTrue(inventory.validate());
    }

    @Test
    void hotbarSourceUsesFirstAvailableStashSlotWhenTheMainInventoryIsFull() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 4));
        for (int slot = 9; slot < 36; slot++) inventory.replaceSyntheticSlot(slot, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.APPLE));
        inventory.replaceSyntheticSlot(38, new ItemStack(Items.GRANITE));

        assertTrue(PlayerInventoryQuickMove.move(inventory, 0));

        assertTrue(inventory.syntheticStack(0).isEmpty());
        assertEquals(4, inventory.syntheticStack(37).getCount());
        assertTrue(inventory.syntheticStack(37).is(Items.DIRT));
        assertTrue(inventory.syntheticStack(39).isEmpty());
        assertTrue(inventory.validate());
    }

    @Test
    void sendsOnlyTheHotbarOverflowToTheTail() {
        DynamicCapacityInventory inventory = inventory();
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 63));
        for (int slot = 1; slot < 9; slot++) inventory.replaceSyntheticSlot(slot, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.DIRT, 64));

        assertTrue(PlayerInventoryQuickMove.move(inventory, 9));

        assertEquals(64, inventory.syntheticStack(0).getCount());
        assertEquals(63, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.syntheticStack(36).is(Items.DIRT));
        assertTrue(inventory.validate());
    }

    private static DynamicCapacityInventory inventory() {
        return new DynamicCapacityInventory(() -> 10_000);
    }
}
