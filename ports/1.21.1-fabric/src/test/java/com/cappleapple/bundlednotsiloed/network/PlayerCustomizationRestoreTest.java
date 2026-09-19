package com.cappleapple.bundlednotsiloed.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.CategoryRule;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerCustomizationRestoreTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void joiningWithSavedViewPreferencesDoesNotRearrangeTheVanillaProjection() {
        RegistryAccess.Frozen access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        PlayerInventoryData departing = new PlayerInventoryData(null);
        ArrayList<ItemStack> persistedSlots = new ArrayList<>();
        for (int slot = 0; slot <= 36; slot++) persistedSlots.add(ItemStack.EMPTY);
        persistedSlots.set(9, new ItemStack(Items.STONE));
        persistedSlots.set(10, new ItemStack(Items.APPLE));
        persistedSlots.set(36, new ItemStack(Items.COBBLESTONE));
        departing.inventory().loadNetworkSnapshot(persistedSlots, 4);

        ResourceLocation categoryId = ResourceLocation.fromNamespaceAndPath(
                "bundlednotsiloed", "restore_test");
        PlayerInventoryData savedPreferences = new PlayerInventoryData(null);
        savedPreferences.categories().replaceAll(List.of(new CategoryDefinition(
                categoryId,
                "All items",
                BuiltInRegistries.ITEM.getKey(Items.STONE),
                0,
                List.of(new CategoryRule(CategoryRule.Type.MOD_ID,
                        ResourceLocation.fromNamespaceAndPath("minecraft", "mod"))),
                List.of(),
                -1,
                SortMode.NAME_ASCENDING,
                true,
                false
        )), true);
        savedPreferences.setSelectedCategoryPreference(categoryId);
        savedPreferences.setInventorySortPreference(SortMode.NAME_ASCENDING);

        PlayerInventoryData joined = new PlayerInventoryData(null);
        joined.deserializeNBT(access, departing.serializeNBT(access));
        ModNetwork.restorePlayerCustomization(joined, access, savedPreferences.saveCustomization(access));

        assertEquals(SortMode.NAME_ASCENDING, joined.inventorySortPreference());
        assertEquals(categoryId, joined.selectedCategoryPreference());
        assertEquals(Items.STONE, joined.inventory().syntheticStack(9).getItem());
        assertEquals(Items.APPLE, joined.inventory().syntheticStack(10).getItem());
        assertEquals(Items.COBBLESTONE, joined.inventory().syntheticStack(36).getItem());
    }
}
