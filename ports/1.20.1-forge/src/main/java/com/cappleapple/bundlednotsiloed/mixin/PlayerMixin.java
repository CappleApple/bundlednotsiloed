package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends vanilla player item lookups and direct writes across the authoritative dynamic inventory. */
@Mixin(Player.class)
public abstract class PlayerMixin implements com.cappleapple.bundlednotsiloed.data.PlayerDataHolder {
    @org.spongepowered.asm.mixin.Unique
    private com.cappleapple.bundlednotsiloed.data.PlayerInventoryData bns$playerData;
    @Override
    public com.cappleapple.bundlednotsiloed.data.PlayerInventoryData bns$inventoryData() {
        if (bns$playerData == null) bns$playerData = new com.cappleapple.bundlednotsiloed.data.PlayerInventoryData((Player)(Object)this);
        return bns$playerData;
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bns$saveInventory(net.minecraft.nbt.CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        tag.put("bundlednotsiloed:player_inventory", bns$inventoryData().serializeNBT(((Player)(Object)this).level().registryAccess()));
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bns$loadInventory(net.minecraft.nbt.CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (tag.contains("bundlednotsiloed:player_inventory", 10)) bns$inventoryData().deserializeNBT(((Player)(Object)this).level().registryAccess(), tag.getCompound("bundlednotsiloed:player_inventory"));
    }

    @org.spongepowered.asm.mixin.Unique
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> bns$itemHandler;
    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void bns$dynamicItemCapability(net.minecraftforge.common.capabilities.Capability<T> capability,
            net.minecraft.core.Direction side, CallbackInfoReturnable<net.minecraftforge.common.util.LazyOptional<T>> callback) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            if (bns$itemHandler == null) bns$itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new com.cappleapple.bundlednotsiloed.compat.DynamicItemHandler((Player)(Object)this));
            callback.setReturnValue(bns$itemHandler.cast());
        }
    }
    @Override
    public void bns$invalidateItemCapability() {
        if (bns$itemHandler != null) { bns$itemHandler.invalidate(); bns$itemHandler = null; }
    }
    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void sns$findBackendProjectile(ItemStack weapon, CallbackInfoReturnable<ItemStack> callback) {
        if (!callback.getReturnValue().isEmpty() || !(weapon.getItem() instanceof ProjectileWeaponItem projectileWeapon)) return;
        Player player = (Player)(Object)this;
        var data = com.cappleapple.bundlednotsiloed.data.ModAttachments.get(player);
        if (!data.migratedVanillaInventory()) return;
        ItemStack projectile = data.inventory().findLiveStackReference(
                Inventory.INVENTORY_SIZE, projectileWeapon.getAllSupportedProjectiles());
        if (!projectile.isEmpty()) callback.setReturnValue(projectile);
    }

    @Redirect(
            method = "setItemSlot",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;")
    )
    private Object sns$setDynamicMainHand(NonNullList<ItemStack> list, int index, Object replacement) {
        Player player = (Player)(Object)this;
        ItemStack stack = (ItemStack)replacement;
        var data = com.cappleapple.bundlednotsiloed.data.ModAttachments.get(player);
        if (list == player.getInventory().items && data.migratedVanillaInventory()) {
            ItemStack previous = data.inventory().syntheticStack(index);
            data.inventory().replaceSyntheticSlotFromItemUse(index, stack);
            return previous;
        }
        return list.set(index, stack);
    }
}
