package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

public record StowMainGridPayload() implements CustomPacketPayload {
    public static final Type<StowMainGridPayload> TYPE = new Type<>(BundledNotSiloed.id("stow_main_grid"), StowMainGridPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, StowMainGridPayload> STREAM_CODEC = StreamCodec.unit(new StowMainGridPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
