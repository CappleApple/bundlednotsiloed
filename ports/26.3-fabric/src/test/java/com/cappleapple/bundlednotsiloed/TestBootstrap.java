package com.cappleapple.bundlednotsiloed;

import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;

/** Completes the 26.3 registry component stage normally performed by server resource loading. */
public final class TestBootstrap {
    private static boolean initialized;
    private TestBootstrap() {}
    public static synchronized void bindComponents() {
        if (initialized) return;
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
                .forEach(DataComponentInitializers.PendingComponents::apply);
        // Unit fixtures do not load server datapacks; bind empty tag sets explicitly.
        ((net.minecraft.core.MappedRegistry<?>) BuiltInRegistries.ITEM).bindAllTagsToEmpty();
        ((net.minecraft.core.MappedRegistry<?>) BuiltInRegistries.BLOCK).bindAllTagsToEmpty();
        BuiltInRegistries.ITEM.freeze();
        BuiltInRegistries.BLOCK.freeze();
        initialized = true;
    }
}
