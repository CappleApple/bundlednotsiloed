package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** Extends vanilla clear/count operations across the unbounded logical inventory. */
public final class InventoryClearing {
    private InventoryClearing() {}

    /**
     * Applies vanilla's clear/count convention to logical slots at or beyond {@code minimumSlot}:
     * zero counts without changing inventory, a negative value clears every match, and a positive
     * value clears at most that many items.
     */
    public static int clearOrCountMatchingItems(
            DynamicCapacityInventory inventory,
            Predicate<ItemStack> predicate,
            int maxCount,
            int minimumSlot
    ) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(predicate, "predicate");
        boolean countOnly = maxCount == 0;
        long affected = 0;
        List<ItemStack> snapshot = inventory.backingStacks();
        for (int slot = Math.max(0, minimumSlot); slot < snapshot.size(); slot++) {
            ItemStack stack = snapshot.get(slot);
            if (stack.isEmpty() || !predicate.test(stack)) continue;
            if (countOnly) {
                affected += stack.getCount();
                continue;
            }

            int amount = maxCount < 0
                    ? stack.getCount()
                    : (int)Math.min(stack.getCount(), (long)maxCount - affected);
            if (amount <= 0) break;
            affected += inventory.extractSyntheticSlot(slot, amount, false).getCount();
            if (maxCount > 0 && affected >= maxCount) break;
        }
        return (int)Math.min(Integer.MAX_VALUE, affected);
    }
}
