package com.cappleapple.bundlednotsiloed.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/** Positions compact integrated-control tooltips to the cursor's left. */
public final class InventoryButtonTooltipRenderer {
    private static final int CURSOR_GAP = 12;
    private static final int SCREEN_MARGIN = 4;
    private static final ClientTooltipPositioner LEFT_POSITIONER =
            InventoryButtonTooltipRenderer::leftPosition;

    private InventoryButtonTooltipRenderer() {}

    public static void render(
            GuiGraphics graphics,
            Font font,
            List<Component> tooltip,
            int mouseX,
            int mouseY,
            boolean keepDefaultPlacement
    ) {
        if (keepDefaultPlacement) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
        List<FormattedCharSequence> lines = tooltip.stream()
                .map(Component::getVisualOrderText)
                .toList();
        graphics.renderTooltip(font, lines, LEFT_POSITIONER, mouseX, mouseY);
    }

    static Vector2ic leftPosition(
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY,
            int tooltipWidth,
            int tooltipHeight
    ) {
        int x = Math.max(SCREEN_MARGIN, mouseX - CURSOR_GAP - tooltipWidth);
        int totalHeight = tooltipHeight + 3;
        int y = mouseY - CURSOR_GAP;
        if (y + totalHeight > screenHeight) y = screenHeight - totalHeight;
        return new Vector2i(x, Math.max(SCREEN_MARGIN, y));
    }
}
