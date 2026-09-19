package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.nbt.CompoundTag;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;

/** Uploads the local player's client-owned tabs and inventory-view preferences. */
public record PlayerCustomizationPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PlayerCustomizationPayload> TYPE = new Type<>(BundledNotSiloed.id("player_customization"), PlayerCustomizationPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCustomizationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerCustomizationPayload decode(RegistryFriendlyByteBuf buffer) {
            CompoundTag tag = buffer.readNbt();
            if (tag == null) throw new IllegalArgumentException("Missing player customization");
            return new PlayerCustomizationPayload(tag);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PlayerCustomizationPayload payload) {
            buffer.writeNbt(payload.data());
        }
    };

    public PlayerCustomizationPayload {
        data = data.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
