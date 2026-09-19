package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.nbt.CompoundTag;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

public record PlayerMetadataPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PlayerMetadataPayload> TYPE = new Type<>(BundledNotSiloed.id("player_metadata"), PlayerMetadataPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerMetadataPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public PlayerMetadataPayload decode(RegistryFriendlyByteBuf buffer) {
            CompoundTag tag = buffer.readNbt();
            if (tag == null) throw new IllegalArgumentException("Missing player metadata");
            return new PlayerMetadataPayload(tag);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, PlayerMetadataPayload payload) { buffer.writeNbt(payload.data()); }
    };

    public PlayerMetadataPayload { data = data.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
