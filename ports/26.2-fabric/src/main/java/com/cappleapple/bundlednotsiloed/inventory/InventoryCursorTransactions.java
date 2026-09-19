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
        return takeFromRange(inventory, prototype, maximum, takeHalf, 36);
    }

    /** Takes from the combined range represented by a logical browser without touching earlier slots. */
    public static ItemStack takeFromRange(
            DynamicCapacityInventory inventory,
            ItemStack prototype,
            int maximum,
            boolean takeHalf,
            int firstSlot
    ) {
        Objects.requireNonNull(inventory, "inventory");
        if (prototype == null || prototype.isEmpty() || maximum <= 0) return ItemStack.EMPTY;
        int minimumSlot = Math.max(0, firstSlot);
        int available = inventory.extractAtOrAfter(prototype, maximum, minimumSlot, true).extractedAmount();
        int amount = takeHalf ? Math.max(1, Math.ceilDiv(available, 2)) : available;
        ExtractionResult extraction = inventory.extractAtOrAfter(prototype, amount, minimumSlot, false);
        return extraction.extractedAmount() == 0
                ? ItemStack.EMPTY
                : prototype.copyWithCount(extraction.extractedAmount());
    }
}
