package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.CapacityUnits;
import net.minecraft.world.entity.player.Inventory;

/** Player-overhaul defaults kept outside the general Stacks Not Slots backend. */
public final class PlayerInventoryDefaults {
    public static final int CAPACITY_UNITS = Inventory.INVENTORY_SIZE * (int)CapacityUnits.STACK_EQUIVALENT;

    private PlayerInventoryDefaults() { }
}
