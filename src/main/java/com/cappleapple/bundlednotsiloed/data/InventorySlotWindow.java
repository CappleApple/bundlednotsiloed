package com.cappleapple.bundlednotsiloed.data;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Transient mapping from vanilla's real 27 main-grid slots to an arbitrary page of logical
 * backing indices. Menu topology stays vanilla-sized, so native and modded Slot interaction paths
 * continue to operate normally while the backing inventory remains dynamically sized.
 */
public final class InventorySlotWindow {
    public static final int MAIN_START = 9;
    public static final int VISIBLE_SLOTS = 27;

    private final int[] logicalSlots = new int[VISIBLE_SLOTS];
    private List<ItemStack> requested = emptyPage();
    private boolean active;
    private boolean identityView;
    private boolean fixedReferences;

    public InventorySlotWindow() {
        resetMapping();
    }

    public boolean active() {
        return active;
    }

    public boolean identityView() {
        return active && identityView;
    }

    public void show(List<ItemStack> prototypes, DynamicCapacityInventory inventory) {
        fixedReferences = false;
        requested = normalize(prototypes);
        active = true;
        identityView = true;
        resolveIdentities(inventory);
    }

    /**
     * Maps the real grid to a contiguous logical range. The first page is always indices 9-35,
     * even when trailing empty backing entries have no serialized representation.
     */
    public void showRange(int firstLogicalSlot) {
        if (firstLogicalSlot < MAIN_START
                || firstLogicalSlot > Integer.MAX_VALUE - VISIBLE_SLOTS) {
            throw new IllegalArgumentException("Invalid inventory range start " + firstLogicalSlot);
        }
        requested = emptyPage();
        active = true;
        identityView = false;
        for (int offset = 0; offset < VISIBLE_SLOTS; offset++) {
            logicalSlots[offset] = firstLogicalSlot + offset;
        }
    }

    public void refresh(DynamicCapacityInventory inventory) {
        if (identityView() && !fixedReferences) resolveIdentities(inventory);
    }

    /** Acknowledged references never re-resolve mutable components behind the other peer's back. */
    public void showSlots(List<Integer> slots, boolean identities) {
        if (slots.size() != VISIBLE_SLOTS || new HashSet<>(slots).size() != VISIBLE_SLOTS
                || slots.stream().anyMatch(index -> index < MAIN_START || index == Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("Invalid logical inventory slots");
        }
        active = true;
        identityView = identities;
        fixedReferences = true;
        requested = emptyPage();
        for (int i = 0; i < VISIBLE_SLOTS; i++) logicalSlots[i] = slots.get(i);
    }

    public List<Integer> logicalSlots() {
        return java.util.Arrays.stream(logicalSlots).boxed().toList();
    }

    public void reset() {
        active = false;
        identityView = false;
        requested = emptyPage();
        resetMapping();
    }

    public int logicalIndex(int vanillaSlot) {
        if (!active || vanillaSlot < MAIN_START || vanillaSlot >= Inventory.INVENTORY_SIZE) {
            return vanillaSlot;
        }
        return logicalSlots[vanillaSlot - MAIN_START];
    }

    public int vanillaSlotForLogical(int logicalIndex) {
        if (!active) {
            return logicalIndex >= 0 && logicalIndex < Inventory.INVENTORY_SIZE ? logicalIndex : -1;
        }
        if (logicalIndex >= 0 && logicalIndex < MAIN_START) return logicalIndex;
        for (int offset = 0; offset < logicalSlots.length; offset++) {
            if (logicalSlots[offset] == logicalIndex) return MAIN_START + offset;
        }
        return -1;
    }

    public List<ItemStack> requestedPrototypes() {
        return requested.stream().map(ItemStack::copy).toList();
    }

    /** Highest row-aligned range start needed for the current sparse logical extent. */
    public static int maximumRangeStart(int syntheticSlotCount) {
        int logicalMainSlots = Math.max(VISIBLE_SLOTS,
                Math.max(0, syntheticSlotCount - MAIN_START));
        int totalRows = Math.ceilDiv(logicalMainSlots, 9);
        int maximumScrollRow = Math.max(0, totalRows - 3);
        long start = (long)MAIN_START + (long)maximumScrollRow * 9L;
        return (int)Math.min(start, Integer.MAX_VALUE - VISIBLE_SLOTS);
    }

    int mappedLogicalSlot(int offset) {
        return logicalSlots[offset];
    }

    private void resolveIdentities(DynamicCapacityInventory inventory) {
        List<ItemStack> stacks = inventory.backingStacks();
        Set<Integer> used = new HashSet<>();
        int append = Math.max(MAIN_START, stacks.size());
        for (int offset = 0; offset < VISIBLE_SLOTS; offset++) {
            ItemStack prototype = requested.get(offset);
            int resolved = prototype.isEmpty() ? -1 : matchingIndex(stacks, prototype, used);
            if (resolved < 0) resolved = emptyIndex(stacks, used);
            if (resolved < 0) {
                while (used.contains(append)) append++;
                resolved = append++;
            }
            logicalSlots[offset] = resolved;
            used.add(resolved);
        }
    }

    private static int matchingIndex(List<ItemStack> stacks, ItemStack prototype, Set<Integer> used) {
        for (int index = MAIN_START; index < stacks.size(); index++) {
            if (!used.contains(index)
                    && ItemStack.isSameItemSameComponents(stacks.get(index), prototype)) return index;
        }
        return -1;
    }

    private static int emptyIndex(List<ItemStack> stacks, Set<Integer> used) {
        for (int index = MAIN_START; index < stacks.size(); index++) {
            if (!used.contains(index) && stacks.get(index).isEmpty()) return index;
        }
        return -1;
    }

    private static List<ItemStack> normalize(List<ItemStack> prototypes) {
        ArrayList<ItemStack> result = new ArrayList<>(VISIBLE_SLOTS);
        if (prototypes != null) {
            for (ItemStack prototype : prototypes) {
                if (result.size() == VISIBLE_SLOTS) break;
                result.add(prototype == null || prototype.isEmpty()
                        ? ItemStack.EMPTY : prototype.copyWithCount(1));
            }
        }
        while (result.size() < VISIBLE_SLOTS) result.add(ItemStack.EMPTY);
        return List.copyOf(result);
    }

    private static List<ItemStack> emptyPage() {
        return java.util.Collections.nCopies(VISIBLE_SLOTS, ItemStack.EMPTY);
    }

    private void resetMapping() {
        for (int offset = 0; offset < logicalSlots.length; offset++) {
            logicalSlots[offset] = MAIN_START + offset;
        }
    }
}
