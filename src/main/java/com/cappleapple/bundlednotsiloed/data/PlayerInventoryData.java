package com.cappleapple.bundlednotsiloed.data;

import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.category.PlayerCategoryData;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.inventory.NewItemDestination;
import com.cappleapple.bundlednotsiloed.hotbar.HotbarBindings;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerInventoryData implements INBTSerializable<CompoundTag> {
    private final Player owner;
    private final DynamicCapacityInventory inventory;
    private final InventorySlotWindow inventoryWindow = new InventorySlotWindow();
    private final com.cappleapple.bundlednotsiloed.network.ClientInventorySync clientSync =
            new com.cappleapple.bundlednotsiloed.network.ClientInventorySync();
    private final PlayerCategoryData categories = new PlayerCategoryData();
    private final HotbarBindings hotbar = new HotbarBindings();
    private final RecentInventoryChanges recentChanges = new RecentInventoryChanges();
    private int suppressRecentChangeTracking;
    private boolean migratedVanillaInventory;
    private boolean initializedCapacityBase;
    private SortMode inventorySortPreference = SortMode.NAME_ASCENDING;
    private ResourceLocation selectedCategoryPreference;
    private NewItemDestination newItemDestination = NewItemDestination.INVENTORY_FIRST;
    private boolean autoRefill = true;

    public PlayerInventoryData(Player owner) {
        this.owner = owner;
        this.inventory = new DynamicCapacityInventory(this::effectiveCapacity, this::onInventoryChanged,
                stack -> true, stack -> !com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard.protects(owner, stack));
    }

    public DynamicCapacityInventory inventory() { return inventory; }
    public InventorySlotWindow inventoryWindow() { return inventoryWindow; }
    public com.cappleapple.bundlednotsiloed.network.ClientInventorySync clientSync() { return clientSync; }
    public void showInventorySlots(List<Integer> slots, boolean identities) {
        inventoryWindow.showSlots(slots, identities);
        syncVanillaCompatibilityView();
    }
    public PlayerCategoryData categories() { return categories; }
    public HotbarBindings hotbar() { return hotbar; }
    public boolean migratedVanillaInventory() { return migratedVanillaInventory; }
    public void setMigratedVanillaInventory() {
        migratedVanillaInventory = true;
        syncVanillaCompatibilityView();
    }
    public boolean initializedCapacityBase() { return initializedCapacityBase; }
    public void setInitializedCapacityBase() { initializedCapacityBase = true; }
    public SortMode inventorySortPreference() { return inventorySortPreference; }
    public void setInventorySortPreference(SortMode preference) { inventorySortPreference = preference; }
    public @Nullable ResourceLocation selectedCategoryPreference() { return selectedCategoryPreference; }
    public void setSelectedCategoryPreference(@Nullable ResourceLocation preference) { selectedCategoryPreference = preference; }
    public NewItemDestination newItemDestination() { return newItemDestination; }
    public void setNewItemDestination(NewItemDestination value) { newItemDestination = Objects.requireNonNull(value); }
    public boolean autoRefill() { return autoRefill; }
    public void setAutoRefill(boolean value) { autoRefill = value; }
    public long recentlyModifiedOrder(ItemStack stack) { return recentChanges.order(stack); }

    public void arrangeWithoutRecordingRecentChanges(Runnable arrangement) {
        suppressRecentChangeTracking++;
        try {
            arrangement.run();
        } finally {
            suppressRecentChangeTracking--;
            recentChanges.observe(inventory.backingStacks(), false);
        }
    }

    public void showInventoryWindow(List<ItemStack> prototypes) {
        inventoryWindow.show(prototypes, inventory);
        syncVanillaCompatibilityView();
    }

    public void showInventoryRange(int firstLogicalSlot) {
        inventoryWindow.showRange(firstLogicalSlot);
        syncVanillaCompatibilityView();
    }

    public void refreshInventoryWindow() {
        inventoryWindow.refresh(inventory);
        syncVanillaCompatibilityView();
    }

    public void resetInventoryWindow() {
        inventoryWindow.reset();
        syncVanillaCompatibilityView();
    }

    /** Client-owned category, hotbar, and inventory-view customization. */
    public CompoundTag saveCustomization(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.put("Categories", categories.save());
        root.put("Hotbar", hotbar.saveClientState(provider));
        root.putString("InventorySortPreference", inventorySortPreference.name());
        if (selectedCategoryPreference != null) root.putString("SelectedCategoryPreference", selectedCategoryPreference.toString());
        root.putString("NewItemDestination", newItemDestination.name());
        root.putBoolean("AutoRefill", autoRefill);
        return root;
    }

    public void loadCustomization(HolderLookup.Provider provider, CompoundTag root) {
        categories.load(root.getCompound("Categories"));
        hotbar.load(provider, root.getCompound("Hotbar"));
        loadUiPreferences(root);
    }

    public CompoundTag saveMetadata(HolderLookup.Provider provider) {
        CompoundTag root = saveCustomization(provider);
        root.putBoolean("MigratedVanillaInventory", migratedVanillaInventory);
        root.putBoolean("InitializedCapacityBase", initializedCapacityBase);
        return root;
    }

    public void loadMetadata(HolderLookup.Provider provider, CompoundTag root) {
        loadCustomization(provider, root);
    }

    public long effectiveCapacity() {
        return Math.max(0, (long)Math.floor(owner.getAttributeValue(ModAttributes.INVENTORY_CAPACITY)));
    }

    private void onInventoryChanged() {
        recentChanges.observe(inventory.backingStacks(), suppressRecentChangeTracking == 0);
        syncVanillaCompatibilityView();
        if (owner != null && !owner.level().isClientSide) ModAttachments.markDirty(owner);
    }

    /** Refreshes the public vanilla list used directly by some third-party inventory mods. */
    public void syncVanillaCompatibilityView() {
        if (owner == null || !migratedVanillaInventory) return;
        List<ItemStack> vanillaItems = owner.getInventory().items;
        int visibleSlots = Math.min(Inventory.INVENTORY_SIZE, vanillaItems.size());
        for (int vanillaSlot = 0; vanillaSlot < visibleSlots; vanillaSlot++) {
            int logicalSlot = inventoryWindow.logicalIndex(vanillaSlot);
            ItemStack live = inventory.vanillaStackReference(logicalSlot);
            if (vanillaItems.get(vanillaSlot) != live) vanillaItems.set(vanillaSlot, live);
        }
    }

    /** Imports API writes made directly through Inventory#items, then restores its live view. */
    public void reconcileVanillaCompatibilityView() {
        if (owner == null || !migratedVanillaInventory) return;
        List<ItemStack> vanillaItems = owner.getInventory().items;
        int visibleSlots = Math.min(Inventory.INVENTORY_SIZE, vanillaItems.size());
        ArrayList<Replacement> replacements = new ArrayList<>();
        for (int vanillaSlot = 0; vanillaSlot < visibleSlots; vanillaSlot++) {
            int logicalSlot = inventoryWindow.logicalIndex(vanillaSlot);
            ItemStack exposed = vanillaItems.get(vanillaSlot);
            if (!ItemStack.matches(exposed, inventory.vanillaStackReference(logicalSlot))) {
                replacements.add(new Replacement(logicalSlot, exposed.copy()));
            }
        }
        for (Replacement replacement : replacements) {
            inventory.replaceSyntheticSlotFromItemUse(replacement.logicalSlot(), replacement.stack());
        }
        syncVanillaCompatibilityView();
    }

    private record Replacement(int logicalSlot, ItemStack stack) {}

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.put("Inventory", inventory.serializeNBT(provider));
        root.put("RecentInventoryChanges", recentChanges.save(provider));
        root.putBoolean("MigratedVanillaInventory", migratedVanillaInventory);
        root.putBoolean("InitializedCapacityBase", initializedCapacityBase);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        inventory.deserializeNBT(provider, root.getCompound("Inventory"));
        recentChanges.load(provider, root.getCompound("RecentInventoryChanges"), inventory.backingStacks());
        // Import pre-0.6.3 customization once so the client can migrate it into BNS-SaveState.json.
        if (root.contains("Categories") || root.contains("Hotbar")) loadCustomization(provider, root);
        migratedVanillaInventory = root.getBoolean("MigratedVanillaInventory");
        initializedCapacityBase = root.getBoolean("InitializedCapacityBase");
        syncVanillaCompatibilityView();
    }

    private void loadUiPreferences(CompoundTag root) {
        try {
            inventorySortPreference = SortMode.valueOf(root.getString("InventorySortPreference"));
        } catch (IllegalArgumentException ignored) {
            inventorySortPreference = SortMode.NAME_ASCENDING;
        }
        selectedCategoryPreference = ResourceLocation.tryParse(root.getString("SelectedCategoryPreference"));
        try {
            newItemDestination = NewItemDestination.valueOf(root.getString("NewItemDestination"));
        } catch (IllegalArgumentException ignored) {
            // 1.1 and older stored a boolean with no exact three-way equivalent. Preserve the
            // behavior those players were seeing at upgrade time.
            newItemDestination = NewItemDestination.INVENTORY_FIRST;
        }
        autoRefill = !root.contains("AutoRefill") || root.getBoolean("AutoRefill");
    }
}
