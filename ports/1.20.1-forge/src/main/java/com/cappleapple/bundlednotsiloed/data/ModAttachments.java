package com.cappleapple.bundlednotsiloed.data;
import net.minecraft.world.entity.player.Player;
public final class ModAttachments {
 private ModAttachments() {}
 public static PlayerInventoryData get(Player player) { return ((PlayerDataHolder)player).bns$inventoryData(); }
 public static void markDirty(Player player) { com.cappleapple.bundlednotsiloed.network.ModNetwork.queueInventorySync(player); }
}
