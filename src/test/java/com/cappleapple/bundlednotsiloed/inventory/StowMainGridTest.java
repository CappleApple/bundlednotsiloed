package com.cappleapple.bundlednotsiloed.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StowMainGridTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void movesOnlyTheMainTwentySevenSlotsIntoStowedStorage() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 512);
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.APPLE, 5));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.STONE, 32));
        inventory.replaceSyntheticSlot(20, new ItemStack(Items.DIRT, 3));
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.STONE, 10));

        assertTrue(inventory.stowMainGrid());
        assertEquals(5, inventory.syntheticStack(0).getCount());
        for (int slot = 9; slot < 36; slot++) assertTrue(inventory.syntheticStack(slot).isEmpty());
        assertEquals(42, quantityAtOrAfter(inventory, Items.STONE));
        assertEquals(3, quantityAtOrAfter(inventory, Items.DIRT));
        assertFalse(inventory.stowMainGrid());
        assertTrue(inventory.validate());
    }

    private static long quantityAtOrAfter(DynamicCapacityInventory inventory, net.minecraft.world.item.Item item) {
        return inventory.entriesAtOrAfter(36).stream()
                .filter(entry -> entry.representative().is(item))
                .mapToLong(entry -> entry.quantity())
                .sum();
    }
}
