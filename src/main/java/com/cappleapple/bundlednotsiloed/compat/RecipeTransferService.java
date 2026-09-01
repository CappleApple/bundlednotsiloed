package com.cappleapple.bundlednotsiloed.compat;

import com.cappleapple.bundlednotsiloed.mixin.RecipeBookAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Validated server-side bridge shared by recipe-viewer integrations. */
public final class RecipeTransferService {
    private RecipeTransferService() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean transfer(ServerPlayer player, ResourceLocation recipeId, boolean placeAll,
                                   RecipeTransferDestination destination) {
        if (!(player.containerMenu instanceof InventoryMenu || player.containerMenu instanceof CraftingMenu)
                || !(player.containerMenu instanceof RecipeBookMenu recipeMenu)) return false;

        RecipeHolder<?> holder = player.serverLevel().getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe craftingRecipe)
                || !craftingRecipe.canCraftInDimensions(recipeMenu.getGridWidth(), recipeMenu.getGridHeight())) return false;

        // ServerPlaceRecipe is vanilla's authoritative, recipe-validating transfer implementation.
        // Temporarily marking an otherwise unknown recipe as known lets JEI/EMI transfer recipes
        // without permanently changing the player's recipe book.
        ServerRecipeBook recipeBook = player.getRecipeBook();
        boolean alreadyKnown = recipeBook.contains(holder);
        RecipeBookAccessor accessor = (RecipeBookAccessor)recipeBook;
        if (!alreadyKnown) accessor.bns$add(recipeId);
        try {
            // This is the same high-level vanilla entrypoint used by recipe-book placement. It
            // calls Inventory.fillStackedContents/findSlotMatchingUnusedItem/removeItem, whose
            // BNS implementations expose and extract the complete logical inventory.
            recipeMenu.handlePlacement(placeAll, holder, player);
            int resultSlot = recipeMenu.getResultSlotIndex();
            if (destination != RecipeTransferDestination.NONE
                    && resultSlot >= 0 && resultSlot < player.containerMenu.slots.size()
                    && player.containerMenu.getSlot(resultSlot).hasItem()) {
                ClickType click = destination == RecipeTransferDestination.CURSOR
                        ? ClickType.PICKUP : ClickType.QUICK_MOVE;
                player.containerMenu.clicked(resultSlot, 0, click, player);
            }
            player.containerMenu.broadcastChanges();
            return true;
        } finally {
            if (!alreadyKnown) accessor.bns$remove(recipeId);
        }
    }
}
