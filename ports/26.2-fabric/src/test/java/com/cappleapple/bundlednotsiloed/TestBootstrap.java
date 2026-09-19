package com.cappleapple.bundlednotsiloed;

import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;

/** Completes the 26.2 registry component stage normally performed by server resource loading. */
public final class TestBootstrap {
    private static boolean initialized;
    private TestBootstrap() {}
    public static synchronized void bindComponents() {
        if (initialized) return;
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                .forEach(DataComponentInitializers.PendingComponents::apply);
        BuiltInRegistries.REGISTRY.forEach(registry -> {
            if (registry instanceof net.minecraft.core.MappedRegistry<?> mapped) mapped.bindAllTagsToEmpty();
        });
        BuiltInRegistries.ITEM.freeze();
        BuiltInRegistries.BLOCK.freeze();
        initialized = true;
    }
}
