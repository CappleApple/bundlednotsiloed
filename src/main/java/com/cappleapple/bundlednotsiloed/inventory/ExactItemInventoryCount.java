package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.world.item.ItemStack;

/** Counts one exact item-and-components identity across the complete logical inventory. */
public final class ExactItemInventoryCount {
    private ExactItemInventoryCount() {}

    public static long count(DynamicCapacityInventory inventory, ItemStack target) {
        if (target.isEmpty()) return 0;
        return inventory.entries().stream()
                .filter(entry -> ItemStack.isSameItemSameComponents(entry.representative(), target))
                .mapToLong(entry -> entry.quantity())
                .findFirst()
                .orElse(0);
    }
}
