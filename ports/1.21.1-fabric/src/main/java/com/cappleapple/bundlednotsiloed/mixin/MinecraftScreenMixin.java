package com.cappleapple.bundlednotsiloed.mixin;
import com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {
 @ModifyVariable(method="setScreen",at=@At("HEAD"),argsOnly=true)
 private Screen bns$replaceInventory(Screen screen) {
  var player=Minecraft.getInstance().player;
  return screen != null && screen.getClass()==InventoryScreen.class && player != null ? new BundledInventoryScreen(player) : screen;
 }
}
