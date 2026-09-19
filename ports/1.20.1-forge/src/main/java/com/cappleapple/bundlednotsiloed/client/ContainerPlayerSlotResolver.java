package com.cappleapple.bundlednotsiloed.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

/** Resolves direct and standard Forge-wrapped slots back to vanilla player indices. */
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
