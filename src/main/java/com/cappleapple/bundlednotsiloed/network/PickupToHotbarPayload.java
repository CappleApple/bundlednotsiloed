package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PickupToHotbarPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<PickupToHotbarPayload> TYPE = new Type<>(BundledNotSiloed.id("pickup_to_hotbar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PickupToHotbarPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public PickupToHotbarPayload decode(RegistryFriendlyByteBuf buffer) {
            return new PickupToHotbarPayload(buffer.readBoolean());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, PickupToHotbarPayload payload) {
            buffer.writeBoolean(payload.enabled());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
