package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CategoryGridLayoutTest {
    @Test
    void laysOutAndScrollsWholeRows() {
        CategoryGridLayout first = CategoryGridLayout.calculate(80, 60, 17, 0);
        assertEquals(4, first.columns());
        assertEquals(3, first.rows());
        assertEquals(2, first.maximumScrollRow());
        assertEquals(0, first.firstIndex());
        assertEquals(12, first.visibleCount());
        assertEquals(6, first.indexAt(45, 25));

        CategoryGridLayout last = CategoryGridLayout.calculate(80, 60, 17, 99);
        assertEquals(2, last.scrollRow());
        assertEquals(8, last.firstIndex());
        assertEquals(9, last.visibleCount());
        assertEquals(16, last.indexAt(5, 45));
        assertEquals(-1, last.indexAt(25, 45));
    }

    @Test
    void clampsEmptyAndUndersizedContent() {
        CategoryGridLayout empty = CategoryGridLayout.calculate(1, 1, 0, -4);
        assertEquals(1, empty.columns());
        assertEquals(1, empty.rows());
        assertEquals(0, empty.scrollRow());
        assertEquals(0, empty.visibleCount());
        assertEquals(-1, empty.indexAt(0, 0));
    }
}
