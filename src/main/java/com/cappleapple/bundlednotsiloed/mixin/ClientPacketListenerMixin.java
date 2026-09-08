package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Redirect(method = "handleContainerSetSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void bns$ignoreUnmappedInventoryEcho(Inventory inventory, int slot, ItemStack stack) {
        if (slot >= 0 && slot < 36 && inventory.player.getData(ModAttachments.PLAYER_DATA).migratedVanillaInventory()) return;
        inventory.setItem(slot, stack);
    }
}
