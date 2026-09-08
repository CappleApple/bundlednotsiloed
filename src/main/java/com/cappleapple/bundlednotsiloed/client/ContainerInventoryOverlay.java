package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.CategoryMatcher;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.client.InventoryBrowserControls.CategoryOption;
import com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen;
import com.cappleapple.bundlednotsiloed.client.screen.CategoryIcons;
import com.cappleapple.bundlednotsiloed.client.screen.CategoryManagerScreen;
import com.cappleapple.bundlednotsiloed.client.screen.InventoryBrowserSettingsScreen;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.data.InventorySlotWindow;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.BrowserTransferPayload;
import com.cappleapple.bundlednotsiloed.network.InventoryActionPayload;
import com.cappleapple.bundlednotsiloed.network.InventoryViewPreferencesPayload;
import com.cappleapple.bundlednotsiloed.network.StowMainGridPayload;
import com.cappleapple.stacksnotslots.api.LogicalInventoryEntry;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Replaces a recognized vanilla-style 27-slot player section in any container screen with the
 * combined logical inventory. The container's own slots, texture, and input remain untouched.
 */
public final class ContainerInventoryOverlay {
    private static final int MAX_QUERY = 96;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int CATEGORY_POPUP_ROWS = 4;
    private static final int CATEGORY_POPUP_HEIGHT =
            CATEGORY_POPUP_ROWS * InventoryScreenLayout.CELL_SIZE;
    private static final int SETTINGS_POPUP_WIDTH = 112;
    private static final int SETTINGS_ROW_HEIGHT = 18;
    private static final int SETTINGS_POPUP_HEIGHT = SETTINGS_ROW_HEIGHT * 3 + 2;
    private static final ResourceLocation INVENTORY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final BrowserSearchQuery SEARCH = new BrowserSearchQuery(MAX_QUERY);
    private static final InventoryCapacityPulse CAPACITY_PULSE = new InventoryCapacityPulse();

    private static AbstractContainerScreen<?> activeScreen;
    private static PlayerGrid activeGrid;
    private static boolean searchFocused;
    private static boolean searchHoverRefocusArmed = true;
    private static double searchFocusMouseX;
    private static double searchFocusMouseY;
    private static boolean categoryMenuOpen;
    private static boolean settingsMenuOpen;
    private static int suppressedTypedKey = GLFW.GLFW_KEY_UNKNOWN;
    private static int consumedReleaseButton = -1;
    private static int scrollRow;
    private static int categoryScrollRow;
    private static UUID cachedPlayer;
    private static long cachedRevision = -1;
    private static long cachedCategoryRevision = -1;
    private static String cachedQuery = "";
    private static ResourceLocation cachedCategory;
    private static SortMode cachedSort;
    private static List<LogicalInventoryEntry> cachedEntries = List.of();
    private static LogicalInventoryEntry hoveredEntry;
    private static CategoryOption hoveredCategory;
    private static List<Component> hoveredTooltip = List.of();
    private static boolean identityWindow;
    private static boolean windowDirty = true;
    private static long appliedWindowRevision = -1;
    private static int appliedWindowScrollRow = -1;
    private static boolean appliedWindowIdentity;

    private ContainerInventoryOverlay() {}

    public static void initialize(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !supports(screen)) {
            restoreActiveScreen();
            return;
        }
        prepare(screen);
    }

    public static void close(Screen screen) {
        if (screen == activeScreen) restoreActiveScreen();
    }

    public static void render(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!supports(screen)) return;
        prepare(screen);
        if (activeGrid == null || Minecraft.getInstance().player == null) return;
        ensureInventoryWindow(false);

        hoveredEntry = null;
        hoveredCategory = null;
        hoveredTooltip = List.of();
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        // This event inherits the container's GUI-local pose while the overlay uses screen coordinates.
        // Keep the controls below vanilla's later tooltip and cursor-held stack passes.
        graphics.pose().translate(-screen.getGuiLeft(), -screen.getGuiTop(), 100);
        renderEntries(graphics, event.getMouseX(), event.getMouseY());
        renderToolbar(graphics, event.getMouseX(), event.getMouseY());
        graphics.pose().popPose();

    }

    public static void renderTooltip(ScreenEvent.Render.Post event) {
        if (event.getScreen() != activeScreen || activeGrid == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 900);
        if (categoryMenuOpen) renderCategoryMenu(graphics, event.getMouseX(), event.getMouseY());
        else if (settingsMenuOpen) renderSettingsMenu(graphics, event.getMouseX(), event.getMouseY());
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        if (hoveredCategory != null) {
            graphics.renderTooltip(Minecraft.getInstance().font, hoveredCategory.name(),
                    event.getMouseX(), event.getMouseY());
        } else if (!categoryMenuOpen && !settingsMenuOpen && !hoveredTooltip.isEmpty()) {
            InventoryButtonTooltipRenderer.render(graphics, Minecraft.getInstance().font,
                    hoveredTooltip, event.getMouseX(), event.getMouseY(), Screen.hasShiftDown());
        }
        graphics.pose().popPose();
    }

    public static boolean mouseButton(int button, int action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen) || !supports(screen)
                || minecraft.player == null) return false;
        prepare(screen);
        if (activeGrid == null) return false;
        if (action == GLFW.GLFW_RELEASE) {
            if (button != consumedReleaseButton) return false;
            consumedReleaseButton = -1;
            return true;
        }
        if (action != GLFW.GLFW_PRESS) return false;

        double mouseX = scaledMouseX(minecraft);
        double mouseY = scaledMouseY(minecraft);

        if (activeGrid.searchContains(mouseX, mouseY)) {
            consumedReleaseButton = button;
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                updateSearch(SEARCH.clear());
                clearSearchFocus();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                focusSearch(mouseX, mouseY);
            }
            return true;
        }

        if (activeGrid.categoryContains(mouseX, mouseY)) {
            consumedReleaseButton = button;
            clearSearchFocus();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(new StowMainGridPayload());
                categoryMenuOpen = false;
                settingsMenuOpen = false;
                scrollRow = 0;
                invalidateEntries();
                ensureInventoryWindow(true);
                return true;
            }
            categoryMenuOpen = !categoryMenuOpen;
            settingsMenuOpen = false;
            categoryScrollRow = 0;
            return true;
        }
        if (activeGrid.sortContains(mouseX, mouseY)) {
            consumedReleaseButton = button;
            clearSearchFocus();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                categoryMenuOpen = false;
                settingsMenuOpen = false;
                selectCategory(InventoryBrowserControls.currentCategory(
                        minecraft.player.getData(ModAttachments.PLAYER_DATA)));
            }
            return true;
        }
        if (activeGrid.settingsContains(mouseX, mouseY)) {
            consumedReleaseButton = button;
            clearSearchFocus();
            settingsMenuOpen = !settingsMenuOpen;
            categoryMenuOpen = false;
            return true;
        }

        if (categoryMenuOpen) {
            if (activeGrid.categoryPopupContains(mouseX, mouseY)) {
                consumedReleaseButton = button;
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    CategoryOption option = categoryOptionAt(mouseX, mouseY);
                    if (option != null) selectCategory(option.category());
                }
                categoryMenuOpen = false;
                return true;
            }
            categoryMenuOpen = false;
        }
        if (settingsMenuOpen) {
            if (activeGrid.settingsPopupContains(mouseX, mouseY)) {
                consumedReleaseButton = button;
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    clickSettingsRow(screen,
                            (int)(mouseY - activeGrid.settingsPopupY()) / SETTINGS_ROW_HEIGHT);
                }
                return true;
            }
            settingsMenuOpen = false;
        }

        clearSearchFocus();
        int visibleEntryCount = visibleEntryCount();
        if (maximumScrollRow(visibleEntryCount) > 0 && activeGrid.scrollbarContains(mouseX, mouseY)) {
            consumedReleaseButton = button;
            scrollRow = scrollRowForMouse(mouseY, visibleEntryCount);
            windowDirty = true;
            ensureInventoryWindow(true);
            return true;
        }

        int cell = activeGrid.cellAt(mouseX, mouseY);
        if (cell < 0 || (button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return false;
        if (Screen.hasShiftDown() && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && screen.getMenu().getCarried().isEmpty()) {
            ItemStack source = activeGrid.cells().get(cell).slot().getItem();
            if (source.isEmpty()) return false;
            if (ClientInventoryWindows.pending()) return true;
            consumedReleaseButton = button;
            PacketDistributor.sendToServer(new BrowserTransferPayload(
                    source, BrowserTransferPayload.Mode.MAXIMUM));
            return true;
        }
        return false;
    }

    public static boolean mouseScrolled(double deltaY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen) || !supports(screen)) return false;
        prepare(screen);
        if (activeGrid == null) return false;
        double mouseX = scaledMouseX(minecraft);
        double mouseY = scaledMouseY(minecraft);
        int direction = -(int)Math.signum(deltaY);
        if (direction == 0) return false;
        if (activeGrid.categoryContains(mouseX, mouseY)) {
            changeCategory(direction);
            categoryMenuOpen = false;
            return true;
        }
        if (categoryMenuOpen && activeGrid.categoryPopupContains(mouseX, mouseY)) {
            CategoryGridLayout layout = categoryMenuLayout();
            categoryScrollRow = clamp(categoryScrollRow + direction, 0, layout.maximumScrollRow());
            return true;
        }
        if (settingsMenuOpen && activeGrid.settingsPopupContains(mouseX, mouseY)) {
            int row = (int)(mouseY - activeGrid.settingsPopupY()) / SETTINGS_ROW_HEIGHT;
            if (row == 1) {
                cycleSort(direction);
                return true;
            }
        }
        if (!activeGrid.gridContains(mouseX, mouseY) && !activeGrid.scrollbarContains(mouseX, mouseY)) return false;
        scrollRow = clamp(scrollRow + direction, 0, maximumScrollRow(visibleEntryCount()));
        windowDirty = true;
        ensureInventoryWindow(true);
        return true;
    }

    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() != activeScreen || activeGrid == null) return;
        if ((categoryMenuOpen || settingsMenuOpen) && event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
            categoryMenuOpen = false;
            settingsMenuOpen = false;
            event.setCanceled(true);
            return;
        }
        if (!searchFocused && hoveredEntry != null
                && Minecraft.getInstance().options.keyDrop.matches(event.getKeyCode(), event.getScanCode())) {
            PacketDistributor.sendToServer(new InventoryActionPayload(
                    Screen.hasControlDown() ? InventoryActionPayload.Action.DROP_STACK
                            : InventoryActionPayload.Action.DROP_ONE,
                    hoveredEntry.representative()));
            event.setCanceled(true);
            return;
        }
        InputConstants.Key pressed = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (!searchFocused && ClientKeyMappings.SEARCH_BROWSER.isActiveAndMatches(pressed)) {
            suppressedTypedKey = event.getKeyCode();
            Minecraft minecraft = Minecraft.getInstance();
            focusSearch(scaledMouseX(minecraft), scaledMouseY(minecraft));
            event.setCanceled(true);
            return;
        }
        if (!searchFocused) return;
        if (Minecraft.getInstance().options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
            return;
        }
        if (event.getKeyCode() == GLFW.GLFW_KEY_A && Screen.hasControlDown()) {
            SEARCH.selectAll();
            event.setCanceled(true);
            return;
        }
        switch (event.getKeyCode()) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                clearSearchFocus();
            }
            case GLFW.GLFW_KEY_BACKSPACE -> updateSearch(SEARCH.backspace());
            case GLFW.GLFW_KEY_DELETE -> updateSearch(SEARCH.deleteSelection());
            default -> { }
        }
        event.setCanceled(true);
    }

    public static boolean isSearchTyping(Screen screen) {
        return searchFocused && screen == activeScreen;
    }

    public static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (event.getKeyCode() == suppressedTypedKey) suppressedTypedKey = GLFW.GLFW_KEY_UNKNOWN;
    }

    public static void characterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() != activeScreen || !searchFocused) return;
        if (suppressedTypedKey != GLFW.GLFW_KEY_UNKNOWN) {
            suppressedTypedKey = GLFW.GLFW_KEY_UNKNOWN;
            event.setCanceled(true);
            return;
        }
        if (SEARCH.append(event.getCodePoint())) {
            updateSearch(true);
            event.setCanceled(true);
        }
    }

    private static void prepare(AbstractContainerScreen<?> screen) {
        if (screen == activeScreen) return;
        restoreActiveScreen();
        PlayerGrid grid = findPlayerGrid(screen);
        if (grid == null) return;
        activeScreen = screen;
        activeGrid = grid;
        com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows.open();
        scrollRow = 0;
        categoryScrollRow = 0;
        categoryMenuOpen = false;
        settingsMenuOpen = false;
        identityWindow = false;
        CAPACITY_PULSE.reset();
        invalidateEntries();
        ensureInventoryWindow(true);
    }

    private static void restoreActiveScreen() {
        if (activeScreen == null) return;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA).resetInventoryWindow();
        }
        if (Minecraft.getInstance().getConnection() != null) {
            com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows.close();
        }
        activeScreen = null;
        activeGrid = null;
        searchFocused = false;
        searchHoverRefocusArmed = true;
        SEARCH.clear();
        CAPACITY_PULSE.reset();
        categoryMenuOpen = false;
        settingsMenuOpen = false;
        hoveredCategory = null;
        hoveredTooltip = List.of();
        consumedReleaseButton = -1;
        scrollRow = 0;
        cachedQuery = "";
        cachedRevision = -1;
        appliedWindowRevision = -1;
        appliedWindowScrollRow = -1;
        appliedWindowIdentity = false;
        identityWindow = false;
        windowDirty = true;
    }

    private static PlayerGrid findPlayerGrid(AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return null;
        Inventory inventory = minecraft.player.getInventory();
        ArrayList<Cell> cells = new ArrayList<>();
        boolean[] seen = new boolean[36];
        for (Slot slot : screen.getMenu().slots) {
            int inventorySlot = ContainerPlayerSlotResolver.resolve(slot, inventory);
            if (inventorySlot < 9 || inventorySlot >= 36) continue;
            if (seen[inventorySlot]) return null;
            seen[inventorySlot] = true;
            cells.add(new Cell(slot, slot.x, slot.y));
        }
        if (cells.size() != InventoryScreenLayout.VISIBLE_ENTRIES) return null;
        cells.sort(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::x));
        List<Integer> rows = cells.stream().map(Cell::y).distinct().toList();
        if (rows.size() != InventoryScreenLayout.GRID_ROWS
                || rows.stream().anyMatch(y -> cells.stream().filter(cell -> cell.y() == y).count()
                != InventoryScreenLayout.GRID_COLUMNS)) return null;
        return new PlayerGrid(List.copyOf(cells), screen.getGuiLeft(), screen.getGuiTop());
    }

    private static boolean supports(AbstractContainerScreen<?> screen) {
        return !(screen instanceof BundledInventoryScreen)
                && !(screen instanceof CreativeModeInventoryScreen)
                && Minecraft.getInstance().player != null;
    }

    public static boolean replacesSlot(AbstractContainerScreen<?> screen, Slot slot) {
        return identityWindow && screen == activeScreen && activeGrid != null
                && activeGrid.cells().stream().anyMatch(cell -> cell.slot() == slot);
    }

    /** True when an expanded integrated menu owns this position instead of a covered native slot. */
    public static boolean expandedMenuCovers(
            AbstractContainerScreen<?> screen, double mouseX, double mouseY
    ) {
        if (screen != activeScreen || activeGrid == null) return false;
        return ExpandedMenuHoverPolicy.blocksUnderlyingSlot(
                categoryMenuOpen, activeGrid.categoryPopupContains(mouseX, mouseY),
                settingsMenuOpen, activeGrid.settingsPopupContains(mouseX, mouseY));
    }

    private static void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        var data = minecraft.player.getData(ModAttachments.PLAYER_DATA);
        InventorySideRail.Rail rail = activeGrid.rail();
        rail.renderBackground(graphics);
        boolean searchHovered = rail.searchContains(mouseX, mouseY);
        if (!searchHovered) searchHoverRefocusArmed = true;
        if (InventorySearchBar.shouldRefocus(
                SEARCH.value(), searchFocused, searchHovered, searchHoverRefocusArmed)) {
            focusSearch(mouseX, mouseY);
        } else if (InventorySearchBar.shouldReleaseFocus(searchFocused,
                mouseX != searchFocusMouseX || mouseY != searchFocusMouseY,
                searchHovered, isSearchBlockingTarget(mouseX, mouseY))) {
            clearSearchFocus();
        }
        boolean categoryHovered = activeGrid.categoryContains(mouseX, mouseY);
        boolean settingsHovered = activeGrid.settingsContains(mouseX, mouseY);
        InventorySearchBar.renderButton(graphics, rail.searchX(), rail.searchY(),
                InventorySearchBar.shouldHighlight(SEARCH.value(), searchFocused, searchHovered,
                        System.currentTimeMillis()));
        InventoryBrowserControls.renderButton(graphics,
                rail.categoryX(), rail.categoryY(),
                Screen.hasShiftDown() ? new ItemStack(Items.STICKY_PISTON)
                        : CategoryIcons.displayStack(InventoryBrowserControls.currentCategory(data)),
                categoryMenuOpen || categoryHovered);
        InventoryBrowserControls.renderButton(graphics,
                rail.settingsX(), rail.settingsY(),
                InventoryBrowserControls.configuredIcon(ClientConfig.SETTINGS_ICON.get()),
                settingsMenuOpen || settingsHovered);
        InventoryBrowserControls.renderSortButton(graphics, rail.sortX(), rail.sortY(),
                activeGrid.sortContains(mouseX, mouseY));
        if (searchFocused) {
            InventorySideRail.renderExpandedSearch(graphics, minecraft.font, rail, SEARCH.value(),
                    SEARCH.allSelected(), ItemSearchExpression.parse(SEARCH.value()).valid(),
                    (System.currentTimeMillis() / 500L) % 2 == 0);
        }
        CAPACITY_PULSE.render(graphics,
                activeGrid.gridLeft(), activeGrid.gridTop() + activeGrid.gridHeight(),
                activeGrid.gridWidth(), InventoryScreenLayout.FULLNESS_HEIGHT,
                data.inventory(), System.currentTimeMillis());

        if (searchHovered) {
            hoveredTooltip = InventorySearchBar.tooltip(data.inventory(), Screen.hasShiftDown());
        } else if (categoryHovered) {
            CategoryDefinition category = InventoryBrowserControls.currentCategory(data);
            Component categoryName = category == null
                    ? Component.translatable("gui.bundlednotsiloed.all")
                    : Component.literal(category.displayName());
            hoveredTooltip = Screen.hasShiftDown()
                    ? List.of(categoryName, Component.translatable("gui.bundlednotsiloed.stow_main_grid_hint"))
                    : List.of(categoryName);
        } else if (settingsHovered) {
            hoveredTooltip = List.of(Component.translatable("gui.bundlednotsiloed.inventory_menu"));
        } else if (activeGrid.sortContains(mouseX, mouseY)) {
            hoveredTooltip = InventoryBrowserControls.sortTooltip(Screen.hasShiftDown());
        }
    }

    private static void renderCategoryMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int popupX = activeGrid.categoryPopupX();
        int popupY = activeGrid.categoryPopupY();
        int popupWidth = activeGrid.gridWidth() + 2;
        graphics.fill(popupX, popupY, popupX + popupWidth,
                popupY + CATEGORY_POPUP_HEIGHT + 2, 0xFF373737);
        graphics.fill(popupX + 1, popupY + 1, popupX + popupWidth - 1,
                popupY + CATEGORY_POPUP_HEIGHT + 1, 0xFFC6C6C6);

        List<CategoryOption> options = categoryOptions();
        CategoryGridLayout layout = categoryMenuLayout();
        categoryScrollRow = layout.scrollRow();
        ResourceLocation selected = Minecraft.getInstance().player
                .getData(ModAttachments.PLAYER_DATA).selectedCategoryPreference();
        for (int offset = 0; offset < layout.visibleCount(); offset++) {
            int optionIndex = layout.firstIndex() + offset;
            CategoryOption option = options.get(optionIndex);
            int x = popupX + 1 + offset % layout.columns() * InventoryScreenLayout.CELL_SIZE;
            int y = popupY + 1 + offset / layout.columns() * InventoryScreenLayout.CELL_SIZE;
            graphics.blit(INVENTORY_TEXTURE, x, y, 7, 83,
                    InventoryScreenLayout.CELL_SIZE, InventoryScreenLayout.CELL_SIZE);
            boolean selectedOption = InventoryBrowserControls.isSelected(option, selected);
            boolean hovered = InventoryScreenLayout.inside(mouseX, mouseY, x, y,
                    InventoryScreenLayout.CELL_SIZE, InventoryScreenLayout.CELL_SIZE);
            if (selectedOption || hovered) {
                graphics.fill(x + 1, y + 1, x + 17, y + 17,
                        hovered ? 0xCC4F72A5 : 0x99356DA5);
            }
            graphics.renderItem(option.icon(), x + 1, y + 1);
            if (hovered) hoveredCategory = option;
        }
    }

    private static boolean isSearchBlockingTarget(double mouseX, double mouseY) {
        if (activeGrid == null || activeScreen == null) return false;
        if (activeGrid.categoryContains(mouseX, mouseY)
                || activeGrid.settingsContains(mouseX, mouseY)
                || activeGrid.sortContains(mouseX, mouseY)
                || activeGrid.scrollbarContains(mouseX, mouseY)
                || (categoryMenuOpen && activeGrid.categoryPopupContains(mouseX, mouseY))
                || (settingsMenuOpen && activeGrid.settingsPopupContains(mouseX, mouseY))
                || activeScreen.getChildAt(mouseX, mouseY).isPresent()) {
            return true;
        }
        for (Slot slot : activeScreen.getMenu().slots) {
            if (InventoryScreenLayout.inside(mouseX, mouseY,
                    activeScreen.getGuiLeft() + slot.x - 1,
                    activeScreen.getGuiTop() + slot.y - 1, 18, 18)) {
                return true;
            }
        }
        return false;
    }

    private static void renderSettingsMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = activeGrid.settingsPopupX();
        int y = activeGrid.settingsPopupY();
        graphics.fill(x, y, x + SETTINGS_POPUP_WIDTH, y + SETTINGS_POPUP_HEIGHT, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SETTINGS_POPUP_WIDTH - 1,
                y + SETTINGS_POPUP_HEIGHT - 1, 0xFFC6C6C6);

        Component[] labels = {
                Component.translatable("gui.bundlednotsiloed.manage_tabs"),
                Component.translatable("gui.bundlednotsiloed.sort",
                        InventoryBrowserControls.sortName(currentSort())),
                Component.translatable("gui.bundlednotsiloed.settings")
        };
        ItemStack[] icons = {
                InventoryBrowserControls.configuredIcon(ClientConfig.MANAGE_TABS_ICON.get()),
                new ItemStack(net.minecraft.world.item.Items.COMPARATOR),
                InventoryBrowserControls.configuredIcon(ClientConfig.SETTINGS_ICON.get())
        };
        for (int row = 0; row < labels.length; row++) {
            int rowY = y + 1 + row * SETTINGS_ROW_HEIGHT;
            boolean hovered = InventoryScreenLayout.inside(mouseX, mouseY,
                    x + 1, rowY, SETTINGS_POPUP_WIDTH - 2, SETTINGS_ROW_HEIGHT);
            if (hovered) {
                graphics.fill(x + 1, rowY, x + SETTINGS_POPUP_WIDTH - 1,
                        rowY + SETTINGS_ROW_HEIGHT, 0xFF8B8B8B);
            }
            graphics.renderItem(icons[row], x + 2, rowY + 1);
            String text = Minecraft.getInstance().font.plainSubstrByWidth(
                    labels[row].getString(), SETTINGS_POPUP_WIDTH - 24);
            graphics.drawString(Minecraft.getInstance().font, text,
                    x + 21, rowY + 5, 0x404040, false);
        }
    }

    private static void renderEntries(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!identityWindow) {
            renderScrollbar(graphics, visibleEntryCount());
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long capacity = minecraft.player.getData(ModAttachments.PLAYER_DATA).inventory().capacity();
        for (Cell cell : activeGrid.cells()) {
            ItemStack displayed = cell.slot().getItem();
            if (displayed.isEmpty()) continue;
            LogicalInventoryEntry entry = aggregate(displayed);
            if (entry == null) continue;
            int x = activeGrid.guiLeft() + cell.x();
            int y = activeGrid.guiTop() + cell.y();
            if (InventoryScreenLayout.inside(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
                hoveredEntry = entry;
            }
            graphics.renderItem(entry.representative(), x, y);
            String count = InventoryCountFormatter.item(entry, capacity);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 250);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(minecraft.font, count,
                    (x + 17) * 2 - minecraft.font.width(count), (y + 11) * 2, 0xFFFFFF, true);
            graphics.pose().popPose();
        }
        renderScrollbar(graphics, visibleEntryCount());
    }

    private static void renderScrollbar(GuiGraphics graphics, int entryCount) {
        int maximum = maximumScrollRow(entryCount);
        if (maximum == 0) return;
        int x = activeGrid.scrollbarX();
        int y = activeGrid.gridTop();
        int thumbHeight = thumbHeight(entryCount);
        int travel = activeGrid.gridHeight() - thumbHeight;
        int thumbY = y + travel * scrollRow / maximum;
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + activeGrid.gridHeight(), 0xFF373737);
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFFFFFFF);
        graphics.fill(x + 1, thumbY + 1, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF8B8B8B);
    }

    private static List<LogicalInventoryEntry> entries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return List.of();
        var data = minecraft.player.getData(ModAttachments.PLAYER_DATA);
        ResourceLocation categoryId = data.selectedCategoryPreference();
        SortMode sortMode = data.inventorySortPreference();
        String query = SEARCH.value();
        if (minecraft.player.getUUID().equals(cachedPlayer)
                && data.inventory().revision() == cachedRevision
                && data.categories().revision() == cachedCategoryRevision
                && query.equals(cachedQuery)
                && Objects.equals(categoryId, cachedCategory)
                && sortMode == cachedSort) return cachedEntries;
        cachedPlayer = minecraft.player.getUUID();
        cachedRevision = data.inventory().revision();
        cachedCategoryRevision = data.categories().revision();
        cachedQuery = query;
        cachedCategory = categoryId;
        cachedSort = sortMode;

        CategoryDefinition category = categoryId == null ? null : data.categories().find(categoryId);
        if (category != null && !category.enabled()) category = null;
        boolean searching = !query.trim().isEmpty();
        ItemSearchExpression search = ItemSearchExpression.parse(query);
        ArrayList<LogicalInventoryEntry> values = new ArrayList<>();
        for (LogicalInventoryEntry entry : data.inventory().entriesAtOrAfter(9)) {
            if (!searching && category != null && !CategoryMatcher.matches(category, entry.representative())) continue;
            if (search.matches(entry.representative(),
                    () -> ClientTooltipSearchIndex.text(entry.representative()))) values.add(entry);
        }
        cachedEntries = List.copyOf(values);
        return cachedEntries;
    }

    private static SortMode currentSort() {
        return Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA)
                .inventorySortPreference();
    }

    private static List<CategoryOption> categoryOptions() {
        return InventoryBrowserControls.categoryOptions(
                Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA));
    }

    private static void changeCategory(int direction) {
        List<CategoryOption> options = categoryOptions();
        if (options.isEmpty()) return;
        ResourceLocation selected = Minecraft.getInstance().player
                .getData(ModAttachments.PLAYER_DATA).selectedCategoryPreference();
        int current = 0;
        for (int index = 0; index < options.size(); index++) {
            if (InventoryBrowserControls.isSelected(options.get(index), selected)) current = index;
        }
        selectCategory(options.get(Math.floorMod(current + direction, options.size())).category());
    }

    private static void selectCategory(CategoryDefinition category) {
        var data = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA);
        SortMode sort = category == null ? data.inventorySortPreference() : category.sortMode();
        ResourceLocation selected = category == null ? null : category.id();
        data.setSelectedCategoryPreference(selected);
        data.setInventorySortPreference(sort);
        PacketDistributor.sendToServer(new InventoryViewPreferencesPayload(sort, selected));
        scrollRow = 0;
        invalidateEntries();
        ensureInventoryWindow(true);
    }

    private static void cycleSort(int direction) {
        var data = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA);
        SortMode[] modes = SortMode.values();
        SortMode next = modes[Math.floorMod(
                data.inventorySortPreference().ordinal() + direction, modes.length)];
        data.setInventorySortPreference(next);
        PacketDistributor.sendToServer(new InventoryViewPreferencesPayload(
                next, data.selectedCategoryPreference()));
        scrollRow = 0;
        invalidateEntries();
        ensureInventoryWindow(true);
    }

    private static void clickSettingsRow(AbstractContainerScreen<?> parent, int row) {
        Minecraft minecraft = Minecraft.getInstance();
        settingsMenuOpen = false;
        switch (row) {
            case 0 -> minecraft.setScreen(new CategoryManagerScreen(parent, minecraft.player));
            case 1 -> {
                cycleSort(1);
                settingsMenuOpen = true;
            }
            case 2 -> minecraft.setScreen(new InventoryBrowserSettingsScreen(parent));
            default -> { }
        }
    }

    private static CategoryGridLayout categoryMenuLayout() {
        return CategoryGridLayout.calculate(
                activeGrid.gridWidth(), CATEGORY_POPUP_HEIGHT,
                categoryOptions().size(), categoryScrollRow);
    }

    private static CategoryOption categoryOptionAt(double mouseX, double mouseY) {
        CategoryGridLayout layout = categoryMenuLayout();
        int index = layout.indexAt(
                mouseX - activeGrid.categoryPopupX() - 1,
                mouseY - activeGrid.categoryPopupY() - 1);
        List<CategoryOption> options = categoryOptions();
        return index >= 0 && index < options.size() ? options.get(index) : null;
    }

    private static int maximumScrollRow(int entryCount) {
        return InventoryScreenLayout.maximumScrollRow(entryCount);
    }

    private static int thumbHeight(int entryCount) {
        int totalRows = Math.max(InventoryScreenLayout.GRID_ROWS,
                Math.ceilDiv(Math.max(0, entryCount), InventoryScreenLayout.GRID_COLUMNS));
        return Math.max(8, activeGrid.gridHeight() * InventoryScreenLayout.GRID_ROWS / totalRows);
    }

    private static int scrollRowForMouse(double mouseY, int entryCount) {
        int maximum = maximumScrollRow(entryCount);
        int thumbHeight = thumbHeight(entryCount);
        int travel = activeGrid.gridHeight() - thumbHeight;
        double centered = mouseY - activeGrid.gridTop() - thumbHeight / 2.0;
        return clamp((int)Math.round(centered * maximum / Math.max(1, travel)), 0, maximum);
    }

    private static void updateSearch(boolean changed) {
        if (!changed) return;
        scrollRow = 0;
        invalidateEntries();
        ensureInventoryWindow(true);
    }

    private static void focusSearch(double mouseX, double mouseY) {
        searchFocused = true;
        searchHoverRefocusArmed = false;
        SEARCH.clearSelection();
        categoryMenuOpen = false;
        settingsMenuOpen = false;
        searchFocusMouseX = mouseX;
        searchFocusMouseY = mouseY;
    }

    private static void clearSearchFocus() {
        searchFocused = false;
        searchHoverRefocusArmed = false;
        SEARCH.clearSelection();
    }

    private static void invalidateEntries() {
        cachedRevision = -1;
        windowDirty = true;
    }

    private static LogicalInventoryEntry aggregate(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return null;
        return minecraft.player.getData(ModAttachments.PLAYER_DATA).inventory().entriesAtOrAfter(9).stream()
                .filter(entry -> ItemStack.isSameItemSameComponents(entry.representative(), stack))
                .findFirst()
                .orElse(null);
    }

    private static boolean usesIdentityView() {
        return !SEARCH.value().trim().isEmpty();
    }

    private static int visibleEntryCount() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return InventorySlotWindow.VISIBLE_SLOTS;
        if (usesIdentityView()) return entries().size();
        int logicalMainSlots = minecraft.player.getData(ModAttachments.PLAYER_DATA)
                .inventory().syntheticSlotCount() - InventorySlotWindow.MAIN_START;
        return Math.max(InventorySlotWindow.VISIBLE_SLOTS, logicalMainSlots);
    }

    private static void ensureInventoryWindow(boolean force) {
        Minecraft minecraft = Minecraft.getInstance();
        if (activeGrid == null || minecraft.player == null || minecraft.getConnection() == null) return;
        if (!InventoryWindowUpdatePolicy.shouldApply(
                !minecraft.player.containerMenu.getCarried().isEmpty(),
                windowDirty,
                appliedWindowRevision >= 0)) return;
        var data = minecraft.player.getData(ModAttachments.PLAYER_DATA);
        boolean identityView = usesIdentityView();
        List<LogicalInventoryEntry> values = identityView ? entries() : List.of();
        int entryCount = identityView ? values.size() : visibleEntryCount();
        scrollRow = clamp(scrollRow, 0, maximumScrollRow(entryCount));
        long revision = data.inventory().revision();
        if (!force && !windowDirty && appliedWindowRevision == revision
                && appliedWindowScrollRow == scrollRow
                && appliedWindowIdentity == identityView) {
            identityWindow = identityView;
            return;
        }
        identityWindow = identityView;
        if (identityView) InventoryBrowserControls.applyWindow(data, values, scrollRow);
        else InventoryBrowserControls.applyRange(data, scrollRow);
        appliedWindowRevision = revision;
        appliedWindowScrollRow = scrollRow;
        appliedWindowIdentity = identityView;
        windowDirty = false;
    }

    private static double scaledMouseX(Minecraft minecraft) {
        return minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth();
    }

    private static double scaledMouseY(Minecraft minecraft) {
        return minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Cell(Slot slot, int x, int y) {}

    private record PlayerGrid(List<Cell> cells, int guiLeft, int guiTop) {
        int gridLeft() { return guiLeft + cells.getFirst().x(); }
        int gridTop() { return guiTop + cells.getFirst().y(); }
        int gridHeight() {
            int bottom = cells.stream().mapToInt(Cell::y).max().orElse(cells.getFirst().y()) + 18;
            return bottom - cells.getFirst().y();
        }
        int gridWidth() {
            int left = cells.stream().mapToInt(Cell::x).min().orElse(cells.getFirst().x());
            int right = cells.stream().mapToInt(Cell::x).max().orElse(cells.getFirst().x()) + 18;
            return right - left;
        }
        InventorySideRail.Rail rail() { return InventorySideRail.at(guiLeft, gridTop()); }
        int categoryPopupX() { return gridLeft() - 1; }
        int categoryPopupY() { return gridTop() - 1; }
        int settingsPopupX() { return gridLeft() - 1; }
        int settingsPopupY() { return gridTop() - 1; }
        int scrollbarX() {
            return guiLeft + cells.stream().mapToInt(Cell::x).max().orElse(cells.getFirst().x()) + 19;
        }
        boolean searchContains(double x, double y) {
            return rail().searchContains(x, y);
        }
        boolean categoryContains(double x, double y) {
            return rail().categoryContains(x, y);
        }
        boolean settingsContains(double x, double y) {
            return rail().settingsContains(x, y);
        }
        boolean sortContains(double x, double y) {
            return rail().sortContains(x, y);
        }
        boolean categoryPopupContains(double x, double y) {
            return InventoryScreenLayout.inside(x, y, categoryPopupX(), categoryPopupY(),
                    gridWidth() + 2, CATEGORY_POPUP_HEIGHT + 2);
        }
        boolean settingsPopupContains(double x, double y) {
            return InventoryScreenLayout.inside(x, y, settingsPopupX(), settingsPopupY(),
                    SETTINGS_POPUP_WIDTH, SETTINGS_POPUP_HEIGHT);
        }
        boolean gridContains(double x, double y) { return cellAt(x, y) >= 0; }
        boolean scrollbarContains(double x, double y) {
            return InventoryScreenLayout.inside(x, y, scrollbarX() - 1, gridTop(),
                    SCROLLBAR_WIDTH + 2, gridHeight());
        }
        int cellAt(double x, double y) {
            for (int index = 0; index < cells.size(); index++) {
                Cell cell = cells.get(index);
                if (InventoryScreenLayout.inside(x, y, guiLeft + cell.x() - 1,
                        guiTop + cell.y() - 1, 18, 18)) return index;
            }
            return -1;
        }
    }
}
