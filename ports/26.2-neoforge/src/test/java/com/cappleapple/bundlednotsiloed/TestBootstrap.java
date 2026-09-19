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
        // Vanilla's generated registry lookup uses placeholder tag sets. NeoForge's IDE-only
        // validator rejects those placeholders, so bind the fixture using production validation.
        boolean wasRunningInIde = net.minecraft.SharedConstants.IS_RUNNING_IN_IDE;
        net.minecraft.SharedConstants.IS_RUNNING_IN_IDE = false;
        try {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                    .forEach(DataComponentInitializers.PendingComponents::apply);
        } finally {
            net.minecraft.SharedConstants.IS_RUNNING_IN_IDE = wasRunningInIde;
        }
        initialized = true;
    }
}
