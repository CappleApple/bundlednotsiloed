package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** The server baseline is independent of predicted clicks and vanilla slot echoes. */
public final class ClientInventorySync {
    private long serverRevision = -1;
    private boolean blocked;
    public boolean blocked() { return blocked; }
    public void block() { blocked = true; }
    private List<ItemStack> baseline = List.of();
    private long lastRecoveryRequest;
    private boolean recoveryRequested;
    public long serverRevision() { return serverRevision; }

    public void snapshot(DynamicCapacityInventory inventory, long revision, List<ItemStack> stacks) {
        baseline = stacks.stream().map(ItemStack::copy).toList();
        serverRevision = revision;
        recoveryRequested = false;
        blocked = false;
        install(inventory);
    }

    public boolean delta(DynamicCapacityInventory inventory, InventoryDeltaPayload payload) {
        if (serverRevision != payload.baseRevision() || payload.revision() < serverRevision
                || payload.resultingSize() < 0) return false;
        var seen = new java.util.HashSet<Integer>();
        for (var change : payload.changes()) {
            if (change.index() < 0 || change.index() >= payload.resultingSize()
                    || !seen.add(change.index())) return false;
        }
        // Validate all indices before mutation, including every appended slot.
        if ((long)payload.resultingSize() > (long)baseline.size() + payload.changes().size()) return false;
        for (int i = baseline.size(); i < payload.resultingSize(); i++) if (!seen.contains(i)) return false;
        ArrayList<ItemStack> updated = new ArrayList<>(payload.resultingSize());
        for (int i = 0; i < payload.resultingSize(); i++) {
            updated.add(i < baseline.size() ? baseline.get(i).copy() : ItemStack.EMPTY);
        }
        payload.changes().forEach(change -> updated.set(change.index(), change.stack().copy()));
        baseline = updated;
        serverRevision = payload.revision();
        recoveryRequested = false;
        blocked = false;
        install(inventory);
        return true;
    }

    public boolean requestRecovery(long nowNanos) {
        if (recoveryRequested && nowNanos - lastRecoveryRequest < java.util.concurrent.TimeUnit.SECONDS.toNanos(1)) return false;
        lastRecoveryRequest = nowNanos;
        recoveryRequested = true;
        return true;
    }

    private void install(DynamicCapacityInventory inventory) {
        // A correction must also invalidate caches after local predictions.
        inventory.loadNetworkSnapshot(baseline, inventory.revision() + 1);
    }
}
