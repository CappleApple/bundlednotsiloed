package com.cappleapple.bundlednotsiloed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;

/** Accesses the cursor owned by CreativeModeInventoryScreen.ItemPickerMenu rather than inventoryMenu. */
public final class CreativeInventoryCursor {
    private CreativeInventoryCursor() {}

    public static void replace(ItemStack carried) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CreativeModeInventoryScreen screen) {
            screen.getMenu().setCarried(carried.copy());
        }
    }
}
