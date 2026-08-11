package com.cappleapple.bundlednotsiloed.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBook.class)
public interface RecipeBookAccessor {
    @Invoker("add") void bns$add(ResourceLocation recipeId);
    @Invoker("remove") void bns$remove(ResourceLocation recipeId);
}
