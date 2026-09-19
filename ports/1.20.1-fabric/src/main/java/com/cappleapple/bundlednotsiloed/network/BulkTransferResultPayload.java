package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.inventory.ContainerTransfers;
import java.util.ArrayList;
import java.util.List;
import com.cappleapple.bundlednotsiloed.platform.RegistryFriendlyByteBuf;
import com.cappleapple.bundlednotsiloed.platform.StreamCodec;
import com.cappleapple.bundlednotsiloed.platform.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record BulkTransferResultPayload(BulkTransferPayload.Direction direction, List<ContainerTransfers.TransferredStack> stacks)
        implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 256;
    public static final Type<BulkTransferResultPayload> TYPE = new Type<>(BundledNotSiloed.id("bulk_transfer_result"), BulkTransferResultPayload.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, BulkTransferResultPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public BulkTransferResultPayload decode(RegistryFriendlyByteBuf buffer) {
            int direction = buffer.readUnsignedByte();
            int count = buffer.readVarInt();
            if (direction >= BulkTransferPayload.Direction.values().length || count < 0 || count > MAX_ENTRIES) {
                throw new IllegalArgumentException("Invalid bulk transfer result");
            }
            ArrayList<ContainerTransfers.TransferredStack> stacks = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                ItemStack prototype = com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.decode(buffer).copyWithCount(1);
                long quantity = buffer.readVarLong();
                if (quantity <= 0) throw new IllegalArgumentException("Invalid transferred quantity");
                stacks.add(new ContainerTransfers.TransferredStack(prototype, quantity));
            }
            return new BulkTransferResultPayload(BulkTransferPayload.Direction.values()[direction], stacks);
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, BulkTransferResultPayload payload) {
            buffer.writeByte(payload.direction().ordinal());
            int count = Math.min(MAX_ENTRIES, payload.stacks().size());
            buffer.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                ContainerTransfers.TransferredStack stack = payload.stacks().get(index);
                com.cappleapple.bundlednotsiloed.platform.LegacyItems.STREAM_CODEC.encode(buffer, stack.prototype());
                buffer.writeVarLong(stack.quantity());
            }
        }
    };

    public BulkTransferResultPayload { stacks = List.copyOf(stacks); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
