package com.cappleapple.bundlednotsiloed.mixin;
import com.cappleapple.bundlednotsiloed.client.ClientEvents;
import com.cappleapple.bundlednotsiloed.platform.UiEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public abstract class KeyboardInputMixin {
 @Inject(method="charTyped",at=@At("HEAD"),cancellable=true)
 private void bns$type(long window,int codepoint,int modifiers,CallbackInfo ci) {
  Minecraft client=Minecraft.getInstance();
  if(window!=client.getWindow().getWindow() || client.screen==null) return;
  UiEvent event=new UiEvent(client.screen,null,0,0,codepoint,0);
  ClientEvents.characterContainerOverlay(event);
  if(event.isCanceled()) ci.cancel();
 }
}
