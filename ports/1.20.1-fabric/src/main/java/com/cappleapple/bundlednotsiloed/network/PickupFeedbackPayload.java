package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.stacksnotslots.api.InsertionRejection;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

public record PickupFeedbackPayload(InsertionRejection reason, long usedCapacity, long capacity) implements CustomPacketPayload {
    public static final Type<PickupFeedbackPayload> TYPE = new Type<>(BundledNotSiloed.id("pickup_feedback"), PickupFeedbackPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, PickupFeedbackPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public PickupFeedbackPayload decode(RegistryFriendlyByteBuf buffer) {
            int reason = buffer.readUnsignedByte();
            if (reason >= InsertionRejection.values().length) throw new IllegalArgumentException("Invalid pickup feedback");
            return new PickupFeedbackPayload(InsertionRejection.values()[reason], buffer.readVarLong(), buffer.readVarLong());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, PickupFeedbackPayload payload) {
            buffer.writeByte(payload.reason().ordinal()); buffer.writeVarLong(payload.usedCapacity()); buffer.writeVarLong(payload.capacity());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
