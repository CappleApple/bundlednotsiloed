package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.hotbar.BindingType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HotbarBindPayload(int slot, BindingType bindingType, ResourceLocation target) implements CustomPacketPayload {
    public static final ResourceLocation EMPTY_TARGET = BundledNotSiloed.id("empty");
    public static final Type<HotbarBindPayload> TYPE = new Type<>(BundledNotSiloed.id("hotbar_bind"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HotbarBindPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public HotbarBindPayload decode(RegistryFriendlyByteBuf buffer) {
            int slot = buffer.readByte();
            int type = buffer.readUnsignedByte();
            if (type >= BindingType.values().length) throw new IllegalArgumentException("Invalid hotbar binding type");
            return new HotbarBindPayload(slot, BindingType.values()[type], ResourceLocation.STREAM_CODEC.decode(buffer));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, HotbarBindPayload payload) {
            buffer.writeByte(payload.slot());
            buffer.writeByte(payload.bindingType().ordinal());
            ResourceLocation.STREAM_CODEC.encode(buffer, payload.target());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
