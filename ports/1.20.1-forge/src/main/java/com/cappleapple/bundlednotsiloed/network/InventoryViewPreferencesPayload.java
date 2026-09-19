package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Client intent for persistent, non-gameplay inventory view preferences. */
public record InventoryViewPreferencesPayload(SortMode sortMode, @Nullable ResourceLocation selectedCategory)
        implements CustomPacketPayload {
    public static final Type<InventoryViewPreferencesPayload> TYPE = new Type<>(BundledNotSiloed.id("inventory_view_preferences"), InventoryViewPreferencesPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryViewPreferencesPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public InventoryViewPreferencesPayload decode(RegistryFriendlyByteBuf buffer) {
            int sort = buffer.readUnsignedByte();
            if (sort >= SortMode.values().length) throw new IllegalArgumentException("Invalid inventory sort mode");
            ResourceLocation category = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            return new InventoryViewPreferencesPayload(SortMode.values()[sort], category);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, InventoryViewPreferencesPayload payload) {
            buffer.writeByte(payload.sortMode().ordinal());
            buffer.writeBoolean(payload.selectedCategory() != null);
            if (payload.selectedCategory() != null) buffer.writeResourceLocation(payload.selectedCategory());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
