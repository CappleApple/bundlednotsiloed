package com.cappleapple.bundlednotsiloed.client;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Identifies item tooltips rendered from native player inventory and hotbar slots. */
public final class InventoryItemTooltipContext {
    private static final int PLAYER_STORAGE_END = 36;
    private static final ThreadLocal<Slot> HOVERED_SLOT = new ThreadLocal<>();

    private InventoryItemTooltipContext() {}

    public static void begin(Slot slot) {
        if (slot == null) HOVERED_SLOT.remove();
        else HOVERED_SLOT.set(slot);
    }

    public static void end() {
        HOVERED_SLOT.remove();
    }

    public static boolean isPlayerStorageTooltip(Container playerInventory, ItemStack tooltipStack) {
        Slot slot = HOVERED_SLOT.get();
        if (slot == null || slot.container != playerInventory) return false;
        int inventorySlot = slot.getContainerSlot();
        return inventorySlot >= 0 && inventorySlot < PLAYER_STORAGE_END
                && slot.hasItem()
                && ItemStack.isSameItemSameComponents(slot.getItem(), tooltipStack);
    }
}
