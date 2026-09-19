package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Shared input-capture state used to suppress unrelated keybinds while search accepts text. */
public final class InventorySearchInputCapture {
    private InventorySearchInputCapture() {}

    public static boolean isTyping() {
        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof BundledInventoryScreen inventoryScreen
                ? inventoryScreen.isSearchTyping()
                : ContainerInventoryOverlay.isSearchTyping(screen);
    }
}
