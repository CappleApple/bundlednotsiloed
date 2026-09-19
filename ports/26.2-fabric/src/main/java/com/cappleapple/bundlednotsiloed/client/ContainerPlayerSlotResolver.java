package com.cappleapple.bundlednotsiloed.client;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
public final class ContainerPlayerSlotResolver {
 private ContainerPlayerSlotResolver() {}
 public static int resolve(Slot slot, Inventory inventory) {
  int index=slot.getContainerSlot();
  return slot.container == inventory && index >= 0 && index < 36 ? index : -1;
 }
}
