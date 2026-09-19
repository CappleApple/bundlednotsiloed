package com.cappleapple.bundlednotsiloed.platform;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import com.cappleapple.stacksnotslots.api.inventory.InventorySnapshot;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import java.util.function.Supplier;
/** Keeps the BNS side of a world transfer in the same transaction as the external storage. */
public final class InventoryTransaction extends SnapshotParticipant<InventorySnapshot> {
    private final DynamicCapacityInventory inventory;
    private final PlayerInventoryData data;
    public InventoryTransaction(PlayerInventoryData data) {
        this.data = data;
        this.inventory = data.inventory();
    }
    public <T> T mutate(TransactionContext transaction, Supplier<T> change) {
        updateSnapshots(transaction);
        T result = inventory.withDeferredNotifications(change);
        data.syncVanillaCompatibilityView();
        return result;
    }
    @Override protected InventorySnapshot createSnapshot() { return inventory.snapshot(); }
    @Override protected void readSnapshot(InventorySnapshot snapshot) {
        inventory.applySnapshot(snapshot);
        data.syncVanillaCompatibilityView();
    }
    @Override protected void onFinalCommit() { inventory.publishDeferredNotifications(); }
}
