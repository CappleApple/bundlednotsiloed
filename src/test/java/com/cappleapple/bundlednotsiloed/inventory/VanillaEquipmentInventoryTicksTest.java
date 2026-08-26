package com.cappleapple.bundlednotsiloed.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaEquipmentInventoryTicksTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void armorAndOffhandStacksKeepTheirVanillaPopAnimationTicks() {
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        ItemStack shield = new ItemStack(Items.SHIELD);
        helmet.setPopTime(3);
        shield.setPopTime(5);
        armor.set(3, helmet);
        offhand.set(0, shield);

        VanillaEquipmentInventoryTicks.tickCompartments(null, null, armor, offhand);

        assertEquals(2, helmet.getPopTime());
        assertEquals(4, shield.getPopTime());
    }
}
