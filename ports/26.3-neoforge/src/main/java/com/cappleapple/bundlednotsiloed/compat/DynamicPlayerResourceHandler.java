package com.cappleapple.bundlednotsiloed.compat;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.inventory.InventoryTransactions;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import com.cappleapple.stacksnotslots.api.inventory.InventorySnapshot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
/** Transactional player capability; category limits apply before each insertion. */
public final class DynamicPlayerResourceHandler extends SnapshotJournal<InventorySnapshot> implements ResourceHandler<ItemResource> {
    private final Player player;
    private final DynamicCapacityInventory inventory;
    public DynamicPlayerResourceHandler(Player player) { this.player = player; inventory = player.getData(ModAttachments.PLAYER_DATA).inventory(); }
    public int size() { return inventory.compatibilitySlotCount(); }
    private boolean valid(int index) { return index >= 0 && index < size(); }
    public ItemResource getResource(int index) { return valid(index) ? ItemResource.of(inventory.syntheticStack(index)) : ItemResource.EMPTY; }
    public long getAmountAsLong(int index) { return valid(index) ? inventory.syntheticStack(index).getCount() : 0; }
    public long getCapacityAsLong(int index, ItemResource resource) { return valid(index) ? resource.isEmpty() ? 64 : resource.getMaxStackSize() : 0; }
    public boolean isValid(int index, ItemResource resource) { return valid(index) && !resource.isEmpty(); }
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!valid(index) || amount == 0) return 0;
        var stack = resource.toStack(amount);
        int accepted = InventoryTransactions.insertIntoSyntheticSlot(player, stack, index, true).acceptedAmount();
        if (accepted == 0) return 0;
        updateSnapshots(transaction);
        int inserted = inventory.withDeferredNotifications(() -> InventoryTransactions.insertIntoSyntheticSlot(player, stack, index, false).acceptedAmount());
        player.getData(ModAttachments.PLAYER_DATA).syncVanillaCompatibilityView();
        return inserted;
    }
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!valid(index) || amount == 0 || !resource.matches(inventory.syntheticStack(index))) return 0;
        int extracted = inventory.extractSyntheticSlot(index, amount, true).getCount();
        if (extracted == 0) return 0;
        updateSnapshots(transaction);
        int extractedAmount = inventory.withDeferredNotifications(() -> inventory.extractSyntheticSlot(index, amount, false).getCount());
        player.getData(ModAttachments.PLAYER_DATA).syncVanillaCompatibilityView();
        return extractedAmount;
    }
    /** Bulk collection keeps incoming items in the backend and observes manual-transfer limits. */
    public int insertBackend(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        var stack = resource.toStack(amount);
        int accepted = InventoryTransactions.insertIntoBackend(player, stack, true).acceptedAmount();
        if (accepted == 0) return 0;
        updateSnapshots(transaction);
        int inserted = inventory.withDeferredNotifications(() -> InventoryTransactions.insertIntoBackend(player, stack, false).acceptedAmount());
        player.getData(ModAttachments.PLAYER_DATA).syncVanillaCompatibilityView();
        return inserted;
    }
    /** Bulk dumping excludes the hotbar and participates in the container's transaction. */
    public int extractDumpable(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        var stack = resource.toStack();
        int available = inventory.extractAtOrAfter(stack, amount, 9, true).extractedAmount();
        if (available == 0) return 0;
        updateSnapshots(transaction);
        int extracted = inventory.withDeferredNotifications(() -> inventory.extractAtOrAfter(stack, amount, 9, false).extractedAmount());
        player.getData(ModAttachments.PLAYER_DATA).syncVanillaCompatibilityView();
        return extracted;
    }
    protected InventorySnapshot createSnapshot() { return inventory.snapshot(); }
    protected void revertToSnapshot(InventorySnapshot snapshot) {
        inventory.applySnapshot(snapshot);
        // Rollback replaces stack instances; discard vanilla references mutated by tentative transfers.
        player.getData(ModAttachments.PLAYER_DATA).syncVanillaCompatibilityView();
    }
    protected void onRootCommit(InventorySnapshot originalState) { inventory.publishDeferredNotifications(); }
}
