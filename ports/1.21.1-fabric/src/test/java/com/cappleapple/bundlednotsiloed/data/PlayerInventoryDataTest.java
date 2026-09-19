package com.cappleapple.bundlednotsiloed.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.bundlednotsiloed.inventory.NewItemDestination;
import java.util.ArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerInventoryDataTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void newItemDestinationPersistsAndLegacyDataUsesInventoryFirst() {
        RegistryAccess.Frozen access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        PlayerInventoryData original = new PlayerInventoryData(null);
        original.setNewItemDestination(NewItemDestination.STOWED_FIRST);
        original.setAutoRefill(false);

        PlayerInventoryData loaded = new PlayerInventoryData(null);
        loaded.loadMetadata(access, original.saveMetadata(access));
        assertEquals(NewItemDestination.STOWED_FIRST, loaded.newItemDestination());
        assertFalse(loaded.autoRefill());

        net.minecraft.nbt.CompoundTag oldCustomization = new net.minecraft.nbt.CompoundTag();
        oldCustomization.putBoolean("PickupIntoHotbar", false);
        PlayerInventoryData legacy = new PlayerInventoryData(null);
        legacy.loadMetadata(access, oldCustomization);
        assertEquals(NewItemDestination.INVENTORY_FIRST, legacy.newItemDestination());
        assertTrue(legacy.autoRefill());
    }

    @Test
    void recentlyModifiedOrderPersistsWithTheInventory() {
        RegistryAccess.Frozen access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        PlayerInventoryData original = new PlayerInventoryData(null);
        ArrayList<ItemStack> sparse = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) sparse.add(ItemStack.EMPTY);
        sparse.add(new ItemStack(Items.APPLE, 4));
        sparse.add(new ItemStack(Items.STONE, 64));
        original.inventory().loadNetworkSnapshot(sparse, 1);
        original.inventory().replaceSyntheticSlotFromItemUse(9, new ItemStack(Items.APPLE, 3));
        original.inventory().replaceSyntheticSlotFromItemUse(10, new ItemStack(Items.STONE, 63));

        PlayerInventoryData loaded = new PlayerInventoryData(null);
        loaded.deserializeNBT(access, original.serializeNBT(access));

        assertTrue(loaded.recentlyModifiedOrder(new ItemStack(Items.STONE))
                > loaded.recentlyModifiedOrder(new ItemStack(Items.APPLE)));
    }
}
