package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventorySideRailTest {
    @Test
    void stacksExistingSizeButtonsOnOneAttachedLeftRail() {
        InventorySideRail.Rail rail = InventorySideRail.at(100, 84);

        assertEquals(83, rail.left());
        assertEquals(81, rail.top());
        assertEquals(104, rail.right());
        assertEquals(130, rail.bottom());
        assertEquals(86, rail.searchX());
        assertEquals(84, rail.searchY());
        assertEquals(99, rail.categoryY());
        assertEquals(114, rail.settingsY());
        assertTrue(rail.searchContains(86, 84));
        assertTrue(rail.categoryContains(90, 100));
        assertTrue(rail.settingsContains(96, 126));
        assertFalse(rail.searchContains(85, 84));
    }
}
