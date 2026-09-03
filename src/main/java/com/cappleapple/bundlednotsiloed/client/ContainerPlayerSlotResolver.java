package com.cappleapple.bundlednotsiloed.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;

/** Resolves direct and standard NeoForge-wrapped slots back to vanilla player indices. */
public final class ContainerPlayerSlotResolver {
    private ContainerPlayerSlotResolver() {}

    public static int resolve(Slot slot, Inventory inventory) {
        int inventorySlot = slot.getContainerSlot();
        if (inventorySlot < 0 || inventorySlot >= 36) return -1;
        if (slot.container == inventory) return inventorySlot;
        if (!(slot instanceof SlotItemHandler itemHandlerSlot)) return -1;

        if (itemHandlerSlot.getItemHandler() instanceof InvWrapper wrapper
                && wrapper.getInv() == inventory) return inventorySlot;
        if (itemHandlerSlot.getItemHandler() instanceof PlayerMainInvWrapper wrapper
                && wrapper.getInventoryPlayer() == inventory) return inventorySlot;
        return -1;
    }
}
