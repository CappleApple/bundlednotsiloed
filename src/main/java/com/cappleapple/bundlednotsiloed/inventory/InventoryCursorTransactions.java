package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.ExtractionResult;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative extraction helpers shared by normal and creative menu cursors. */
public final class InventoryCursorTransactions {
    private InventoryCursorTransactions() {}

    /** Takes at most one legal cursor stack from hidden storage, optionally rounding half upward. */
    public static ItemStack takeFromBackend(
            DynamicCapacityInventory inventory,
            ItemStack prototype,
            int maximum,
            boolean takeHalf
    ) {
        Objects.requireNonNull(inventory, "inventory");
        if (prototype == null || prototype.isEmpty() || maximum <= 0) return ItemStack.EMPTY;
        int available = inventory.extractAtOrAfter(prototype, maximum, 36, true).extractedAmount();
        int amount = takeHalf ? Math.max(1, Math.ceilDiv(available, 2)) : available;
        ExtractionResult extraction = inventory.extractAtOrAfter(prototype, amount, 36, false);
        return extraction.extractedAmount() == 0
                ? ItemStack.EMPTY
                : prototype.copyWithCount(extraction.extractedAmount());
    }
}
