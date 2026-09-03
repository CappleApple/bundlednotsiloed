package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.client.InventorySearchInputCapture;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void bns$suppressUnrelatedKeybindsWhileSearching(
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (InventorySearchInputCapture.isTyping()) callback.setReturnValue(false);
    }
}
