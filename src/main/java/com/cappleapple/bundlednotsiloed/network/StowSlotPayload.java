package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Slot -1 means the menu cursor; slots 0-35 mean a visible player compatibility slot. */
public record StowSlotPayload(int slot) implements CustomPacketPayload {
    public static final Type<StowSlotPayload> TYPE = new Type<>(BundledNotSiloed.id("stow_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StowSlotPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public StowSlotPayload decode(RegistryFriendlyByteBuf buffer) {
            return new StowSlotPayload(buffer.readByte());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, StowSlotPayload payload) {
            buffer.writeByte(payload.slot());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
