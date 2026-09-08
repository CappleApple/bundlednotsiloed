package com.cappleapple.bundlednotsiloed.client.screen;

import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.CategoryMatcher;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.client.CategoryGridLayout;
import com.cappleapple.bundlednotsiloed.client.BrowserSearchQuery;
import com.cappleapple.bundlednotsiloed.client.ClientKeyMappings;
import com.cappleapple.bundlednotsiloed.client.ClientTooltipSearchIndex;
import com.cappleapple.bundlednotsiloed.client.ExpandedMenuHoverPolicy;
import com.cappleapple.bundlednotsiloed.client.InventoryBrowserControls;
import com.cappleapple.bundlednotsiloed.client.InventoryBrowserControls.CategoryOption;
import com.cappleapple.bundlednotsiloed.client.InventoryButtonTooltipRenderer;
import com.cappleapple.bundlednotsiloed.client.InventoryCapacityPulse;
import com.cappleapple.bundlednotsiloed.client.InventoryCountFormatter;
import com.cappleapple.bundlednotsiloed.client.InventorySearchBar;
import com.cappleapple.bundlednotsiloed.client.InventorySideRail;
import com.cappleapple.bundlednotsiloed.client.InventorySideRail.Rail;
import com.cappleapple.bundlednotsiloed.client.InventoryScreenLayout;
import com.cappleapple.bundlednotsiloed.client.InventoryWindowUpdatePolicy;
import com.cappleapple.bundlednotsiloed.client.ItemSearchExpression;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.data.InventorySlotWindow;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.mixin.SlotAccessor;
import com.cappleapple.bundlednotsiloed.network.InventoryActionPayload;
import com.cappleapple.bundlednotsiloed.network.InventoryViewPreferencesPayload;
import com.cappleapple.bundlednotsiloed.network.StowMainGridPayload;
import com.cappleapple.stacksnotslots.api.LogicalInventoryEntry;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Minecraft's native inventory profile with the complete logical inventory integrated into its main grid. */
public final class BundledInventoryScreen extends InventoryScreen {
    private static final int MAX_QUERY = 96;
    private static final int CATEGORY_POPUP_ROWS = 4;
    private static final int CATEGORY_POPUP_HEIGHT = CATEGORY_POPUP_ROWS * InventoryScreenLayout.CELL_SIZE;
    private static final int SETTINGS_POPUP_X = InventoryScreenLayout.GRID_X - 1;
    private static final int SETTINGS_POPUP_Y = InventoryScreenLayout.GRID_Y - 1;
    private static final int SETTINGS_POPUP_WIDTH = 112;
    private static final int SETTINGS_ROW_HEIGHT = 18;
    private static final int SETTINGS_POPUP_HEIGHT = SETTINGS_ROW_HEIGHT * 3 + 2;
    private final Player player;
    private final BrowserSearchQuery search = new BrowserSearchQuery(MAX_QUERY);
    private final InventoryCapacityPulse capacityPulse = new InventoryCapacityPulse();
    private boolean searchFocused;
    private boolean searchHoverRefocusArmed = true;
    private double searchFocusMouseX;
    private double searchFocusMouseY;
    private boolean categoryMenuOpen;
    private boolean settingsMenuOpen;
    private boolean suppressNextCharacter;
    private boolean browserStateSent;
    private int entryScrollRow;
    private int categoryScrollRow;
    private UUID cachedPlayer;
    private long cachedRevision = -1;
    private long cachedCategoryRevision = -1;
    private String cachedQuery = "";
    private ResourceLocation cachedCategory;
    private SortMode cachedSort;
    private List<LogicalInventoryEntry> cachedEntries = List.of();
    private LogicalInventoryEntry hoveredEntry;
    private CategoryOption hoveredCategory;
    private List<Component> hoveredTooltip = List.of();
    private boolean identityWindow;
    private boolean windowDirty = true;
    private long appliedWindowRevision = -1;
    private int appliedWindowScrollRow = -1;
    private boolean appliedWindowIdentity;

    public BundledInventoryScreen(Player player) {
        super(player);
        this.player = player;
        this.imageHeight = InventoryScreenLayout.IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        restoreInventorySlotCoordinates();
        super.init();
        shiftInventorySlots();
        capacityPulse.reset();

        if (!browserStateSent && minecraft.getConnection() != null) {
            com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows.open();
            browserStateSent = true;
        }
        ensureInventoryWindow();
    }

    @Override
    public void removed() {
        search.clear();
        searchFocused = false;
        searchHoverRefocusArmed = true;
        restoreInventorySlotCoordinates();
        if (browserStateSent && minecraft.getConnection() != null) {
            com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows.close();
        }
        player.getData(ModAttachments.PLAYER_DATA).resetInventoryWindow();
        browserStateSent = false;
        super.removed();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(INVENTORY_LOCATION, x, y, 0, 0,
                InventoryScreenLayout.VANILLA_IMAGE_WIDTH, InventoryScreenLayout.VANILLA_IMAGE_HEIGHT);

        Rail rail = sideRail();
        rail.renderBackground(graphics);
        boolean searchHovered = rail.searchContains(mouseX, mouseY);
        InventorySearchBar.renderButton(graphics, rail.searchX(), rail.searchY(),
                InventorySearchBar.shouldHighlight(search.value(), searchFocused, searchHovered,
                        System.currentTimeMillis()));
        InventoryBrowserControls.renderButton(graphics, rail.categoryX(), rail.categoryY(),
                Screen.hasShiftDown() ? new ItemStack(Items.STICKY_PISTON)
                        : CategoryIcons.displayStack(currentCategory()),
                categoryMenuOpen || rail.categoryContains(mouseX, mouseY));
        InventoryBrowserControls.renderButton(graphics, rail.settingsX(), rail.settingsY(),
                InventoryBrowserControls.configuredIcon(ClientConfig.SETTINGS_ICON.get()),
                settingsMenuOpen || rail.settingsContains(mouseX, mouseY));
        InventoryBrowserControls.renderSortButton(graphics, rail.sortX(), rail.sortY(),
                rail.sortContains(mouseX, mouseY));
        capacityPulse.render(graphics,
                x + InventoryScreenLayout.GRID_X,
                y + InventoryScreenLayout.FULLNESS_Y,
                InventoryScreenLayout.GRID_COLUMNS * InventoryScreenLayout.CELL_SIZE,
                InventoryScreenLayout.FULLNESS_HEIGHT,
                player.getData(ModAttachments.PLAYER_DATA).inventory(),
                System.currentTimeMillis());

        renderEntityInInventoryFollowsMouse(graphics, x + 26, y + 8, x + 75, y + 78,
                30, 0.0625F, mouseX, mouseY, this.minecraft.player);
        renderEntries(graphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredEntry = null;
        hoveredCategory = null;
        hoveredTooltip = List.of();
        ensureInventoryWindow();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (blockedByRecipeBook()) return;

        if (searchFocused) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 425);
            InventorySideRail.renderExpandedSearch(graphics, font, sideRail(), search.value(),
                    search.allSelected(), ItemSearchExpression.parse(search.value()).valid(),
                    (System.currentTimeMillis() / 500L) % 2 == 0);
            graphics.pose().popPose();
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 450);
        if (categoryMenuOpen) renderCategoryMenu(graphics, mouseX, mouseY);
        else if (settingsMenuOpen) renderSettingsMenu(graphics, mouseX, mouseY);
        graphics.pose().popPose();

        resolveToolbarHover(mouseX, mouseY);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        if (hoveredCategory != null) {
            graphics.renderTooltip(font, hoveredCategory.name(), mouseX, mouseY);
        } else if (!hoveredTooltip.isEmpty()) {
            InventoryButtonTooltipRenderer.render(
                    graphics, font, hoveredTooltip, mouseX, mouseY, Screen.hasShiftDown());
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Rail rail = sideRail();
        boolean searchClicked = rail.searchContains(mouseX, mouseY);
        if (!searchClicked) clearSearchFocus();
        if (blockedByRecipeBook()) return super.mouseClicked(mouseX, mouseY, button);
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;

        if (searchClicked) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                updateSearch(search.clear());
                clearSearchFocus();
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                focusSearch(mouseX, mouseY);
                return true;
            }
        }

        if (rail.categoryContains(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(new StowMainGridPayload());
                categoryMenuOpen = false;
                settingsMenuOpen = false;
                entryScrollRow = 0;
                invalidateEntries();
                ensureInventoryWindow();
                return true;
            }
            categoryMenuOpen = !categoryMenuOpen;
            settingsMenuOpen = false;
            categoryScrollRow = 0;
            return true;
        }
        if (rail.sortContains(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                categoryMenuOpen = false;
                settingsMenuOpen = false;
                selectCategory(currentCategory());
            }
            return true;
        }
        if (rail.settingsContains(mouseX, mouseY)) {
            settingsMenuOpen = !settingsMenuOpen;
            categoryMenuOpen = false;
            return true;
        }

        if (categoryMenuOpen) {
            if (insideCategoryPopup(localX, localY)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    CategoryOption option = categoryOptionAt(localX, localY);
                    if (option != null) selectCategory(option.category());
                }
                categoryMenuOpen = false;
                return true;
            }
            categoryMenuOpen = false;
        }
        if (settingsMenuOpen) {
            if (insideSettingsPopup(localX, localY)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) clickSettingsRow((int)(localY - SETTINGS_POPUP_Y) / SETTINGS_ROW_HEIGHT);
                return true;
            }
            settingsMenuOpen = false;
        }

        int visibleEntryCount = visibleEntryCount();
        if (InventoryScreenLayout.maximumScrollRow(visibleEntryCount) > 0
                && InventoryScreenLayout.inside(localX, localY,
                InventoryScreenLayout.SCROLLBAR_X - 1, InventoryScreenLayout.SCROLLBAR_Y,
                InventoryScreenLayout.SCROLLBAR_WIDTH + 2, InventoryScreenLayout.SCROLLBAR_HEIGHT)) {
            entryScrollRow = InventoryScreenLayout.scrollRowForScrollbar(localY, visibleEntryCount);
            windowDirty = true;
            ensureInventoryWindow();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (blockedByRecipeBook()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        int direction = -(int)Math.signum(scrollY);
        if (direction == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        if (sideRail().categoryContains(mouseX, mouseY)) {
            changeCategory(direction);
            categoryMenuOpen = false;
            return true;
        }
        if (categoryMenuOpen && insideCategoryPopup(localX, localY)) {
            CategoryGridLayout layout = categoryMenuLayout();
            categoryScrollRow = clamp(categoryScrollRow + direction, 0, layout.maximumScrollRow());
            return true;
        }
        if (settingsMenuOpen && insideSettingsPopup(localX, localY)) {
            int row = (int)(localY - SETTINGS_POPUP_Y) / SETTINGS_ROW_HEIGHT;
            if (row == 1) {
                cycleSort(direction);
                return true;
            }
        }
        if (InventoryScreenLayout.inside(localX, localY,
                InventoryScreenLayout.GRID_X, InventoryScreenLayout.GRID_Y,
                InventoryScreenLayout.GRID_COLUMNS * InventoryScreenLayout.CELL_SIZE,
                InventoryScreenLayout.GRID_ROWS * InventoryScreenLayout.CELL_SIZE)) {
            entryScrollRow = clamp(entryScrollRow + direction, 0,
                    InventoryScreenLayout.maximumScrollRow(visibleEntryCount()));
            windowDirty = true;
            ensureInventoryWindow();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (categoryMenuOpen || settingsMenuOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                categoryMenuOpen = false;
                settingsMenuOpen = false;
                return true;
            }
        }
        if (hoveredEntry != null && minecraft.options.keyDrop.matches(keyCode, scanCode)) {
            PacketDistributor.sendToServer(new InventoryActionPayload(
                    Screen.hasControlDown() ? InventoryActionPayload.Action.DROP_STACK : InventoryActionPayload.Action.DROP_ONE,
                    hoveredEntry.representative()));
            return true;
        }
        InputConstants.Key pressed = InputConstants.getKey(keyCode, scanCode);
        if (!searchFocused && ClientKeyMappings.SEARCH_BROWSER.isActiveAndMatches(pressed)) {
            focusSearch(currentMouseX(), currentMouseY());
            suppressNextCharacter = true;
            return true;
        }
        if (searchFocused) return captureSearchKeyPressed(keyCode, scanCode);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean captureSearchKeyPressed(int keyCode, int scanCode) {
        if (!searchFocused) return false;
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            clearSearchFocus();
        } else if (keyCode == GLFW.GLFW_KEY_A && Screen.hasControlDown()) {
            search.selectAll();
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            updateSearch(search.backspace());
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            updateSearch(search.deleteSelection());
        }
        return true;
    }

    public boolean isSearchTyping() {
        return searchFocused;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (suppressNextCharacter) {
            suppressNextCharacter = false;
            return true;
        }
        if (searchFocused && search.append(codePoint)) {
            updateSearch(true);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        boolean searchHovered = sideRail().searchContains(mouseX, mouseY);
        if (!searchHovered) searchHoverRefocusArmed = true;
        if (InventorySearchBar.shouldRefocus(
                search.value(), searchFocused, searchHovered, searchHoverRefocusArmed)) {
            focusSearch(mouseX, mouseY);
            return;
        }
        if (InventorySearchBar.shouldReleaseFocus(searchFocused,
                mouseX != searchFocusMouseX || mouseY != searchFocusMouseY,
                searchHovered, isSearchBlockingTarget(mouseX, mouseY))) {
            clearSearchFocus();
        }
    }

    private boolean isSearchBlockingTarget(double mouseX, double mouseY) {
        Rail rail = sideRail();
        if (rail.categoryContains(mouseX, mouseY) || rail.settingsContains(mouseX, mouseY)
                || rail.sortContains(mouseX, mouseY)) {
            return true;
        }
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if ((categoryMenuOpen && insideCategoryPopup(localX, localY))
                || (settingsMenuOpen && insideSettingsPopup(localX, localY))) {
            return true;
        }
        if (InventoryScreenLayout.inside(localX, localY,
                InventoryScreenLayout.SCROLLBAR_X - 1, InventoryScreenLayout.SCROLLBAR_Y,
                InventoryScreenLayout.SCROLLBAR_WIDTH + 2, InventoryScreenLayout.SCROLLBAR_HEIGHT)
                || this.getChildAt(mouseX, mouseY).isPresent()) {
            return true;
        }
        for (Slot slot : this.menu.slots) {
            if (InventoryScreenLayout.inside(mouseX, mouseY,
                    this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18)) {
                return true;
            }
        }
        return false;
    }

    private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!identityWindow) {
            renderScrollbar(graphics, visibleEntryCount());
            return;
        }
        long capacity = player.getData(ModAttachments.PLAYER_DATA).inventory().capacity();
        Inventory playerInventory = player.getInventory();
        for (Slot slot : this.menu.slots) {
            int vanillaSlot = slot.getContainerSlot();
            if (slot.container != playerInventory || vanillaSlot < 9 || vanillaSlot >= 36) continue;
            ItemStack displayed = slot.getItem();
            if (displayed.isEmpty()) continue;
            LogicalInventoryEntry entry = aggregate(displayed);
            if (entry == null) continue;
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            if (!categoryMenuOpen && !settingsMenuOpen
                    && InventoryScreenLayout.inside(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                renderSlotHighlight(graphics, x, y, 0);
                hoveredEntry = entry;
            }
            graphics.renderItem(entry.representative(), x, y);
            String count = InventoryCountFormatter.item(entry, capacity);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 250);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(font, count,
                    (x + 17) * 2 - font.width(count), (y + 11) * 2, 0xFFFFFF, true);
            graphics.pose().popPose();
        }
        renderScrollbar(graphics, visibleEntryCount());
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        int vanillaSlot = slot.getContainerSlot();
        if (identityWindow && slot.container == player.getInventory()
                && vanillaSlot >= 9 && vanillaSlot < 36) return;
        super.renderSlot(graphics, slot);
    }

    private void renderScrollbar(GuiGraphics graphics, int entryCount) {
        if (InventoryScreenLayout.maximumScrollRow(entryCount) == 0) return;
        int x = this.leftPos + InventoryScreenLayout.SCROLLBAR_X;
        int y = this.topPos + InventoryScreenLayout.SCROLLBAR_Y;
        int thumbY = this.topPos + InventoryScreenLayout.scrollbarThumbY(entryCount, entryScrollRow);
        int thumbHeight = InventoryScreenLayout.scrollbarThumbHeight(entryCount);
        graphics.fill(x, y, x + InventoryScreenLayout.SCROLLBAR_WIDTH,
                y + InventoryScreenLayout.SCROLLBAR_HEIGHT, 0xFF373737);
        graphics.fill(x, thumbY, x + InventoryScreenLayout.SCROLLBAR_WIDTH,
                thumbY + thumbHeight, 0xFFFFFFFF);
        graphics.fill(x + 1, thumbY + 1, x + InventoryScreenLayout.SCROLLBAR_WIDTH,
                thumbY + thumbHeight, 0xFF8B8B8B);
    }

    private void renderCategoryMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int popupX = this.leftPos + InventoryScreenLayout.GRID_X - 1;
        int popupY = this.topPos + InventoryScreenLayout.GRID_Y - 1;
        int popupWidth = InventoryScreenLayout.GRID_COLUMNS * InventoryScreenLayout.CELL_SIZE + 2;
        graphics.fill(popupX, popupY, popupX + popupWidth, popupY + CATEGORY_POPUP_HEIGHT + 2, 0xFF373737);
        graphics.fill(popupX + 1, popupY + 1, popupX + popupWidth - 1, popupY + CATEGORY_POPUP_HEIGHT + 1, 0xFFC6C6C6);

        List<CategoryOption> options = categoryOptions();
        CategoryGridLayout layout = categoryMenuLayout();
        categoryScrollRow = layout.scrollRow();
        ResourceLocation selected = player.getData(ModAttachments.PLAYER_DATA).selectedCategoryPreference();
        for (int offset = 0; offset < layout.visibleCount(); offset++) {
            int optionIndex = layout.firstIndex() + offset;
            CategoryOption option = options.get(optionIndex);
            int x = popupX + 1 + offset % layout.columns() * InventoryScreenLayout.CELL_SIZE;
            int y = popupY + 1 + offset / layout.columns() * InventoryScreenLayout.CELL_SIZE;
            graphics.blit(INVENTORY_LOCATION, x, y, 7, 83,
                    InventoryScreenLayout.CELL_SIZE, InventoryScreenLayout.CELL_SIZE);
            boolean isSelected = InventoryBrowserControls.isSelected(option, selected);
            boolean hovered = InventoryScreenLayout.inside(mouseX, mouseY, x, y,
                    InventoryScreenLayout.CELL_SIZE, InventoryScreenLayout.CELL_SIZE);
            if (isSelected || hovered) graphics.fill(x + 1, y + 1, x + 17, y + 17,
                    hovered ? 0xCC4F72A5 : 0x99356DA5);
            graphics.renderItem(option.icon(), x + 1, y + 1);
            if (hovered) hoveredCategory = option;
        }
    }

    private void renderSettingsMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos + SETTINGS_POPUP_X;
        int y = this.topPos + SETTINGS_POPUP_Y;
        graphics.fill(x, y, x + SETTINGS_POPUP_WIDTH, y + SETTINGS_POPUP_HEIGHT, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SETTINGS_POPUP_WIDTH - 1, y + SETTINGS_POPUP_HEIGHT - 1, 0xFFC6C6C6);

        Component[] labels = {
                Component.translatable("gui.bundlednotsiloed.manage_tabs"),
                Component.translatable("gui.bundlednotsiloed.sort",
                        InventoryBrowserControls.sortName(currentSort())),
                Component.translatable("gui.bundlednotsiloed.settings")
        };
        ItemStack[] icons = {
                InventoryBrowserControls.configuredIcon(ClientConfig.MANAGE_TABS_ICON.get()),
                new ItemStack(Items.COMPARATOR),
                InventoryBrowserControls.configuredIcon(ClientConfig.SETTINGS_ICON.get())
        };
        for (int row = 0; row < labels.length; row++) {
            int rowY = y + 1 + row * SETTINGS_ROW_HEIGHT;
            boolean hovered = InventoryScreenLayout.inside(mouseX, mouseY,
                    x + 1, rowY, SETTINGS_POPUP_WIDTH - 2, SETTINGS_ROW_HEIGHT);
            if (hovered) graphics.fill(x + 1, rowY, x + SETTINGS_POPUP_WIDTH - 1,
                    rowY + SETTINGS_ROW_HEIGHT, 0xFF8B8B8B);
            graphics.renderItem(icons[row], x + 2, rowY + 1);
            String text = font.plainSubstrByWidth(labels[row].getString(), SETTINGS_POPUP_WIDTH - 24);
            graphics.drawString(font, text, x + 21, rowY + 5, 0x404040, false);
        }
    }

    private void resolveToolbarHover(int mouseX, int mouseY) {
        Rail rail = sideRail();
        if (rail.categoryContains(mouseX, mouseY)) {
            CategoryDefinition category = currentCategory();
            Component categoryName = category == null
                    ? Component.translatable("gui.bundlednotsiloed.all")
                    : Component.literal(category.displayName());
            hoveredTooltip = Screen.hasShiftDown()
                    ? List.of(categoryName, Component.translatable("gui.bundlednotsiloed.stow_main_grid_hint"))
                    : List.of(categoryName);
        } else if (rail.settingsContains(mouseX, mouseY)) {
            hoveredTooltip = List.of(Component.translatable("gui.bundlednotsiloed.inventory_menu"));
        } else if (rail.sortContains(mouseX, mouseY)) {
            hoveredTooltip = InventoryBrowserControls.sortTooltip(Screen.hasShiftDown());
        } else if (rail.searchContains(mouseX, mouseY)) {
            hoveredTooltip = InventorySearchBar.tooltip(
                    player.getData(ModAttachments.PLAYER_DATA).inventory(), Screen.hasShiftDown());
        }
    }

    private List<LogicalInventoryEntry> entries() {
        var data = player.getData(ModAttachments.PLAYER_DATA);
        ResourceLocation categoryId = data.selectedCategoryPreference();
        SortMode sortMode = data.inventorySortPreference();
        String value = search.value();
        if (player.getUUID().equals(cachedPlayer)
                && data.inventory().revision() == cachedRevision
                && data.categories().revision() == cachedCategoryRevision
                && value.equals(cachedQuery)
                && Objects.equals(categoryId, cachedCategory)
                && sortMode == cachedSort) return cachedEntries;

        cachedPlayer = player.getUUID();
        cachedRevision = data.inventory().revision();
        cachedCategoryRevision = data.categories().revision();
        cachedQuery = value;
        cachedCategory = categoryId;
        cachedSort = sortMode;
        CategoryDefinition category = currentCategory();
        boolean searching = !value.trim().isEmpty();
        ItemSearchExpression search = ItemSearchExpression.parse(value);
        ArrayList<LogicalInventoryEntry> values = new ArrayList<>();
        for (LogicalInventoryEntry entry : data.inventory().entriesAtOrAfter(9)) {
            if (!searching && category != null && !CategoryMatcher.matches(category, entry.representative())) continue;
            if (search.matches(entry.representative(), () -> ClientTooltipSearchIndex.text(entry.representative()))) {
                values.add(entry);
            }
        }
        cachedEntries = List.copyOf(values);
        entryScrollRow = Math.min(entryScrollRow, InventoryScreenLayout.maximumScrollRow(cachedEntries.size()));
        return cachedEntries;
    }

    private CategoryDefinition currentCategory() {
        return InventoryBrowserControls.currentCategory(player.getData(ModAttachments.PLAYER_DATA));
    }

    private SortMode currentSort() {
        return player.getData(ModAttachments.PLAYER_DATA).inventorySortPreference();
    }

    private List<CategoryOption> categoryOptions() {
        return InventoryBrowserControls.categoryOptions(player.getData(ModAttachments.PLAYER_DATA));
    }

    private void changeCategory(int direction) {
        List<CategoryOption> options = categoryOptions();
        if (options.isEmpty()) return;
        ResourceLocation selected = player.getData(ModAttachments.PLAYER_DATA).selectedCategoryPreference();
        int current = 0;
        for (int index = 0; index < options.size(); index++) {
            if (InventoryBrowserControls.isSelected(options.get(index), selected)) current = index;
        }
        selectCategory(options.get(Math.floorMod(current + direction, options.size())).category());
    }

    private void selectCategory(CategoryDefinition category) {
        var data = player.getData(ModAttachments.PLAYER_DATA);
        SortMode sort = category == null ? data.inventorySortPreference() : category.sortMode();
        ResourceLocation selected = category == null ? null : category.id();
        data.setSelectedCategoryPreference(selected);
        data.setInventorySortPreference(sort);
        PacketDistributor.sendToServer(new InventoryViewPreferencesPayload(sort, selected));
        entryScrollRow = 0;
        invalidateEntries();
        ensureInventoryWindow();
    }

    private void cycleSort(int direction) {
        var data = player.getData(ModAttachments.PLAYER_DATA);
        SortMode[] modes = SortMode.values();
        SortMode next = modes[Math.floorMod(data.inventorySortPreference().ordinal() + direction, modes.length)];
        data.setInventorySortPreference(next);
        PacketDistributor.sendToServer(new InventoryViewPreferencesPayload(next, data.selectedCategoryPreference()));
        entryScrollRow = 0;
        invalidateEntries();
        ensureInventoryWindow();
    }

    private void clickSettingsRow(int row) {
        settingsMenuOpen = false;
        switch (row) {
            case 0 -> minecraft.setScreen(new CategoryManagerScreen(this, player));
            case 1 -> {
                cycleSort(1);
                settingsMenuOpen = true;
            }
            case 2 -> minecraft.setScreen(new InventoryBrowserSettingsScreen(this));
            default -> { }
        }
    }

    private CategoryGridLayout categoryMenuLayout() {
        return CategoryGridLayout.calculate(
                InventoryScreenLayout.GRID_COLUMNS * InventoryScreenLayout.CELL_SIZE,
                CATEGORY_POPUP_HEIGHT,
                categoryOptions().size(),
                categoryScrollRow);
    }

    private CategoryOption categoryOptionAt(double localX, double localY) {
        CategoryGridLayout layout = categoryMenuLayout();
        int index = layout.indexAt(localX - (InventoryScreenLayout.GRID_X),
                localY - (InventoryScreenLayout.GRID_Y));
        List<CategoryOption> options = categoryOptions();
        return index >= 0 && index < options.size() ? options.get(index) : null;
    }

    private boolean insideCategoryPopup(double localX, double localY) {
        return InventoryScreenLayout.inside(localX, localY,
                InventoryScreenLayout.GRID_X - 1, InventoryScreenLayout.GRID_Y - 1,
                InventoryScreenLayout.GRID_COLUMNS * InventoryScreenLayout.CELL_SIZE + 2,
                CATEGORY_POPUP_HEIGHT + 2);
    }

    private static boolean insideSettingsPopup(double localX, double localY) {
        return InventoryScreenLayout.inside(localX, localY,
                SETTINGS_POPUP_X, SETTINGS_POPUP_Y, SETTINGS_POPUP_WIDTH, SETTINGS_POPUP_HEIGHT);
    }

    /** True when an expanded menu, rather than a covered native slot, owns this screen position. */
    public boolean expandedMenuCovers(double mouseX, double mouseY) {
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        return ExpandedMenuHoverPolicy.blocksUnderlyingSlot(
                categoryMenuOpen, insideCategoryPopup(localX, localY),
                settingsMenuOpen, insideSettingsPopup(localX, localY));
    }

    private void updateSearch(boolean changed) {
        if (!changed) return;
        entryScrollRow = 0;
        invalidateEntries();
    }

    private void focusSearch(double mouseX, double mouseY) {
        categoryMenuOpen = false;
        settingsMenuOpen = false;
        searchFocused = true;
        searchHoverRefocusArmed = false;
        search.clearSelection();
        searchFocusMouseX = mouseX;
        searchFocusMouseY = mouseY;
    }

    private void clearSearchFocus() {
        if (!searchFocused) return;
        searchFocused = false;
        searchHoverRefocusArmed = false;
        search.clearSelection();
    }

    private Rail sideRail() {
        return InventorySideRail.at(this.leftPos, this.topPos + InventoryScreenLayout.GRID_Y);
    }

    private double currentMouseX() {
        return minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth();
    }

    private double currentMouseY() {
        return minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight();
    }

    private void invalidateEntries() {
        cachedRevision = -1;
        windowDirty = true;
    }

    private boolean blockedByRecipeBook() {
        return this.width < 379 && this.getRecipeBookComponent().isVisible();
    }

    private void shiftInventorySlots() {
        Inventory inventory = player.getInventory();
        for (Slot slot : this.menu.slots) {
            if (slot.container != inventory) continue;
            int inventorySlot = slot.getContainerSlot();
            SlotAccessor accessor = (SlotAccessor)(Object)slot;
            if (inventorySlot >= 9 && inventorySlot < 36) {
                int mainIndex = inventorySlot - 9;
                accessor.bns$setX(InventoryScreenLayout.GRID_X + mainIndex % InventoryScreenLayout.GRID_COLUMNS
                        * InventoryScreenLayout.CELL_SIZE);
                accessor.bns$setY(InventoryScreenLayout.GRID_Y + mainIndex / InventoryScreenLayout.GRID_COLUMNS
                        * InventoryScreenLayout.CELL_SIZE);
            } else if (inventorySlot >= 0 && inventorySlot < 9) {
                accessor.bns$setX(InventoryScreenLayout.GRID_X + inventorySlot * InventoryScreenLayout.CELL_SIZE);
                accessor.bns$setY(InventoryScreenLayout.HOTBAR_Y);
            }
        }
    }

    private void restoreInventorySlotCoordinates() {
        Inventory inventory = player.getInventory();
        for (Slot slot : this.menu.slots) {
            if (slot.container != inventory) continue;
            int inventorySlot = slot.getContainerSlot();
            SlotAccessor accessor = (SlotAccessor)(Object)slot;
            if (inventorySlot >= 9 && inventorySlot < 36) {
                int mainIndex = inventorySlot - 9;
                accessor.bns$setX(InventoryScreenLayout.GRID_X + mainIndex % InventoryScreenLayout.GRID_COLUMNS
                        * InventoryScreenLayout.CELL_SIZE);
                accessor.bns$setY(84 + mainIndex / InventoryScreenLayout.GRID_COLUMNS
                        * InventoryScreenLayout.CELL_SIZE);
            } else if (inventorySlot >= 0 && inventorySlot < 9) {
                accessor.bns$setX(InventoryScreenLayout.GRID_X + inventorySlot * InventoryScreenLayout.CELL_SIZE);
                accessor.bns$setY(142);
            }
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private LogicalInventoryEntry aggregate(ItemStack stack) {
        return player.getData(ModAttachments.PLAYER_DATA).inventory().entriesAtOrAfter(9).stream()
                .filter(entry -> ItemStack.isSameItemSameComponents(entry.representative(), stack))
                .findFirst()
                .orElse(null);
    }

    private boolean usesIdentityView() {
        return !search.value().trim().isEmpty();
    }

    private int visibleEntryCount() {
        if (usesIdentityView()) return entries().size();
        int logicalMainSlots = player.getData(ModAttachments.PLAYER_DATA)
                .inventory().syntheticSlotCount() - InventorySlotWindow.MAIN_START;
        return Math.max(InventorySlotWindow.VISIBLE_SLOTS, logicalMainSlots);
    }

    private void ensureInventoryWindow() {
        if (!browserStateSent || minecraft.getConnection() == null) return;
        var data = player.getData(ModAttachments.PLAYER_DATA);
        if (!InventoryWindowUpdatePolicy.shouldApply(
                !this.menu.getCarried().isEmpty(), windowDirty, appliedWindowRevision >= 0)) return;
        boolean identityView = usesIdentityView();
        List<LogicalInventoryEntry> values = identityView ? entries() : List.of();
        int entryCount = identityView ? values.size() : visibleEntryCount();
        entryScrollRow = clamp(entryScrollRow, 0,
                InventoryScreenLayout.maximumScrollRow(entryCount));
        long revision = data.inventory().revision();
        if (!windowDirty && appliedWindowRevision == revision
                && appliedWindowScrollRow == entryScrollRow
                && appliedWindowIdentity == identityView) {
            identityWindow = identityView;
            return;
        }
        identityWindow = identityView;
        if (identityView) InventoryBrowserControls.applyWindow(data, values, entryScrollRow);
        else InventoryBrowserControls.applyRange(data, entryScrollRow);
        appliedWindowRevision = revision;
        appliedWindowScrollRow = entryScrollRow;
        appliedWindowIdentity = identityView;
        windowDirty = false;
    }
}
