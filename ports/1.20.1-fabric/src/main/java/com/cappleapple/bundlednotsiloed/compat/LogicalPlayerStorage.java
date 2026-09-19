package com.cappleapple.bundlednotsiloed.compat;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.inventory.InventoryTransactions;
import com.cappleapple.bundlednotsiloed.platform.InventoryTransaction;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Stable logical slots, with vanilla armor/offhand indices and stowed entries appended after them. */
public final class LogicalPlayerStorage implements PlayerInventoryStorage {
    private final Player player;
    private final PlayerInventoryStorage equipment;
    private final DynamicCapacityInventory inventory;
    private final InventoryTransaction transaction;
    private final List<SingleSlotStorage<ItemVariant>> slots;
    public LogicalPlayerStorage(Player player, PlayerInventoryStorage equipment) {
        this.player = player;
        this.equipment = equipment;
        inventory = ModAttachments.get(player).inventory();
        transaction = new InventoryTransaction(ModAttachments.get(player));
        slots = Collections.unmodifiableList(new AbstractList<>() {
            @Override public int size() { return Math.max(41, inventory.compatibilitySlotCount() + 5); }
            @Override public SingleSlotStorage<ItemVariant> get(int index) {
                java.util.Objects.checkIndex(index, size());
                if (index >= 36 && index <= 40) return equipment.getSlot(index);
                return new LogicalSlot(index < 36 ? index : index - 5);
            }
        });
    }
    @Override public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<ItemVariant>> iterator() {
        return new java.util.ArrayList<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<ItemVariant>>(slots).iterator();
    }
    @Override public long extract(ItemVariant resource, long amount, TransactionContext tx) {
        if (amount < 0) throw new IllegalArgumentException("Negative transfer amount");
        long extracted = 0;
        for (var slot : List.copyOf(slots)) {
            if (extracted == amount) break;
            extracted += slot.extract(resource, amount - extracted, tx);
        }
        return extracted;
    }
    @Override public List<SingleSlotStorage<ItemVariant>> getSlots() { return slots; }
    @Override public long insert(ItemVariant resource, long amount, TransactionContext tx) { return offer(resource, amount, tx); }
    @Override public long offer(ItemVariant resource, long amount, TransactionContext tx) {
        if (amount < 0) throw new IllegalArgumentException("Negative transfer amount");
        if (resource.isBlank() || amount == 0) return 0;
        var stack = resource.toStack((int)Math.min(Integer.MAX_VALUE, amount));
        if (InventoryTransactions.insertIntoBackend(player, stack, true).acceptedAmount() == 0) return 0;
        return transaction.mutate(tx, () -> InventoryTransactions.insertIntoBackend(player, stack, false).acceptedAmount());
    }
    @Override public void drop(ItemVariant resource, long amount, boolean random, boolean ownership, TransactionContext tx) {
        equipment.drop(resource, amount, random, ownership, tx);
    }
    @Override public SingleSlotStorage<ItemVariant> getHandSlot(InteractionHand hand) {
        return getSlot(hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40);
    }
    private final class LogicalSlot implements SingleSlotStorage<ItemVariant> {
        private final int index;
        LogicalSlot(int index) { this.index = index; }
        @Override public ItemVariant getResource() { return ItemVariant.of(inventory.syntheticStack(index)); }
        @Override public boolean isResourceBlank() { return inventory.syntheticStack(index).isEmpty(); }
        @Override public long getAmount() { return inventory.syntheticStack(index).getCount(); }
        @Override public long getCapacity() { var stack = inventory.syntheticStack(index); return stack.isEmpty() ? 64 : stack.getMaxStackSize(); }
        @Override public long insert(ItemVariant resource, long amount, TransactionContext tx) {
            if (amount < 0) throw new IllegalArgumentException("Negative transfer amount");
            if (resource.isBlank() || amount == 0) return 0;
            var stack = resource.toStack((int)Math.min(Integer.MAX_VALUE, amount));
            if (InventoryTransactions.insertIntoSyntheticSlot(player, stack, index, true).acceptedAmount() == 0) return 0;
            return transaction.mutate(tx, () -> InventoryTransactions.insertIntoSyntheticSlot(player, stack, index, false).acceptedAmount());
        }
        @Override public long extract(ItemVariant resource, long amount, TransactionContext tx) {
            if (amount < 0) throw new IllegalArgumentException("Negative transfer amount");
            if (resource.isBlank() || amount == 0 || !resource.equals(getResource())) return 0;
            return transaction.mutate(tx, () -> inventory.extractSyntheticSlot(index, (int)Math.min(Integer.MAX_VALUE, amount), false).getCount());
        }
    }
}
