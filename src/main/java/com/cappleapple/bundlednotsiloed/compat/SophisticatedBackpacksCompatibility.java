package com.cappleapple.bundlednotsiloed.compat;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Registers BNS logical storage through Sophisticated Backpacks' public inventory-provider hook. */
public final class SophisticatedBackpacksCompatibility {
    private static final String MOD_ID = "sophisticatedbackpacks";
    private static final String HANDLER_NAME = BundledNotSiloed.MOD_ID;
    private static boolean registered;

    private SophisticatedBackpacksCompatibility() {}

    public static void commonSetup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded(MOD_ID)) event.enqueueWork(SophisticatedBackpacksCompatibility::register);
    }

    private static synchronized void register() {
        if (registered) return;
        try {
            Class<?> providerType = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider");
            Class<?> slotCountType = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler$SlotCountGetter");
            Class<?> slotStackType = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler$SlotStackGetter");
            Object provider = providerType.getMethod("get").invoke(null);
            Object slotCount = Proxy.newProxyInstance(
                    slotCountType.getClassLoader(), new Class<?>[]{slotCountType},
                    (proxy, method, arguments) -> method.getDeclaringClass() == Object.class
                            ? objectMethod(proxy, method, arguments)
                            : LogicalPlayerInventoryView.slotCount((Player)arguments[0]));
            Object slotStack = Proxy.newProxyInstance(
                    slotStackType.getClassLoader(), new Class<?>[]{slotStackType},
                    (proxy, method, arguments) -> method.getDeclaringClass() == Object.class
                            ? objectMethod(proxy, method, arguments)
                            : LogicalPlayerInventoryView.stackInSlot(
                                    (Player)arguments[0], (Integer)arguments[2]));
            Function<Player, Set<String>> identifiers = ignored -> Set.of("");
            Method addHandler = providerType.getMethod("addPlayerInventoryHandler",
                    String.class, Function.class, slotCountType, slotStackType,
                    boolean.class, boolean.class, boolean.class, boolean.class);
            addHandler.invoke(provider, HANDLER_NAME, identifiers, slotCount, slotStack,
                    false, false, false, false);
            registered = true;
            BundledNotSiloed.LOGGER.info(
                    "Registered complete logical inventory with Sophisticated Backpacks");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            BundledNotSiloed.LOGGER.warn(
                    "Unable to register logical inventory with Sophisticated Backpacks", exception);
        }
    }

    private static Object objectMethod(Object proxy, Method method, Object[] arguments) {
        return switch (method.getName()) {
            case "toString" -> "Bundled Not Siloed logical inventory provider";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == arguments[0];
            default -> throw new UnsupportedOperationException(method.toString());
        };
    }
}
