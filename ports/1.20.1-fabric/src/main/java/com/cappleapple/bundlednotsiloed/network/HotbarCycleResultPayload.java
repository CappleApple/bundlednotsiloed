package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record HotbarCycleResultPayload(String bindingName, ItemStack selected) implements CustomPacketPayload {
    public static final Type<HotbarCycleResultPayload> TYPE = new Type<>(BundledNotSiloed.id("hotbar_cycle_result"), HotbarCycleResultPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, HotbarCycleResultPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public HotbarCycleResultPayload decode(RegistryFriendlyByteBuf buffer) {
            return new HotbarCycleResultPayload(buffer.readUtf(64), com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.decode(buffer));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, HotbarCycleResultPayload payload) {
            buffer.writeUtf(payload.bindingName(), 64);
            com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.encode(buffer, payload.selected());
        }
    };
    public HotbarCycleResultPayload { selected = selected.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
