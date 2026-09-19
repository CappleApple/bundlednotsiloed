package com.cappleapple.bundlednotsiloed.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class InventorySlotWindowTest {
    @Test
    void emptyInventoryStillExposesAFullVanillaMainGridAndOnlyExtendsByRows() {
        InventorySlotWindow window = new InventorySlotWindow();

        window.showRange(9);

        assertTrue(window.active());
        assertFalse(window.identityView());
        assertEquals(9, window.logicalIndex(9));
        assertEquals(35, window.logicalIndex(35));

        window.showRange(18);

        assertEquals(18, window.logicalIndex(9));
        assertEquals(44, window.logicalIndex(35));
        assertEquals(9, InventorySlotWindow.maximumRangeStart(0));
        assertEquals(9, InventorySlotWindow.maximumRangeStart(36));
        assertEquals(18, InventorySlotWindow.maximumRangeStart(37));
    }

    @Test
    void mapsRealMainGridSlotsToDistinctLogicalEntriesAndResetsToVanilla() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 512);
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.APPLE));
        inventory.replaceSyntheticSlot(40, new ItemStack(Items.STONE));
        InventorySlotWindow window = new InventorySlotWindow();

        window.show(List.of(
                new ItemStack(Items.APPLE),
                new ItemStack(Items.STONE),
                new ItemStack(Items.STONE)), inventory);

        assertTrue(window.active());
        assertTrue(window.identityView());
        assertEquals(36, window.logicalIndex(9));
        assertEquals(9, window.logicalIndex(10));
        assertEquals(40, window.logicalIndex(11));
        assertEquals(9, window.vanillaSlotForLogical(36));
        assertEquals(10, window.vanillaSlotForLogical(9));
        assertEquals(11, window.vanillaSlotForLogical(40));

        window.reset();

        assertFalse(window.active());
        assertEquals(9, window.logicalIndex(9));
        assertEquals(35, window.logicalIndex(35));
        assertEquals(-1, window.vanillaSlotForLogical(36));
    }

    @Test
    void refreshRebindsAnAggregateIdentityAfterItsDisplayedStackMoves() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 512);
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.APPLE));
        InventorySlotWindow window = new InventorySlotWindow();
        window.show(List.of(new ItemStack(Items.APPLE)), inventory);
        assertEquals(36, window.logicalIndex(9));

        inventory.swapSyntheticSlots(36, 50);
        window.refresh(inventory);

        assertEquals(50, window.logicalIndex(9));
    }
}
