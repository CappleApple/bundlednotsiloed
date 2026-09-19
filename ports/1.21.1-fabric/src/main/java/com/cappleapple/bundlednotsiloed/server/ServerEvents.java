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

public final class ServerEvents {
    private ServerEvents() {}
    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> playerLogin(handler.player));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> playerLogout(handler.player));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(ModNetwork::flush);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> CategoryPresetManager.reload());
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> ModCommands.register(dispatcher));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> clonePlayer(oldPlayer, newPlayer));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> playerRespawn(newPlayer));
    }

    public static void playerLogin(ServerPlayer player) {
        if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.cappleapple.bundlednotsiloed.network.InventorySnapshotPayload.TYPE)) {
            player.connection.disconnect(net.minecraft.network.chat.Component.literal("This server requires Bundled Not Siloed and Stacks Not Slots on the client."));
            return;
        }
        PlayerInventoryData data = ModAttachments.get(player);
        initializeCapacityBase(player, data);
        migrateVanillaInventory(player, data);
        // Attachment and vanilla inventory load order must not decide which first-36 view wins.
        // Republish the persisted logical positions after the player is fully loaded.
        data.syncVanillaCompatibilityView();
        CategoryPresetManager.initialize(data.categories());
        CategoryPresetManager.upgradeLegacyDefaults(data.categories());
        ModNetwork.sendInitial(player);
    }




    public static void playerLogout(ServerPlayer newPlayer) {
        ModNetwork.forget(newPlayer.getUUID());
        PickupFeedback.forget(newPlayer.getUUID());
    }

    public static void clonePlayer(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        PlayerInventoryData original = ModAttachments.get(oldPlayer);
        PlayerInventoryData replacement = ModAttachments.get(newPlayer);
        replacement.deserializeNBT(newPlayer.registryAccess(), original.serializeNBT(oldPlayer.registryAccess()));
        replacement.loadCustomization(newPlayer.registryAccess(), original.saveCustomization(oldPlayer.registryAccess()));
    }

    public static void playerRespawn(ServerPlayer player) {
        PlayerInventoryData data = ModAttachments.get(player);
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
        AttributeInstance attribute = player.getAttribute(ModAttributes.INVENTORY_CAPACITY);
        if (attribute != null) attribute.setBaseValue(CommonConfig.BASE_CAPACITY.getAsInt());
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
