package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record BrowserTransferPayload(ItemStack prototype, Mode mode) implements CustomPacketPayload {
    public static final Type<BrowserTransferPayload> TYPE = new Type<>(BundledNotSiloed.id("browser_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BrowserTransferPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public BrowserTransferPayload decode(RegistryFriendlyByteBuf buffer) {
            ItemStack prototype = ItemStack.STREAM_CODEC.decode(buffer).copyWithCount(1);
            int mode = buffer.readUnsignedByte();
            if (mode >= Mode.values().length) throw new IllegalArgumentException("Invalid browser transfer mode");
            return new BrowserTransferPayload(prototype, Mode.values()[mode]);
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, BrowserTransferPayload payload) {
            ItemStack.STREAM_CODEC.encode(buffer, payload.prototype().copyWithCount(1));
            buffer.writeByte(payload.mode().ordinal());
        }
    };

    public BrowserTransferPayload { prototype = prototype.copyWithCount(1); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Mode { SINGLE_STACK, MAXIMUM }
}
