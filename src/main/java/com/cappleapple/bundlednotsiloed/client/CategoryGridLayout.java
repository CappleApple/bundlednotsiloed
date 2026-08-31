package com.cappleapple.bundlednotsiloed.client;

/** Responsive row-scrolled geometry for the inventory browser's category chooser. */
public record CategoryGridLayout(
        int columns,
        int rows,
        int scrollRow,
        int maximumScrollRow,
        int firstIndex,
        int visibleCount,
        int categoryCount
) {
    private static final int CELL_SIZE = InventoryScreenLayout.CELL_SIZE;

    public static CategoryGridLayout calculate(int width, int height, int categoryCount, int requestedScrollRow) {
        int columns = Math.max(1, width / CELL_SIZE);
        int rows = Math.max(1, height / CELL_SIZE);
        int count = Math.max(0, categoryCount);
        int totalRows = Math.ceilDiv(count, columns);
        int maximumScrollRow = Math.max(0, totalRows - rows);
        int scrollRow = clamp(requestedScrollRow, 0, maximumScrollRow);
        int firstIndex = scrollRow * columns;
        int visibleCount = Math.min(rows * columns, Math.max(0, count - firstIndex));
        return new CategoryGridLayout(columns, rows, scrollRow, maximumScrollRow,
                firstIndex, visibleCount, count);
    }

    public int indexAt(double localX, double localY) {
        if (localX < 0 || localY < 0) return -1;
        int column = (int)localX / CELL_SIZE;
        int row = (int)localY / CELL_SIZE;
        if (column >= columns || row >= rows) return -1;
        int index = firstIndex + row * columns + column;
        return index < categoryCount ? index : -1;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
