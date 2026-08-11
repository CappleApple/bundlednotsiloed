package com.cappleapple.bundlednotsiloed.compat.jei;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.client.ContainerInventoryOverlay;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.RecipeTransferPayload;
import java.util.Collection;
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
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/** Reserves the floating browser on every container screen so JEI lays its ingredient list around it. */
@JeiPlugin
public final class BundledNotSiloedJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = BundledNotSiloed.id("jei_integration");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // A global extra-area provider does not join or replace a concrete screen's own JEI handler.
        // That distinction matters for custom screens such as Sophisticated Core's StorageScreenBase.
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                        ? ContainerInventoryOverlay.currentAreas(screen)
                        : List.of();
            }
        });
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
        @Override public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() { return RecipeTypes.CRAFTING; }

        @Override
        public IRecipeTransferError transferRecipe(
                C menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots,
                Player player, boolean maxTransfer, boolean doTransfer
        ) {
            if (!(menu instanceof RecipeBookMenu<?, ?> recipeMenu)) return helper.createInternalError();
            if (!recipe.value().canCraftInDimensions(recipeMenu.getGridWidth(), recipeMenu.getGridHeight())) {
                return helper.createUserErrorWithTooltip(Component.translatable("message.bundlednotsiloed.recipe_too_large"));
            }

            StackedContents contents = new StackedContents();
            player.getData(ModAttachments.PLAYER_DATA).inventory().backingStacks().forEach(contents::accountSimpleStack);
            recipeMenu.fillCraftSlotsStackedContents(contents);
            if (!contents.canCraft(recipe.value(), null)) {
                return helper.createUserErrorForMissingSlots(
                        Component.translatable("message.bundlednotsiloed.recipe_missing"),
                        recipeSlots.getSlotViews(RecipeIngredientRole.INPUT));
            }

            if (doTransfer) PacketDistributor.sendToServer(new RecipeTransferPayload(recipe.id(), maxTransfer));
            return null;
        }
    }
}
