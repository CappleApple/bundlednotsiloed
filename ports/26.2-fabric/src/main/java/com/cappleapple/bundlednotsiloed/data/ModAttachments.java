package com.cappleapple.bundlednotsiloed.data;
import net.minecraft.world.entity.player.Player;
import com.cappleapple.bundlednotsiloed.platform.PlayerDataAccess;
public final class ModAttachments {
 private ModAttachments() {}
 public static PlayerInventoryData get(Player player) { return ((PlayerDataAccess)player).bns$inventoryData(); }
 public static void markDirty(Player player) { com.cappleapple.bundlednotsiloed.network.ModNetwork.queueInventorySync(player); }
}
