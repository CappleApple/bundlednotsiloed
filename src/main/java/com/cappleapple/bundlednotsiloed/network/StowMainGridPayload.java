package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StowMainGridPayload() implements CustomPacketPayload {
    public static final Type<StowMainGridPayload> TYPE = new Type<>(BundledNotSiloed.id("stow_main_grid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StowMainGridPayload> STREAM_CODEC = StreamCodec.unit(new StowMainGridPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
