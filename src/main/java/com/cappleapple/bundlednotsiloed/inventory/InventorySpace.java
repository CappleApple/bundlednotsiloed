package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.world.item.ItemStack;

/** Exact, simulation-only capacity queries shared by client feedback and tests. */
public final class InventorySpace {
    private InventorySpace() {}

    /** Returns true when at least one item from the supplied stack can enter logical storage. */
    public static boolean canAcceptAny(DynamicCapacityInventory inventory, ItemStack stack) {
        return inventory != null && stack != null && !stack.isEmpty()
                && inventory.insertAtOrAfter(stack, 0, true).acceptedAnything();
    }
}
