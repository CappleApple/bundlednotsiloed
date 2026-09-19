package com.cappleapple.bundlednotsiloed.compat.jei;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.compat.RecipeTransferDestination;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.RecipeTransferPayload;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.cappleapple.bundlednotsiloed.platform.PacketDistributor;

/** Supplies complete logical-inventory crafting data to JEI's native screen integration. */
@JeiPlugin
public final class BundledNotSiloedJeiPlugin implements IModPlugin {
    private static final Identifier ID = BundledNotSiloed.id("jei_integration");

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(
                new UnifiedCraftingTransferHandler<>(InventoryMenu.class, Optional.empty(), helper), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(
                new UnifiedCraftingTransferHandler<>(CraftingMenu.class, Optional.of(MenuType.CRAFTING), helper),
                RecipeTypes.CRAFTING);
    }

    private static final class UnifiedCraftingTransferHandler<C extends AbstractContainerMenu>
            implements IRecipeTransferHandler<C, RecipeHolder<CraftingRecipe>> {
        private final Class<? extends C> menuClass;
        private final Optional<MenuType<C>> menuType;
        private final IRecipeTransferHandlerHelper helper;

        private UnifiedCraftingTransferHandler(
                Class<? extends C> menuClass, Optional<MenuType<C>> menuType, IRecipeTransferHandlerHelper helper
        ) {
            this.menuClass = menuClass;
            this.menuType = menuType;
            this.helper = helper;
        }

        @Override public Class<? extends C> getContainerClass() { return menuClass; }
        @Override public Optional<MenuType<C>> getMenuType() { return menuType; }
        @Override public mezz.jei.api.recipe.types.IRecipeHolderType<CraftingRecipe> getRecipeType() { return RecipeTypes.CRAFTING; }

        @Override
        public IRecipeTransferError transferRecipe(
                C menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots,
                Player player, boolean maxTransfer, boolean doTransfer
        ) {
            if (!(menu instanceof net.minecraft.world.inventory.AbstractCraftingMenu recipeMenu)) return helper.createInternalError();
            if (!com.cappleapple.bundlednotsiloed.compat.RecipeTransferService.fits(recipe.value(), recipeMenu.getGridWidth(), recipeMenu.getGridHeight())) {
                return helper.createUserErrorWithTooltip(Component.translatable("message.bundlednotsiloed.recipe_too_large"));
            }

            StackedItemContents contents = new StackedItemContents();
            ModAttachments.get(player).inventory().backingStacks().forEach(contents::accountSimpleStack);
            recipeMenu.fillCraftSlotsStackedContents(contents);
            if (!contents.canCraft(recipe.value(), null)) {
                return helper.createUserErrorForMissingSlots(
                        Component.translatable("message.bundlednotsiloed.recipe_missing"),
                        recipeSlots.getSlotViews(RecipeIngredientRole.INPUT));
            }

            if (doTransfer) com.cappleapple.bundlednotsiloed.platform.PacketDistributor.sendToServer(new RecipeTransferPayload(
                    recipe.id().identifier(), maxTransfer, RecipeTransferDestination.NONE));
            return null;
        }
    }
}
