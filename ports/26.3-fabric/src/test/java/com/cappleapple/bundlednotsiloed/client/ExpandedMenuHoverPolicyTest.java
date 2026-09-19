package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExpandedMenuHoverPolicyTest {
    @Test
    void onlyAnOpenMenuContainingThePointerBlocksTheUnderlyingSlot() {
        assertTrue(ExpandedMenuHoverPolicy.blocksUnderlyingSlot(true, true, false, false));
        assertTrue(ExpandedMenuHoverPolicy.blocksUnderlyingSlot(false, false, true, true));
        assertFalse(ExpandedMenuHoverPolicy.blocksUnderlyingSlot(true, false, false, false));
        assertFalse(ExpandedMenuHoverPolicy.blocksUnderlyingSlot(false, true, false, true));
    }
}
