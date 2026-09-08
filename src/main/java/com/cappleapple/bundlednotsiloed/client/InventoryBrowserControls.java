package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.client.screen.CategoryIcons;
import com.cappleapple.bundlednotsiloed.data.InventorySlotWindow;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import com.cappleapple.stacksnotslots.api.LogicalInventoryEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared compact controls and category options for integrated inventory toolbars. */
public final class InventoryBrowserControls {
    public static final int SIZE = InventorySearchBar.HEIGHT;
    public static final int GAP = 3;
    public static final ResourceLocation DEFAULT_ALL_CATEGORY = BundledNotSiloed.id("all");

    private static final ResourceLocation BUTTON =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final float ICON_SCALE = 10.0F / 16.0F;

    private InventoryBrowserControls() {}

    public static void renderButton(
            GuiGraphics graphics,
            int x,
            int y,
            ItemStack icon,
            boolean highlighted
    ) {
        renderButtonBackground(graphics, x, y, highlighted);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1.5F, y + 1.5F, 100.0F);
        graphics.pose().scale(ICON_SCALE, ICON_SCALE, 1.0F);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
    }

    /** Descending rows and a down arrow make the one-shot sort action recognizable at button scale. */
    public static void renderSortButton(GuiGraphics graphics, int x, int y, boolean highlighted) {
        renderButtonBackground(graphics, x, y, highlighted);
        int color = 0xFF303030;
        graphics.fill(x + 2, y + 3, x + 8, y + 4, color);
        graphics.fill(x + 2, y + 6, x + 6, y + 7, color);
        graphics.fill(x + 2, y + 9, x + 4, y + 10, color);
        graphics.fill(x + 9, y + 3, x + 10, y + 9, color);
        graphics.fill(x + 8, y + 8, x + 11, y + 9, color);
        graphics.fill(x + 9, y + 9, x + 10, y + 10, color);
    }

    public static List<Component> sortTooltip(boolean detailed) {
        Component title = Component.translatable("gui.bundlednotsiloed.sort_inventory");
        return detailed ? List.of(title, Component.translatable("tooltip.bundlednotsiloed.sort_inventory"))
                : List.of(title);
    }

    public static void renderButtonBackground(
            GuiGraphics graphics, int x, int y, boolean highlighted
    ) {
        graphics.blitSprite(highlighted ? BUTTON_HIGHLIGHTED : BUTTON, x, y, SIZE, SIZE);
    }

    public static List<CategoryOption> categoryOptions(PlayerInventoryData data) {
        List<CategoryDefinition> enabled = distinctEnabledCategories(data.categories().categories());
        ArrayList<CategoryOption> options = new ArrayList<>();
        if (needsSyntheticAll(enabled)) {
            options.add(new CategoryOption(
                    null, new ItemStack(Items.CHEST), Component.translatable("gui.bundlednotsiloed.all")));
        }
        enabled.forEach(category -> options.add(new CategoryOption(
                category, CategoryIcons.displayStack(category), Component.literal(category.displayName()))));
        return List.copyOf(options);
    }

    static List<CategoryDefinition> distinctEnabledCategories(List<CategoryDefinition> categories) {
        Set<ResourceLocation> seen = new HashSet<>();
        return categories.stream()
                .filter(CategoryDefinition::enabled)
                .filter(category -> seen.add(category.id()))
                .toList();
    }

    static boolean needsSyntheticAll(List<CategoryDefinition> enabledCategories) {
        return enabledCategories.stream().noneMatch(category -> DEFAULT_ALL_CATEGORY.equals(category.id()));
    }

    public static boolean isSelected(CategoryOption option, ResourceLocation selectedCategory) {
        if (option.category() == null) return selectedCategory == null;
        return option.category().id().equals(selectedCategory)
                || selectedCategory == null && DEFAULT_ALL_CATEGORY.equals(option.category().id());
    }

    public static CategoryDefinition currentCategory(PlayerInventoryData data) {
        ResourceLocation selected = data.selectedCategoryPreference();
        if (selected == null) return null;
        CategoryDefinition category = data.categories().find(selected);
        return category != null && category.enabled() ? category : null;
    }

    public static ItemStack configuredIcon(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return new ItemStack(Items.BARRIER);
        return BuiltInRegistries.ITEM.getOptional(location).map(Item::getDefaultInstance)
                .filter(stack -> !stack.isEmpty()).orElseGet(() -> new ItemStack(Items.BARRIER));
    }

    public static Component sortName(SortMode mode) {
        return Component.translatable("sort.bundlednotsiloed." + mode.name().toLowerCase(Locale.ROOT));
    }

    public static List<ItemStack> pagePrototypes(List<LogicalInventoryEntry> entries, int scrollRow) {
        ArrayList<ItemStack> prototypes = new ArrayList<>(InventorySlotWindow.VISIBLE_SLOTS);
        int first = Math.max(0, scrollRow) * InventoryScreenLayout.GRID_COLUMNS;
        for (int offset = 0; offset < InventorySlotWindow.VISIBLE_SLOTS; offset++) {
            int index = first + offset;
            prototypes.add(index < entries.size()
                    ? entries.get(index).representative().copyWithCount(1) : ItemStack.EMPTY);
        }
        return List.copyOf(prototypes);
    }

    public static void applyWindow(
            PlayerInventoryData data,
            List<LogicalInventoryEntry> entries,
            int scrollRow
    ) {
        List<ItemStack> prototypes = pagePrototypes(entries, scrollRow);
        InventorySlotWindow requested = new InventorySlotWindow();
        requested.show(prototypes, data.inventory());
        ClientInventoryWindows.request(requested.logicalSlots(), true);
    }

    public static void applyRange(PlayerInventoryData data, int scrollRow) {
        int firstLogicalSlot = rangeStart(scrollRow);
        InventorySlotWindow requested = new InventorySlotWindow();
        requested.showRange(firstLogicalSlot);
        ClientInventoryWindows.request(requested.logicalSlots(), false);
    }

    static int rangeStart(int scrollRow) {
        long first = (long)InventorySlotWindow.MAIN_START
                + (long)Math.max(0, scrollRow) * InventoryScreenLayout.GRID_COLUMNS;
        if (first > Integer.MAX_VALUE - InventorySlotWindow.VISIBLE_SLOTS) {
            throw new IllegalArgumentException("Inventory scroll row is too large");
        }
        return (int)first;
    }

    public record CategoryOption(CategoryDefinition category, ItemStack icon, Component name) {}
}
