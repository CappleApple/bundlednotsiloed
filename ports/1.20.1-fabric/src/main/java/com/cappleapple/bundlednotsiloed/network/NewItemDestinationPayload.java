package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.inventory.NewItemDestination;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

public record NewItemDestinationPayload(NewItemDestination destination) implements CustomPacketPayload {
    public static final Type<NewItemDestinationPayload> TYPE = new Type<>(BundledNotSiloed.id("new_item_destination"), NewItemDestinationPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, NewItemDestinationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public NewItemDestinationPayload decode(RegistryFriendlyByteBuf buffer) {
            return new NewItemDestinationPayload(NewItemDestination.values()[buffer.readUnsignedByte()]);
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, NewItemDestinationPayload payload) {
            buffer.writeByte(payload.destination().ordinal());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
