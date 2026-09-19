package com.cappleapple.bundlednotsiloed.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
 @Mutable @Accessor("imageHeight") void bns$setImageHeight(int height);
 @Accessor("leftPos") int bns$left();
 @Accessor("topPos") int bns$top();
}
