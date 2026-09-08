package com.cappleapple.bundlednotsiloed.gametest;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.compat.LogicalBackpackContext;
import com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.InventorySyncPreflight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BundledNotSiloed.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InventoryNetworkGameTests {
    private InventoryNetworkGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
    public static void identicalVanillaWritesKeepLiveItemReferences(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = player.getData(ModAttachments.PLAYER_DATA);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(42, new ItemStack(Items.CHEST));
        data.showInventoryRange(18);
        ItemStack live = data.inventory().vanillaStackReference(42);
        long revision = data.inventory().revision();
        player.getInventory().setItem(33, live.copy());
        helper.assertTrue(data.inventory().vanillaStackReference(42) == live, "Vanilla echo replaced the live item object");
        helper.assertTrue(data.inventory().revision() == revision, "Identical echo advanced the inventory revision");
        player.getInventory().items.set(33, live.copy());
        data.reconcileVanillaCompatibilityView();
        helper.assertTrue(player.getInventory().items.get(33) == live, "List reconciliation replaced the live item object");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
    public static void sophisticatedHoverContextAndOpenItemStayStable(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) {
            BundledNotSiloed.LOGGER.info("Skipping optional Sophisticated Backpacks runtime assertions: mod is absent");
            helper.succeed();
            return;
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = player.getData(ModAttachments.PLAYER_DATA);
        data.setMigratedVanillaInventory();
        ItemStack backpack = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("sophisticatedbackpacks:netherite_backpack")));
        helper.assertTrue(!backpack.isEmpty(), "Netherite backpack was not registered");
        data.inventory().replaceSyntheticSlotFromItemUse(42, backpack);
        data.showInventoryRange(18); // Hovered vanilla slot 33 resolves to logical slot 42.
        Class<?> contextType = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext");
        Class<?> itemContextType = Class.forName(contextType.getName() + "$Item");
        Object context = itemContextType.getConstructor(String.class, String.class, int.class, boolean.class)
                .newInstance("main", "", 33, true);
        Object wrapper = contextType.getMethod("getBackpackWrapper", Player.class).invoke(context, player);
        helper.assertTrue(context instanceof LogicalBackpackContext, "Optional context mixin was not applied");
        helper.assertValueEqual(((LogicalBackpackContext)context).bns$logicalBackpackSlot(), 42,
                "Hovered open retained a viewport-dependent vanilla slot");
        // Fill real, separately stored backpack contents before checking its item codec.
        Object contents = wrapper.getClass().getMethod("getInventoryHandler").invoke(wrapper);
        int size = (Integer)contents.getClass().getMethod("getSlots").invoke(contents);
        for (int slot = 0; slot < size; slot++) contents.getClass().getMethod("setStackInSlot", int.class, ItemStack.class)
                .invoke(contents, slot, new ItemStack(Items.DIAMOND, 64));
        InventorySyncPreflight.validate(player.registryAccess(), data.inventory().vanillaStackReference(42));
        data.resetInventoryWindow();
        Object resolved = contextType.getMethod("getBackpackWrapper", Player.class).invoke(context, player);
        helper.assertTrue(resolved == wrapper, "Closing the old screen changed the backpack wrapper");
        Class<?> menuType = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer");
        AbstractContainerMenu menu = (AbstractContainerMenu)menuType.getConstructor(int.class, Player.class, contextType)
                .newInstance(1, player, context);
        player.containerMenu = menu;
        data.showInventoryRange(36);
        helper.assertValueEqual(OpenBackpackGuard.logicalSlot(player), 42, "Open backpack root was not protected");
        helper.assertTrue(data.inventory().extractSyntheticSlot(42, 1, false).isEmpty(), "Backend extraction removed the open backpack");
        int visible = data.inventoryWindow().vanillaSlotForLogical(42);
        var slot = menu.slots.stream().filter(value -> value.container == player.getInventory()
                && value.getContainerSlot() == visible).findFirst().orElseThrow();
        helper.assertTrue(!slot.mayPickup(player), "The native slot allowed pickup of the open backpack");
        menu.clicked(slot.index, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "Native click picked up the open backpack");
        helper.assertTrue(data.inventory().vanillaStackReference(42) == wrapper.getClass().getMethod("getBackpack").invoke(wrapper),
                "Backpack movement guard replaced the stored object");
        // Nested contexts inherit the same logical root reference and must protect the outer backpack.
        ItemStack child = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("sophisticatedbackpacks:backpack")));
        contents.getClass().getMethod("setStackInSlot", int.class, ItemStack.class).invoke(contents, 0, child);
        Object nestedContext = contextType.getMethod("getSubBackpackContext", int.class, boolean.class).invoke(context, 0, true);
        AbstractContainerMenu nestedMenu = (AbstractContainerMenu)menuType.getConstructor(int.class, Player.class, contextType)
                .newInstance(2, player, nestedContext);
        player.containerMenu = nestedMenu;
        helper.assertValueEqual(OpenBackpackGuard.logicalSlot(player), 42, "Nested backpack lost its root item lock");
        helper.assertTrue(data.inventory().extractSyntheticSlot(42, 1, false).isEmpty(), "Nested menu allowed extracting its outer backpack");
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!data.inventory().extractSyntheticSlot(42, 1, false).isEmpty(), "Backpack remained locked after closing");
        helper.succeed();
    }
}
