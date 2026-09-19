package com.cappleapple.bundlednotsiloed.gametest;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public final class FabricInventoryGameTests {
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void nativeStorageTransactionsExposeStowedEntries(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
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

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void playerSaveRestoresLogicalPositions(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(80, new ItemStack(Items.DIAMOND, 17));
        CompoundTag saved = new CompoundTag();
        player.saveWithoutId(saved);
        var replacement = helper.makeMockSurvivalPlayer();
        replacement.load(saved);
        var restored = ModAttachments.get(replacement);
        helper.assertTrue(restored.migratedVanillaInventory(), "Player data migration flag was not restored");
        helper.assertTrue(restored.inventory().syntheticStack(80).getCount() == 17, "Player save did not preserve stowed storage");
        helper.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void abortedVisibleExtractionSurvivesVanillaReconciliation(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(9, new ItemStack(Items.DIAMOND, 17));
        var storage = PlayerInventoryStorage.of(player);
        long revision = data.inventory().revision();
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(9).extract(ItemVariant.of(Items.DIAMOND), 10, transaction) == 10,
                    "Visible slot extraction failed");
        }
        helper.assertTrue(player.getInventory().items.get(9) == data.inventory().vanillaStackReference(9),
                "Aborted transaction left a stale vanilla stack reference");
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 17,
                "Vanilla reconciliation discarded items restored by rollback");
        helper.assertTrue(player.getInventory().items.get(9).getCount() == 17,
                "Vanilla view retained the aborted extraction");
        helper.assertTrue(data.inventory().revision() == revision,
                "Aborted extraction changed the inventory revision after reconciliation");
        helper.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void mixedLogicalAndEquipmentTransactionsPreserveBothInventories(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        var storage = PlayerInventoryStorage.of(player);
        var diamonds = ItemVariant.of(Items.DIAMOND);
        var stone = ItemVariant.of(Items.STONE);
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(9).insert(diamonds, 17, transaction) == 17,
                    "Logical insertion failed");
            helper.assertTrue(storage.getSlot(40).insert(stone, 3, transaction) == 3,
                    "Offhand insertion failed");
            transaction.commit();
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 17,
                "Equipment commit discarded logical insertion");
        helper.assertTrue(player.getOffhandItem().is(Items.STONE) && player.getOffhandItem().getCount() == 3,
                "Mixed commit failed to preserve the offhand");
        long revision = data.inventory().revision();
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(9).extract(diamonds, 10, transaction) == 10,
                    "Tentative logical extraction failed");
            helper.assertTrue(storage.getSlot(40).extract(stone, 2, transaction) == 2,
                    "Tentative offhand extraction failed");
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 17,
                "Mixed rollback lost logical items during reconciliation");
        helper.assertTrue(player.getOffhandItem().getCount() == 3,
                "Mixed rollback lost offhand items");
        helper.assertTrue(data.inventory().revision() == revision,
                "Mixed rollback changed the logical inventory revision");
        helper.succeed();
    }
}
