package com.cappleapple.bundlednotsiloed.inventory;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Retains vanilla inventory-tick behavior for the real armor and offhand compartments. */
public final class VanillaEquipmentInventoryTicks {
    private VanillaEquipmentInventoryTicks() {}

    public static void tick(Player player) {
        Inventory inventory = player.getInventory();
        tickCompartments(player.level(), player, inventory.armor, inventory.offhand);
    }

    static void tickCompartments(
            Level level, Entity entity, List<ItemStack> armor, List<ItemStack> offhand
    ) {
        tickCompartment(level, entity, armor, Inventory.INVENTORY_SIZE);
        tickCompartment(level, entity, offhand, Inventory.SLOT_OFFHAND);
    }

    private static void tickCompartment(
            Level level, Entity entity, List<ItemStack> stacks, int firstSlot
    ) {
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (!stack.isEmpty()) stack.inventoryTick(level, entity, firstSlot + index, false);
        }
    }
}
