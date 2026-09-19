package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Carries the client-owned creative menu cursor to an authorized server-side stow transaction. */
public record CreativeInventoryStowPayload(ItemStack carried) implements CustomPacketPayload {
    public static final Type<CreativeInventoryStowPayload> TYPE =
            new Type<>(BundledNotSiloed.id("creative_inventory_stow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryStowPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CreativeInventoryStowPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new CreativeInventoryStowPayload(ItemStack.STREAM_CODEC.decode(buffer));
                }

                @Override public void encode(RegistryFriendlyByteBuf buffer, CreativeInventoryStowPayload payload) {
                    ItemStack.STREAM_CODEC.encode(buffer, payload.carried);
                }
            };

    public CreativeInventoryStowPayload { carried = carried.copy(); }
    @Override public ItemStack carried() { return carried.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
