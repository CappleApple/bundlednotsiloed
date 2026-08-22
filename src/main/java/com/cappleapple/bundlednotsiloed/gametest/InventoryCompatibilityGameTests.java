package com.cappleapple.bundlednotsiloed.gametest;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import com.cappleapple.bundlednotsiloed.inventory.InventoryCursorTransactions;
import com.cappleapple.bundlednotsiloed.inventory.InventoryTransactions;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BundledNotSiloed.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("deprecation")
public final class InventoryCompatibilityGameTests {
    private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";

    private InventoryCompatibilityGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void clearVisitsHiddenLogicalStorage(GameTestHelper helper) {
        Player player = migratedPlayer(helper, GameType.SURVIVAL);
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        inventory.replaceSyntheticSlotFromItemUse(0, new ItemStack(Items.STONE, 3));
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.STONE, 7));
        inventory.replaceSyntheticSlotFromItemUse(37, new ItemStack(Items.DIRT, 2));

        int removed = player.getInventory().clearOrCountMatchingItems(
                stack -> stack.is(Items.STONE), -1, new SimpleContainer(0));

        helper.assertValueEqual(removed, 10, "/clear did not include hidden logical storage");
        helper.assertTrue(inventory.entries().stream().noneMatch(entry -> entry.representative().is(Items.STONE)),
                "/clear left matching items in logical storage");
        helper.assertValueEqual(inventory.syntheticStack(37).getCount(), 2,
                "/clear removed a non-matching backend stack");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void creativePickAndPlacementUseLogicalStorage(GameTestHelper helper) {
        Player player = migratedPlayer(helper, GameType.CREATIVE);
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        DynamicCapacityInventory inventory = data.inventory();
        inventory.replaceSyntheticSlotFromItemUse(0, new ItemStack(Items.APPLE));
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.STONE));
        player.getInventory().selected = 0;

        player.getInventory().setPickedItem(new ItemStack(Items.STONE));

        helper.assertTrue(player.getInventory().getSelected().is(Items.STONE),
                "Creative pick block did not update the authoritative held stack");
        helper.assertTrue(inventory.syntheticStack(0).is(Items.APPLE),
                "Creative pick block changed an occupied hotbar slot despite an empty alternative");
        helper.assertTrue(inventory.syntheticStack(36).is(Items.STONE),
                "Creative pick block unexpectedly moved a hidden backend stack");

        AttributeInstance capacity = player.getAttribute(ModAttributes.INVENTORY_CAPACITY);
        helper.assertTrue(capacity != null, "Inventory capacity attribute was missing");
        capacity.setBaseValue(2);
        Slot emptyPlayerSlot = new Slot(player.getInventory(), 2, 0, 0);
        helper.assertValueEqual(emptyPlayerSlot.getMaxStackSize(new ItemStack(Items.DIRT)), 64,
                "Creative inventory placement was still limited by carrying capacity");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void creativeCursorTransactionsUseHiddenStorage(GameTestHelper helper) {
        Player player = migratedPlayer(helper, GameType.CREATIVE);
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        AttributeInstance capacity = player.getAttribute(ModAttributes.INVENTORY_CAPACITY);
        helper.assertTrue(capacity != null, "Inventory capacity attribute was missing");
        capacity.setBaseValue(512);
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.STONE, 7));

        ItemStack taken = InventoryCursorTransactions.takeFromBackend(
                inventory, new ItemStack(Items.STONE), 64, true);
        helper.assertValueEqual(taken.getCount(), 4,
                "Creative cursor extraction did not take half of the backend stack");
        helper.assertValueEqual(inventory.syntheticStack(36).getCount(), 3,
                "Creative cursor extraction did not update hidden storage");

        var stowed = InventoryTransactions.insertIntoBackend(player, new ItemStack(Items.DIRT, 5), false);
        helper.assertValueEqual(stowed.acceptedAmount(), 5,
                "Creative cursor stow did not insert into hidden storage");
        helper.assertTrue(inventory.entriesAtOrAfter(36).stream()
                        .anyMatch(entry -> entry.representative().is(Items.DIRT) && entry.quantity() == 5),
                "Creative cursor stow was not visible in backend entries");
        helper.succeed();
    }

    private static Player migratedPlayer(GameTestHelper helper, GameType gameType) {
        Player player = helper.makeMockPlayer(gameType);
        gameType.updatePlayerAbilities(player.getAbilities());
        player.getData(ModAttachments.PLAYER_DATA).setMigratedVanillaInventory();
        return player;
    }
}
