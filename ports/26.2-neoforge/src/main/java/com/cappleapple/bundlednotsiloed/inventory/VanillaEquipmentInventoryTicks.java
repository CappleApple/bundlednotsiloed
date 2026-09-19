package com.cappleapple.bundlednotsiloed.inventory;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
/** Equipment now ticks independently in LivingEntity, outside Inventory.tick. */
public final class VanillaEquipmentInventoryTicks {
    private VanillaEquipmentInventoryTicks() {}
    public static void tick(Player player) {}
    static void tickCompartments(Level level, Entity entity, List<ItemStack> armor, List<ItemStack> offhand) {
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < armor.size(); i++) if (!armor.get(i).isEmpty()) armor.get(i).inventoryTick(level, entity, slots[i]);
        for (ItemStack stack : offhand) if (!stack.isEmpty()) stack.inventoryTick(level, entity, EquipmentSlot.OFFHAND);
    }
}
