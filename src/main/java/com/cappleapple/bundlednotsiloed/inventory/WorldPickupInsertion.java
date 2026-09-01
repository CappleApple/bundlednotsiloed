package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.InsertionRejection;
import com.cappleapple.stacksnotslots.api.InsertionResult;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Identity-aware placement policy for newly acquired items. */
public final class WorldPickupInsertion {
    private static final int HOTBAR_START = 0;
    private static final int MAIN_GRID_START = 9;
    private static final int BACKEND_START = 36;

    private WorldPickupInsertion() {}

    /**
     * Keeps an item's existing location stable. A visible matching identity is topped up before
     * overflow is stowed; otherwise an existing stowed identity is joined. Only brand-new
     * identities use the configured destination order.
     */
    public static InsertionResult insert(
            DynamicCapacityInventory inventory, ItemStack stack, NewItemDestination destination, boolean simulate
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
            switch (destination) {
                case HOTBAR_FIRST -> {
                    remaining = fillEmptyRange(inventory, remaining, HOTBAR_START, MAIN_GRID_START);
                    remaining = fillEmptyRange(inventory, remaining, MAIN_GRID_START, BACKEND_START);
                }
                case INVENTORY_FIRST -> {
                    remaining = fillEmptyRange(inventory, remaining, MAIN_GRID_START, BACKEND_START);
                    remaining = fillEmptyRange(inventory, remaining, HOTBAR_START, MAIN_GRID_START);
                }
                case STOWED_FIRST -> remaining = inventory.insertAtOrAfter(
                        remaining, BACKEND_START, false).remainder();
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

    /**
     * Repositions only the positive visible-slot deltas produced by a custom menu which bypassed
     * the normal quick-move helper. Pre-existing stacks remain in place, while the received items
     * are reinserted through the same destination policy as ordinary pickups and quick moves.
     */
    public static boolean relocateReceivedVisibleStacks(
            DynamicCapacityInventory inventory, List<ItemStack> before, NewItemDestination destination
    ) {
        if (before == null || before.size() < BACKEND_START) return false;
        ArrayList<ReceivedStack> received = new ArrayList<>();
        for (int slot = HOTBAR_START; slot < BACKEND_START; slot++) {
            ItemStack previous = before.get(slot);
            ItemStack current = inventory.syntheticStack(slot);
            int amount = previous.isEmpty() ? current.getCount()
                    : ItemStack.isSameItemSameComponents(previous, current)
                            ? Math.max(0, current.getCount() - previous.getCount()) : 0;
            if (amount > 0) received.add(new ReceivedStack(slot, current.copyWithCount(amount)));
        }
        if (received.isEmpty()) return false;

        ArrayList<ItemStack> extracted = new ArrayList<>(received.size());
        for (ReceivedStack value : received) {
            ItemStack moved = inventory.extractSyntheticSlot(value.slot(), value.stack().getCount(), false);
            if (!moved.isEmpty()) extracted.add(moved);
        }
        for (ItemStack moved : extracted) {
            InsertionResult insertion = insert(inventory, moved, destination, false);
            if (!insertion.acceptedAll()) {
                inventory.insertAtOrAfter(insertion.remainder(), BACKEND_START, false);
            }
        }
        return !extracted.isEmpty();
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

    private record ReceivedStack(int slot, ItemStack stack) {}
}
