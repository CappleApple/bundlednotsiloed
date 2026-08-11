package com.cappleapple.bundlednotsiloed.data;

import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.category.PlayerCategoryData;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.inventory.NewItemDestination;
import com.cappleapple.stacksnotslots.api.compat.VanillaInventoryMirror;
import com.cappleapple.bundlednotsiloed.hotbar.HotbarBindings;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerInventoryData implements INBTSerializable<CompoundTag> {
    private final Player owner;
    private final DynamicCapacityInventory inventory;
    private final PlayerCategoryData categories = new PlayerCategoryData();
    private final HotbarBindings hotbar = new HotbarBindings();
    private boolean migratedVanillaInventory;
    private boolean initializedCapacityBase;
    private SortMode inventorySortPreference = SortMode.NAME_ASCENDING;
    private ResourceLocation selectedCategoryPreference;
    private NewItemDestination newItemDestination = NewItemDestination.INVENTORY_FIRST;
    private boolean autoRefill = true;

    public PlayerInventoryData(Player owner) {
        this.owner = owner;
        this.inventory = new DynamicCapacityInventory(this::effectiveCapacity, this::onInventoryChanged);
    }

    public DynamicCapacityInventory inventory() { return inventory; }
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
        syncVanillaCompatibilityView();
        if (owner != null && !owner.level().isClientSide) ModAttachments.markDirty(owner);
    }

    /** Refreshes the public vanilla list used directly by some third-party inventory mods. */
    public void syncVanillaCompatibilityView() {
        if (owner == null || !migratedVanillaInventory) return;
        VanillaInventoryMirror.publish(inventory, owner.getInventory().items);
    }

    /** Imports API writes made directly through Inventory#items, then restores its live view. */
    public void reconcileVanillaCompatibilityView() {
        if (owner == null || !migratedVanillaInventory) return;
        VanillaInventoryMirror.reconcileDirectWrites(inventory, owner.getInventory().items);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.put("Inventory", inventory.serializeNBT(provider));
        root.putBoolean("MigratedVanillaInventory", migratedVanillaInventory);
        root.putBoolean("InitializedCapacityBase", initializedCapacityBase);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        inventory.deserializeNBT(provider, root.getCompound("Inventory"));
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
