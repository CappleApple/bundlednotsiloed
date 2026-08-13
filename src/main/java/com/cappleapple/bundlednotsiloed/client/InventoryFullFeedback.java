package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.inventory.InventorySpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Client-only capacity warnings for cursor-held stacks. */
public final class InventoryFullFeedback {
    private static final ItemStack BARRIER_ICON = new ItemStack(Items.BARRIER);

    private InventoryFullFeedback() {}

    public static boolean cursorCannotFit() {
        Player player = Minecraft.getInstance().player;
        return player != null && cursorCannotFit(player);
    }

    public static void renderBarrierIcons(ContainerScreenEvent.Render.Foreground event) {
        if (!ClientConfig.FULL_INVENTORY_BARRIER_ICONS.getAsBoolean() || !cursorCannotFit()) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Inventory playerInventory = player.getInventory();
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(0.0F, 0.0F, 101.0F);
        for (Slot slot : event.getContainerScreen().getMenu().slots) {
            if (isProjectedPlayerSlot(slot, playerInventory) && slot.isActive() && !slot.hasItem()) {
                event.getGuiGraphics().renderFakeItem(BARRIER_ICON, slot.x, slot.y, slot.index);
            }
        }
        event.getGuiGraphics().pose().popPose();
    }

    public static void playForFailedPlacement(ScreenEvent.MouseButtonReleased.Pre event) {
        if ((event.getButton() != 0 && event.getButton() != 1)
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Player player = Minecraft.getInstance().player;
        if (player == null || !cursorCannotFit(player)) return;

        Slot target = playerSlotAt(screen, player.getInventory(), event.getMouseX(), event.getMouseY());
        if (target != null && placementWouldFailForCapacity(target, player)) playSound();
    }

    public static void playSound() {
        String configured = ClientConfig.INVENTORY_FULL_SOUND.get().trim();
        if (configured.isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(configured);
        if (id == null) return;
        BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(sound -> Minecraft.getInstance()
                .getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F)));
    }

    private static boolean cursorCannotFit(Player player) {
        ItemStack carried = player.containerMenu.getCarried();
        var data = player.getData(ModAttachments.PLAYER_DATA);
        return data.migratedVanillaInventory() && !carried.isEmpty()
                && !InventorySpace.canAcceptAny(data.inventory(), carried);
    }

    private static boolean placementWouldFailForCapacity(Slot target, Player player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || !target.mayPlace(carried)) return false;
        ItemStack existing = target.getItem();
        int maximum = target.getMaxStackSize(carried);
        if (existing.isEmpty()) return maximum <= 0;
        if (ItemStack.isSameItemSameComponents(existing, carried)) return existing.getCount() >= maximum;
        return target.mayPickup(player) && carried.getCount() > maximum;
    }

    private static Slot playerSlotAt(AbstractContainerScreen<?> screen, Inventory inventory,
                                     double mouseX, double mouseY) {
        double localX = mouseX - screen.getGuiLeft();
        double localY = mouseY - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (isProjectedPlayerSlot(slot, inventory) && slot.isActive()
                    && localX >= slot.x - 1 && localX < slot.x + 17
                    && localY >= slot.y - 1 && localY < slot.y + 17) return slot;
        }
        return null;
    }

    private static boolean isProjectedPlayerSlot(Slot slot, Inventory inventory) {
        return slot.container == inventory && slot.getContainerSlot() >= 0
                && slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
    }
}
