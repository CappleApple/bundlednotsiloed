package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.stacksnotslots.api.CapacityAmount;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared search-button icon, inventory-capacity tooltip, and continuous fullness colors. */
public final class InventorySearchBar {
    public static final int HEIGHT = 13;

    static final int EMPTY_COLOR = 0xFF71866B;
    static final int MIDDLE_COLOR = 0xFFC18445;
    static final int FULL_COLOR = 0xFFB34B4B;

    private static final ResourceLocation SEARCH_ICON =
            BundledNotSiloed.id("textures/gui/search_button.png");
    private static final int ICON_TEXTURE_SIZE = 48;
    private static final int ICON_SIZE = 9;

    private InventorySearchBar() {}

    public static void renderButton(GuiGraphics graphics, int x, int y, boolean highlighted) {
        InventoryBrowserControls.renderButtonBackground(graphics, x, y, highlighted);
        graphics.blit(SEARCH_ICON, x + 2, y + 2, ICON_SIZE, ICON_SIZE,
                8, 8, 32, 32,
                ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
    }

    public static boolean shouldHighlight(
            String query, boolean focused, boolean hovered, long currentTimeMillis
    ) {
        return focused || hovered || (!query.trim().isEmpty()
                && (currentTimeMillis / 500L) % 2L == 0L);
    }

    public static boolean shouldRefocus(
            String query, boolean focused, boolean searchHovered, boolean hoverRefocusArmed
    ) {
        return hoverRefocusArmed && !focused && searchHovered && !query.trim().isEmpty();
    }

    public static boolean shouldReleaseFocus(
            boolean focused,
            boolean pointerMoved,
            boolean searchHovered,
            boolean interactiveTargetHovered
    ) {
        return focused && pointerMoved && !searchHovered && interactiveTargetHovered;
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

    static double fillRatio(CapacityAmount used, long capacity) {
        if (capacity <= 0) return used.isZero() ? 0.0 : 1.0;
        return Math.max(0.0, Math.min(1.0, used.doubleValue() / capacity));
    }

    static int fillColor(double ratio) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        return clamped <= 0.5
                ? interpolate(EMPTY_COLOR, MIDDLE_COLOR, clamped * 2.0)
                : interpolate(MIDDLE_COLOR, FULL_COLOR, (clamped - 0.5) * 2.0);
    }

    private static int interpolate(int from, int to, double amount) {
        int red = interpolateChannel(from >> 16, to >> 16, amount);
        int green = interpolateChannel(from >> 8, to >> 8, amount);
        int blue = interpolateChannel(from, to, amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int interpolateChannel(int from, int to, double amount) {
        return (int)Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
    }
}
