package com.cappleapple.bundlednotsiloed.mixin;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Keeps the opt-in development smoke client hidden from window creation onward. */
@Mixin(Window.class)
public abstract class WindowSmokeMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void bns$hideSmokeWindow(CallbackInfo callback) {
        if (Boolean.getBoolean("bundlednotsiloed.clientSmoke")) org.lwjgl.sdl.SDLVideo.SDL_HideWindow(((com.mojang.blaze3d.platform.Window)(Object)this).handle());
    }
}
