package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AutoRefillPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<AutoRefillPayload> TYPE = new Type<>(BundledNotSiloed.id("auto_refill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AutoRefillPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public AutoRefillPayload decode(RegistryFriendlyByteBuf buffer) {
            return new AutoRefillPayload(buffer.readBoolean());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, AutoRefillPayload payload) {
            buffer.writeBoolean(payload.enabled());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
