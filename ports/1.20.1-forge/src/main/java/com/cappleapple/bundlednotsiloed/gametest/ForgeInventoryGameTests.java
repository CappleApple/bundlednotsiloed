package com.cappleapple.bundlednotsiloed.gametest;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BundledNotSiloed.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForgeInventoryGameTests {
    @GameTest(templateNamespace = BundledNotSiloed.MOD_ID, template = "empty")
    public static void capabilityExposesStowedEntries(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(80, new ItemStack(Items.DIAMOND, 17));
        var handler = player.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new IllegalStateException("Missing player inventory capability"));
        helper.assertTrue(handler.getSlots() > 80, "Capability cannot address stowed entries");
        helper.assertTrue(handler.extractItem(80, 10, true).getCount() == 10, "Simulated extraction failed");
        helper.assertTrue(data.inventory().syntheticStack(80).getCount() == 17, "Simulation mutated inventory");
        helper.assertTrue(handler.extractItem(80, 10, false).getCount() == 10, "Extraction failed");
        helper.assertTrue(data.inventory().syntheticStack(80).getCount() == 7, "Extraction lost or duplicated items");
        helper.succeed();
    }

    @GameTest(templateNamespace = BundledNotSiloed.MOD_ID, template = "empty")
    public static void playerSaveRestoresLogicalPositions(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        var data = ModAttachments.get(player);
        data.setMigratedVanillaInventory();
        data.inventory().replaceSyntheticSlotFromItemUse(80, new ItemStack(Items.DIAMOND, 17));
        CompoundTag saved = new CompoundTag();
        player.saveWithoutId(saved);
        var replacement = helper.makeMockSurvivalPlayer();
        replacement.load(saved);
        var restored = ModAttachments.get(replacement);
        helper.assertTrue(restored.migratedVanillaInventory(), "Migration flag was not restored");
        helper.assertTrue(restored.inventory().syntheticStack(80).getCount() == 17, "Saved logical positions changed");
        helper.succeed();
    }
    @GameTest(templateNamespace = BundledNotSiloed.MOD_ID, template = "empty")
    public static void targetVersionCategoryTagsAreLoaded(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(Items.APPLE).is(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, BundledNotSiloed.id("foods"))),
                "The 1.20.1 food fallback tag was not loaded");
        helper.assertTrue(new ItemStack(Items.IRON_ORE).is(net.minecraftforge.common.Tags.Items.ORES),
                "Forge ore tags are unavailable to category rules");
        helper.succeed();
    }
}
