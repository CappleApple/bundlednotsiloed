package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Vector2ic;
import org.junit.jupiter.api.Test;

class InventoryButtonTooltipRendererTest {
    @Test
    void placesCompactTooltipLeftOfCursorAndClampsToScreen() {
        Vector2ic normal = InventoryButtonTooltipRenderer.leftPosition(
                320, 240, 200, 100, 60, 20);
        assertEquals(128, normal.x());
        assertEquals(88, normal.y());

        Vector2ic corner = InventoryButtonTooltipRenderer.leftPosition(
                320, 240, 20, 238, 60, 20);
        assertEquals(4, corner.x());
        assertEquals(217, corner.y());
    }
}
