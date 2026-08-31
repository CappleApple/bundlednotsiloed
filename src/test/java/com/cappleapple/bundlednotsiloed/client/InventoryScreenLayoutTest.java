package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InventoryScreenLayoutTest {
    @Test
    void compactToolbarMovesTheInventoryRowsByOnlySixteenPixels() {
        assertEquals(182, InventoryScreenLayout.IMAGE_HEIGHT);
        assertEquals(100, InventoryScreenLayout.GRID_Y);
        assertEquals(158, InventoryScreenLayout.HOTBAR_Y);
        assertEquals(16, InventoryScreenLayout.GRID_Y - 84);
        assertEquals(16, InventoryScreenLayout.HOTBAR_Y - 142);
        assertEquals(InventoryScreenLayout.SEARCH_HEIGHT, InventoryScreenLayout.CONTROL_SIZE);
        assertEquals(3, InventoryScreenLayout.CATEGORY_X
                - InventoryScreenLayout.SEARCH_X - InventoryScreenLayout.SEARCH_WIDTH);
    }

    @Test
    void scrollsOnlyWhenCombinedEntriesExceedTheVisibleTwentySeven() {
        assertEquals(0, InventoryScreenLayout.maximumScrollRow(0));
        assertEquals(0, InventoryScreenLayout.maximumScrollRow(27));
        assertEquals(1, InventoryScreenLayout.maximumScrollRow(28));
        assertEquals(2, InventoryScreenLayout.maximumScrollRow(45));
        assertEquals(0, InventoryScreenLayout.entryIndexAt(8, 100, 0));
        assertEquals(26, InventoryScreenLayout.entryIndexAt(169, 153, 0));
        assertEquals(27, InventoryScreenLayout.entryIndexAt(8, 100, 3));
    }

    @Test
    void scrollbarThumbTracksTheFirstAndLastRows() {
        int entries = 63;
        int maximum = InventoryScreenLayout.maximumScrollRow(entries);
        int thumbHeight = InventoryScreenLayout.scrollbarThumbHeight(entries);
        assertEquals(4, maximum);
        assertEquals(23, thumbHeight);
        assertEquals(InventoryScreenLayout.SCROLLBAR_Y,
                InventoryScreenLayout.scrollbarThumbY(entries, 0));
        assertEquals(InventoryScreenLayout.SCROLLBAR_Y
                        + InventoryScreenLayout.SCROLLBAR_HEIGHT - thumbHeight,
                InventoryScreenLayout.scrollbarThumbY(entries, maximum));
        assertEquals(maximum, InventoryScreenLayout.scrollRowForScrollbar(
                InventoryScreenLayout.SCROLLBAR_Y + InventoryScreenLayout.SCROLLBAR_HEIGHT, entries));
    }
}
