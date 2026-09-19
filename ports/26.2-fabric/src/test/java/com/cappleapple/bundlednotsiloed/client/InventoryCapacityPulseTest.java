package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.CapacityAmount;
import org.junit.jupiter.api.Test;

class InventoryCapacityPulseTest {
    @Test
    void appearsOnlyAfterUsedCapacityChangesThenFadesAway() {
        InventoryCapacityPulse pulse = new InventoryCapacityPulse();

        assertFalse(pulse.observe(CapacityAmount.ZERO, 0));
        assertEquals(0, pulse.opacity(0));
        assertFalse(pulse.observe(CapacityAmount.ZERO, 100));
        assertTrue(pulse.observe(CapacityAmount.of(1), 100));
        assertEquals(255, pulse.opacity(1_600));
        assertEquals(128, pulse.opacity(1_850));
        assertEquals(0, pulse.opacity(2_100));
    }
}
