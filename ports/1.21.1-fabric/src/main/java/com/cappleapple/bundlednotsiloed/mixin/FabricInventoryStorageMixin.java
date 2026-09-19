package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.compat.LogicalPlayerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InventoryStorage.class, remap = false)
public interface FabricInventoryStorageMixin {
    @Inject(method = "of", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bns$logicalStorage(Container container, Direction direction, CallbackInfoReturnable<InventoryStorage> ci) {
        if (!(container instanceof Inventory inventory) || !ModAttachments.get(inventory.player).migratedVanillaInventory()) return;
        if (!(ci.getReturnValue() instanceof PlayerInventoryStorage original) || original instanceof LogicalPlayerStorage) return;
        ci.setReturnValue(ModAttachments.get(inventory.player).fabricStorage(original));
    }
}
