package com.cappleapple.bundlednotsiloed.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client inventory-screen positioning access for vanilla's immutable slot coordinates. */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void bns$setX(int x);

    @Mutable
    @Accessor("y")
    void bns$setY(int y);
}
