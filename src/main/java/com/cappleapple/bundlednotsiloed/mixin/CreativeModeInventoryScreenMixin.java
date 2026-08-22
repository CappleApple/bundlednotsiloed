package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.network.ClearCreativeInventoryPayload;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends the creative inventory tab's shift-click trash action to hidden logical storage. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow @Nullable private Slot destroyItemSlot;

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void sns$clearHiddenInventory(
            @Nullable Slot slot,
            int slotId,
            int mouseButton,
            ClickType clickType,
            CallbackInfo callback
    ) {
        if (slot == destroyItemSlot && clickType == ClickType.QUICK_MOVE) {
            PacketDistributor.sendToServer(new ClearCreativeInventoryPayload());
        }
    }
}
