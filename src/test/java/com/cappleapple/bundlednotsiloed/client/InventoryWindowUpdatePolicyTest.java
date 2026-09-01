package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventoryWindowUpdatePolicyTest {
    @Test
    void freezesAutomaticRemappingWhileTheCursorCarriesAnItem() {
        assertFalse(InventoryWindowUpdatePolicy.shouldApply(true, false, true));
    }

    @Test
    void permitsExplicitNavigationWhileTheCursorCarriesAnItem() {
        assertTrue(InventoryWindowUpdatePolicy.shouldApply(true, true, true));
    }

    @Test
    void permitsTheInitialWindowAndCursorEmptyRefreshes() {
        assertTrue(InventoryWindowUpdatePolicy.shouldApply(true, false, false));
        assertTrue(InventoryWindowUpdatePolicy.shouldApply(false, false, true));
    }
}
