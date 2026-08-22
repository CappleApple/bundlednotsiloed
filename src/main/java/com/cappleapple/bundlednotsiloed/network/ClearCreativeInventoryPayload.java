package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Requests the hidden-storage half of creative inventory tab's clear-all action. */
public record ClearCreativeInventoryPayload() implements CustomPacketPayload {
    public static final Type<ClearCreativeInventoryPayload> TYPE =
            new Type<>(BundledNotSiloed.id("clear_creative_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearCreativeInventoryPayload> STREAM_CODEC =
            StreamCodec.unit(new ClearCreativeInventoryPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
