package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.InsertionRejection;
import com.cappleapple.stacksnotslots.api.InsertionResult;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.world.item.ItemStack;

/** Placement policy for items collected from the world. */
public final class WorldPickupInsertion {
    private static final int HOTBAR_START = 0;
    private static final int MAIN_GRID_START = 9;
    private static final int BACKEND_START = 36;

    private WorldPickupInsertion() {}

    /**
     * Keeps an item's existing location stable. A visible matching identity is topped up before
     * overflow is stowed; otherwise an existing stowed identity is joined. Brand-new identities
     * enter the main grid left-to-right/top-to-bottom, then the optional hotbar, then the backend.
     */
    public static InsertionResult insert(
            DynamicCapacityInventory inventory, ItemStack stack, boolean allowNewHotbarStack, boolean simulate
    ) {
        InsertionResult proposal = inventory.insert(stack, true);
        if (simulate || !proposal.acceptedAnything()) return proposal;

        int requested = stack.getCount();
        ItemStack remaining = stack.copyWithCount(proposal.acceptedAmount());
        boolean hasVisibleIdentity = containsIdentity(inventory, stack, HOTBAR_START, BACKEND_START);
        boolean hasBackendIdentity = containsIdentity(inventory, stack, BACKEND_START, inventory.syntheticSlotCount());

        if (hasVisibleIdentity) {
            remaining = mergeRange(inventory, remaining, MAIN_GRID_START, BACKEND_START);
            remaining = mergeRange(inventory, remaining, HOTBAR_START, MAIN_GRID_START);
        } else if (!hasBackendIdentity) {
            remaining = fillEmptyRange(inventory, remaining, MAIN_GRID_START, BACKEND_START);
            if (allowNewHotbarStack) {
                remaining = fillEmptyRange(inventory, remaining, HOTBAR_START, MAIN_GRID_START);
            }
        }

        if (!remaining.isEmpty()) {
            remaining = inventory.insertAtOrAfter(remaining, BACKEND_START, false).remainder();
        }

        int accepted = proposal.acceptedAmount() - remaining.getCount();
        ItemStack remainder = accepted == requested ? ItemStack.EMPTY : stack.copyWithCount(requested - accepted);
        InsertionRejection rejection = accepted == requested ? InsertionRejection.NONE : proposal.rejection();
        return new InsertionResult(requested, accepted, remainder, proposal.capacityConsumed(), rejection);
    }

    private static boolean containsIdentity(DynamicCapacityInventory inventory, ItemStack prototype, int start, int end) {
        for (int slot = Math.max(0, start); slot < Math.min(end, inventory.syntheticSlotCount()); slot++) {
            ItemStack stored = inventory.syntheticStack(slot);
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, prototype)) return true;
        }
        return false;
    }

    private static ItemStack mergeRange(DynamicCapacityInventory inventory, ItemStack stack, int start, int end) {
        ItemStack remaining = stack;
        for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
            ItemStack stored = inventory.syntheticStack(slot);
            if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, remaining)
                    || stored.getCount() >= stored.getMaxStackSize()) continue;
            remaining = inventory.insertIntoSyntheticSlot(remaining, slot, false).remainder();
        }
        return remaining;
    }

    private static ItemStack fillEmptyRange(DynamicCapacityInventory inventory, ItemStack stack, int start, int end) {
        ItemStack remaining = stack;
        for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
            if (!inventory.syntheticStack(slot).isEmpty()) continue;
            remaining = inventory.insertIntoSyntheticSlot(remaining, slot, false).remainder();
        }
        return remaining;
    }
}
