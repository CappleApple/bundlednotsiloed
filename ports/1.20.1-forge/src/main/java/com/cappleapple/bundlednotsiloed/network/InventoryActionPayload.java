package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record InventoryActionPayload(Action action, ItemStack prototype) implements CustomPacketPayload {
    public static final Type<InventoryActionPayload> TYPE = new Type<>(BundledNotSiloed.id("inventory_action"), InventoryActionPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public InventoryActionPayload decode(RegistryFriendlyByteBuf buffer) {
            int action = buffer.readUnsignedByte();
            if (action >= Action.values().length) throw new IllegalArgumentException("Invalid inventory action");
            return new InventoryActionPayload(Action.values()[action], com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.decode(buffer).copyWithCount(1));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, InventoryActionPayload payload) {
            buffer.writeByte(payload.action().ordinal());
            com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.encode(buffer, payload.prototype().copyWithCount(1));
        }
    };

    public InventoryActionPayload { prototype = prototype.copyWithCount(1); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Action { TAKE_STACK, TAKE_HALF, DROP_ONE, DROP_STACK }
}
