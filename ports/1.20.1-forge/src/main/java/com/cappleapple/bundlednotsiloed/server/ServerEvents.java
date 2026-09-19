package com.cappleapple.bundlednotsiloed.server;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.config.CommonConfig;
import com.cappleapple.bundlednotsiloed.category.CategoryPresetManager;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import com.cappleapple.bundlednotsiloed.network.ModNetwork;
import com.cappleapple.bundlednotsiloed.pickup.PickupFeedback;
import com.cappleapple.bundlednotsiloed.command.ModCommands;
import java.util.ArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerInventoryData data = com.cappleapple.bundlednotsiloed.data.ModAttachments.get(player);
        initializeCapacityBase(player, data);
        migrateVanillaInventory(player, data);
        // Attachment and vanilla inventory load order must not decide which first-36 view wins.
        // Republish the persisted logical positions after the player is fully loaded.
        data.syncVanillaCompatibilityView();
        CategoryPresetManager.initialize(data.categories());
        CategoryPresetManager.upgradeLegacyDefaults(data.categories());
        ModNetwork.sendInitial(player);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ModNetwork.flush(event.getServer());
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        CategoryPresetManager.reload();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ModNetwork.forget(event.getEntity().getUUID());
        PickupFeedback.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        PlayerInventoryData original = com.cappleapple.bundlednotsiloed.data.ModAttachments.get(event.getOriginal());
        PlayerInventoryData replacement = com.cappleapple.bundlednotsiloed.data.ModAttachments.get(event.getEntity());
        replacement.deserializeNBT(event.getEntity().level().registryAccess(), original.serializeNBT(event.getOriginal().level().registryAccess()));
        replacement.loadCustomization(event.getEntity().level().registryAccess(), original.saveCustomization(event.getOriginal().level().registryAccess()));
    }

    @SubscribeEvent
    public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerInventoryData data = com.cappleapple.bundlednotsiloed.data.ModAttachments.get(player);
        // The replacement player owns a fresh vanilla Inventory and a fresh client attachment.
        // Republish both views after vanilla has finished replacing the entity, then discard the
        // old UUID-keyed delta baseline so the new client attachment receives a complete snapshot.
        data.syncVanillaCompatibilityView();
        player.inventoryMenu.broadcastFullState();
        ModNetwork.forget(player.getUUID());
        ModNetwork.sendInitial(player);
    }

    private static void initializeCapacityBase(ServerPlayer player, PlayerInventoryData data) {
        if (data.initializedCapacityBase()) return;
        AttributeInstance attribute = player.getAttribute(ModAttributes.INVENTORY_CAPACITY.get());
        if (attribute != null) attribute.setBaseValue(CommonConfig.BASE_CAPACITY.get());
        data.setInitializedCapacityBase();
    }

    private static void migrateVanillaInventory(ServerPlayer player, PlayerInventoryData data) {
        if (data.migratedVanillaInventory()) return;
        ArrayList<ItemStack> vanillaStacks = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) if (!stack.isEmpty()) vanillaStacks.add(stack.copy());
        data.inventory().importUnbounded(vanillaStacks);
        player.getInventory().items.clear();
        data.setMigratedVanillaInventory();
        BundledNotSiloed.LOGGER.info("Migrated {} vanilla inventory stacks for {}", vanillaStacks.size(), player.getGameProfile().getName());
    }
}
