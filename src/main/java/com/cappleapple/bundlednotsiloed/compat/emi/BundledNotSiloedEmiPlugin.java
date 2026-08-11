package com.cappleapple.bundlednotsiloed.compat.emi;

import com.cappleapple.bundlednotsiloed.client.ContainerInventoryOverlay;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.RecipeTransferPayload;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.handler.CraftingRecipeHandler;
import dev.emi.emi.handler.InventoryRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/** Reserves the floating browser on every container screen so EMI lays its ingredient list around it. */
@EmiEntrypoint
public final class BundledNotSiloedEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        // Use EMI's global provider rather than attaching a competing handler to every subclass.
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
            for (Rect2i area : ContainerInventoryOverlay.currentAreas(containerScreen)) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });

        BnsInventoryRecipeHandler inventoryHandler = new BnsInventoryRecipeHandler();
        BnsCraftingRecipeHandler craftingHandler = new BnsCraftingRecipeHandler();
        registry.addRecipeHandler((MenuType<InventoryMenu>)null, inventoryHandler);
        registry.addRecipeHandler(MenuType.CRAFTING, craftingHandler);
        prioritize((MenuType<?>)null, inventoryHandler);
        prioritize(MenuType.CRAFTING, craftingHandler);
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

    private static boolean transferable(EmiRecipe recipe) {
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        return backing != null && backing.value() instanceof CraftingRecipe;
    }

    private static boolean canCraft(EmiRecipe recipe, EmiCraftContext<?> context) {
        return context.getInventory().canCraft(recipe, Math.max(1, context.getAmount()));
    }

    private static boolean transfer(EmiRecipe recipe, EmiCraftContext<?> context) {
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        if (backing == null || !(backing.value() instanceof CraftingRecipe)) return false;
        PacketDistributor.sendToServer(new RecipeTransferPayload(backing.id(), context.getAmount() > 1));
        return true;
    }

    private static final class BnsInventoryRecipeHandler extends InventoryRecipeHandler {
        @Override public EmiPlayerInventory getInventory(AbstractContainerScreen<InventoryMenu> screen) {
            return fullInventory(getCraftingSlots(screen.getMenu()));
        }

        @Override public boolean supportsRecipe(EmiRecipe recipe) {
            return transferable(recipe) && super.supportsRecipe(recipe);
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
            return transferable(recipe) && super.supportsRecipe(recipe);
        }

        @Override public boolean canCraft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
            return supportsRecipe(recipe) && BundledNotSiloedEmiPlugin.canCraft(recipe, context);
        }

        @Override public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
            return transfer(recipe, context);
        }
    }
}
