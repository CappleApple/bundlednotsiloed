package com.cappleapple.bundlednotsiloed.pickup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.stacksnotslots.api.CapacityAmount;
import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.CategoryRule;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.List;
import net.minecraft.SharedConstants;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PickupLimitCalculatorTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); com.cappleapple.bundlednotsiloed.ForgeTestBootstrap.initializeEventLists(); Bootstrap.bootStrap(); }

    @Test
    void overlappingLimitsUseMostRestrictiveAndSupportPartialPickup() {
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 1_000);
        inventory.insert(new ItemStack(Items.DIAMOND, 4), false);
        CategoryDefinition broad = category("broad", 10, false);
        CategoryDefinition restrictive = category("restrictive", 5, false);
        CategoryDefinition unlimited = category("unlimited", -1, false);
        assertEquals(1, PickupLimitCalculator.maximumAccepted(inventory, List.of(broad, restrictive, unlimited), new ItemStack(Items.DIAMOND, 32), 32));
    }

    @Test
    void exclusionsWinOverIncludes() {
        CategoryDefinition excluded = category("excluded", 5, true);
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 1_000);
        assertEquals(32, PickupLimitCalculator.maximumAccepted(inventory, List.of(excluded), new ItemStack(Items.DIAMOND, 32), 32));
    }

    @Test
    void categoryLimitsRetainFractionalUsageForLargeStacks() throws ReflectiveOperationException {
        java.lang.reflect.Field stackSize = net.minecraft.world.item.Item.class.getDeclaredField("maxStackSize");
        stackSize.setAccessible(true);
        int original = stackSize.getInt(Items.DIAMOND);
        stackSize.setInt(Items.DIAMOND, 128);
        try {
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        DynamicCapacityInventory inventory = new DynamicCapacityInventory(() -> 10);
        inventory.insert(diamond, false);
        CategoryDefinition category = new CategoryDefinition(new ResourceLocation("bundlednotsiloed", "large_stack"),
                "large_stack", new ResourceLocation("minecraft", "diamond"), 0, List.of(), List.of(), 1,
                SortMode.NAME_ASCENDING, true, true);

        assertEquals(CapacityAmount.fraction(1, 2),
                PickupLimitCalculator.usageForCategoryExact(inventory, category));
        assertEquals(1, PickupLimitCalculator.maximumAccepted(inventory, List.of(category), diamond.copyWithCount(4), 4));
        } finally {
            stackSize.setInt(Items.DIAMOND, original);
        }
    }

    private static CategoryDefinition category(String name, long limit, boolean excludeDiamond) {
        ResourceLocation diamond = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
        return new CategoryDefinition(new ResourceLocation("bundlednotsiloed", name), name, diamond, 0,
                List.of(new CategoryRule(CategoryRule.Type.ITEM, diamond)),
                excludeDiamond ? List.of(new CategoryRule(CategoryRule.Type.ITEM, diamond)) : List.of(), limit,
                SortMode.NAME_ASCENDING, true, false);
    }
}
