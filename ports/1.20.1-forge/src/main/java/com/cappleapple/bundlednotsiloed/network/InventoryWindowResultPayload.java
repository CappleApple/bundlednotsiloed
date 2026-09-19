package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

/** Sent before vanilla slot updates so their indices have the same meaning on both peers. */
public record InventoryWindowResultPayload(InventoryWindowPayload window, long revision) implements CustomPacketPayload {
    public static final Type<InventoryWindowResultPayload> TYPE = new Type<>(BundledNotSiloed.id("inventory_window_result"), InventoryWindowResultPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryWindowResultPayload> STREAM_CODEC =
            StreamCodec.composite(InventoryWindowPayload.STREAM_CODEC, InventoryWindowResultPayload::window,
                    com.cappleapple.bundlednotsiloed.platform.ByteBufCodecs.VAR_LONG, InventoryWindowResultPayload::revision,
                    InventoryWindowResultPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
