package com.cappleapple.bundlednotsiloed;

import java.lang.reflect.Method;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.network.NetworkEvent;

/** Supplies the listener-list initialization normally injected into Forge events by ModLauncher. */
public final class ForgeTestBootstrap {
    private static boolean initialized;
    private ForgeTestBootstrap() {}
    public static synchronized void initializeEventLists() {
        if (initialized) return;
        try {
            Method initialize = EventListenerHelper.class.getDeclaredMethod("getListenerListInternal", Class.class, boolean.class);
            initialize.setAccessible(true);
            initializeFamily(NetworkEvent.class, initialize);
            initialized = true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not initialize Forge event listener lists for the unit-test JVM", exception);
        }
    }
    private static void initializeFamily(Class<?> type, Method initialize) throws ReflectiveOperationException {
        if (Event.class.isAssignableFrom(type)) initializeType(type, initialize);
        for (Class<?> nested : type.getDeclaredClasses()) initializeFamily(nested, initialize);
    }
    private static void initializeType(Class<?> type, Method initialize) throws ReflectiveOperationException {
        if (type != Event.class) initializeType(type.getSuperclass(), initialize);
        initialize.invoke(null, type, true);
    }
}
