package com.cappleapple.bundlednotsiloed.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoryCursorTransactionsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void takeStackUsesOnlyHiddenStorageAndHonorsCursorMaximum() {
        DynamicCapacityInventory inventory = inventoryWithHiddenStone();

        ItemStack taken = InventoryCursorTransactions.takeFromBackend(
                inventory, new ItemStack(Items.STONE), 64, false);

        assertSame(Items.STONE, taken.getItem());
        assertEquals(64, taken.getCount());
        assertEquals(3, inventory.syntheticStack(0).getCount());
        assertEquals(16, hiddenStoneCount(inventory));
        assertTrue(inventory.validate());
    }

    @Test
    void takeHalfRoundsTheAvailableCursorStackUpward() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 512);
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.STONE, 7));

        ItemStack taken = InventoryCursorTransactions.takeFromBackend(
                inventory, new ItemStack(Items.STONE), 64, true);

        assertEquals(4, taken.getCount());
        assertEquals(3, inventory.syntheticStack(36).getCount());
        assertTrue(inventory.validate());
    }

    private static DynamicCapacityInventory inventoryWithHiddenStone() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 512);
        inventory.replaceSyntheticSlotFromItemUse(0, new ItemStack(Items.STONE, 3));
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.STONE, 40));
        inventory.replaceSyntheticSlotFromItemUse(37, new ItemStack(Items.STONE, 40));
        return inventory;
    }

    private static int hiddenStoneCount(DynamicCapacityInventory inventory) {
        return inventory.entriesAtOrAfter(36).stream()
                .filter(entry -> entry.representative().is(Items.STONE))
                .mapToInt(entry -> (int)entry.quantity())
                .sum();
    }
}
