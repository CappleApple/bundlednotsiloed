package com.cappleapple.bundlednotsiloed.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class ContainerTransfersTest {
    @Test
    void dumpCandidatesAndExtractionNeverTouchHotbar() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 256);
        inventory.replaceSyntheticSlot(0, new ItemStack(Items.APPLE, 5));
        inventory.replaceSyntheticSlot(9, new ItemStack(Items.APPLE, 3));
        inventory.replaceSyntheticSlot(36, new ItemStack(Items.DIRT, 7));

        List<ItemStack> candidates = ContainerTransfers.dumpableStacks(inventory);

        assertEquals(2, candidates.size());
        assertEquals(3, candidates.get(0).getCount());
        assertEquals(Items.APPLE, candidates.get(0).getItem());
        assertEquals(Items.DIRT, candidates.get(1).getItem());

        ContainerTransfers.extractDumpedStack(inventory, candidates.get(0), 3);

        assertEquals(5, inventory.syntheticStack(0).getCount());
        assertTrue(inventory.syntheticStack(9).isEmpty());
    }
}
