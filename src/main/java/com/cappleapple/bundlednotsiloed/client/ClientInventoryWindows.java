package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.BrowserStatePayload;
import com.cappleapple.bundlednotsiloed.network.InventoryWindowPayload;
import com.cappleapple.bundlednotsiloed.network.InventoryWindowResultPayload;
import com.cappleapple.bundlednotsiloed.network.InventoryWindowSession;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Native clicks wait until both the inventory state and the requested page are acknowledged. */
public final class ClientInventoryWindows {
    private static long nextSession;
    private static InventoryWindowSession state;
    private ClientInventoryWindows() {}

    public static void open() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        state = new InventoryWindowSession(++nextSession, player.containerMenu.containerId);
        PacketDistributor.sendToServer(new BrowserStatePayload(true, state.session(), state.containerId()));
    }
    public static void close() {
        if (state == null) return;
        PacketDistributor.sendToServer(new BrowserStatePayload(false, state.session(), state.containerId()));
        reset();
    }
    public static void reset() { state = null; }
    public static boolean pending() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getData(ModAttachments.PLAYER_DATA).clientSync().blocked()
                || state != null && state.pending();
    }
    public static void request(List<Integer> slots, boolean identities) {
        if (state == null) return;
        state.request(slots, identities);
        sendDesired();
    }
    private static void sendDesired() {
        InventoryWindowPayload request = state.nextRequest();
        if (request != null) PacketDistributor.sendToServer(request);
    }
    private static boolean activeMenu() {
        var player = Minecraft.getInstance().player;
        return state != null && player != null && player.containerMenu.containerId == state.containerId();
    }
    public static void acknowledge(InventoryWindowResultPayload payload) {
        if (!activeMenu()) return;
        var data = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA);
        apply(state.acknowledge(payload, data.clientSync().serverRevision()));
    }
    public static void inventorySynced() {
        if (!activeMenu()) return;
        var data = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA);
        apply(state.inventorySynced(data.clientSync().serverRevision()));
    }
    private static void apply(InventoryWindowPayload accepted) {
        if (accepted == null) return;
        Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA)
                .showInventorySlots(accepted.slots(), accepted.identities());
        sendDesired();
    }
}
