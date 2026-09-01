package com.cappleapple.bundlednotsiloed.compat.emi;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.compat.RecipeTransferDestination;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.RecipeTransferPayload;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.handler.CraftingRecipeHandler;
import dev.emi.emi.handler.InventoryRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/** Supplies complete logical-inventory crafting data to EMI's native screen integration. */
@EmiEntrypoint
public final class BundledNotSiloedEmiPlugin implements EmiPlugin {
    private static final List<ResourceLocation> COMPATIBLE_CRAFTING_MENU_TYPES = List.of(
            ResourceLocation.fromNamespaceAndPath("visualworkbench", "crafting")
    );

    @Override
    public void register(EmiRegistry registry) {
        BnsInventoryRecipeHandler inventoryHandler = new BnsInventoryRecipeHandler();
        BnsCraftingRecipeHandler craftingHandler = new BnsCraftingRecipeHandler();
        registry.addRecipeHandler((MenuType<InventoryMenu>)null, inventoryHandler);
        prioritize((MenuType<?>)null, inventoryHandler);
        registerCraftingHandler(registry, MenuType.CRAFTING, craftingHandler);
        for (ResourceLocation id : COMPATIBLE_CRAFTING_MENU_TYPES) {
            BuiltInRegistries.MENU.getOptional(id).ifPresent(menuType -> {
                // Visual Workbench subclasses CraftingMenu but returns its own MenuType.
                // EMI dispatches by that type rather than by the menu's Java superclass.
                @SuppressWarnings("unchecked")
                MenuType<CraftingMenu> compatibleType = (MenuType<CraftingMenu>)(MenuType<?>)menuType;
                registerCraftingHandler(registry, compatibleType, craftingHandler);
                BundledNotSiloed.LOGGER.info("Registered full-inventory EMI crafting handler for {}", id);
            });
        }
    }

    private static void registerCraftingHandler(EmiRegistry registry, MenuType<CraftingMenu> menuType,
                                                BnsCraftingRecipeHandler handler) {
        registry.addRecipeHandler(menuType, handler);
        prioritize(menuType, handler);
    }

    /** EMI keeps the first supporting handler; BNS must precede its vanilla click-based handlers. */
    private static void prioritize(MenuType<?> menuType, Object handler) {
        List<?> handlers = EmiRecipeFiller.handlers.get(menuType);
        if (handlers == null || !handlers.remove(handler)) return;
        @SuppressWarnings("unchecked") List<Object> mutable = (List<Object>)handlers;
        mutable.addFirst(handler);
    }

    private static EmiPlayerInventory fullInventory(List<Slot> craftingSlots) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return new EmiPlayerInventory(List.of());
        ArrayList<EmiStack> stacks = new ArrayList<>();
        minecraft.player.getData(ModAttachments.PLAYER_DATA).inventory().backingStacks().stream()
                .filter(stack -> !stack.isEmpty()).map(EmiStack::of).forEach(stacks::add);
        craftingSlots.stream().filter(java.util.Objects::nonNull).map(Slot::getItem)
                .filter(stack -> !stack.isEmpty()).map(EmiStack::of).forEach(stacks::add);
        return new EmiPlayerInventory(stacks);
    }

    private static boolean transferable(EmiRecipe recipe, int gridWidth, int gridHeight) {
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        return backing != null && backing.value() instanceof CraftingRecipe craftingRecipe
                && craftingRecipe.canCraftInDimensions(gridWidth, gridHeight);
    }

    private static boolean canCraft(EmiRecipe recipe, EmiCraftContext<?> context) {
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || backing == null
                || !(backing.value() instanceof CraftingRecipe craftingRecipe)
                || !(context.getScreenHandler() instanceof RecipeBookMenu<?, ?> recipeMenu)) return false;

        // Match EMI's StandardRecipeHandler semantics: canCraft answers whether one batch is
        // possible. EMI uses Integer.MAX_VALUE to request "all" and caps the actual placement
        // later; treating that sentinel as a required craft count rejects every Shift-click.
        StackedContents contents = new StackedContents();
        minecraft.player.getInventory().fillStackedContents(contents);
        recipeMenu.fillCraftSlotsStackedContents(contents);
        return contents.canCraft(craftingRecipe, null);
    }

    private static boolean transfer(EmiRecipe recipe, EmiCraftContext<?> context) {
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        if (backing == null || !(backing.value() instanceof CraftingRecipe)) return false;
        RecipeTransferDestination destination = switch (context.getDestination()) {
            case NONE -> RecipeTransferDestination.NONE;
            case CURSOR -> RecipeTransferDestination.CURSOR;
            case INVENTORY -> RecipeTransferDestination.INVENTORY;
        };
        PacketDistributor.sendToServer(new RecipeTransferPayload(
                backing.id(), context.getAmount() > 1, destination));
        return true;
    }

    private static final class BnsInventoryRecipeHandler extends InventoryRecipeHandler {
        @Override public EmiPlayerInventory getInventory(AbstractContainerScreen<InventoryMenu> screen) {
            return fullInventory(getCraftingSlots(screen.getMenu()));
        }

        @Override public boolean supportsRecipe(EmiRecipe recipe) {
            // JEMI wraps some ordinary CraftingRecipes in a JEI-owned category instead of
            // VanillaEmiRecipeCategories.CRAFTING. The authoritative backing recipe and its
            // dimensions decide support; requiring EMI's category would fall back to the
            // visible-slot-only JEMI transfer handler.
            return transferable(recipe, 2, 2);
        }

        @Override public boolean canCraft(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
            return supportsRecipe(recipe) && BundledNotSiloedEmiPlugin.canCraft(recipe, context);
        }

        @Override public boolean craft(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
            return transfer(recipe, context);
        }
    }

    private static final class BnsCraftingRecipeHandler extends CraftingRecipeHandler {
        @Override public EmiPlayerInventory getInventory(AbstractContainerScreen<CraftingMenu> screen) {
            return fullInventory(getCraftingSlots(screen.getMenu()));
        }

        @Override public boolean supportsRecipe(EmiRecipe recipe) {
            return transferable(recipe, 3, 3);
        }

        @Override public boolean canCraft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
            return supportsRecipe(recipe) && BundledNotSiloedEmiPlugin.canCraft(recipe, context);
        }

        @Override public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
            return transfer(recipe, context);
        }
    }
}
