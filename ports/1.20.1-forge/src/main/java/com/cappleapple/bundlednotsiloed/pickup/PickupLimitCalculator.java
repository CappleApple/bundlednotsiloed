package com.cappleapple.bundlednotsiloed.pickup;

import com.cappleapple.stacksnotslots.api.CapacityAmount;
import com.cappleapple.stacksnotslots.api.LogicalInventoryEntry;
import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.CategoryMatcher;
import com.cappleapple.stacksnotslots.api.CapacityUnits;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class PickupLimitCalculator {
    private PickupLimitCalculator() {}

    public static int maximumAccepted(DynamicCapacityInventory inventory, List<CategoryDefinition> categories, ItemStack stack, int requested) {
        CapacityAmount unitCost = CapacityUnits.exactUnitCost(stack);
        int allowed = Math.max(0, requested);
        for (CategoryDefinition category : categories) {
            if (!category.enabled() || category.pickupLimit() < 0 || !CategoryMatcher.matches(category, stack)) continue;
            CapacityAmount remaining = CapacityAmount.of(category.pickupLimit())
                    .subtract(usageForCategoryExact(inventory, category)).maxZero();
            allowed = Math.min(allowed, (int)Math.min(Integer.MAX_VALUE, remaining.divideFloorToLong(unitCost)));
        }
        return allowed;
    }

    /** Conservative whole-unit compatibility view of the exact category usage. */
    public static long usageForCategory(DynamicCapacityInventory inventory, CategoryDefinition category) {
        return usageForCategoryExact(inventory, category).ceilToLong();
    }

    public static CapacityAmount usageForCategoryExact(DynamicCapacityInventory inventory, CategoryDefinition category) {
        CapacityAmount used = CapacityAmount.ZERO;
        for (LogicalInventoryEntry entry : inventory.entries()) {
            if (!CategoryMatcher.matches(category, entry.representative())) continue;
            used = used.add(CapacityUnits.exactCost(entry.representative(), entry.quantity()));
        }
        return used;
    }
}
