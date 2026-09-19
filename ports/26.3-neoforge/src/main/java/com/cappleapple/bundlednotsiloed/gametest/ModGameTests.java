package com.cappleapple.bundlednotsiloed.gametest;
import java.util.function.Consumer;
import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, BundledNotSiloed.MOD_ID);
    static {
        FUNCTIONS.register("native_item_capability_sees_backend_and_rolls_back", () -> InventoryCompatibilityGameTests::nativeItemCapabilitySeesBackendAndRollsBack);
        FUNCTIONS.register("clear_visits_hidden_logical_storage", () -> InventoryCompatibilityGameTests::clearVisitsHiddenLogicalStorage);
        FUNCTIONS.register("creative_pick_and_placement_use_logical_storage", () -> InventoryCompatibilityGameTests::creativePickAndPlacementUseLogicalStorage);
        FUNCTIONS.register("creative_cursor_transactions_use_hidden_storage", () -> InventoryCompatibilityGameTests::creativeCursorTransactionsUseHiddenStorage);
        FUNCTIONS.register("external_quick_move_honors_selected_new_item_destination", () -> InventoryCompatibilityGameTests::externalQuickMoveHonorsSelectedNewItemDestination);
        FUNCTIONS.register("player_inventory_quick_move_uses_direction_by_source", () -> InventoryCompatibilityGameTests::playerInventoryQuickMoveUsesDirectionBySource);
        FUNCTIONS.register("recipe_inventory_hooks_detect_and_extract_stowed_ingredients", () -> InventoryCompatibilityGameTests::recipeInventoryHooksDetectAndExtractStowedIngredients);
        FUNCTIONS.register("identical_vanilla_writes_keep_live_item_references", () -> InventoryNetworkGameTests::identicalVanillaWritesKeepLiveItemReferences);
        FUNCTIONS.register("sophisticated_hover_context_and_open_item_stay_stable", () -> helper -> { try { InventoryNetworkGameTests.sophisticatedHoverContextAndOpenItemStayStable(helper); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); } });
    }
    public static void register(IEventBus bus) { FUNCTIONS.register(bus); }
}
