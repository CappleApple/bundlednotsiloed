package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.InsertionResult;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Placement order for Shift-clicks made inside the open player inventory. */
public final class PlayerInventoryQuickMove {
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 9;
    private static final int MAIN_GRID_START = 9;
    private static final int STOWED_START = 36;

    private PlayerInventoryQuickMove() {}

    /**
     * Moves hotbar sources through the main grid and then stowed storage. Main-grid sources move
     * through the hotbar before appending to stowed storage, while stowed sources fall back from
     * the hotbar to the main grid and otherwise remain in place.
     */
    public static boolean move(DynamicCapacityInventory inventory, int sourceSlot) {
        Objects.requireNonNull(inventory, "inventory");
        if (sourceSlot < 0 || sourceSlot >= inventory.syntheticSlotCount()) return false;

        ItemStack source = inventory.syntheticStack(sourceSlot);
        if (source.isEmpty()) return false;
        int originalCount = source.getCount();
        ItemStack remaining = inventory.extractSyntheticSlot(sourceSlot, originalCount, false);
        if (remaining.isEmpty()) return false;

        if (sourceSlot < HOTBAR_END) {
            remaining = mergeRange(inventory, remaining, MAIN_GRID_START, STOWED_START, sourceSlot);
            remaining = fillRange(inventory, remaining, MAIN_GRID_START, STOWED_START, sourceSlot);
            if (!remaining.isEmpty()) {
                remaining = inventory.insertAtOrAfter(remaining, STOWED_START, false).remainder();
            }
        } else {
            remaining = mergeRange(inventory, remaining, HOTBAR_START, HOTBAR_END, sourceSlot);
            remaining = fillRange(inventory, remaining, HOTBAR_START, HOTBAR_END, sourceSlot);
            if (sourceSlot >= STOWED_START) {
                remaining = mergeRange(inventory, remaining, MAIN_GRID_START, STOWED_START, sourceSlot);
                remaining = fillRange(inventory, remaining, MAIN_GRID_START, STOWED_START, sourceSlot);
            } else if (!remaining.isEmpty()) {
                int tailSlot = Math.max(STOWED_START, inventory.syntheticSlotCount());
                // Extracting the last occupied stack trims the backend extent, which can make the
                // append slot equal the source. Reinserting there and reporting success causes
                // Minecraft's QUICK_MOVE loop to retry forever because the source never changes.
                if (tailSlot != sourceSlot) {
                    remaining = inventory.insertIntoSyntheticSlot(
                            remaining, tailSlot, false).remainder();
                }
            }
        }

        if (remaining.isEmpty()) return true;

        int movedCount = originalCount - remaining.getCount();
        InsertionResult restored = inventory.insertIntoSyntheticSlot(remaining, sourceSlot, false);
        if (!restored.acceptedAll()) {
            inventory.insertAtOrAfter(restored.remainder(), STOWED_START, false);
        }
        return movedCount > 0;
    }

    private static ItemStack mergeRange(
            DynamicCapacityInventory inventory, ItemStack stack, int start, int end, int sourceSlot
    ) {
        ItemStack remaining = stack;
        for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
            if (slot == sourceSlot) continue;
            ItemStack stored = inventory.syntheticStack(slot);
            if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, remaining)
                    || stored.getCount() >= stored.getMaxStackSize()) continue;
            remaining = inventory.insertIntoSyntheticSlot(remaining, slot, false).remainder();
        }
        return remaining;
    }

    private static ItemStack fillRange(
            DynamicCapacityInventory inventory, ItemStack stack, int start, int end, int sourceSlot
    ) {
        ItemStack remaining = stack;
        for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
            if (slot == sourceSlot || !inventory.syntheticStack(slot).isEmpty()) continue;
            remaining = inventory.insertIntoSyntheticSlot(remaining, slot, false).remainder();
        }
        return remaining;
    }
}
