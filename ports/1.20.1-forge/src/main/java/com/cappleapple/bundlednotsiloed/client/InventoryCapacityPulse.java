package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.stacksnotslots.api.CapacityAmount;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import net.minecraft.client.gui.GuiGraphics;

/** Briefly reveals a thin capacity bar when the inventory's used capacity changes. */
public final class InventoryCapacityPulse {
    static final long HOLD_MILLIS = 1_500L;
    static final long FADE_MILLIS = 500L;

    private CapacityAmount lastUsed;
    private long changedAt = Long.MIN_VALUE;

    public void reset() {
        lastUsed = null;
        changedAt = Long.MIN_VALUE;
    }

    boolean observe(CapacityAmount used, long now) {
        if (lastUsed == null) {
            lastUsed = used;
            return false;
        }
        if (lastUsed.equals(used)) return false;
        lastUsed = used;
        changedAt = now;
        return true;
    }

    int opacity(long now) {
        if (changedAt == Long.MIN_VALUE || now < changedAt) return 0;
        long elapsed = now - changedAt;
        if (elapsed <= HOLD_MILLIS) return 255;
        if (elapsed >= HOLD_MILLIS + FADE_MILLIS) return 0;
        return (int)Math.round(255.0 * (HOLD_MILLIS + FADE_MILLIS - elapsed) / FADE_MILLIS);
    }

    public void render(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            DynamicCapacityInventory inventory,
            long now
    ) {
        CapacityAmount used = inventory.exactUsedCapacity();
        observe(used, now);
        int opacity = opacity(now);
        if (opacity <= 0 || width <= 0 || height <= 0) return;

        double ratio = InventorySearchBar.fillRatio(used, inventory.capacity());
        int fillWidth = ratio <= 0.0 ? 0 : Math.max(1, (int)Math.round(width * ratio));
        graphics.fill(x, y, x + width, y + height, withAlpha(0x202020, opacity / 2));
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height,
                    withAlpha(InventorySearchBar.fillColor(ratio), opacity));
        }
    }

    private static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }
}
