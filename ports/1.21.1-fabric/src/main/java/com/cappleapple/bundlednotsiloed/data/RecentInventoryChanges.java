package com.cappleapple.bundlednotsiloed.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Persistent per-identity ordering derived from real inventory content and placement mutations. */
final class RecentInventoryChanges {
    private final ArrayList<ModifiedIdentity> modified = new ArrayList<>();
    private List<ItemStack> observedSlots = List.of();
    private long sequence;

    void observe(List<ItemStack> currentSlots, boolean recordChanges) {
        ArrayList<ItemStack> changed = changedIdentities(observedSlots, currentSlots);
        if (recordChanges && !changed.isEmpty()) {
            long order = nextSequence();
            for (ItemStack identity : changed) {
                if (containsIdentity(currentSlots, identity)) mark(identity, order);
            }
        }
        modified.removeIf(entry -> !containsIdentity(currentSlots, entry.prototype()));
        observedSlots = copySlots(currentSlots);
    }

    long order(ItemStack stack) {
        for (ModifiedIdentity entry : modified) {
            if (ItemStack.isSameItemSameComponents(entry.prototype(), stack)) return entry.order();
        }
        return 0;
    }

    CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putLong("Sequence", sequence);
        ListTag entries = new ListTag();
        for (ModifiedIdentity entry : modified) {
            CompoundTag encoded = new CompoundTag();
            encoded.put("Stack", entry.prototype().save(provider, new CompoundTag()));
            encoded.putLong("Order", entry.order());
            entries.add(encoded);
        }
        root.put("Entries", entries);
        return root;
    }

    void load(HolderLookup.Provider provider, CompoundTag root, List<ItemStack> currentSlots) {
        modified.clear();
        sequence = Math.max(0, root.getLong("Sequence"));
        ListTag entries = root.getList("Entries", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag encoded = entries.getCompound(index);
            ItemStack.parse(provider, encoded.getCompound("Stack"))
                    .filter(stack -> !stack.isEmpty() && containsIdentity(currentSlots, stack))
                    .ifPresent(stack -> {
                        long order = Math.max(0, encoded.getLong("Order"));
                        mark(stack, order);
                        sequence = Math.max(sequence, order);
                    });
        }
        observedSlots = copySlots(currentSlots);
    }

    private long nextSequence() {
        if (sequence != Long.MAX_VALUE) return ++sequence;
        modified.sort(java.util.Comparator.comparingLong(ModifiedIdentity::order));
        for (int index = 0; index < modified.size(); index++) {
            ModifiedIdentity entry = modified.get(index);
            modified.set(index, new ModifiedIdentity(entry.prototype(), index + 1L));
        }
        sequence = modified.size();
        return ++sequence;
    }

    private void mark(ItemStack stack, long order) {
        for (int index = 0; index < modified.size(); index++) {
            if (ItemStack.isSameItemSameComponents(modified.get(index).prototype(), stack)) {
                modified.set(index, new ModifiedIdentity(stack.copyWithCount(1), order));
                return;
            }
        }
        modified.add(new ModifiedIdentity(stack.copyWithCount(1), order));
    }

    private static ArrayList<ItemStack> changedIdentities(
            List<ItemStack> previous,
            List<ItemStack> current
    ) {
        ArrayList<ItemStack> changed = new ArrayList<>();
        int size = Math.max(previous.size(), current.size());
        for (int slot = 0; slot < size; slot++) {
            ItemStack before = slot < previous.size() ? previous.get(slot) : ItemStack.EMPTY;
            ItemStack after = slot < current.size() ? current.get(slot) : ItemStack.EMPTY;
            if (ItemStack.matches(before, after)) continue;
            addIdentity(changed, before);
            addIdentity(changed, after);
        }
        return changed;
    }

    private static void addIdentity(List<ItemStack> identities, ItemStack candidate) {
        if (candidate.isEmpty() || containsIdentity(identities, candidate)) return;
        identities.add(candidate.copyWithCount(1));
    }

    private static boolean containsIdentity(List<ItemStack> stacks, ItemStack candidate) {
        return stacks.stream().anyMatch(stack ->
                !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, candidate));
    }

    private static List<ItemStack> copySlots(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private record ModifiedIdentity(ItemStack prototype, long order) {}
}
