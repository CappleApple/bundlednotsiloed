package com.cappleapple.bundlednotsiloed.inventory;

import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;

import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.mixin.AbstractContainerMenuAccessor;
import com.cappleapple.bundlednotsiloed.network.BulkTransferPayload;
import com.cappleapple.bundlednotsiloed.network.BrowserTransferPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Server-authoritative bulk and browser transfers through native menu or item-handler semantics. */
public final class ContainerTransfers {
    private static final int MAIN_GRID_START = 9;
    private static final int MAX_WORLD_STACK_OPERATIONS = 65_536;
    private static final ThreadLocal<java.util.UUID> BULK_BACKEND_PLAYER = new ThreadLocal<>();

    private ContainerTransfers() {}

    public static boolean isBulkBackendRedirect(Player player) {
        return player != null && player.getUUID().equals(BULK_BACKEND_PLAYER.get());
    }

    public static List<TransferredStack> bulk(ServerPlayer player, BulkTransferPayload payload) {
        return payload.target() == BulkTransferPayload.Target.OPEN_MENU
                ? payload.direction() == BulkTransferPayload.Direction.TO_CONTAINER
                        ? movePlayerToOpenMenu(player) : moveOpenMenuToPlayer(player)
                : payload.direction() == BulkTransferPayload.Direction.TO_CONTAINER
                        ? movePlayerToLookedAt(player) : moveLookedAtToPlayer(player);
    }

    public static boolean moveBackendEntryToMenu(ServerPlayer player, ItemStack prototype) {
        return moveBrowserEntryToMenu(
                player, prototype, BrowserTransferPayload.Mode.SINGLE_STACK) > 0;
    }

    public static int moveBrowserEntryToMenu(
            ServerPlayer player,
            ItemStack prototype,
            BrowserTransferPayload.Mode mode
    ) {
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        if (player.containerMenu == player.inventoryMenu) {
            return inventory.moveBackendStackToMain(prototype) ? 1 : 0;
        }
        int movedTotal = 0;
        int maximumOperations = mode == BrowserTransferPayload.Mode.MAXIMUM
                ? MAX_WORLD_STACK_OPERATIONS : 1;
        for (int operation = 0; operation < maximumOperations; operation++) {
            player.getData(ModAttachments.PLAYER_DATA).refreshInventoryWindow();
            int moved = moveVisibleEntryWithNativeQuickMove(player, prototype, inventory);
            if (moved <= 0) moved = moveOneThroughExternalRanges(player, prototype, inventory);
            if (moved <= 0) break;
            movedTotal += moved;
            if (mode == BrowserTransferPayload.Mode.SINGLE_STACK) break;
        }
        if (movedTotal > 0) player.containerMenu.broadcastChanges();
        return movedTotal;
    }

    private static int moveOneThroughExternalRanges(
            ServerPlayer player,
            ItemStack prototype,
            DynamicCapacityInventory inventory
    ) {
        List<Range> ranges = externalRanges(player.containerMenu, player);
        if (ranges.isEmpty()) return 0;
        int available = inventory.extractAtOrAfter(
                prototype, prototype.getMaxStackSize(), MAIN_GRID_START, true).extractedAmount();
        if (available <= 0) return 0;
        ItemStack moving = prototype.copyWithCount(available);
        int before = moving.getCount();
        moveThroughRanges(player.containerMenu, moving, ranges);
        int moved = before - moving.getCount();
        if (moved <= 0) return 0;
        inventory.extractAtOrAfter(prototype, moved, MAIN_GRID_START, false);
        return moved;
    }

    /** Uses the active menu's native quick-move path for the currently mapped real player slot. */
    private static int moveVisibleEntryWithNativeQuickMove(ServerPlayer player, ItemStack prototype,
                                                            DynamicCapacityInventory inventory) {
        AbstractContainerMenu menu = player.containerMenu;
        long before = quantityAtOrAfter(inventory, prototype, MAIN_GRID_START);
        for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
            Slot slot = menu.slots.get(menuIndex);
            if (!isPlayerSlot(slot, player) || !slot.isActive() || !slot.hasItem()
                    || slot.getContainerSlot() < MAIN_GRID_START
                    || slot.getContainerSlot() >= 36
                    || !slot.mayPickup(player)
                    || !ItemStack.isSameItemSameComponents(slot.getItem(), prototype)) continue;
            menu.quickMoveStack(player, menuIndex);
            long after = quantityAtOrAfter(inventory, prototype, MAIN_GRID_START);
            if (after < before) return (int)Math.min(Integer.MAX_VALUE, before - after);
        }
        return 0;
    }

    static long quantityAtOrAfter(
            DynamicCapacityInventory inventory,
            ItemStack prototype,
            int minimumSlot
    ) {
        long quantity = 0;
        List<ItemStack> stacks = inventory.backingStacks();
        for (int slot = Math.max(0, minimumSlot); slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (ItemStack.isSameItemSameComponents(stack, prototype)) quantity += stack.getCount();
        }
        return quantity;
    }

    private static List<TransferredStack> movePlayerToOpenMenu(ServerPlayer player) {
        if (player.containerMenu == player.inventoryMenu) return List.of();
        List<Range> ranges = externalRanges(player.containerMenu, player);
        if (ranges.isEmpty()) return List.of();
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        TransferAccumulator moved = new TransferAccumulator();
        for (ItemStack owned : dumpableStacks(inventory)) {
            if (owned.isEmpty()) continue;
            ItemStack moving = owned.copy();
            int before = moving.getCount();
            moveThroughRanges(player.containerMenu, moving, ranges);
            int accepted = before - moving.getCount();
            if (accepted <= 0) continue;
            extractDumpedStack(inventory, owned, accepted);
            moved.add(owned, accepted);
        }
        player.containerMenu.broadcastChanges();
        return moved.values();
    }

    private static List<TransferredStack> moveOpenMenuToPlayer(ServerPlayer player) {
        if (player.containerMenu == player.inventoryMenu) return List.of();
        TransferAccumulator moved = new TransferAccumulator();
        AbstractContainerMenu menu = player.containerMenu;
        BULK_BACKEND_PLAYER.set(player.getUUID());
        try {
            for (int index = 0; index < menu.slots.size(); index++) {
                Slot slot = menu.slots.get(index);
                if (isPlayerSlot(slot, player) || !slot.hasItem() || !slot.mayPickup(player)) continue;
                ItemStack before = slot.getItem().copy();
                menu.quickMoveStack(player, index);
                ItemStack after = slot.getItem();
                int amount = ItemStack.isSameItemSameComponents(before, after) ? before.getCount() - after.getCount() : before.getCount();
                if (amount > 0) moved.add(before, amount);
            }
        } finally {
            BULK_BACKEND_PLAYER.remove();
        }
        menu.broadcastChanges();
        return moved.values();
    }

    private static List<TransferredStack> movePlayerToLookedAt(ServerPlayer player) {
        IItemHandler handler = lookedAtHandler(player);
        if (handler == null) return List.of();
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        TransferAccumulator moved = new TransferAccumulator();
        for (ItemStack owned : dumpableStacks(inventory)) {
            if (owned.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, owned.copy(), false);
            int accepted = owned.getCount() - remainder.getCount();
            if (accepted <= 0) continue;
            extractDumpedStack(inventory, owned, accepted);
            moved.add(owned, accepted);
        }
        return moved.values();
    }

    /** The dump action includes the main grid and stowed backend, but never the hotbar. */
    static List<ItemStack> dumpableStacks(DynamicCapacityInventory inventory) {
        ArrayList<ItemStack> result = new ArrayList<>();
        for (int slot = MAIN_GRID_START; slot < inventory.syntheticSlotCount(); slot++) {
            ItemStack stack = inventory.syntheticStack(slot);
            if (!stack.isEmpty()) result.add(stack);
        }
        return List.copyOf(result);
    }

    static void extractDumpedStack(DynamicCapacityInventory inventory, ItemStack prototype, int amount) {
        inventory.extractAtOrAfter(prototype, amount, MAIN_GRID_START, false);
    }

    private static List<TransferredStack> moveLookedAtToPlayer(ServerPlayer player) {
        IItemHandler handler = lookedAtHandler(player);
        if (handler == null) return List.of();
        TransferAccumulator moved = new TransferAccumulator();
        int operations = 0;
        for (int slot = 0; slot < handler.getSlots() && operations < MAX_WORLD_STACK_OPERATIONS; slot++) {
            while (operations++ < MAX_WORLD_STACK_OPERATIONS) {
                ItemStack available = handler.extractItem(slot, Integer.MAX_VALUE, true);
                if (available.isEmpty()) break;
                var proposal = InventoryTransactions.insertIntoBackend(player, available, true);
                int accepted = proposal.acceptedAmount();
                if (accepted <= 0) return moved.values();
                ItemStack extracted = handler.extractItem(slot, accepted, false);
                if (extracted.isEmpty()) break;
                var insertion = InventoryTransactions.insertIntoBackend(player, extracted, false);
                if (insertion.acceptedAmount() > 0) moved.add(extracted, insertion.acceptedAmount());
                if (insertion.acceptedAmount() < extracted.getCount()) {
                    ItemStack remainder = extracted.copyWithCount(extracted.getCount() - insertion.acceptedAmount());
                    ItemHandlerHelper.insertItemStacked(handler, remainder, false);
                    return moved.values();
                }
            }
        }
        return moved.values();
    }

    private static IItemHandler lookedAtHandler(ServerPlayer player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return null;
        if (!player.mayInteract(player.level(), blockHit.getBlockPos())) return null;
        return player.level().getCapability(Capabilities.ItemHandler.BLOCK, blockHit.getBlockPos(), blockHit.getDirection());
    }

    private static void moveThroughRanges(AbstractContainerMenu menu, ItemStack moving, List<Range> ranges) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor)menu;
        for (Range range : ranges) {
            if (moving.isEmpty()) break;
            accessor.sns$moveItemStackTo(moving, range.start(), range.end(), false);
        }
    }

    private static List<Range> externalRanges(AbstractContainerMenu menu, ServerPlayer player) {
        ArrayList<Range> result = new ArrayList<>();
        int start = -1;
        for (int index = 0; index <= menu.slots.size(); index++) {
            boolean external = index < menu.slots.size() && !isPlayerSlot(menu.slots.get(index), player);
            if (external && start < 0) start = index;
            else if (!external && start >= 0) {
                result.add(new Range(start, index));
                start = -1;
            }
        }
        return result;
    }

    private static boolean isPlayerSlot(Slot slot, ServerPlayer player) {
        return slot.container == player.getInventory();
    }

    public record TransferredStack(ItemStack prototype, long quantity) {
        public TransferredStack { prototype = prototype.copyWithCount(1); }
        @Override public ItemStack prototype() { return prototype.copy(); }
    }

    private record Range(int start, int end) {}

    private static final class TransferAccumulator {
        private final ArrayList<TransferredStack> values = new ArrayList<>();
        private void add(ItemStack stack, long amount) {
            for (int index = 0; index < values.size(); index++) {
                TransferredStack current = values.get(index);
                if (!ItemStack.isSameItemSameComponents(current.prototype, stack)) continue;
                values.set(index, new TransferredStack(stack, current.quantity + amount));
                return;
            }
            values.add(new TransferredStack(stack, amount));
        }
        private List<TransferredStack> values() { return List.copyOf(values); }
    }
}
