package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Requests a stack for the cursor owned specifically by the client-only creative inventory menu. */
public record CreativeInventoryTakePayload(boolean takeHalf, ItemStack prototype) implements CustomPacketPayload {
    public static final Type<CreativeInventoryTakePayload> TYPE =
            new Type<>(BundledNotSiloed.id("creative_inventory_take"), CreativeInventoryTakePayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryTakePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CreativeInventoryTakePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new CreativeInventoryTakePayload(
                            buffer.readBoolean(), com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.decode(buffer).copyWithCount(1));
                }

                @Override public void encode(RegistryFriendlyByteBuf buffer, CreativeInventoryTakePayload payload) {
                    buffer.writeBoolean(payload.takeHalf);
                    com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.encode(buffer, payload.prototype.copyWithCount(1));
                }
            };

    public CreativeInventoryTakePayload { prototype = prototype.copyWithCount(1); }
    @Override public ItemStack prototype() { return prototype.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
