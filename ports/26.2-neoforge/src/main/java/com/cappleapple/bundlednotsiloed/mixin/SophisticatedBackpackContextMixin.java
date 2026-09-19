package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.compat.LogicalBackpackContext;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Canonicalize explicit/hovered vanilla opens before the context is sent or the old screen closes. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext$Item", remap = false)
public abstract class SophisticatedBackpackContextMixin implements LogicalBackpackContext {
    @Shadow @Final @Mutable protected String handlerName;
    @Shadow @Final @Mutable protected int backpackSlotIndex;

    @Inject(method = "getBackpackWrapper", at = @At("HEAD"))
    private void bns$useLogicalReference(Player player, CallbackInfoReturnable<Object> callback) {
        if (!com.cappleapple.bundlednotsiloed.compat.SophisticatedBackpacksCompatibility.registered()) return;
        if (!handlerName.equals("main") || backpackSlotIndex < 0 || backpackSlotIndex >= 36) return;
        var data = player.getData(ModAttachments.PLAYER_DATA);
        if (!data.migratedVanillaInventory()) return;
        backpackSlotIndex = data.inventoryWindow().logicalIndex(backpackSlotIndex);
        handlerName = BundledNotSiloed.MOD_ID;
    }

    @Override public int bns$logicalBackpackSlot() {
        return handlerName.equals(BundledNotSiloed.MOD_ID) ? backpackSlotIndex : -1;
    }
}
