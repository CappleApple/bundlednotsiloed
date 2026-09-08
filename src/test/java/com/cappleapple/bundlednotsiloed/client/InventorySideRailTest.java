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
        assertEquals(145, rail.bottom());
        assertEquals(86, rail.searchX());
        assertEquals(84, rail.searchY());
        assertEquals(99, rail.categoryY());
        assertEquals(129, rail.settingsY());
        assertEquals(86, rail.sortX());
        assertEquals(114, rail.sortY());
        assertTrue(rail.searchContains(86, 84));
        assertTrue(rail.categoryContains(90, 100));
        assertTrue(rail.settingsContains(96, 141));
        assertTrue(rail.sortContains(96, 126));
        assertFalse(rail.settingsContains(96, 142));
        assertFalse(rail.sortContains(96, 129));
        assertFalse(rail.searchContains(85, 84));
    }
}
