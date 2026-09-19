package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.data.InventorySlotWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Fixed-size references; never upload item components merely to scroll or search. */
public record InventoryWindowPayload(long session, long request, int containerId, boolean identities,
                                     List<Integer> slots) implements CustomPacketPayload {
    public static final Type<InventoryWindowPayload> TYPE = new Type<>(BundledNotSiloed.id("inventory_window"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryWindowPayload> STREAM_CODEC = new StreamCodec<>() {
        public InventoryWindowPayload decode(RegistryFriendlyByteBuf buffer) {
            long session = buffer.readVarLong();
            long request = buffer.readVarLong();
            int menu = buffer.readVarInt();
            boolean identities = buffer.readBoolean();
            List<Integer> slots = new ArrayList<>(InventorySlotWindow.VISIBLE_SLOTS);
            for (int i = 0; i < InventorySlotWindow.VISIBLE_SLOTS; i++) slots.add(buffer.readVarInt());
            return new InventoryWindowPayload(session, request, menu, identities, slots);
        }
        public void encode(RegistryFriendlyByteBuf buffer, InventoryWindowPayload value) {
            buffer.writeVarLong(value.session);
            buffer.writeVarLong(value.request);
            buffer.writeVarInt(value.containerId);
            buffer.writeBoolean(value.identities);
            value.slots.forEach(buffer::writeVarInt);
        }
    };
    public InventoryWindowPayload {
        if (session < 0 || request < 0) throw new IllegalArgumentException("Invalid window sequence");
        new InventorySlotWindow().showSlots(slots, identities);
        slots = List.copyOf(slots);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
