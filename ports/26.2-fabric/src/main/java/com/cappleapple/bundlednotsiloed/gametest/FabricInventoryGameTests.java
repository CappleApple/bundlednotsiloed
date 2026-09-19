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
    public void mixedLogicalAndEquipmentTransactionsStayAtomic(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        var storage = PlayerInventoryStorage.of(player);
        var stone = ItemVariant.of(Items.STONE);
        var apple = ItemVariant.of(Items.APPLE);
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(9).insert(stone, 3, transaction) == 3,
                    "Logical slot insertion failed");
            helper.assertTrue(storage.getSlot(40).insert(apple, 1, transaction) == 1,
                    "Offhand insertion failed");
            transaction.commit();
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 3,
                "Equipment commit erased logical insertion");
        helper.assertTrue(player.getOffhandItem().is(Items.APPLE),
                "Equipment insertion did not commit");
        long revision = data.inventory().revision();
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(9).extract(stone, 2, transaction) == 2,
                    "Logical slot extraction failed");
            helper.assertTrue(storage.getSlot(40).extract(apple, 1, transaction) == 1,
                    "Offhand extraction failed");
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(9).getCount() == 3,
                "Aborted mixed extraction changed logical storage");
        helper.assertTrue(player.getOffhandItem().is(Items.APPLE),
                "Aborted mixed extraction changed offhand");
        helper.assertTrue(data.inventory().revision() == revision,
                "Aborted mixed extraction changed revision");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void abortedVisibleExtractionRestoresVanillaProjection(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(0, new ItemStack(Items.STONE, 25));
        data.syncVanillaCompatibilityView();
        long revision = data.inventory().revision();
        var storage = PlayerInventoryStorage.of(player);
        try (var transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.getSlot(0).extract(ItemVariant.of(Items.STONE), 10, transaction) == 10,
                    "Visible slot extraction failed");
        }
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(data.inventory().syntheticStack(0).getCount() == 25,
                "Vanilla reconciliation imported aborted extraction");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 25,
                "Vanilla inventory did not restore its visible stack");
        helper.assertTrue(data.inventory().revision() == revision,
                "Aborted extraction changed inventory revision");
        helper.succeed();
    }

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
}
