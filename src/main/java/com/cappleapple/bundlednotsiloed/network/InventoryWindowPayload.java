package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.data.InventorySlotWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Bounded client intent describing the logical range or identities shown by 27 real slots. */
public record InventoryWindowPayload(Mode mode, int firstLogicalSlot, List<ItemStack> prototypes)
        implements CustomPacketPayload {
    public static final Type<InventoryWindowPayload> TYPE =
            new Type<>(BundledNotSiloed.id("inventory_window"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryWindowPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public InventoryWindowPayload decode(RegistryFriendlyByteBuf buffer) {
                    int encodedMode = buffer.readUnsignedByte();
                    if (encodedMode >= Mode.values().length) {
                        throw new IllegalArgumentException("Invalid inventory window mode");
                    }
                    Mode mode = Mode.values()[encodedMode];
                    if (mode == Mode.RANGE) {
                        return InventoryWindowPayload.range(buffer.readVarInt());
                    }
                    int count = buffer.readVarInt();
                    if (count < 0 || count > InventorySlotWindow.VISIBLE_SLOTS) {
                        throw new IllegalArgumentException("Invalid inventory window size");
                    }
                    ArrayList<ItemStack> prototypes = new ArrayList<>(count);
                    for (int index = 0; index < count; index++) {
                        prototypes.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
                    }
                    return InventoryWindowPayload.identities(prototypes);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, InventoryWindowPayload payload) {
                    buffer.writeByte(payload.mode.ordinal());
                    if (payload.mode == Mode.RANGE) {
                        buffer.writeVarInt(payload.firstLogicalSlot);
                        return;
                    }
                    buffer.writeVarInt(payload.prototypes.size());
                    payload.prototypes.forEach(stack ->
                            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack));
                }
            };

    public InventoryWindowPayload {
        if (mode == null) throw new IllegalArgumentException("Inventory window mode is required");
        if (mode == Mode.RANGE) {
            if (firstLogicalSlot < InventorySlotWindow.MAIN_START
                    || firstLogicalSlot > Integer.MAX_VALUE - InventorySlotWindow.VISIBLE_SLOTS
                    || (firstLogicalSlot - InventorySlotWindow.MAIN_START) % 9 != 0) {
                throw new IllegalArgumentException("Invalid inventory range start");
            }
            prototypes = List.of();
        } else {
            firstLogicalSlot = -1;
            if (prototypes == null || prototypes.size() > InventorySlotWindow.VISIBLE_SLOTS) {
                throw new IllegalArgumentException("Inventory window exceeds visible slot count");
            }
            prototypes = prototypes.stream()
                    .map(stack -> stack == null || stack.isEmpty()
                            ? ItemStack.EMPTY : stack.copyWithCount(1))
                    .toList();
        }
    }

    public static InventoryWindowPayload range(int firstLogicalSlot) {
        return new InventoryWindowPayload(Mode.RANGE, firstLogicalSlot, List.of());
    }

    public static InventoryWindowPayload identities(List<ItemStack> prototypes) {
        return new InventoryWindowPayload(Mode.IDENTITIES, -1, prototypes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Mode { RANGE, IDENTITIES }
}
