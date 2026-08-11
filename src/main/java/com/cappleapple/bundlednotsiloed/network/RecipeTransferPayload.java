package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecipeTransferPayload(ResourceLocation recipeId, boolean placeAll) implements CustomPacketPayload {
    public static final Type<RecipeTransferPayload> TYPE = new Type<>(BundledNotSiloed.id("recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeTransferPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public RecipeTransferPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RecipeTransferPayload(buffer.readResourceLocation(), buffer.readBoolean());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, RecipeTransferPayload payload) {
            buffer.writeResourceLocation(payload.recipeId());
            buffer.writeBoolean(payload.placeAll());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
