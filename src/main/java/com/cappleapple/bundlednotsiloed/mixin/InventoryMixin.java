package com.cappleapple.bundlednotsiloed.mixin;

import com.cappleapple.stacksnotslots.api.InsertionResult;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import com.cappleapple.bundlednotsiloed.inventory.InsertionContext;
import com.cappleapple.bundlednotsiloed.inventory.InventoryClearing;
import com.cappleapple.bundlednotsiloed.inventory.InventoryTransactions;
import com.cappleapple.bundlednotsiloed.inventory.VisibleStackRefill;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps vanilla's 36 item indices as stable compatibility positions; backend storage remains dynamic. */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Unique private static final int BNS_VIRTUAL_BACKEND_SLOT = Integer.MAX_VALUE;
    @Shadow @Final public Player player;
    @Shadow @Final public NonNullList<ItemStack> items;
    @Unique private int sns$recipeBackingSlot = -1;

    @Unique
    private PlayerInventoryData sns$data() { return player.getData(ModAttachments.PLAYER_DATA); }

    @Unique
    private boolean sns$active() { return sns$data().migratedVanillaInventory(); }

    @Unique
    private int sns$logicalIndex(int vanillaSlot) {
        return vanillaSlot;
    }

    @Unique
    private int sns$vanillaSlotForLogical(int logicalIndex) {
        return logicalIndex >= 0 && logicalIndex < Inventory.INVENTORY_SIZE ? logicalIndex : -1;
    }

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void sns$getItem(int slot, CallbackInfoReturnable<ItemStack> callback) {
        if (sns$active() && slot == BNS_VIRTUAL_BACKEND_SLOT && sns$recipeBackingSlot >= Inventory.INVENTORY_SIZE) {
            callback.setReturnValue(sns$data().inventory().vanillaStackReference(sns$recipeBackingSlot));
            return;
        }
        if (sns$active() && slot >= 0 && slot < Inventory.INVENTORY_SIZE) {
            callback.setReturnValue(sns$data().inventory().vanillaStackReference(sns$logicalIndex(slot)));
        }
    }

    @Inject(method = "getSelected", at = @At("HEAD"), cancellable = true)
    private void sns$getSelected(CallbackInfoReturnable<ItemStack> callback) {
        if (sns$active()) callback.setReturnValue(sns$data().inventory().vanillaStackReference(sns$logicalIndex(((Inventory)(Object)this).selected)));
    }

    /** Vanilla writes Inventory#items directly for pick block; keep that operation authoritative. */
    @Inject(method = "setPickedItem", at = @At("HEAD"), cancellable = true)
    private void sns$setPickedItem(ItemStack stack, CallbackInfo callback) {
        if (!sns$active()) return;
        Inventory self = (Inventory)(Object)this;
        DynamicCapacityInventory inventory = sns$data().inventory();
        int matchingSlot = self.findSlotMatchingItem(stack);
        if (Inventory.isHotbarSlot(matchingSlot)) {
            self.selected = matchingSlot;
        } else if (matchingSlot >= 0) {
            sns$pickLogicalSlot(matchingSlot);
        } else {
            self.selected = self.getSuitableHotbarSlot();
            ItemStack replaced = inventory.syntheticStack(self.selected);
            if (!replaced.isEmpty()) {
                int freeSlot = self.getFreeSlot();
                if (freeSlot >= 0) inventory.replaceSyntheticSlotFromItemUse(freeSlot, replaced);
            }
            inventory.replaceSyntheticSlotFromItemUse(self.selected, stack);
        }
        callback.cancel();
    }

    /** Vanilla's survival pick-item swap has the same direct-list behavior as creative pick block. */
    @Inject(method = "pickSlot", at = @At("HEAD"), cancellable = true)
    private void sns$pickSlot(int slot, CallbackInfo callback) {
        if (!sns$active() || slot < 0 || slot >= Inventory.INVENTORY_SIZE) return;
        sns$pickLogicalSlot(slot);
        callback.cancel();
    }

    @Unique
    private void sns$pickLogicalSlot(int slot) {
        Inventory self = (Inventory)(Object)this;
        DynamicCapacityInventory inventory = sns$data().inventory();
        int selectedSlot = self.getSuitableHotbarSlot();
        ItemStack selectedStack = inventory.syntheticStack(selectedSlot);
        ItemStack pickedStack = inventory.syntheticStack(slot);
        self.selected = selectedSlot;
        inventory.replaceSyntheticSlotFromItemUse(selectedSlot, pickedStack);
        inventory.replaceSyntheticSlotFromItemUse(slot, selectedStack);
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void sns$setItem(int slot, ItemStack stack, CallbackInfo callback) {
        if (!sns$active() || slot < 0 || slot >= Inventory.INVENTORY_SIZE) return;
        DynamicCapacityInventory inventory = sns$data().inventory();
        int index = sns$logicalIndex(slot);
        inventory.replaceSyntheticSlotFromItemUse(index, stack);
        callback.cancel();
    }

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void sns$add(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (!sns$active()) return;
        InsertionResult result = InventoryTransactions.insert(player, stack, InsertionContext.MANUAL_TRANSFER, false);
        stack.setCount(result.remainder().getCount());
        callback.setReturnValue(result.acceptedAnything());
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void sns$addAt(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (!sns$active() || slot < 0 || slot >= Inventory.INVENTORY_SIZE) return;
        InsertionResult result = InventoryTransactions.insertIntoSyntheticSlot(player, stack, sns$logicalIndex(slot), false);
        stack.setCount(result.remainder().getCount());
        callback.setReturnValue(result.acceptedAnything());
    }

    @Inject(method = "removeItem", at = @At("HEAD"), cancellable = true)
    private void sns$removeItem(int slot, int amount, CallbackInfoReturnable<ItemStack> callback) {
        if (sns$active() && slot == BNS_VIRTUAL_BACKEND_SLOT && sns$recipeBackingSlot >= Inventory.INVENTORY_SIZE) {
            int backingSlot = sns$recipeBackingSlot;
            sns$recipeBackingSlot = -1;
            callback.setReturnValue(sns$data().inventory().extractSyntheticSlot(backingSlot, amount, false));
            return;
        }
        if (sns$active() && slot >= 0 && slot < Inventory.INVENTORY_SIZE) {
            callback.setReturnValue(sns$data().inventory().extractSyntheticSlot(sns$logicalIndex(slot), amount, false));
        }
    }

    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void sns$removeItemNoUpdate(int slot, CallbackInfoReturnable<ItemStack> callback) {
        if (sns$active() && slot == BNS_VIRTUAL_BACKEND_SLOT && sns$recipeBackingSlot >= Inventory.INVENTORY_SIZE) {
            int backingSlot = sns$recipeBackingSlot;
            sns$recipeBackingSlot = -1;
            callback.setReturnValue(sns$data().inventory().extractSyntheticSlot(backingSlot, Integer.MAX_VALUE, false));
            return;
        }
        if (!sns$active() || slot < 0 || slot >= Inventory.INVENTORY_SIZE) return;
        DynamicCapacityInventory inventory = sns$data().inventory();
        int index = sns$logicalIndex(slot);
        callback.setReturnValue(inventory.extractSyntheticSlot(index, Integer.MAX_VALUE, false));
    }

    @Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void sns$removeReference(ItemStack stack, CallbackInfo callback) {
        if (!sns$active()) return;
        sns$data().inventory().removeReference(stack);
        callback.cancel();
    }

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void sns$reconcileChangedSlot(CallbackInfo callback) {
        if (!sns$active()) return;
        sns$data().reconcileVanillaCompatibilityView();
        sns$data().inventory().reconcileExternalMutations();
    }

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void sns$getFreeSlot(CallbackInfoReturnable<Integer> callback) {
        if (!sns$active()) return;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (sns$data().inventory().syntheticStack(slot).isEmpty()) {
                callback.setReturnValue(slot);
                return;
            }
        }
        callback.setReturnValue(-1);
    }

    @Inject(method = "findSlotMatchingItem", at = @At("HEAD"), cancellable = true)
    private void sns$findMatching(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (!sns$active()) return;
        int index = sns$data().inventory().indexOf(stack);
        callback.setReturnValue(index < 0 ? -1 : sns$vanillaSlotForLogical(index));
    }

    @Inject(method = "findSlotMatchingUnusedItem", at = @At("HEAD"), cancellable = true)
    private void sns$findUnusedMatching(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (!sns$active()) return;
        sns$recipeBackingSlot = -1;
        List<ItemStack> stacks = sns$data().inventory().backingStacks();
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stored = stacks.get(index);
            if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stack, stored)
                    || stored.isDamaged() || stored.isEnchanted() || stored.has(DataComponents.CUSTOM_NAME)) continue;
            if (index < Inventory.INVENTORY_SIZE) callback.setReturnValue(index);
            else {
                sns$recipeBackingSlot = index;
                callback.setReturnValue(BNS_VIRTUAL_BACKEND_SLOT);
            }
            return;
        }
        callback.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void sns$remainingSpace(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (!sns$active()) return;
        List<ItemStack> stacks = sns$data().inventory().backingStacks();
        for (int i = 0; i < Math.min(stacks.size(), Inventory.INVENTORY_SIZE); i++) {
            ItemStack stored = stacks.get(i);
            if (stored.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() < stored.getMaxStackSize()) {
                callback.setReturnValue(i);
                return;
            }
        }
        callback.setReturnValue(-1);
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void sns$destroySpeed(BlockState state, CallbackInfoReturnable<Float> callback) {
        if (sns$active()) callback.setReturnValue(((Inventory)(Object)this).getSelected().getDestroySpeed(state));
    }

    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true)
    private void sns$isEmpty(CallbackInfoReturnable<Boolean> callback) {
        if (sns$active() && sns$data().inventory().syntheticSlotCount() > 0) callback.setReturnValue(false);
    }

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void sns$containsStack(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (sns$active() && sns$data().inventory().indexOf(stack) >= 0) callback.setReturnValue(true);
    }

    @Inject(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void sns$containsTag(TagKey<Item> tag, CallbackInfoReturnable<Boolean> callback) {
        if (sns$active() && sns$data().inventory().backingStacks().stream().anyMatch(stack -> stack.is(tag))) callback.setReturnValue(true);
    }

    @Inject(method = "contains(Ljava/util/function/Predicate;)Z", at = @At("HEAD"), cancellable = true)
    private void sns$containsPredicate(Predicate<ItemStack> predicate, CallbackInfoReturnable<Boolean> callback) {
        if (sns$active() && sns$data().inventory().backingStacks().stream().anyMatch(predicate)) callback.setReturnValue(true);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void sns$tick(CallbackInfo callback) {
        if (!sns$active()) return;
        sns$data().reconcileVanillaCompatibilityView();
        sns$data().inventory().tick(player);
        if (!player.level().isClientSide && sns$data().autoRefill()
                && VisibleStackRefill.refill(sns$data().inventory())) {
            player.containerMenu.broadcastChanges();
        }
        callback.cancel();
    }

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void sns$dropAll(CallbackInfo callback) {
        if (sns$active()) for (ItemStack stack : sns$data().inventory().drain()) player.drop(stack, true, false);
    }

    @Inject(method = "clearContent", at = @At("HEAD"))
    private void sns$clear(CallbackInfo callback) {
        if (sns$active()) sns$data().inventory().clear();
    }

    /** Vanilla already handles visible, equipment, crafting, and carried stacks; append hidden storage. */
    @Inject(method = "clearOrCountMatchingItems", at = @At("RETURN"), cancellable = true)
    private void sns$clearOrCountHiddenItems(
            Predicate<ItemStack> predicate,
            int maxCount,
            Container craftingSlots,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (!sns$active()) return;
        DynamicCapacityInventory inventory = sns$data().inventory();
        inventory.reconcileExternalMutations();
        int vanillaAffected = callback.getReturnValue();
        if (maxCount > 0 && vanillaAffected >= maxCount) return;
        int backendLimit = maxCount > 0 ? maxCount - vanillaAffected : maxCount;
        int hiddenAffected = InventoryClearing.clearOrCountMatchingItems(
                inventory, predicate, backendLimit, Inventory.INVENTORY_SIZE);
        callback.setReturnValue((int)Math.min(Integer.MAX_VALUE, (long)vanillaAffected + hiddenAffected));
    }

    @Inject(method = "fillStackedContents", at = @At("HEAD"), cancellable = true)
    private void sns$fillStacked(StackedContents contents, CallbackInfo callback) {
        if (!sns$active()) return;
        sns$data().inventory().backingStacks().forEach(contents::accountSimpleStack);
        callback.cancel();
    }
}
