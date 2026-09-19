package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

public record AutoRefillPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<AutoRefillPayload> TYPE = new Type<>(BundledNotSiloed.id("auto_refill"), AutoRefillPayload.class);
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
