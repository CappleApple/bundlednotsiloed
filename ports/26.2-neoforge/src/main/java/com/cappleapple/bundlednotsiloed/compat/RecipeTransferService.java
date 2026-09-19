package com.cappleapple.bundlednotsiloed.compat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
/** Validated server-side bridge shared by recipe-viewer integrations. */
public final class RecipeTransferService {
    private RecipeTransferService() {}
    public static boolean fits(CraftingRecipe recipe, int width, int height) {
        if (recipe.placementInfo().isImpossibleToPlace()) return false;
        if (recipe instanceof ShapedRecipe shaped) return shaped.getWidth() <= width && shaped.getHeight() <= height;
        return recipe.placementInfo().ingredients().size() <= width * height;
    }
    public static boolean transfer(ServerPlayer player, Identifier recipeId, boolean placeAll, RecipeTransferDestination destination) {
        if (!(player.containerMenu instanceof InventoryMenu || player.containerMenu instanceof CraftingMenu)
                || !(player.containerMenu instanceof AbstractCraftingMenu menu)) return false;
        RecipeHolder<?> holder = player.level().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, recipeId)).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe recipe)
                || !fits(recipe, menu.getGridWidth(), menu.getGridHeight())) return false;
        // The server's placement entrypoint validates ingredients using the logical Inventory view.
        menu.handlePlacement(placeAll, player.isCreative(), holder, player.level(), player.getInventory());
        int resultSlot = menu.getResultSlot().index;
        if (destination != RecipeTransferDestination.NONE && menu.getResultSlot().hasItem()) {
            menu.clicked(resultSlot, 0, destination == RecipeTransferDestination.CURSOR ? ContainerInput.PICKUP : ContainerInput.QUICK_MOVE, player);
        }
        menu.broadcastChanges();
        return true;
    }
}
