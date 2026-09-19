package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

public record HotbarCyclePayload(int slot, int direction) implements CustomPacketPayload {
    public static final Type<HotbarCyclePayload> TYPE = new Type<>(BundledNotSiloed.id("hotbar_cycle"), HotbarCyclePayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, HotbarCyclePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public HotbarCyclePayload decode(RegistryFriendlyByteBuf buffer) { return new HotbarCyclePayload(buffer.readByte(), buffer.readByte()); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, HotbarCyclePayload payload) { buffer.writeByte(payload.slot()); buffer.writeByte(payload.direction()); }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
