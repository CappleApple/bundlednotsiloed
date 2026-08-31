package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.stacksnotslots.api.CapacityAmount;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared search-field texture, capacity fill, and hover text for every integrated inventory UI. */
public final class InventorySearchBar {
    public static final int WIDTH = 91;
    public static final int HEIGHT = 13;
    public static final int INNER_X = 2;
    public static final int INNER_Y = 2;
    public static final int INNER_WIDTH = 88;
    public static final int INNER_HEIGHT = 10;

    static final int LOW_COLOR = 0xFF71866B;
    static final int MEDIUM_COLOR = 0xFF92734F;
    static final int HIGH_COLOR = 0xFF8E5959;

    private static final ResourceLocation TEXTURE =
            BundledNotSiloed.id("textures/gui/inventory_search.png");

    private InventorySearchBar() {}

    public static void render(GuiGraphics graphics, int x, int y, DynamicCapacityInventory inventory) {
        graphics.blit(TEXTURE, x, y, WIDTH, HEIGHT, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        int fillWidth = fillWidth(inventory.exactUsedCapacity(), inventory.capacity());
        if (fillWidth > 0) {
            graphics.fill(x + INNER_X, y + INNER_Y,
                    x + INNER_X + fillWidth, y + INNER_Y + INNER_HEIGHT,
                    fillColor(fillRatio(inventory.exactUsedCapacity(), inventory.capacity())));
        }
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y) {
        return InventoryScreenLayout.inside(mouseX, mouseY, x, y, WIDTH, HEIGHT);
    }

    public static List<Component> tooltip(DynamicCapacityInventory inventory, boolean detailed) {
        if (detailed) {
            return List.of(
                    Component.translatable("tooltip.bundlednotsiloed.browser_search.line_1"),
                    Component.translatable("tooltip.bundlednotsiloed.browser_search.line_2"),
                    Component.translatable("tooltip.bundlednotsiloed.browser_search.line_3"));
        }
        return List.of(Component.literal(InventoryCountFormatter.overall(
                inventory.exactUsedCapacity(), inventory.capacity(), ClientConfig.OverallCountMode.STACKS)));
    }

    static int fillWidth(CapacityAmount used, long capacity) {
        double ratio = fillRatio(used, capacity);
        if (ratio <= 0.0) return 0;
        return Math.max(1, (int)Math.round(INNER_WIDTH * ratio));
    }

    static double fillRatio(CapacityAmount used, long capacity) {
        if (capacity <= 0) return used.isZero() ? 0.0 : 1.0;
        return Math.max(0.0, Math.min(1.0, used.doubleValue() / capacity));
    }

    static int fillColor(double ratio) {
        if (ratio < 0.5) return LOW_COLOR;
        if (ratio < 0.8) return MEDIUM_COLOR;
        return HIGH_COLOR;
    }
}
