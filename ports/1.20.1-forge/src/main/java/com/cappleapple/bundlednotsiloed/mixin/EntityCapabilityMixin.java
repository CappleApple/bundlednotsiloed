package com.cappleapple.bundlednotsiloed.mixin;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = net.minecraftforge.common.capabilities.CapabilityProvider.class, remap = false)
public abstract class EntityCapabilityMixin {
 @Inject(method="invalidateCaps",at=@At("TAIL"),remap=false)
 private void bns$invalidate(CallbackInfo ci) {
  if ((Object)this instanceof com.cappleapple.bundlednotsiloed.data.PlayerDataHolder holder) holder.bns$invalidateItemCapability();
 }
}
