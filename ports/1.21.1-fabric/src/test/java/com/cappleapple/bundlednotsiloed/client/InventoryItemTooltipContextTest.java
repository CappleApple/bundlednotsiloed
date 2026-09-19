package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoryItemTooltipContextTest {
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @AfterEach
    void clearContext() {
        InventoryItemTooltipContext.end();
    }

    @Test
    void acceptsOnlyMatchingHotbarAndMainInventorySlots() {
        SimpleContainer playerInventory = new SimpleContainer(41);
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 32);
        playerInventory.setItem(0, cobblestone);
        InventoryItemTooltipContext.begin(new Slot(playerInventory, 0, 0, 0));

        assertTrue(InventoryItemTooltipContext.isPlayerStorageTooltip(
                playerInventory, new ItemStack(Items.COBBLESTONE)));
        assertFalse(InventoryItemTooltipContext.isPlayerStorageTooltip(
                playerInventory, new ItemStack(Items.STONE)));
    }

    @Test
    void rejectsStowedEquipmentAndExternalSlots() {
        SimpleContainer playerInventory = new SimpleContainer(41);
        SimpleContainer chest = new SimpleContainer(1);
        playerInventory.setItem(36, new ItemStack(Items.COBBLESTONE));
        chest.setItem(0, new ItemStack(Items.COBBLESTONE));

        InventoryItemTooltipContext.begin(new Slot(playerInventory, 36, 0, 0));
        assertFalse(InventoryItemTooltipContext.isPlayerStorageTooltip(
                playerInventory, new ItemStack(Items.COBBLESTONE)));

        InventoryItemTooltipContext.begin(new Slot(chest, 0, 0, 0));
        assertFalse(InventoryItemTooltipContext.isPlayerStorageTooltip(
                playerInventory, new ItemStack(Items.COBBLESTONE)));
    }

    @Test
    void rejectsTooltipsOutsideNativeSlotRendering() {
        SimpleContainer playerInventory = new SimpleContainer(41);

        assertFalse(InventoryItemTooltipContext.isPlayerStorageTooltip(
                playerInventory, new ItemStack(Items.COBBLESTONE)));
    }
}
