package com.cappleapple.bundlednotsiloed.network;

import java.util.List;

/** One screen's request/acknowledgement state, independent of rendering and local prediction. */
public final class InventoryWindowSession {
    private final long session;
    private final int containerId;
    private long sequence;
    private InventoryWindowPayload pending;
    private InventoryWindowPayload desired;
    private InventoryWindowPayload acknowledged;
    private InventoryWindowResultPayload waitingForInventory;

    public InventoryWindowSession(long session, int containerId) {
        this.session = session;
        this.containerId = containerId;
    }
    public long session() { return session; }
    public int containerId() { return containerId; }
    public boolean pending() { return pending != null || acknowledged == null; }

    public void request(List<Integer> slots, boolean identities) {
        desired = new InventoryWindowPayload(session, ++sequence, containerId, identities, slots);
    }
    public InventoryWindowPayload nextRequest() {
        if (pending != null || desired == null || sameMapping(desired, acknowledged)) return null;
        pending = desired;
        return pending;
    }
    public InventoryWindowPayload acknowledge(InventoryWindowResultPayload payload, long serverRevision) {
        InventoryWindowPayload result = payload.window();
        if (pending == null || result.session() != session || result.containerId() != containerId
                || result.request() != pending.request()) return null;
        waitingForInventory = payload;
        if (serverRevision < payload.revision()) return null;
        waitingForInventory = null;
        if (desired.request() == pending.request()) desired = result;
        acknowledged = result;
        pending = null;
        return result;
    }
    public InventoryWindowPayload inventorySynced(long revision) {
        return waitingForInventory == null ? null : acknowledge(waitingForInventory, revision);
    }
    private static boolean sameMapping(InventoryWindowPayload first, InventoryWindowPayload second) {
        return first != null && second != null && first.identities() == second.identities()
                && first.slots().equals(second.slots());
    }
}
