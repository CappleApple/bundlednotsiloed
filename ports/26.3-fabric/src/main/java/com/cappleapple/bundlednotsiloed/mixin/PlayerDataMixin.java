package com.cappleapple.bundlednotsiloed.mixin;
import com.cappleapple.bundlednotsiloed.data.*;
import com.cappleapple.bundlednotsiloed.platform.PlayerDataAccess;
import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Player.class)
public abstract class PlayerDataMixin implements PlayerDataAccess {
 @Unique private PlayerInventoryData bns$data;
 @Override public PlayerInventoryData bns$inventoryData() {
  if (bns$data == null) bns$data=new PlayerInventoryData((Player)(Object)this);
  return bns$data;
 }
 @Inject(method="createAttributes",at=@At("RETURN"))
 private static void bns$attribute(CallbackInfoReturnable<AttributeSupplier.Builder> ci) { ci.getReturnValue().add(ModAttributes.INVENTORY_CAPACITY); }
 @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
 private void bns$save(net.minecraft.world.level.storage.ValueOutput output,CallbackInfo ci) {
  output.store("bundlednotsiloed:player_inventory",CompoundTag.CODEC,bns$inventoryData().serializeNBT(((Player)(Object)this).registryAccess()));
 }
 @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
 private void bns$load(net.minecraft.world.level.storage.ValueInput input,CallbackInfo ci) {
  input.read("bundlednotsiloed:player_inventory",CompoundTag.CODEC).ifPresent(tag -> bns$inventoryData().deserializeNBT(((Player)(Object)this).registryAccess(),tag));
 }
}
