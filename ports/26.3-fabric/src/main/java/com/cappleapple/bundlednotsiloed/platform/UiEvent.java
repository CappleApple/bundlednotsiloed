package com.cappleapple.bundlednotsiloed.platform;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Input/render arguments shared by the inventory screen and container overlay. */
public final class UiEvent {
    private final Screen screen;
    private final GuiGraphicsExtractor graphics;
    private final int mouseX, mouseY, key, scanCode;
    private boolean canceled;
    private net.minecraft.client.input.KeyEvent keyInput;
    public UiEvent(Screen screen, net.minecraft.client.input.KeyEvent input) {
        this(screen, null, 0, 0, input.key(), input.keycode()); keyInput = input;
    }
    public net.minecraft.client.input.KeyEvent getKeyEvent() { return keyInput != null ? keyInput : new net.minecraft.client.input.KeyEvent(key, scanCode, 0); }
    public int getKey() { return key; }
    public int getKeycode() { return scanCode; }
    public GuiGraphicsExtractor getGuiGraphics() { return graphics; }
    public UiEvent(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, int key, int scanCode) {
        this.screen = screen; this.graphics = graphics; this.mouseX = mouseX; this.mouseY = mouseY; this.key = key; this.scanCode = scanCode;
    }
    public static UiEvent screen(Screen screen) { return new UiEvent(screen, null, 0, 0, 0, 0); }
    public Screen getScreen() { return screen; }
    public AbstractContainerScreen<?> getContainerScreen() { return (AbstractContainerScreen<?>)screen; }
    public GuiGraphicsExtractor getGuiGraphicsExtractor() { return graphics; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public int getKeyCode() { return key; }
    public int getScanCode() { return scanCode; }
    public int getButton() { return key; }
    public int getCodePoint() { return key; }
    public void setCanceled(boolean value) { canceled = value; }
    public boolean isCanceled() { return canceled; }
}
