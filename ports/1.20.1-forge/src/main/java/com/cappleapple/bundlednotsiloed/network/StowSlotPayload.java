package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

/** Slot -1 means the menu cursor; slots 0-35 mean a visible player compatibility slot. */
public record StowSlotPayload(int slot) implements CustomPacketPayload {
    public static final Type<StowSlotPayload> TYPE = new Type<>(BundledNotSiloed.id("stow_slot"), StowSlotPayload.class);
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
