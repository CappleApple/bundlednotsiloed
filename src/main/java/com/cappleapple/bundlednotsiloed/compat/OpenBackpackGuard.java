package com.cappleapple.bundlednotsiloed.compat;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Protect the logical root backpack, including when viewing one of its nested backpacks. */
public final class OpenBackpackGuard {
    private static final ClassValue<Optional<Method>> CONTEXT_METHODS = new ClassValue<>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            if (!type.getName().startsWith("net.p3pp3rf1y.sophisticatedbackpacks.")) return Optional.empty();
            try { return Optional.of(type.getMethod("getBackpackContext")); }
            catch (NoSuchMethodException ignored) { return Optional.empty(); }
        }
    };
    private static boolean warned;
    private OpenBackpackGuard() {}

    public static int logicalSlot(Player player) {
        if (player == null || player.containerMenu == null) return -1;
        var method = CONTEXT_METHODS.get(player.containerMenu.getClass());
        if (method.isEmpty()) return -1;
        try {
            Object context = method.get().invoke(player.containerMenu);
            if (!(context instanceof LogicalBackpackContext logical)) return -1;
            // AnotherPlayer contexts inherit Item, but must not lock the viewer's inventory.
            Optional<?> owner = (Optional<?>)context.getClass().getMethod("getOwnerPlayer", Player.class).invoke(context, player);
            return owner.orElse(null) == player ? logical.bns$logicalBackpackSlot() : -1;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warned) {
                warned = true;
                BundledNotSiloed.LOGGER.warn("Unable to resolve the open backpack's logical slot", exception);
            }
            return -1;
        }
    }

    public static boolean protects(Player player, ItemStack stack) {
        int slot = logicalSlot(player);
        return slot >= 0 && !stack.isEmpty() && ItemStack.isSameItemSameComponents(
                player.getData(ModAttachments.PLAYER_DATA).inventory().vanillaStackReference(slot), stack);
    }

    public static boolean blocksArrangement(Player player) {
        if (logicalSlot(player) < 0) return false;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.bundlednotsiloed.close_backpack_to_arrange"), true);
        return true;
    }
}
