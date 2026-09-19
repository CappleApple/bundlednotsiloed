package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Screen session prevents late closes from resetting a newer menu. */
public record BrowserStatePayload(boolean open, long session, int containerId) implements CustomPacketPayload {
    public static final Type<BrowserStatePayload> TYPE = new Type<>(BundledNotSiloed.id("browser_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BrowserStatePayload> STREAM_CODEC = new StreamCodec<>() {
        public BrowserStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new BrowserStatePayload(buffer.readBoolean(), buffer.readVarLong(), buffer.readVarInt());
        }
        public void encode(RegistryFriendlyByteBuf buffer, BrowserStatePayload value) {
            buffer.writeBoolean(value.open); buffer.writeVarLong(value.session); buffer.writeVarInt(value.containerId);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
