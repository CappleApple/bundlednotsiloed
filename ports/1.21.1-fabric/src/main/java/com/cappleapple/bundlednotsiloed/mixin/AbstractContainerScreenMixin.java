package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.bundlednotsiloed.client.ContainerInventoryOverlay;
import com.cappleapple.bundlednotsiloed.client.InventoryItemTooltipContext;
import com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the logical browser draw aggregate counts while the underlying real Slots retain input. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected Slot hoveredSlot;

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void bns$beginInventoryItemTooltip(
            GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback
    ) {
        InventoryItemTooltipContext.begin(hoveredSlot);
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void bns$endInventoryItemTooltip(
            GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback
    ) {
        InventoryItemTooltipContext.end();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void bns$renderMappedPlayerSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        if (ContainerInventoryOverlay.replacesSlot(
                (AbstractContainerScreen<?>)(Object)this, slot)) callback.cancel();
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void bns$blockSlotHoverBehindExpandedMenu(
            Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> callback
    ) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        boolean covered = screen instanceof BundledInventoryScreen inventoryScreen
                ? inventoryScreen.expandedMenuCovers(mouseX, mouseY)
                : ContainerInventoryOverlay.expandedMenuCovers(screen, mouseX, mouseY);
        if (covered) callback.setReturnValue(false);
    }
}
