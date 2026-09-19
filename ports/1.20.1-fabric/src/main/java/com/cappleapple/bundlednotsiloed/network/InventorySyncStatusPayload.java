package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

/** Contains diagnostics only, never the item that failed to encode/decode. */
public record InventorySyncStatusPayload(int logicalSlot, String itemId) implements CustomPacketPayload {
    public static final Type<InventorySyncStatusPayload> TYPE = new Type<>(BundledNotSiloed.id("inventory_sync_status"), InventorySyncStatusPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, InventorySyncStatusPayload> STREAM_CODEC = new StreamCodec<>() {
        public InventorySyncStatusPayload decode(RegistryFriendlyByteBuf buffer) {
            return new InventorySyncStatusPayload(buffer.readVarInt(), buffer.readUtf(256));
        }
        public void encode(RegistryFriendlyByteBuf buffer, InventorySyncStatusPayload value) {
            buffer.writeVarInt(value.logicalSlot); buffer.writeUtf(value.itemId, 256);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
