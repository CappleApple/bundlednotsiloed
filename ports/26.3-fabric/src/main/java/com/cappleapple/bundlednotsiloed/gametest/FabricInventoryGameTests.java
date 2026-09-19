package com.cappleapple.bundlednotsiloed.gametest;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public final class FabricInventoryGameTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void nativeStorageTransactionsExposeStowedEntries(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(80, new ItemStack(Items.DIAMOND, 17));
        var storage = PlayerInventoryStorage.of(player);
        var resource = ItemVariant.of(Items.DIAMOND);
        long revision = data.inventory().revision();
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.extract(resource, 10, transaction) == 10, "Native extraction cannot reach stowed entries");
        }
        helper.assertTrue(data.inventory().count(new ItemStack(Items.DIAMOND)) == 17, "Aborted transaction changed inventory");
        helper.assertTrue(data.inventory().revision() == revision, "Aborted transaction changed revision");
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.extract(resource, 10, transaction) == 10, "Committed extraction failed");
            transaction.commit();
        }
        helper.assertTrue(data.inventory().count(new ItemStack(Items.DIAMOND)) == 7, "Committed transaction lost or duplicated items");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void playerSaveRestoresLogicalPositions(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(80, new ItemStack(Items.DIAMOND, 17));
        var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(saved);
        var replacement = helper.makeMockPlayer(GameType.SURVIVAL);
        replacement.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, replacement.registryAccess(), saved.buildResult()));
        var restored = ModAttachments.get(replacement);
        helper.assertTrue(restored.migratedVanillaInventory(), "Player data migration flag was not restored");
        helper.assertTrue(restored.inventory().syntheticStack(80).getCount() == 17, "Player save did not preserve stowed storage");
        helper.succeed();
    }
    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void abortedNativeTransferRestoresVisibleProjection(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(9, new ItemStack(Items.DIAMOND, 17));
        data.syncVanillaCompatibilityView();
        var storage = PlayerInventoryStorage.of(player);
        var resource = ItemVariant.of(Items.DIAMOND);
        long revision = data.inventory().revision();
        try (var outer = Transaction.openOuter()) {
            helper.assertTrue(storage.extract(resource, 4, outer) == 4, "Outer extraction failed");
            try (var nested = outer.openNested()) {
                helper.assertTrue(storage.extract(resource, 3, nested) == 3, "Nested extraction failed");
            }
            helper.assertTrue(player.getInventory().getNonEquipmentItems().get(9).getCount() == 13,
                    "Nested rollback left a stale vanilla stack reference");
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 17,
                "Reconciliation lost items restored by rollback");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().get(9).getCount() == 17,
                "Outer rollback did not restore the visible projection");
        helper.assertTrue(data.inventory().revision() == revision, "Rollback changed the inventory revision");
        helper.succeed();
    }
    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void nativeEquipmentAndLogicalTransfersShareCommitAndRollback(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.syncVanillaCompatibilityView();
        var storage = PlayerInventoryStorage.of(player);
        var diamond = ItemVariant.of(Items.DIAMOND);
        var apple = ItemVariant.of(Items.APPLE);
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(9).insert(diamond, 17, transaction) == 17,
                    "Logical insertion failed");
            helper.assertTrue(storage.getSlot(40).insert(apple, 1, transaction) == 1,
                    "Offhand insertion failed");
            transaction.commit();
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 17,
                "Equipment commit reconciled a stale logical projection");
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "Offhand commit was not applied");
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(9).extract(diamond, 4, transaction) == 4,
                    "Logical extraction failed");
            helper.assertTrue(storage.getSlot(40).extract(apple, 1, transaction) == 1,
                    "Offhand extraction failed");
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 17,
                "Mixed rollback lost logical contents");
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "Mixed rollback lost offhand contents");
        helper.succeed();
    }
}
