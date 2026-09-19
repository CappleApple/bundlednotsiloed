package com.cappleapple.bundlednotsiloed.client;
import com.cappleapple.bundlednotsiloed.platform.UiEvent;

import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.inventory.ExactItemInventoryCount;
import com.cappleapple.bundlednotsiloed.network.AutoRefillPayload;
import com.cappleapple.bundlednotsiloed.network.HotbarCyclePayload;
import com.cappleapple.bundlednotsiloed.network.BulkTransferPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.cappleapple.bundlednotsiloed.platform.PacketDistributor;

public final class ClientEvents {
    private ClientEvents() {}

    public static void appendExactInventoryCount(ItemStack stack, java.util.List<Component> tooltip) {
        var player = Minecraft.getInstance().player;
        if (player == null || stack.isEmpty()
                || !InventoryItemTooltipContext.isPlayerStorageTooltip(
                        player.getInventory(), stack)) return;
        long count = ExactItemInventoryCount.count(
                ModAttachments.get(player).inventory(),
                stack);
        tooltip.add(Component.translatable(
                "tooltip.bundlednotsiloed.exact_inventory_count", count).withStyle(ChatFormatting.GRAY));
    }

    public static void awaitWindowKey(UiEvent event) {
        if (ClientInventoryWindows.pending() && !InventorySearchInputCapture.isTyping() && event.getKeyCode() != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) event.setCanceled(true);
    }

    public static void playerLoggingIn(net.minecraft.client.player.LocalPlayer player) {
        ClientSaveState.beginConnection(player);
    }

    public static void playerLoggingOut() {
        ClientSaveState.endConnection();
        ClientInventoryWindows.reset();
        com.cappleapple.bundlednotsiloed.network.ModNetwork.clearClientSync();
    }


    public static void closeContainerOverlay(UiEvent event) {
        ContainerInventoryOverlay.close(event.getScreen());
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        KeyBindingCompatibility.warnAboutUnsafeExternalBindings(minecraft);
        while (ClientKeyMappings.CYCLE_FORWARD.consumeClick()) {
            com.cappleapple.bundlednotsiloed.platform.PacketDistributor.sendToServer(new HotbarCyclePayload(minecraft.player.getInventory().getSelectedSlot(), 1));
        }
        while (ClientKeyMappings.CYCLE_BACKWARD.consumeClick()) {
            com.cappleapple.bundlednotsiloed.platform.PacketDistributor.sendToServer(new HotbarCyclePayload(minecraft.player.getInventory().getSelectedSlot(), -1));
        }
        BulkTransferPayload.Target target = minecraft.gui.screen() instanceof AbstractContainerScreen<?>
                ? BulkTransferPayload.Target.OPEN_MENU : BulkTransferPayload.Target.LOOKED_AT;
        while (ClientKeyMappings.DUMP_TO_CONTAINER.consumeClick()) {
            if (!net.minecraft.client.Minecraft.getInstance().hasControlDown()) continue;
            PacketDistributor.sendToServer(new BulkTransferPayload(BulkTransferPayload.Direction.TO_CONTAINER, target));
        }
        while (ClientKeyMappings.EXTRACT_FROM_CONTAINER.consumeClick()) {
            if (!net.minecraft.client.Minecraft.getInstance().hasControlDown()) continue;
            PacketDistributor.sendToServer(new BulkTransferPayload(BulkTransferPayload.Direction.FROM_CONTAINER, target));
        }
        while (ClientKeyMappings.TOGGLE_AUTO_REFILL.consumeClick()) {
            var data = ModAttachments.get(minecraft.player);
            boolean enabled = !data.autoRefill();
            data.setAutoRefill(enabled);
            com.cappleapple.bundlednotsiloed.platform.PacketDistributor.sendToServer(new AutoRefillPayload(enabled));
            minecraft.player.sendOverlayMessage(Component.translatable(
                    enabled ? "message.bundlednotsiloed.auto_refill_on" : "message.bundlednotsiloed.auto_refill_off"));
        }
    }

    public static void renderHud(UiEvent event) {
        if (Minecraft.getInstance().gui.screen() != null) return;
        renderCycleOverlay(event.getGuiGraphicsExtractor());
        renderTransferOverlay(event.getGuiGraphicsExtractor());
    }

    private static void renderCycleOverlay(GuiGraphicsExtractor graphics) {
        if (!ClientConfig.HOTBAR_CYCLE_OVERLAY.getAsBoolean()) return;
        ClientTransientState.CycleOverlay overlay = ClientTransientState.cycleOverlay();
        if (overlay == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(minecraft.font.width(overlay.bindingName()), minecraft.font.width(overlay.selected().getHoverName())) + 34;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 72;
        graphics.fill(x, y, x + width, y + 34, 0xB0101010);
        graphics.item(overlay.selected(), x + 5, y + 9);
        graphics.text(minecraft.font, overlay.bindingName(), x + 26, y + 5, 0xAAAAAA, false);
        graphics.text(minecraft.font, overlay.selected().isEmpty() ? Component.translatable("gui.bundlednotsiloed.empty") : overlay.selected().getHoverName(), x + 26, y + 18, 0xFFFFFF, false);
    }

    private static void renderTransferOverlay(GuiGraphicsExtractor graphics) {
        if (!ClientConfig.BULK_TRANSFER_OVERLAY.getAsBoolean()) return;
        ClientTransientState.TransferOverlay overlay = ClientTransientState.transferOverlay();
        if (overlay == null || overlay.stacks().isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        int shown = Math.min(24, overlay.stacks().size());
        int columns = Math.min(6, shown);
        int rows = Math.ceilDiv(shown, columns);
        int boxWidth = columns * 22 + 8;
        int boxHeight = rows * 22 + 22;
        int x = (graphics.guiWidth() - boxWidth) / 2;
        int y = Math.max(8, graphics.guiHeight() / 2 - boxHeight / 2);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xD0101010);
        Component title = Component.translatable(overlay.direction() == BulkTransferPayload.Direction.TO_CONTAINER
                ? "gui.bundlednotsiloed.transferred_to_container" : "gui.bundlednotsiloed.transferred_from_container");
        graphics.centeredText(minecraft.font, title, x + boxWidth / 2, y + 6, 0xFFFFFF);
        for (int index = 0; index < shown; index++) {
            var moved = overlay.stacks().get(index);
            ItemStack stack = moved.prototype();
            int cellX = x + 5 + index % columns * 22;
            int cellY = y + 18 + index / columns * 22;
            graphics.item(stack, cellX, cellY);
            long stacks = Math.ceilDiv(moved.quantity(), Math.max(1, stack.getMaxStackSize()));
            String count = stacks + "S";
            graphics.pose().pushMatrix();
            graphics.nextStratum();
            graphics.pose().scale(0.5F, 0.5F);
            graphics.text(minecraft.font, count, (cellX + 18) * 2 - minecraft.font.width(count), (cellY + 11) * 2,
                    0xFFFFFF, true);
            graphics.pose().popMatrix();
        }
        if (overlay.stacks().size() > shown) {
            graphics.text(minecraft.font, "+" + (overlay.stacks().size() - shown), x + boxWidth - 24, y + 6, 0xAAAAAA, false);
        }
    }

    public static void renderContainerOverlay(UiEvent event) {
        ContainerInventoryOverlay.render(event);
    }

    public static void renderContainerTooltips(UiEvent event) {
        ContainerInventoryOverlay.renderTooltip(event);
        renderCycleOverlay(event.getGuiGraphicsExtractor());
    }

    public static void initializeContainerOverlay(UiEvent event) {
        ContainerInventoryOverlay.initialize(event);
    }

    public static void renderFullInventoryBarriers(UiEvent event) {
        InventoryFullFeedback.renderBarrierIcons(event);
    }

    public static void releaseFullInventoryPlacement(UiEvent event) {
        InventoryFullFeedback.playForFailedPlacement(event);
    }

    public static void keyPlayerInventorySearch(UiEvent event) {
        if (event.getScreen() instanceof BundledInventoryScreen screen
                && screen.captureSearchKeyPressed(event.getKey(), event.getKeycode())) {
            event.setCanceled(true);
        }
    }

    public static void keyContainerOverlay(UiEvent event) {
        ContainerInventoryOverlay.keyPressed(event);
    }

    public static void releaseKeyContainerOverlay(UiEvent event) {
        ContainerInventoryOverlay.keyReleased(event);
    }

    public static void characterContainerOverlay(UiEvent event) {
        ContainerInventoryOverlay.characterTyped(event);
    }

}
