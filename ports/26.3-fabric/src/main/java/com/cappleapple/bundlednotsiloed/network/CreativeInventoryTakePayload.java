package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Requests a stack for the cursor owned specifically by the client-only creative inventory menu. */
public record CreativeInventoryTakePayload(boolean takeHalf, ItemStack prototype) implements CustomPacketPayload {
    public static final Type<CreativeInventoryTakePayload> TYPE =
            new Type<>(BundledNotSiloed.id("creative_inventory_take"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryTakePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CreativeInventoryTakePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new CreativeInventoryTakePayload(
                            buffer.readBoolean(), ItemStack.STREAM_CODEC.decode(buffer).copyWithCount(1));
                }

                @Override public void encode(RegistryFriendlyByteBuf buffer, CreativeInventoryTakePayload payload) {
                    buffer.writeBoolean(payload.takeHalf);
                    ItemStack.STREAM_CODEC.encode(buffer, payload.prototype.copyWithCount(1));
                }
            };

    public CreativeInventoryTakePayload { prototype = prototype.copyWithCount(1); }
    @Override public ItemStack prototype() { return prototype.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
