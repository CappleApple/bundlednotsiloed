package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.CapacityAmount;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import org.junit.jupiter.api.Test;

class InventorySearchBarTest {
    @Test
    void capacityRatioClampsAtEmptyAndFull() {
        assertEquals(0.0, InventorySearchBar.fillRatio(CapacityAmount.ZERO, 100));
        assertEquals(0.5, InventorySearchBar.fillRatio(CapacityAmount.of(50), 100));
        assertEquals(1.0, InventorySearchBar.fillRatio(CapacityAmount.of(150), 100));
    }

    @Test
    void capacityColorContinuouslyInterpolatesFromGreenThroughOrangeToRed() {
        assertEquals(InventorySearchBar.EMPTY_COLOR, InventorySearchBar.fillColor(0.0));
        assertEquals(InventorySearchBar.MIDDLE_COLOR, InventorySearchBar.fillColor(0.5));
        assertEquals(InventorySearchBar.FULL_COLOR, InventorySearchBar.fillColor(1.0));
        assertNotEquals(InventorySearchBar.EMPTY_COLOR, InventorySearchBar.fillColor(0.25));
        assertNotEquals(InventorySearchBar.MIDDLE_COLOR, InventorySearchBar.fillColor(0.75));
    }

    @Test
    void normalHoverShowsStacksAndShiftHoverUsesExactlyThreeHelpLines() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 2_304);

        assertEquals("0S / 36S", InventorySearchBar.tooltip(inventory, false).getFirst().getString());
        assertEquals(3, InventorySearchBar.tooltip(inventory, true).size());
    }

    @Test
    void nonEmptySearchBlinksUntilClearedWhileFocusAndHoverRemainHighlighted() {
        assertTrue(InventorySearchBar.shouldHighlight("stone", false, false, 0));
        assertEquals(false, InventorySearchBar.shouldHighlight("stone", false, false, 500));
        assertEquals(false, InventorySearchBar.shouldHighlight("", false, false, 0));
        assertTrue(InventorySearchBar.shouldHighlight("", true, false, 500));
        assertTrue(InventorySearchBar.shouldHighlight("", false, true, 500));
    }

    @Test
    void activeQueryRefocusesOnSearchHoverAndOnlyInteractiveTargetsReleaseTyping() {
        assertTrue(InventorySearchBar.shouldRefocus("stone", false, true, true));
        assertEquals(false, InventorySearchBar.shouldRefocus("", false, true, true));
        assertEquals(false, InventorySearchBar.shouldRefocus("stone", true, true, true));
        assertEquals(false, InventorySearchBar.shouldRefocus("stone", false, true, false));

        assertTrue(InventorySearchBar.shouldReleaseFocus(true, true, false, true));
        assertEquals(false, InventorySearchBar.shouldReleaseFocus(true, true, false, false));
        assertEquals(false, InventorySearchBar.shouldReleaseFocus(true, true, true, true));
        assertEquals(false, InventorySearchBar.shouldReleaseFocus(true, false, false, true));
    }
}
