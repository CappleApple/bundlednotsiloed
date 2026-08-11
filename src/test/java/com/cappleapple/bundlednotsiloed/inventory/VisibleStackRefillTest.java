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

class VisibleStackRefillTest {
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void topsUpVisibleStacksOnlyFromBackend() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 10_000);
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.DIRT, 60));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.DIRT, 5));
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.DIRT, 10));

        assertTrue(VisibleStackRefill.refill(inventory));

        assertEquals(64, inventory.syntheticStack(0).getCount());
        assertEquals(11, inventory.syntheticStack(9).getCount());
        assertTrue(inventory.syntheticStack(36).isEmpty());
        assertFalse(VisibleStackRefill.refill(inventory));
    }
}
