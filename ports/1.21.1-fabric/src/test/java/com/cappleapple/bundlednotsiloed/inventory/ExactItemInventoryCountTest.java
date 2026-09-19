package com.cappleapple.bundlednotsiloed.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExactItemInventoryCountTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void countsAllStacksOfOnlyTheExactIdentity() {
        ItemStack pristinePickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack damagedPickaxe = pristinePickaxe.copy();
        damagedPickaxe.setDamageValue(1);
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 512);
        inventory.loadNetworkSnapshot(List.of(
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.STONE, 12),
                pristinePickaxe,
                damagedPickaxe), 1);

        assertEquals(192, ExactItemInventoryCount.count(
                inventory, new ItemStack(Items.COBBLESTONE)));
        assertEquals(12, ExactItemInventoryCount.count(
                inventory, new ItemStack(Items.STONE)));
        assertEquals(0, ExactItemInventoryCount.count(
                inventory, new ItemStack(Items.DIRT)));
        assertEquals(1, ExactItemInventoryCount.count(inventory, pristinePickaxe));
        assertEquals(1, ExactItemInventoryCount.count(inventory, damagedPickaxe));
    }
}
