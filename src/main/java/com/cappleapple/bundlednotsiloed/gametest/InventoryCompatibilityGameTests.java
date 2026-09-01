package com.cappleapple.bundlednotsiloed.gametest;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import com.cappleapple.bundlednotsiloed.inventory.InventoryCursorTransactions;
import com.cappleapple.bundlednotsiloed.inventory.InventoryTransactions;
import com.cappleapple.bundlednotsiloed.inventory.NewItemDestination;
import com.cappleapple.bundlednotsiloed.inventory.PlayerInventoryQuickMove;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
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

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void externalQuickMoveHonorsSelectedNewItemDestination(GameTestHelper helper) {
        Player player = migratedPlayer(helper, GameType.SURVIVAL);
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        DynamicCapacityInventory inventory = data.inventory();
        for (int slot = 0; slot < 9; slot++) {
            inventory.replaceSyntheticSlotFromItemUse(slot, new ItemStack(Items.STONE));
        }
        inventory.replaceSyntheticSlotFromItemUse(9, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlotFromItemUse(11, new ItemStack(Items.STONE));
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.APPLE, 10));

        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.DIRT, 4));
        chest.setItem(1, new ItemStack(Items.COBBLESTONE, 5));
        chest.setItem(2, new ItemStack(Items.OAK_LOG, 6));
        chest.setItem(3, new ItemStack(Items.APPLE, 3));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);

        data.setNewItemDestination(NewItemDestination.INVENTORY_FIRST);
        menu.quickMoveStack(player, 0);

        helper.assertTrue(chest.getItem(0).isEmpty(),
                "Chest Shift-click left the brand-new stack in the source slot");
        helper.assertTrue(inventory.syntheticStack(10).is(Items.DIRT),
                "Chest Shift-click did not use the first available main-grid slot");
        helper.assertValueEqual(inventory.syntheticStack(10).getCount(), 4,
                "Chest Shift-click inserted the wrong brand-new quantity");

        data.setNewItemDestination(NewItemDestination.HOTBAR_FIRST);
        menu.quickMoveStack(player, 1);

        helper.assertTrue(inventory.syntheticStack(12).is(Items.COBBLESTONE),
                "Hotbar-first Shift-click did not fall back to the first available main-grid slot");

        data.setNewItemDestination(NewItemDestination.STOWED_FIRST);
        menu.quickMoveStack(player, 2);

        helper.assertTrue(inventory.syntheticStack(37).is(Items.OAK_LOG),
                "Stowed-first Shift-click occupied an available visible slot");

        data.setNewItemDestination(NewItemDestination.HOTBAR_FIRST);
        menu.quickMoveStack(player, 3);

        helper.assertTrue(chest.getItem(1).isEmpty(),
                "Chest Shift-click left a fallback stack in the source slot");
        helper.assertTrue(chest.getItem(2).isEmpty(),
                "Chest Shift-click left a stowed-first stack in the source slot");
        helper.assertTrue(chest.getItem(3).isEmpty(),
                "Chest Shift-click left the matching stack in the source slot");
        helper.assertValueEqual(inventory.syntheticStack(36).getCount(), 13,
                "Destination selection overrode the existing stowed identity");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void playerInventoryQuickMoveUsesDirectionBySource(GameTestHelper helper) {
        Player player = migratedPlayer(helper, GameType.SURVIVAL);
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        inventory.replaceSyntheticSlotFromItemUse(0, new ItemStack(Items.DIRT, 63));
        for (int slot = 1; slot < 9; slot++) {
            inventory.replaceSyntheticSlotFromItemUse(slot, new ItemStack(Items.STONE));
        }
        inventory.replaceSyntheticSlotFromItemUse(9, new ItemStack(Items.DIRT, 64));
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.APPLE));
        inventory.replaceSyntheticSlotFromItemUse(38, new ItemStack(Items.GRANITE));

        helper.assertTrue(PlayerInventoryQuickMove.move(inventory, 9),
                "Player-inventory Shift-click did not move its source stack");
        helper.assertValueEqual(inventory.syntheticStack(0).getCount(), 64,
                "Player-inventory Shift-click did not merge into the matching hotbar stack first");
        helper.assertTrue(inventory.syntheticStack(37).isEmpty(),
                "Player-inventory Shift-click filled an earlier logical-storage hole");
        helper.assertTrue(inventory.syntheticStack(39).is(Items.DIRT),
                "Player-inventory Shift-click did not append its hotbar overflow at the logical tail");
        helper.assertValueEqual(inventory.syntheticStack(39).getCount(), 63,
                "Player-inventory Shift-click appended the wrong overflow quantity");

        helper.assertTrue(PlayerInventoryQuickMove.move(inventory, 1),
                "Hotbar Shift-click did not move its source stack");
        helper.assertTrue(inventory.syntheticStack(9).is(Items.STONE),
                "Hotbar Shift-click did not use the first available main-inventory slot");
        helper.assertTrue(inventory.syntheticStack(1).isEmpty(),
                "Hotbar Shift-click left the source stack in the hotbar");
        helper.assertTrue(inventory.validate(),
                "Player-inventory Shift-click left the dynamic inventory invalid");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void recipeInventoryHooksDetectAndExtractStowedIngredients(GameTestHelper helper) {
        Player player = migratedPlayer(helper, GameType.SURVIVAL);
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        DynamicCapacityInventory inventory = data.inventory();
        inventory.replaceSyntheticSlotFromItemUse(36, new ItemStack(Items.OAK_PLANKS, 2));
        data.showInventoryRange(36);

        RecipeHolder<?> holder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.withDefaultNamespace("stick")).orElseThrow();
        helper.assertTrue(holder.value() instanceof CraftingRecipe,
                "The vanilla stick recipe was not a crafting recipe");
        CraftingRecipe recipe = (CraftingRecipe)holder.value();
        StackedContents contents = new StackedContents();
        player.getInventory().fillStackedContents(contents);
        helper.assertTrue(contents.canCraft(recipe, null),
                "Recipe detection did not count ingredients in stowed logical storage");

        int sourceSlot = player.getInventory().findSlotMatchingUnusedItem(new ItemStack(Items.OAK_PLANKS));
        helper.assertValueEqual(sourceSlot, Integer.MAX_VALUE,
                "Recipe extraction exposed a window-dependent vanilla slot");
        ItemStack sourceSnapshot = player.getInventory().getItem(sourceSlot);
        helper.assertValueEqual(sourceSnapshot.getCount(), 2,
                "Recipe extraction did not read the complete stowed ingredient stack");
        ItemStack removed = player.getInventory().removeItemNoUpdate(sourceSlot);
        helper.assertValueEqual(removed.getCount(), 2,
                "Recipe extraction removed the wrong stowed quantity");
        helper.assertValueEqual(sourceSnapshot.getCount(), 2,
                "Removing the complete ingredient stack erased vanilla's placement count");
        helper.assertTrue(inventory.syntheticStack(36).isEmpty(),
                "Recipe transfer did not consume the stowed ingredient stack");

        inventory.replaceSyntheticSlotFromItemUse(9, new ItemStack(Items.OAK_PLANKS));
        sourceSlot = player.getInventory().findSlotMatchingUnusedItem(new ItemStack(Items.OAK_PLANKS));
        helper.assertValueEqual(sourceSlot, Integer.MAX_VALUE,
                "Recipe extraction used the wrong vanilla slot for a logical item hidden by scrolling");
        helper.assertTrue(player.getInventory().removeItemNoUpdate(sourceSlot).is(Items.OAK_PLANKS),
                "Recipe extraction did not reach a logical item hidden by scrolling");
        helper.assertTrue(inventory.syntheticStack(9).isEmpty(),
                "Recipe extraction consumed the displayed replacement instead of the hidden logical item");
        helper.succeed();
    }

    private static Player migratedPlayer(GameTestHelper helper, GameType gameType) {
        Player player = helper.makeMockPlayer(gameType);
        gameType.updatePlayerAbilities(player.getAbilities());
        player.getData(ModAttachments.PLAYER_DATA).setMigratedVanillaInventory();
        return player;
    }
}
