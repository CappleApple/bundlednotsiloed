package com.cappleapple.bundlednotsiloed.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Resolves direct and standard NeoForge-wrapped slots back to vanilla player indices. */
public final class ContainerPlayerSlotResolver {
    private static final java.lang.reflect.Field RANGE_START;
    static {
        try {
            RANGE_START = net.neoforged.neoforge.transfer.RangedResourceHandler.class.getDeclaredField("start");
            RANGE_START.setAccessible(true);
        } catch (ReflectiveOperationException exception) { throw new ExceptionInInitializerError(exception); }
    }
    private ContainerPlayerSlotResolver() {}

    private static int resolveNative(net.neoforged.neoforge.transfer.item.ResourceHandlerSlot slot, Inventory inventory) {
        var handler = slot.getResourceHandler();
        int index = slot.getSlotIndex();
        // NeoForge exposes the delegate but not the range offset. This field is verified for
        // this target; unwrapping it keeps ranged player-slot coordinates authoritative.
        try {
            for (int depth = 0; depth < 16 && handler instanceof net.neoforged.neoforge.transfer.RangedResourceHandler<?> range; depth++) {
                index += RANGE_START.getInt(range);
                handler = ((net.neoforged.neoforge.transfer.RangedResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource>)range).getDelegate();
            }
        } catch (IllegalAccessException exception) { return -1; }
        return index >= 0 && index < 36 && handler == net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper.of(inventory) ? index : -1;
    }

    public static int resolve(Slot slot, Inventory inventory) {
        if (slot instanceof net.neoforged.neoforge.transfer.item.ResourceHandlerSlot resourceSlot) return resolveNative(resourceSlot, inventory);
        int inventorySlot = slot.getContainerSlot();
        if (inventorySlot < 0 || inventorySlot >= 36) return -1;
        if (slot.container == inventory) return inventorySlot;
        return -1;
    }
}
