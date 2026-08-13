package com.cappleapple.bundlednotsiloed.client.screen;

import com.cappleapple.bundlednotsiloed.client.ClientSaveState;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.inventory.NewItemDestination;
import com.cappleapple.bundlednotsiloed.network.AutoRefillPayload;
import com.cappleapple.bundlednotsiloed.network.NewItemDestinationPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InventoryBrowserSettingsScreen extends Screen {
    private final Screen parent;
    private ClientConfig.BrowserViewMode viewMode = ClientConfig.BROWSER_VIEW_MODE.get();
    private ClientConfig.ItemCountMode itemCountMode = ClientConfig.ITEM_COUNT_MODE.get();
    private ClientConfig.OverallCountMode overallCountMode = ClientConfig.OVERALL_COUNT_MODE.get();
    private ClientConfig.BrowserDefaultPlacement defaultPlacement = ClientConfig.BROWSER_DEFAULT_PLACEMENT.get();
    private boolean autoSide = ClientConfig.AUTO_BROWSER_DOCK_SIDE.getAsBoolean();
    private boolean transferOverlay = ClientConfig.BULK_TRANSFER_OVERLAY.getAsBoolean();
    private boolean fullInventoryBarrierIcons = ClientConfig.FULL_INVENTORY_BARRIER_ICONS.getAsBoolean();
    private boolean autoRefill;
    private NewItemDestination newItemDestination = NewItemDestination.INVENTORY_FIRST;
    private Button viewButton;
    private Button itemCountButton;
    private Button overallCountButton;
    private Button autoSideButton;
    private Button transferOverlayButton;
    private Button fullInventoryBarrierIconsButton;
    private Button autoRefillButton;
    private Button newItemDestinationButton;
    private Button defaultPlacementButton;
    private EditBox columns;
    private EditBox rows;
    private EditBox deadZoneX;
    private EditBox deadZoneY;
    private EditBox overlaySeconds;
    private EditBox handleIcon;
    private EditBox manageIcon;
    private EditBox settingsIcon;
    private EditBox inventoryFullSound;

    public InventoryBrowserSettingsScreen(Screen parent) {
        super(Component.translatable("gui.bundlednotsiloed.browser_settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 160;
        autoRefill = minecraft.player == null
                || minecraft.player.getData(ModAttachments.PLAYER_DATA).autoRefill();
        if (minecraft.player != null) {
            newItemDestination = minecraft.player.getData(ModAttachments.PLAYER_DATA).newItemDestination();
        }
        viewButton = button(left, 24, 158, ignored -> {
            viewMode = next(viewMode);
            updateButtons();
        }, "tooltip.bundlednotsiloed.browser_view");
        autoSideButton = button(left + 162, 24, 158, ignored -> {
            autoSide = !autoSide;
            updateButtons();
        }, "tooltip.bundlednotsiloed.auto_browser_side");

        columns = field(left, 44, 158, Integer.toString(ClientConfig.BROWSER_GRID_COLUMNS.getAsInt()),
                "gui.bundlednotsiloed.grid_columns", "tooltip.bundlednotsiloed.grid_columns");
        rows = field(left + 162, 44, 158, Integer.toString(ClientConfig.BROWSER_GRID_ROWS.getAsInt()),
                "gui.bundlednotsiloed.grid_rows", "tooltip.bundlednotsiloed.grid_rows");
        itemCountButton = button(left, 64, 158, ignored -> {
            itemCountMode = next(itemCountMode);
            updateButtons();
        }, "tooltip.bundlednotsiloed.item_count_mode");
        overallCountButton = button(left + 162, 64, 158, ignored -> {
            overallCountMode = next(overallCountMode);
            updateButtons();
        }, "tooltip.bundlednotsiloed.overall_count_mode");

        deadZoneX = field(left, 84, 158, Integer.toString(ClientConfig.AUTO_DOCK_DEAD_ZONE_X.getAsInt()),
                "gui.bundlednotsiloed.dead_zone_x", "tooltip.bundlednotsiloed.dead_zone_x");
        deadZoneY = field(left + 162, 84, 158, Integer.toString(ClientConfig.AUTO_DOCK_DEAD_ZONE_Y.getAsInt()),
                "gui.bundlednotsiloed.dead_zone_y", "tooltip.bundlednotsiloed.dead_zone_y");
        transferOverlayButton = button(left, 104, 158, ignored -> {
            transferOverlay = !transferOverlay;
            updateButtons();
        }, "tooltip.bundlednotsiloed.bulk_overlay");
        autoRefillButton = button(left + 162, 104, 158, ignored -> {
            autoRefill = !autoRefill;
            updateButtons();
        }, "tooltip.bundlednotsiloed.auto_refill");
        overlaySeconds = field(left, 124, 158, Double.toString(ClientConfig.BULK_TRANSFER_OVERLAY_SECONDS.get()),
                "gui.bundlednotsiloed.overlay_seconds", "tooltip.bundlednotsiloed.overlay_seconds");

        handleIcon = field(left, 144, 158, ClientConfig.BROWSER_HANDLE_ICON.get(),
                "gui.bundlednotsiloed.handle_icon", "tooltip.bundlednotsiloed.handle_icon");
        defaultPlacementButton = button(left + 162, 124, 158, ignored -> {
            defaultPlacement = next(defaultPlacement);
            updateButtons();
        }, "tooltip.bundlednotsiloed.default_browser_placement");
        manageIcon = field(left + 162, 144, 158, ClientConfig.MANAGE_TABS_ICON.get(),
                "gui.bundlednotsiloed.manage_icon", "tooltip.bundlednotsiloed.configurable_icon");
        settingsIcon = field(left, 164, 158, ClientConfig.SETTINGS_ICON.get(),
                "gui.bundlednotsiloed.settings_icon", "tooltip.bundlednotsiloed.configurable_icon");
        newItemDestinationButton = button(left + 162, 164, 158, ignored -> {
            newItemDestination = next(newItemDestination);
            updateButtons();
        }, "tooltip.bundlednotsiloed.new_item_destination");
        fullInventoryBarrierIconsButton = button(left, 184, 158, ignored -> {
            fullInventoryBarrierIcons = !fullInventoryBarrierIcons;
            updateButtons();
        }, "tooltip.bundlednotsiloed.full_inventory_barrier_icons");
        inventoryFullSound = field(left + 162, 184, 158, ClientConfig.INVENTORY_FULL_SOUND.get(),
                "gui.bundlednotsiloed.inventory_full_sound", "tooltip.bundlednotsiloed.inventory_full_sound");
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> saveAndClose())
                .bounds(left, Math.max(208, height - 24), 320, 20).build());
        updateButtons();
    }

    private Button button(int x, int y, int width, Button.OnPress press, String tooltipKey) {
        return addRenderableWidget(Button.builder(Component.empty(), press)
                .tooltip(Tooltip.create(Component.translatable(tooltipKey))).bounds(x, y, width, 20).build());
    }

    private EditBox field(int x, int y, int width, String value, String hintKey, String tooltipKey) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.translatable(hintKey));
        box.setValue(value);
        box.setHint(Component.translatable(hintKey));
        box.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        box.setMaxLength(128);
        addRenderableWidget(box);
        return box;
    }

    private void updateButtons() {
        viewButton.setMessage(Component.translatable("gui.bundlednotsiloed.browser_view", display(viewMode)));
        autoSideButton.setMessage(Component.translatable("gui.bundlednotsiloed.auto_browser_side", onOff(autoSide)));
        itemCountButton.setMessage(Component.translatable("gui.bundlednotsiloed.item_count_mode", display(itemCountMode)));
        overallCountButton.setMessage(Component.translatable("gui.bundlednotsiloed.overall_count_mode", display(overallCountMode)));
        transferOverlayButton.setMessage(Component.translatable("gui.bundlednotsiloed.bulk_overlay", onOff(transferOverlay)));
        fullInventoryBarrierIconsButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.full_inventory_barrier_icons", onOff(fullInventoryBarrierIcons)));
        autoRefillButton.setMessage(Component.translatable("gui.bundlednotsiloed.auto_refill", onOff(autoRefill)));
        newItemDestinationButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.new_item_destination", display(newItemDestination)));
        defaultPlacementButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.default_browser_placement", display(defaultPlacement)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
    }

    private void saveAndClose() {
        ClientConfig.BROWSER_VIEW_MODE.set(viewMode);
        ClientConfig.ITEM_COUNT_MODE.set(itemCountMode);
        ClientConfig.OVERALL_COUNT_MODE.set(overallCountMode);
        ClientConfig.AUTO_BROWSER_DOCK_SIDE.set(autoSide);
        ClientConfig.BROWSER_DEFAULT_PLACEMENT.set(defaultPlacement);
        ClientConfig.BROWSER_HANDLE_X.set(-1);
        ClientConfig.BROWSER_HANDLE_Y.set(-1);
        ClientConfig.BULK_TRANSFER_OVERLAY.set(transferOverlay);
        ClientConfig.FULL_INVENTORY_BARRIER_ICONS.set(fullInventoryBarrierIcons);
        ClientConfig.BROWSER_GRID_COLUMNS.set(parseBounded(columns.getValue(), 1, 16, 4));
        ClientConfig.BROWSER_GRID_ROWS.set(parseBounded(rows.getValue(), 1, 20, 6));
        ClientConfig.AUTO_DOCK_DEAD_ZONE_X.set(parseBounded(deadZoneX.getValue(), 0, 4096, 48));
        ClientConfig.AUTO_DOCK_DEAD_ZONE_Y.set(parseBounded(deadZoneY.getValue(), 0, 4096, 36));
        ClientConfig.BULK_TRANSFER_OVERLAY_SECONDS.set(parseDouble(overlaySeconds.getValue(), 0.25D, 30.0D, 2.5D));
        ClientConfig.BROWSER_HANDLE_ICON.set(handleIcon.getValue().trim());
        ClientConfig.MANAGE_TABS_ICON.set(manageIcon.getValue().trim());
        ClientConfig.SETTINGS_ICON.set(settingsIcon.getValue().trim());
        ClientConfig.INVENTORY_FULL_SOUND.set(inventoryFullSound.getValue().trim());
        if (minecraft.player != null) {
            var data = minecraft.player.getData(ModAttachments.PLAYER_DATA);
            data.setAutoRefill(autoRefill);
            data.setNewItemDestination(newItemDestination);
            PacketDistributor.sendToServer(new AutoRefillPayload(autoRefill));
            PacketDistributor.sendToServer(new NewItemDestinationPayload(newItemDestination));
        }
        ClientSaveState.saveClientSettings();
        onClose();
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    private static int parseBounded(String value, int minimum, int maximum, int fallback) {
        try { return Math.max(minimum, Math.min(maximum, Integer.parseInt(value.trim()))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static double parseDouble(String value, double minimum, double maximum, double fallback) {
        try { return Math.max(minimum, Math.min(maximum, Double.parseDouble(value.trim()))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static Component onOff(boolean value) { return Component.translatable(value ? "options.on" : "options.off"); }

    private static Component display(Enum<?> value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Component.literal(Character.toUpperCase(text.charAt(0)) + text.substring(1));
    }

    private static <T extends Enum<T>> T next(T value) {
        T[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }
}
