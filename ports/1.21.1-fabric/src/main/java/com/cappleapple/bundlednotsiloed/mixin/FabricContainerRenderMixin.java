package com.cappleapple.bundlednotsiloed.mixin;
import com.cappleapple.bundlednotsiloed.client.ClientEvents;
import com.cappleapple.bundlednotsiloed.platform.UiEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractContainerScreen.class)
public abstract class FabricContainerRenderMixin {
 @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",shift=At.Shift.AFTER))
 private void bns$foreground(GuiGraphics graphics,int mouseX,int mouseY,float delta,CallbackInfo ci) {
  UiEvent event=new UiEvent((AbstractContainerScreen<?>)(Object)this,graphics,mouseX,mouseY,0,0);
  ClientEvents.renderContainerOverlay(event); ClientEvents.renderFullInventoryBarriers(event);
 }
}
