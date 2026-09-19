package com.cappleapple.bundlednotsiloed.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.stacksnotslots.api.LogicalInventoryEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class InventoryBrowserControlsTest {
    @Test
    void syntheticAllIsOnlyAddedWhenTheRealPresetIsUnavailable() {
        CategoryDefinition all = category(InventoryBrowserControls.DEFAULT_ALL_CATEGORY);
        CategoryDefinition blocks = category(ResourceLocation.withDefaultNamespace("blocks"));

        assertFalse(InventoryBrowserControls.needsSyntheticAll(List.of(all, blocks)));
        assertTrue(InventoryBrowserControls.needsSyntheticAll(List.of(blocks)));
        assertTrue(InventoryBrowserControls.needsSyntheticAll(List.of()));

        InventoryBrowserControls.CategoryOption actualAll =
                new InventoryBrowserControls.CategoryOption(all, null, null);
        assertTrue(InventoryBrowserControls.isSelected(actualAll, null));
        assertTrue(InventoryBrowserControls.isSelected(
                actualAll, InventoryBrowserControls.DEFAULT_ALL_CATEGORY));
        assertFalse(InventoryBrowserControls.isSelected(actualAll, blocks.id()));
    }

    @Test
    void duplicateRealAllCategoriesAreCollapsedBeforeDisplayAndWheelCycling() {
        CategoryDefinition all = category(InventoryBrowserControls.DEFAULT_ALL_CATEGORY);

        List<CategoryDefinition> distinct =
                InventoryBrowserControls.distinctEnabledCategories(List.of(all, all));

        assertEquals(List.of(all), distinct);
        assertFalse(InventoryBrowserControls.needsSyntheticAll(distinct));
    }

    @Test
    void pagePrototypeWindowScrollsByRowsAndPadsToTwentySevenRealSlots() {
        ArrayList<LogicalInventoryEntry> entries = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            entries.add(new LogicalInventoryEntry(new ItemStack(Items.STONE), 1, 1));
        }
        entries.add(new LogicalInventoryEntry(new ItemStack(Items.DIAMOND), 4, 1));

        List<ItemStack> page = InventoryBrowserControls.pagePrototypes(entries, 1);

        assertEquals(27, page.size());
        assertTrue(page.getFirst().is(Items.DIAMOND));
        assertEquals(1, page.getFirst().getCount());
        assertTrue(page.get(1).isEmpty());
        assertTrue(page.getLast().isEmpty());
    }

    @Test
    void directRangeStartsWithTheVanillaGridAndExtendsByNinePerRow() {
        assertEquals(9, InventoryBrowserControls.rangeStart(0));
        assertEquals(18, InventoryBrowserControls.rangeStart(1));
        assertEquals(27, InventoryBrowserControls.rangeStart(2));
    }

    private static CategoryDefinition category(ResourceLocation id) {
        return new CategoryDefinition(id, id.getPath(), ResourceLocation.withDefaultNamespace("chest"),
                0, List.of(), List.of(), -1, SortMode.NAME_ASCENDING, true, true);
    }
}
