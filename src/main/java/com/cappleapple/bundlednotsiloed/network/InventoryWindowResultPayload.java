package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent before vanilla slot updates so their indices have the same meaning on both peers. */
public record InventoryWindowResultPayload(InventoryWindowPayload window, long revision) implements CustomPacketPayload {
    public static final Type<InventoryWindowResultPayload> TYPE = new Type<>(BundledNotSiloed.id("inventory_window_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryWindowResultPayload> STREAM_CODEC =
            StreamCodec.composite(InventoryWindowPayload.STREAM_CODEC, InventoryWindowResultPayload::window,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_LONG, InventoryWindowResultPayload::revision,
                    InventoryWindowResultPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
