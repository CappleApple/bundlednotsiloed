package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.ExtractionResult;
import com.cappleapple.stacksnotslots.api.InsertionResult;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.world.item.ItemStack;

/** Tops up existing hotbar/main-grid stacks exclusively from stowed slots. */
public final class VisibleStackRefill {
    private static final int VISIBLE_SLOT_COUNT = 36;

    private VisibleStackRefill() {}

    public static boolean refill(DynamicCapacityInventory inventory) {
        boolean changed = false;
        for (int slot = 0; slot < VISIBLE_SLOT_COUNT; slot++) {
            ItemStack visible = inventory.syntheticStack(slot);
            if (visible.isEmpty() || !visible.isStackable()) continue;
            int missing = visible.getMaxStackSize() - visible.getCount();
            if (missing <= 0) continue;

            ExtractionResult extraction = inventory.extractAtOrAfter(visible, missing, VISIBLE_SLOT_COUNT, false);
            if (extraction.extractedAmount() <= 0) continue;
            ItemStack refill = visible.copyWithCount(extraction.extractedAmount());
            InsertionResult insertion = inventory.insertIntoSyntheticSlot(refill, slot, false);
            if (!insertion.acceptedAll()) {
                inventory.insertAtOrAfter(insertion.remainder(), VISIBLE_SLOT_COUNT, false);
            }
            changed |= insertion.acceptedAnything();
        }
        return changed;
    }
}
