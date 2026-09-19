package com.cappleapple.bundlednotsiloed.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.stats.RecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBook.class)
public interface RecipeBookAccessor {
    @Invoker("add") void bns$add(Identifier recipeId);
    @Invoker("remove") void bns$remove(Identifier recipeId);
}
