package com.cappleapple.bundlednotsiloed.compat;

import com.cappleapple.stacksnotslots.api.InsertionResult;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import com.cappleapple.bundlednotsiloed.inventory.InsertionContext;
import com.cappleapple.bundlednotsiloed.inventory.InventoryTransactions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Stack-oriented convenience view (use the native resource handler for capabilities): one index per legal backing stack plus an always-growing append slot. */
public final class DynamicItemHandler {
    private final DynamicCapacityInventory inventory;
    private final Player player;

    public DynamicItemHandler(Player player) {
        this.player = player;
        this.inventory = player.getData(com.cappleapple.bundlednotsiloed.data.ModAttachments.PLAYER_DATA).inventory();
    }

    /** Core-only constructor used by isolated adapter tests. */
    public DynamicItemHandler(DynamicCapacityInventory inventory) {
        this.player = null;
        this.inventory = inventory;
    }

    public int getSlots() { return inventory.compatibilitySlotCount(); }
    public ItemStack getStackInSlot(int slot) { return inventory.syntheticStack(slot); }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        checkSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack existing = inventory.syntheticStack(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) return stack;
        InsertionResult result = player == null
                ? inventory.insertIntoSyntheticSlot(stack, slot, simulate)
                : InventoryTransactions.insertIntoSyntheticSlot(player, stack, slot, simulate);
        return result.remainder();
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        checkSlot(slot);
        return inventory.extractSyntheticSlot(slot, amount, simulate);
    }

    public int getSlotLimit(int slot) {
        checkSlot(slot);
        ItemStack stack = inventory.syntheticStack(slot);
        return stack.isEmpty() ? 64 : stack.getMaxStackSize();
    }

    public boolean isItemValid(int slot, ItemStack stack) { checkSlot(slot); return !stack.isEmpty(); }
    public void setStackInSlot(int slot, ItemStack stack) { checkSlot(slot); inventory.replaceSyntheticSlot(slot, stack); }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) throw new RuntimeException("Slot " + slot + " not in valid range [0," + getSlots() + ")");
    }
}
