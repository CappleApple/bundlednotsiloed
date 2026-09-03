package com.cappleapple.bundlednotsiloed.compat;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Stable logical-slot view for integrations that support inventories beyond vanilla's 36 slots. */
public final class LogicalPlayerInventoryView {
    private LogicalPlayerInventoryView() {}

    public static int slotCount(Player player) {
        return slotCount(player.getData(ModAttachments.PLAYER_DATA));
    }

    public static ItemStack stackInSlot(Player player, int logicalSlot) {
        return stackInSlot(player.getData(ModAttachments.PLAYER_DATA), logicalSlot);
    }

    static int slotCount(PlayerInventoryData data) {
        return data.migratedVanillaInventory() ? data.inventory().syntheticSlotCount() : 0;
    }

    static ItemStack stackInSlot(PlayerInventoryData data, int logicalSlot) {
        if (logicalSlot < 0 || logicalSlot >= slotCount(data)) return ItemStack.EMPTY;
        return data.inventory().vanillaStackReference(logicalSlot);
    }
}
