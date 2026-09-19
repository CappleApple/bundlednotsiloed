package com.cappleapple.bundlednotsiloed.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Validate with the receiving codec before putting a bad item on BNS's connection. */
public final class InventorySyncPreflight {
    private static final int MAX_ENCODED_ITEM_BYTES = 8 * 1024 * 1024;
    private InventorySyncPreflight() {}

    public static void validate(RegistryAccess registries, ItemStack stack) { encodedSize(registries, stack); }

    public static int encodedSize(RegistryAccess registries, ItemStack stack) {
        if (stack.isEmpty()) return 1;
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(256, MAX_ENCODED_ITEM_BYTES), registries);
        try {
            com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.encode(buffer, stack);
            com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.decode(buffer);
            if (buffer.isReadable()) throw new IllegalArgumentException("Item codec left unread bytes");
            return buffer.writerIndex();
        } finally {
            buffer.release();
        }
    }
}
