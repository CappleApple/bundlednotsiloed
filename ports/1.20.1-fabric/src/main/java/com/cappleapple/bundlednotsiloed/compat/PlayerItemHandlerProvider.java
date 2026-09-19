package com.cappleapple.bundlednotsiloed.compat;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.stacksnotslots.api.compat.DynamicItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.world.entity.player.Player;
/** Used by the PlayerInventoryStorage mixin so Fabric automation sees every logical entry. */
public final class PlayerItemHandlerProvider {
 private PlayerItemHandlerProvider() {}
 public static DynamicItemStorage storage(Player player) { return new DynamicItemStorage(ModAttachments.get(player).inventory()); }
}
