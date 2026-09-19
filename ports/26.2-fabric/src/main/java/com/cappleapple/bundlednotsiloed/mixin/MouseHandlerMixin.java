package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.client.ContainerInventoryOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the integrated logical player grid first refusal before a container's native slot input. */
@Mixin(value = MouseHandler.class, priority = 2000)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void sns$browserMouseButton(long windowPointer, net.minecraft.client.input.MouseButtonInfo input, int action, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (windowPointer != minecraft.getWindow().handle() || minecraft.gui.overlay() != null) return;
        if (ContainerInventoryOverlay.mouseButton(input.button(), action)) callback.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void sns$browserMouseScroll(long windowPointer, double deltaX, double deltaY, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (windowPointer != minecraft.getWindow().handle() || minecraft.gui.overlay() != null) return;
        if (ContainerInventoryOverlay.mouseScrolled(deltaY)) callback.cancel();
    }
}
