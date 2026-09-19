package com.cappleapple.bundlednotsiloed.network;

import static org.junit.jupiter.api.Assertions.*;
import com.cappleapple.bundlednotsiloed.data.InventorySlotWindow;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounterException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventorySyncRegressionTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void predictedRemovalAndVanillaEchoDoNotRejectTheNextDelta() {
        var inventory = new DynamicCapacityInventory(() -> 512);
        var sync = new ClientInventorySync();
        sync.snapshot(inventory, 10, List.of(new ItemStack(Items.STONE, 4), new ItemStack(Items.DIRT, 2)));
        inventory.replaceSyntheticSlotFromItemUse(0, new ItemStack(Items.STONE, 4));
        inventory.extractSyntheticSlot(1, 1, false);
        long predictedGeneration = inventory.revision();
        assertTrue(sync.delta(inventory, new InventoryDeltaPayload(10, 11, 2,
                List.of(new InventoryDeltaPayload.SlotChange(1, new ItemStack(Items.DIRT))))));
        assertEquals(11, sync.serverRevision());
        assertTrue(inventory.revision() > predictedGeneration);
        assertEquals(1, inventory.syntheticStack(1).getCount());
    }

    @Test void serverMirrorRepairsIncorrectPredictionsEvenOnUnchangedSlots() {
        var inventory = new DynamicCapacityInventory(() -> 512);
        var sync = new ClientInventorySync();
        sync.snapshot(inventory, 10, List.of(new ItemStack(Items.STONE, 4), new ItemStack(Items.DIRT, 2)));
        inventory.replaceSyntheticSlotFromItemUse(0, ItemStack.EMPTY);
        assertTrue(sync.delta(inventory, new InventoryDeltaPayload(10, 11, 2,
                List.of(new InventoryDeltaPayload.SlotChange(1, new ItemStack(Items.DIRT))))));
        assertEquals(4, inventory.syntheticStack(0).getCount());
        // Mutating installed stacks cannot alter the server mirror either.
        inventory.vanillaStackReference(0).shrink(2);
        assertTrue(sync.delta(inventory, new InventoryDeltaPayload(11, 11, 2, List.of())));
        assertEquals(4, inventory.syntheticStack(0).getCount());
    }

    @Test void malformedDeltaNeverPartiallyMutatesBaselineAndRecoveryIsBounded() {
        var inventory = new DynamicCapacityInventory(() -> 512);
        var sync = new ClientInventorySync();
        sync.snapshot(inventory, 10, List.of(new ItemStack(Items.STONE, 4)));
        assertFalse(sync.delta(inventory, new InventoryDeltaPayload(10, 11, 1,
                List.of(new InventoryDeltaPayload.SlotChange(0, ItemStack.EMPTY),
                        new InventoryDeltaPayload.SlotChange(1, new ItemStack(Items.DIRT))))));
        assertFalse(sync.delta(inventory, new InventoryDeltaPayload(10, 11, Integer.MAX_VALUE, List.of())));
        assertFalse(sync.delta(inventory, new InventoryDeltaPayload(9, 11, 1, List.of())));
        assertEquals(10, sync.serverRevision());
        assertEquals(4, inventory.syntheticStack(0).getCount());
        assertTrue(sync.requestRecovery(1));
        assertFalse(sync.requestRecovery(2));
        assertTrue(sync.requestRecovery(1_000_000_001));
        assertTrue(sync.delta(inventory, new InventoryDeltaPayload(10, 11, 1, List.of())));
        assertEquals(4, inventory.syntheticStack(0).getCount());
    }

    @Test void acknowledgedSearchSlotSurvivesComponentChangesAndDoesNotBecomeAnEmptySlot() {
        var inventory = new DynamicCapacityInventory(() -> 512);
        inventory.replaceSyntheticSlotFromItemUse(42, new ItemStack(Items.CHEST));
        var resolver = new InventorySlotWindow();
        resolver.show(List.of(new ItemStack(Items.CHEST)), inventory);
        var window = new InventorySlotWindow();
        window.showSlots(resolver.logicalSlots(), true);
        inventory.vanillaStackReference(42).set(DataComponents.CUSTOM_NAME, Component.literal("changed metadata"));
        window.refresh(inventory);
        assertEquals(42, window.logicalIndex(9));
        inventory.swapSyntheticSlots(42, 50);
        window.refresh(inventory);
        assertEquals(42, window.logicalIndex(9)); // Remapping requires a new acknowledgement.
        assertTrue(inventory.syntheticStack(42).isEmpty());
    }

    @Test void navigationCoalescesAndWaitsForBothInventoryAndMappingAcknowledgements() {
        var state = new InventoryWindowSession(7, 3);
        var first = java.util.stream.IntStream.range(9, 36).boxed().toList();
        var second = java.util.stream.IntStream.range(18, 45).boxed().toList();
        state.request(first, false);
        var request = state.nextRequest();
        state.request(second, false);
        assertNull(state.nextRequest());
        assertNull(state.acknowledge(new InventoryWindowResultPayload(request, 40), 39));
        assertTrue(state.pending());
        assertEquals(first, state.inventorySynced(40).slots());
        var next = state.nextRequest();
        assertEquals(second, next.slots());
        assertTrue(state.pending());
        assertNull(state.acknowledge(new InventoryWindowResultPayload(request, 40), 40));
        assertEquals(next, state.acknowledge(new InventoryWindowResultPayload(next, 40), 40));
        assertFalse(state.pending());
        state.request(second, false);
        assertNull(state.nextRequest());
        var reopened = new InventoryWindowSession(8, 3);
        reopened.request(first, false);
        reopened.nextRequest();
        assertNull(reopened.acknowledge(new InventoryWindowResultPayload(request, 40), 40));
        assertTrue(reopened.pending());
    }

    @Test void serverPageCorrectionDoesNotLoopAndPreservesNewerUserNavigation() {
        var state = new InventoryWindowSession(7, 3);
        var first = java.util.stream.IntStream.range(90, 117).boxed().toList();
        var corrected = java.util.stream.IntStream.range(9, 36).boxed().toList();
        state.request(first, false);
        var request = state.nextRequest();
        var result = new InventoryWindowPayload(7, request.request(), 3, false, corrected);
        assertEquals(result, state.acknowledge(new InventoryWindowResultPayload(result, 40), 40));
        assertNull(state.nextRequest());
        assertFalse(state.pending());
    }

    @Test void snapshotChunksRespectByteAndSlotBudgetsWithoutLosingSparseSlots() {
        var chunks = InventorySnapshotPlan.chunks(List.of(150_000, 150_000, 1, 300_000, 1));
        assertEquals(List.of(new InventorySnapshotPlan.Chunk(0, 1), new InventorySnapshotPlan.Chunk(1, 3),
                new InventorySnapshotPlan.Chunk(3, 4), new InventorySnapshotPlan.Chunk(4, 5)), chunks);
        var sparse = InventorySnapshotPlan.chunks(java.util.Collections.nCopies(257, 1));
        assertEquals(List.of(new InventorySnapshotPlan.Chunk(0, 256), new InventorySnapshotPlan.Chunk(256, 257)), sparse);
        assertEquals(List.of(new InventorySnapshotPlan.Chunk(0, 0)), InventorySnapshotPlan.chunks(List.of()));
    }

    @Test void oversizedItemIsDetectedBeforeSendingWithoutRemovingAnyData() {
        CompoundTag tag = new CompoundTag();
        tag.putByteArray("oversized", new byte[2 * 1024 * 1024]);
        ItemStack item = new ItemStack(Items.CHEST);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        assertThrows(NbtAccounterException.class, () -> InventorySyncPreflight.validate(registries, item));
        assertEquals(2 * 1024 * 1024,
                item.get(DataComponents.CUSTOM_DATA).copyTag().getByteArray("oversized").length);
        assertEquals(1, item.getCount());
        assertDoesNotThrow(() -> InventorySyncPreflight.validate(registries, new ItemStack(Items.STONE, 64)));
    }
}
