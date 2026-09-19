package com.cappleapple.bundlednotsiloed.api;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.stacksnotslots.api.ICapacityInventory;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Public player-integration surface; general inventory creation remains in Stacks Not Slots. */
public final class BundledNotSiloedApi {
    public static final Identifier INVENTORY_CAPACITY_ATTRIBUTE = BundledNotSiloed.id("inventory_capacity");

    private BundledNotSiloedApi() { }

    public static ICapacityInventory inventory(Player player) {
        return ModAttachments.get(player).inventory();
    }

    public static List<CategoryView> categories(Player player) {
        return ModAttachments.get(player).categories().categories().stream()
                .map(CategoryView::fromDefinition)
                .toList();
    }
}
