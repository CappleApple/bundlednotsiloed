package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

public record RequestFullSyncPayload() implements CustomPacketPayload {
    public static final Type<RequestFullSyncPayload> TYPE = new Type<>(BundledNotSiloed.id("request_full_sync"), RequestFullSyncPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestFullSyncPayload> STREAM_CODEC = StreamCodec.unit(new RequestFullSyncPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
