package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.stacksnotslots.api.CapacityAmount;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import org.junit.jupiter.api.Test;

class InventorySearchBarTest {
    @Test
    void capacityFillClampsAndRetainsAVisibleFirstPixel() {
        assertEquals(0, InventorySearchBar.fillWidth(CapacityAmount.ZERO, 100));
        assertEquals(1, InventorySearchBar.fillWidth(CapacityAmount.fraction(1, 1_000), 100));
        assertEquals(44, InventorySearchBar.fillWidth(CapacityAmount.of(50), 100));
        assertEquals(88, InventorySearchBar.fillWidth(CapacityAmount.of(150), 100));
        assertEquals(0.5, InventorySearchBar.fillRatio(CapacityAmount.of(50), 100));
    }

    @Test
    void capacityColorMovesFromMutedGreenThroughOrangeToRed() {
        assertEquals(InventorySearchBar.LOW_COLOR, InventorySearchBar.fillColor(0.49));
        assertEquals(InventorySearchBar.MEDIUM_COLOR, InventorySearchBar.fillColor(0.5));
        assertEquals(InventorySearchBar.MEDIUM_COLOR, InventorySearchBar.fillColor(0.79));
        assertEquals(InventorySearchBar.HIGH_COLOR, InventorySearchBar.fillColor(0.8));
    }

    @Test
    void normalHoverShowsStacksAndShiftHoverUsesExactlyThreeHelpLines() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 2_304);

        assertEquals("0S / 36S", InventorySearchBar.tooltip(inventory, false).getFirst().getString());
        assertEquals(3, InventorySearchBar.tooltip(inventory, true).size());
    }
}
