package com.cappleapple.bundlednotsiloed.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import java.util.ArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LogicalPlayerInventoryViewTest {
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void exposesStableLogicalIndicesIndependentlyOfTheVisibleWindow() {
        PlayerInventoryData data = new PlayerInventoryData(null);
        ArrayList<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot <= 42; slot++) stacks.add(ItemStack.EMPTY);
        ItemStack backpackStandIn = new ItemStack(Items.CHEST);
        stacks.set(42, backpackStandIn);
        data.inventory().loadNetworkSnapshot(stacks, 1);

        assertEquals(0, LogicalPlayerInventoryView.slotCount(data));
        data.setMigratedVanillaInventory();
        ItemStack stableReference = LogicalPlayerInventoryView.stackInSlot(data, 42);
        data.showInventoryRange(18);

        assertEquals(43, LogicalPlayerInventoryView.slotCount(data));
        assertSame(stableReference, LogicalPlayerInventoryView.stackInSlot(data, 42));
        assertTrue(LogicalPlayerInventoryView.stackInSlot(data, 42).is(Items.CHEST));
        assertTrue(LogicalPlayerInventoryView.stackInSlot(data, 43).isEmpty());
    }
}
