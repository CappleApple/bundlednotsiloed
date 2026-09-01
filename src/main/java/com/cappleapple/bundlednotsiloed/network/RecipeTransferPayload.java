package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.compat.RecipeTransferDestination;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecipeTransferPayload(ResourceLocation recipeId, boolean placeAll,
                                    RecipeTransferDestination destination) implements CustomPacketPayload {
    public static final Type<RecipeTransferPayload> TYPE = new Type<>(BundledNotSiloed.id("recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeTransferPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public RecipeTransferPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RecipeTransferPayload(
                    buffer.readResourceLocation(), buffer.readBoolean(), buffer.readEnum(RecipeTransferDestination.class));
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, RecipeTransferPayload payload) {
            buffer.writeResourceLocation(payload.recipeId());
            buffer.writeBoolean(payload.placeAll());
            buffer.writeEnum(payload.destination());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
