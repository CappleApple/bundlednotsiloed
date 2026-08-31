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

/** Settings that remain relevant to the integrated inventory and container grids. */
public final class InventoryBrowserSettingsScreen extends Screen {
    private final Screen parent;
    private ClientConfig.ItemCountMode itemCountMode = ClientConfig.ITEM_COUNT_MODE.get();
    private ClientConfig.OverallCountMode overallCountMode = ClientConfig.OVERALL_COUNT_MODE.get();
    private boolean transferOverlay = ClientConfig.BULK_TRANSFER_OVERLAY.getAsBoolean();
    private boolean fullInventoryBarrierIcons = ClientConfig.FULL_INVENTORY_BARRIER_ICONS.getAsBoolean();
    private boolean autoRefill;
    private NewItemDestination newItemDestination = NewItemDestination.INVENTORY_FIRST;
    private Button itemCountButton;
    private Button overallCountButton;
    private Button transferOverlayButton;
    private Button fullInventoryBarrierIconsButton;
    private Button autoRefillButton;
    private Button newItemDestinationButton;
    private EditBox overlaySeconds;
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

        itemCountButton = button(left, 34, 158, ignored -> {
            itemCountMode = next(itemCountMode);
            updateButtons();
        }, "tooltip.bundlednotsiloed.item_count_mode");
        overallCountButton = button(left + 162, 34, 158, ignored -> {
            overallCountMode = next(overallCountMode);
            updateButtons();
        }, "tooltip.bundlednotsiloed.overall_count_mode");
        transferOverlayButton = button(left, 56, 158, ignored -> {
            transferOverlay = !transferOverlay;
            updateButtons();
        }, "tooltip.bundlednotsiloed.bulk_overlay");
        autoRefillButton = button(left + 162, 56, 158, ignored -> {
            autoRefill = !autoRefill;
            updateButtons();
        }, "tooltip.bundlednotsiloed.auto_refill");
        newItemDestinationButton = button(left, 78, 158, ignored -> {
            newItemDestination = next(newItemDestination);
            updateButtons();
        }, "tooltip.bundlednotsiloed.new_item_destination");
        fullInventoryBarrierIconsButton = button(left + 162, 78, 158, ignored -> {
            fullInventoryBarrierIcons = !fullInventoryBarrierIcons;
            updateButtons();
        }, "tooltip.bundlednotsiloed.full_inventory_barrier_icons");

        overlaySeconds = field(left, 100, 158, Double.toString(ClientConfig.BULK_TRANSFER_OVERLAY_SECONDS.get()),
                "gui.bundlednotsiloed.overlay_seconds", "tooltip.bundlednotsiloed.overlay_seconds");
        inventoryFullSound = field(left + 162, 100, 158, ClientConfig.INVENTORY_FULL_SOUND.get(),
                "gui.bundlednotsiloed.inventory_full_sound", "tooltip.bundlednotsiloed.inventory_full_sound");
        manageIcon = field(left, 122, 158, ClientConfig.MANAGE_TABS_ICON.get(),
                "gui.bundlednotsiloed.manage_icon", "tooltip.bundlednotsiloed.configurable_icon");
        settingsIcon = field(left + 162, 122, 158, ClientConfig.SETTINGS_ICON.get(),
                "gui.bundlednotsiloed.settings_icon", "tooltip.bundlednotsiloed.configurable_icon");

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> saveAndClose())
                .bounds(left, Math.max(148, height - 24), 320, 20).build());
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
        itemCountButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.item_count_mode", display(itemCountMode)));
        overallCountButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.overall_count_mode", display(overallCountMode)));
        transferOverlayButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.bulk_overlay", onOff(transferOverlay)));
        autoRefillButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.auto_refill", onOff(autoRefill)));
        newItemDestinationButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.new_item_destination", display(newItemDestination)));
        fullInventoryBarrierIconsButton.setMessage(Component.translatable(
                "gui.bundlednotsiloed.full_inventory_barrier_icons", onOff(fullInventoryBarrierIcons)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
    }

    private void saveAndClose() {
        ClientConfig.ITEM_COUNT_MODE.set(itemCountMode);
        ClientConfig.OVERALL_COUNT_MODE.set(overallCountMode);
        ClientConfig.BULK_TRANSFER_OVERLAY.set(transferOverlay);
        ClientConfig.FULL_INVENTORY_BARRIER_ICONS.set(fullInventoryBarrierIcons);
        ClientConfig.BULK_TRANSFER_OVERLAY_SECONDS.set(
                parseDouble(overlaySeconds.getValue(), 0.25D, 30.0D, 2.5D));
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

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static double parseDouble(String value, double minimum, double maximum, double fallback) {
        try {
            return Math.max(minimum, Math.min(maximum, Double.parseDouble(value.trim())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static Component display(Enum<?> value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Component.literal(Character.toUpperCase(text.charAt(0)) + text.substring(1));
    }

    private static <T extends Enum<T>> T next(T value) {
        T[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }
}
