package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Replaces the cursor owned by the client-only creative inventory menu after a server transaction. */
public record CreativeInventoryCursorPayload(ItemStack carried) implements CustomPacketPayload {
    public static final Type<CreativeInventoryCursorPayload> TYPE =
            new Type<>(BundledNotSiloed.id("creative_inventory_cursor"), CreativeInventoryCursorPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryCursorPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CreativeInventoryCursorPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new CreativeInventoryCursorPayload(com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.decode(buffer));
                }

                @Override public void encode(RegistryFriendlyByteBuf buffer, CreativeInventoryCursorPayload payload) {
                    com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.encode(buffer, payload.carried);
                }
            };

    public CreativeInventoryCursorPayload { carried = carried.copy(); }
    @Override public ItemStack carried() { return carried.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
