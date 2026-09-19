package com.cappleapple.bundlednotsiloed.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Shared creative-tab-style side rail for integrated inventory controls. */
public final class InventorySideRail {
    public static final int ATTACH_OFFSET = 2;
    public static final int BUTTON_GAP = 2;
    public static final int PADDING = 3;
    public static final int WIDTH = InventoryBrowserControls.SIZE + PADDING * 2;
    public static final int HEIGHT = PADDING * 2 + InventoryBrowserControls.SIZE * 4
            + BUTTON_GAP * 3;
    private static final int MAX_SEARCH_TEXT_WIDTH = 160;

    private InventorySideRail() {}

    public static Rail at(int guiLeft, int gridTop) {
        int left = Math.max(0, guiLeft - WIDTH + ATTACH_OFFSET);
        int top = gridTop - PADDING;
        int buttonX = Math.max(0, guiLeft - InventoryBrowserControls.SIZE - PADDING + ATTACH_OFFSET);
        return new Rail(left, top, guiLeft + 2 + ATTACH_OFFSET, top + HEIGHT, buttonX, gridTop);
    }

    public static int expandedSearchWidth(Font font, String query, int availableWidth) {
        int wanted = Math.max(8, font.width(query) + 6);
        return Math.max(0, Math.min(Math.min(MAX_SEARCH_TEXT_WIDTH, wanted), availableWidth));
    }

    public static void renderExpandedSearch(
            GuiGraphics graphics,
            Font font,
            Rail rail,
            String query,
            boolean allSelected,
            boolean valid,
            boolean cursorVisible
    ) {
        int width = expandedSearchWidth(font, query, MAX_SEARCH_TEXT_WIDTH);
        if (width <= 0) return;
        int left = rail.searchX() + InventoryBrowserControls.SIZE + 2;
        int top = rail.searchY();
        graphics.fill(left, top, left + width,
                top + InventoryBrowserControls.SIZE, 0xB0101010);
        String shown = font.plainSubstrByWidth(query, Math.max(0, width - 6));
        int textX = left + 3;
        if (allSelected && !shown.isEmpty()) {
            graphics.fill(textX, top + 2, textX + font.width(shown), top + 11, 0xCC2F5F8F);
        }
        graphics.drawString(font, shown, textX, top + 2,
                valid ? 0xFFFFFF : 0xFF5555, false);
        if (cursorVisible) {
            int cursorX = textX + font.width(shown);
            graphics.fill(cursorX, top + 2, cursorX + 1, top + 11, 0xFFFFFFFF);
        }
    }

    public record Rail(
            int left, int top, int right, int bottom, int buttonX, int buttonTop
    ) {
        public int searchX() { return buttonX; }
        public int searchY() { return buttonTop; }
        public int categoryX() { return buttonX; }
        public int categoryY() { return buttonTop + InventoryBrowserControls.SIZE + BUTTON_GAP; }
        public int sortX() { return buttonX; }
        public int sortY() { return categoryY() + InventoryBrowserControls.SIZE + BUTTON_GAP; }
        public int settingsX() { return buttonX; }
        public int settingsY() { return sortY() + InventoryBrowserControls.SIZE + BUTTON_GAP; }

        public void renderBackground(GuiGraphics graphics) {
            graphics.fill(left + 2, top, right, bottom, 0xFF373737);
            graphics.fill(left, top + 2, right, bottom - 2, 0xFF373737);
            graphics.fill(left + 2, top + 1, right, bottom - 1, 0xFFC6C6C6);
            graphics.fill(left + 1, top + 2, right, bottom - 2, 0xFFC6C6C6);
            graphics.fill(left + 2, top + 1, right - 2, top + 2, 0xFFFFFFFF);
            graphics.fill(left + 1, top + 2, left + 2, bottom - 2, 0xFFFFFFFF);
            graphics.fill(left + 2, bottom - 2, right - 2, bottom - 1, 0xFF555555);
            graphics.fill(right - 3, top + 3, right, bottom - 3, 0xFFC6C6C6);
        }

        public boolean searchContains(double x, double y) {
            return contains(x, y, searchX(), searchY());
        }
        public boolean categoryContains(double x, double y) {
            return contains(x, y, categoryX(), categoryY());
        }
        public boolean settingsContains(double x, double y) {
            return contains(x, y, settingsX(), settingsY());
        }

        public boolean sortContains(double x, double y) {
            return contains(x, y, sortX(), sortY());
        }

        private boolean contains(double x, double y, int buttonX, int buttonY) {
            return InventoryScreenLayout.inside(x, y, buttonX, buttonY,
                    InventoryBrowserControls.SIZE, InventoryBrowserControls.SIZE);
        }
    }
}
