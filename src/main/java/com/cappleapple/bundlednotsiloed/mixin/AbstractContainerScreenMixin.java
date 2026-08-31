package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.client.ContainerInventoryOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the logical browser draw aggregate counts while the underlying real Slots retain input. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void bns$renderMappedPlayerSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        if (ContainerInventoryOverlay.replacesSlot(
                (AbstractContainerScreen<?>)(Object)this, slot)) callback.cancel();
    }
}
