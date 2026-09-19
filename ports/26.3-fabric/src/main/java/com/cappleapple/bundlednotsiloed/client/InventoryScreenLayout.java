package com.cappleapple.bundlednotsiloed.client;

/** Fixed geometry for the native-width integrated inventory browser. */
public final class InventoryScreenLayout {
    public static final int VANILLA_IMAGE_WIDTH = 176;
    public static final int VANILLA_IMAGE_HEIGHT = 166;
    public static final int IMAGE_HEIGHT = VANILLA_IMAGE_HEIGHT;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 84;
    public static final int GRID_COLUMNS = 9;
    public static final int GRID_ROWS = 3;
    public static final int CELL_SIZE = 18;
    public static final int HOTBAR_Y = 142;
    public static final int FULLNESS_Y = 138;
    public static final int FULLNESS_HEIGHT = 2;
    public static final int VISIBLE_ENTRIES = GRID_COLUMNS * GRID_ROWS;
    public static final int SCROLLBAR_X = GRID_X + GRID_COLUMNS * CELL_SIZE + 1;
    public static final int SCROLLBAR_Y = GRID_Y;
    public static final int SCROLLBAR_WIDTH = 2;
    public static final int SCROLLBAR_HEIGHT = GRID_ROWS * CELL_SIZE;

    private InventoryScreenLayout() {}

    public static int entryIndexAt(double localX, double localY, int scrollRow) {
        if (!inside(localX, localY, GRID_X, GRID_Y,
                GRID_COLUMNS * CELL_SIZE, GRID_ROWS * CELL_SIZE)) return -1;
        int column = (int)(localX - GRID_X) / CELL_SIZE;
        int row = (int)(localY - GRID_Y) / CELL_SIZE;
        return Math.max(0, scrollRow) * GRID_COLUMNS + row * GRID_COLUMNS + column;
    }

    public static int maximumScrollRow(int entryCount) {
        return Math.max(0, Math.ceilDiv(Math.max(0, entryCount), GRID_COLUMNS) - GRID_ROWS);
    }

    public static int scrollbarThumbHeight(int entryCount) {
        int totalRows = Math.max(GRID_ROWS, Math.ceilDiv(Math.max(0, entryCount), GRID_COLUMNS));
        return Math.max(8, SCROLLBAR_HEIGHT * GRID_ROWS / totalRows);
    }

    public static int scrollbarThumbY(int entryCount, int scrollRow) {
        int maximum = maximumScrollRow(entryCount);
        if (maximum == 0) return SCROLLBAR_Y;
        int travel = SCROLLBAR_HEIGHT - scrollbarThumbHeight(entryCount);
        return SCROLLBAR_Y + travel * Math.max(0, Math.min(maximum, scrollRow)) / maximum;
    }

    public static int scrollRowForScrollbar(double localY, int entryCount) {
        int maximum = maximumScrollRow(entryCount);
        if (maximum == 0) return 0;
        int thumbHeight = scrollbarThumbHeight(entryCount);
        int travel = SCROLLBAR_HEIGHT - thumbHeight;
        double centered = localY - SCROLLBAR_Y - thumbHeight / 2.0;
        return Math.max(0, Math.min(maximum, (int)Math.round(centered * maximum / Math.max(1, travel))));
    }

    public static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
