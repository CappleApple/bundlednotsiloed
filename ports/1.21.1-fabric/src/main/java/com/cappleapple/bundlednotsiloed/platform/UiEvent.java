package com.cappleapple.bundlednotsiloed.platform;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Input/render arguments shared by the inventory screen and container overlay. */
public final class UiEvent {
    private final Screen screen;
    private final GuiGraphics graphics;
    private final int mouseX, mouseY, key, scanCode;
    private boolean canceled;
    public UiEvent(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, int key, int scanCode) {
        this.screen = screen; this.graphics = graphics; this.mouseX = mouseX; this.mouseY = mouseY; this.key = key; this.scanCode = scanCode;
    }
    public static UiEvent screen(Screen screen) { return new UiEvent(screen, null, 0, 0, 0, 0); }
    public Screen getScreen() { return screen; }
    public AbstractContainerScreen<?> getContainerScreen() { return (AbstractContainerScreen<?>)screen; }
    public GuiGraphics getGuiGraphics() { return graphics; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public int getKeyCode() { return key; }
    public int getScanCode() { return scanCode; }
    public int getButton() { return key; }
    public int getCodePoint() { return key; }
    public void setCanceled(boolean value) { canceled = value; }
    public boolean isCanceled() { return canceled; }
}
