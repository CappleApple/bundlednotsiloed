package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void bns$waitForWindow(int containerId, int slot, int button, ContainerInput type, Player player, CallbackInfo callback) {
        if (ClientInventoryWindows.pending()) callback.cancel();
    }
}
