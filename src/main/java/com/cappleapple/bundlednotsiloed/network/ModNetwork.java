package com.cappleapple.bundlednotsiloed.network;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.data.InventorySlotWindow;
import com.cappleapple.bundlednotsiloed.data.PlayerInventoryData;
import com.cappleapple.stacksnotslots.api.inventory.DynamicCapacityInventory;
import com.cappleapple.bundlednotsiloed.client.ClientTransientState;
import com.cappleapple.bundlednotsiloed.client.ClientSaveState;
import com.cappleapple.bundlednotsiloed.client.CreativeInventoryCursor;
import com.cappleapple.bundlednotsiloed.category.CategoryDefinition;
import com.cappleapple.bundlednotsiloed.category.CategoryPresetManager;
import com.cappleapple.bundlednotsiloed.category.CategoryRule;
import com.cappleapple.bundlednotsiloed.category.PlayerCategoryData;
import com.cappleapple.bundlednotsiloed.compat.InventoryProjection;
import com.cappleapple.bundlednotsiloed.compat.RecipeTransferService;
import com.cappleapple.stacksnotslots.api.ExtractionResult;
import com.cappleapple.bundlednotsiloed.hotbar.BindingType;
import com.cappleapple.bundlednotsiloed.hotbar.HotbarBinding;
import com.cappleapple.bundlednotsiloed.hotbar.HotbarBindings;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.category.SortMode;
import com.cappleapple.bundlednotsiloed.inventory.InventoryTransactions;
import com.cappleapple.bundlednotsiloed.inventory.InventoryCursorTransactions;
import com.cappleapple.bundlednotsiloed.inventory.ContainerTransfers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetwork {
    public static final int SNAPSHOT_CHUNK_SIZE = 256;
    /** Byte-limited chunks can contain just one logical slot. */
    public static final int MAX_SNAPSHOT_CHUNKS = Integer.MAX_VALUE;
    public static final int MAX_DELTA_CHANGES = 4096;
    private static final int MAX_PLAYER_CATEGORIES = 256;
    private static final int MAX_CATEGORY_RULES_PER_SIDE = 128;
    private static final int MAX_CATEGORY_NAME_LENGTH = 64;
    private static final int MAX_CATEGORY_REGEX_LENGTH = 512;
    private static final int MAX_CLIENT_ACTIONS_PER_SECOND = 80;
    private static final Map<UUID, String> REPORTED_SYNC_FAILURES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FAILED_SYNC_RETRY = new ConcurrentHashMap<>();
    private static final Set<UUID> DIRTY_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> OPEN_BROWSERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, SentState> SENT_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, SnapshotAssembly> CLIENT_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, BrowserStatePayload> BROWSER_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, InventoryWindowPayload> PENDING_WINDOWS = new ConcurrentHashMap<>();
    private static final Set<UUID> FULL_SYNC_REQUESTS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> NEXT_FULL_SYNC = new ConcurrentHashMap<>();
    private static final Map<UUID, ActionRate> ACTION_RATES = new ConcurrentHashMap<>();

    private ModNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("12");
        registrar.playToClient(InventorySyncStatusPayload.TYPE, InventorySyncStatusPayload.STREAM_CODEC, (payload, context) -> {
            context.player().getData(ModAttachments.PLAYER_DATA).clientSync().block();
            context.player().displayClientMessage(Component.translatable("message.bundlednotsiloed.sync_failed",
                    payload.itemId(), payload.logicalSlot()), false);
        });
        registrar.playToClient(InventorySnapshotPayload.TYPE, InventorySnapshotPayload.STREAM_CODEC, ModNetwork::receiveSnapshot);
        registrar.playToClient(InventoryDeltaPayload.TYPE, InventoryDeltaPayload.STREAM_CODEC, ModNetwork::receiveDelta);
        registrar.playToClient(PlayerMetadataPayload.TYPE, PlayerMetadataPayload.STREAM_CODEC, ModNetwork::receiveMetadata);
        registrar.playToServer(PlayerCustomizationPayload.TYPE, PlayerCustomizationPayload.STREAM_CODEC, ModNetwork::updatePlayerCustomization);
        registrar.playToServer(RequestFullSyncPayload.TYPE, RequestFullSyncPayload.STREAM_CODEC, ModNetwork::requestFullSync);
        registrar.playToServer(InventoryActionPayload.TYPE, InventoryActionPayload.STREAM_CODEC, ModNetwork::inventoryAction);
        registrar.playToServer(CreativeInventoryStowPayload.TYPE, CreativeInventoryStowPayload.STREAM_CODEC, ModNetwork::stowCreativeInventoryCursor);
        registrar.playToServer(CreativeInventoryTakePayload.TYPE, CreativeInventoryTakePayload.STREAM_CODEC, ModNetwork::takeCreativeInventoryCursor);
        registrar.playToClient(CreativeInventoryCursorPayload.TYPE, CreativeInventoryCursorPayload.STREAM_CODEC, ModNetwork::receiveCreativeInventoryCursor);
        registrar.playToServer(HotbarCyclePayload.TYPE, HotbarCyclePayload.STREAM_CODEC, ModNetwork::cycleHotbar);
        registrar.playToClient(HotbarCycleResultPayload.TYPE, HotbarCycleResultPayload.STREAM_CODEC, ModNetwork::cycleHotbarResult);
        registrar.playToServer(CategoryEditPayload.TYPE, CategoryEditPayload.STREAM_CODEC, ModNetwork::editCategory);
        registrar.playToServer(HotbarBindPayload.TYPE, HotbarBindPayload.STREAM_CODEC, ModNetwork::bindHotbar);
        registrar.playToServer(InventoryViewPreferencesPayload.TYPE, InventoryViewPreferencesPayload.STREAM_CODEC, ModNetwork::updateViewPreferences);
        registrar.playToClient(InventoryWindowResultPayload.TYPE, InventoryWindowResultPayload.STREAM_CODEC,
                (payload, context) -> com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows.acknowledge(payload));
        registrar.playToServer(InventoryWindowPayload.TYPE, InventoryWindowPayload.STREAM_CODEC, ModNetwork::updateInventoryWindow);
        registrar.playToServer(StowSlotPayload.TYPE, StowSlotPayload.STREAM_CODEC, ModNetwork::stowSlot);
        registrar.playToServer(StowMainGridPayload.TYPE, StowMainGridPayload.STREAM_CODEC, ModNetwork::stowMainGrid);
        registrar.playToServer(ClearCreativeInventoryPayload.TYPE, ClearCreativeInventoryPayload.STREAM_CODEC, ModNetwork::clearCreativeInventory);
        registrar.playToServer(NewItemDestinationPayload.TYPE, NewItemDestinationPayload.STREAM_CODEC, ModNetwork::updateNewItemDestination);
        registrar.playToServer(AutoRefillPayload.TYPE, AutoRefillPayload.STREAM_CODEC, ModNetwork::updateAutoRefill);
        registrar.playToServer(RecipeTransferPayload.TYPE, RecipeTransferPayload.STREAM_CODEC, ModNetwork::transferRecipe);
        registrar.playToServer(BrowserTransferPayload.TYPE, BrowserTransferPayload.STREAM_CODEC, ModNetwork::transferBrowserEntry);
        registrar.playToServer(BrowserStatePayload.TYPE, BrowserStatePayload.STREAM_CODEC, ModNetwork::browserState);
        registrar.playToServer(BulkTransferPayload.TYPE, BulkTransferPayload.STREAM_CODEC, ModNetwork::bulkTransfer);
        registrar.playToClient(BulkTransferResultPayload.TYPE, BulkTransferResultPayload.STREAM_CODEC, ModNetwork::bulkTransferResult);
        registrar.playToClient(PickupFeedbackPayload.TYPE, PickupFeedbackPayload.STREAM_CODEC, ModNetwork::pickupFeedback);
    }

    public static void queueInventorySync(Player player) {
        if (player instanceof ServerPlayer) DIRTY_PLAYERS.add(player.getUUID());
    }

    public static void sendInitial(ServerPlayer player) {
        sendSnapshot(player);
        sendMetadata(player);
    }

    public static void sendMetadata(ServerPlayer player) {
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        PacketDistributor.sendToPlayer(player, new PlayerMetadataPayload(data.saveMetadata(player.registryAccess())));
    }

    public static void flush(MinecraftServer server) {
        long now = System.nanoTime();
        for (UUID id : List.copyOf(FULL_SYNC_REQUESTS)) {
            if (awaitingRetry(NEXT_FULL_SYNC, id, now)) continue;
            FULL_SYNC_REQUESTS.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                if (!sendSnapshot(player)) FULL_SYNC_REQUESTS.add(id);
                NEXT_FULL_SYNC.put(id, now + java.util.concurrent.TimeUnit.SECONDS.toNanos(1));
                DIRTY_PLAYERS.remove(id);
            }
        }
        for (UUID id : List.copyOf(PENDING_WINDOWS.keySet())) {
            InventoryWindowPayload window = PENDING_WINDOWS.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && window != null) applyInventoryWindow(player, window);
        }
        if (DIRTY_PLAYERS.isEmpty()) return;
        List<UUID> queued = List.copyOf(DIRTY_PLAYERS);
        DIRTY_PLAYERS.removeAll(queued);
        for (UUID id : queued) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) sendDeltaOrSnapshot(player);
        }
    }

    private static boolean awaitingRetry(Map<UUID, Long> deadlines, UUID id, long now) {
        Long deadline = deadlines.get(id);
        return deadline != null && now - deadline < 0;
    }

    public static void forget(UUID playerId) {
        DIRTY_PLAYERS.remove(playerId);
        SENT_STATES.remove(playerId);
        ACTION_RATES.remove(playerId);
        OPEN_BROWSERS.remove(playerId);
        BROWSER_SESSIONS.remove(playerId);
        PENDING_WINDOWS.remove(playerId);
        FULL_SYNC_REQUESTS.remove(playerId);
        NEXT_FULL_SYNC.remove(playerId);
        FAILED_SYNC_RETRY.remove(playerId);
        REPORTED_SYNC_FAILURES.remove(playerId);
    }

    public static void clearClientSync() { CLIENT_SNAPSHOTS.clear(); }

    public static boolean isInventorySyncBlocked(Player player) { return FAILED_SYNC_RETRY.containsKey(player.getUUID()); }

    public static boolean isBrowserOpen(Player player) { return OPEN_BROWSERS.contains(player.getUUID()); }

    private static boolean sendDeltaOrSnapshot(ServerPlayer player) {
        if (awaitingRetry(FAILED_SYNC_RETRY, player.getUUID(), System.nanoTime())) return false;
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        List<ItemStack> current = inventory.backingStacks();
        SentState previous = SENT_STATES.get(player.getUUID());
        if (previous == null) return sendSnapshot(player);
        ArrayList<InventoryDeltaPayload.SlotChange> changes = new ArrayList<>();
        int compared = Math.min(previous.stacks.size(), current.size());
        for (int i = 0; i < compared; i++) {
            if (!ItemStack.matches(previous.stacks.get(i), current.get(i))) changes.add(new InventoryDeltaPayload.SlotChange(i, current.get(i)));
        }
        for (int i = compared; i < current.size(); i++) changes.add(new InventoryDeltaPayload.SlotChange(i, current.get(i)));
        if (changes.size() > MAX_DELTA_CHANGES || changes.size() > Math.max(64, current.size() / 2)) {
            return sendSnapshot(player);
        }
        long changedBytes = 0;
        for (var change : changes) {
            int bytes = validateSyncStack(player, change.index(), change.stack());
            if (bytes < 0) return false;
            changedBytes += bytes;
        }
        if (changedBytes > InventorySnapshotPlan.TARGET_BYTES) return sendSnapshot(player);
        PacketDistributor.sendToPlayer(player, new InventoryDeltaPayload(previous.revision, inventory.revision(), current.size(), changes));
        SENT_STATES.put(player.getUUID(), new SentState(inventory.revision(), current));
        FAILED_SYNC_RETRY.remove(player.getUUID());
        REPORTED_SYNC_FAILURES.remove(player.getUUID());
        return true;
    }

    private static boolean sendSnapshot(ServerPlayer player) {
        if (awaitingRetry(FAILED_SYNC_RETRY, player.getUUID(), System.nanoTime())) return false;
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        List<ItemStack> stacks = inventory.backingStacks();
        // Validate all items before sending the first chunk. Bound batches by bytes as well as slot count.
        List<Integer> sizes = new ArrayList<>(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            int bytes = validateSyncStack(player, i, stacks.get(i));
            if (bytes < 0) return false;
            sizes.add(bytes);
        }
        var chunks = InventorySnapshotPlan.chunks(sizes);
        UUID snapshotId = UUID.randomUUID();
        for (int index = 0; index < chunks.size(); index++) {
            var chunk = chunks.get(index);
            PacketDistributor.sendToPlayer(player, new InventorySnapshotPayload(snapshotId, inventory.revision(),
                    index, chunks.size(), stacks.subList(chunk.from(), chunk.to())));
        }
        SENT_STATES.put(player.getUUID(), new SentState(inventory.revision(), stacks));
        FAILED_SYNC_RETRY.remove(player.getUUID());
        REPORTED_SYNC_FAILURES.remove(player.getUUID());
        return true;
    }

    private static int validateSyncStack(ServerPlayer player, int slot, ItemStack stack) {
        try {
            return InventorySyncPreflight.encodedSize(player.registryAccess(), stack);
        } catch (RuntimeException exception) {
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String problem = slot + ":" + itemId;
            if (!problem.equals(REPORTED_SYNC_FAILURES.put(player.getUUID(), problem))) {
                BundledNotSiloed.LOGGER.error("Inventory sync paused for player {} ({}) at logical slot {}, item {}. "
                    + "The item remains in server storage; its network codec failed preflight.",
                        player.getGameProfile().getName(), player.getUUID(), slot, itemId, exception);
                PacketDistributor.sendToPlayer(player, new InventorySyncStatusPayload(slot,
                        itemId.substring(0, Math.min(256, itemId.length()))));
            }
            FAILED_SYNC_RETRY.put(player.getUUID(), System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5));
            FULL_SYNC_REQUESTS.add(player.getUUID());
            return -1;
        }
    }

    private static void receiveSnapshot(InventorySnapshotPayload payload, IPayloadContext context) {
        long expiry = System.nanoTime() - java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
        CLIENT_SNAPSHOTS.values().removeIf(assembly -> assembly.createdAtNanos < expiry);
        SnapshotAssembly assembly = CLIENT_SNAPSHOTS.computeIfAbsent(payload.snapshotId(), ignored -> new SnapshotAssembly(payload.revision(), payload.chunkCount()));
        if (assembly.revision != payload.revision() || assembly.chunkCount != payload.chunkCount()) {
            CLIENT_SNAPSHOTS.remove(payload.snapshotId());
            return;
        }
        assembly.chunks.put(payload.chunkIndex(), payload.stacks());
        if (assembly.chunks.size() == assembly.chunkCount) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < assembly.chunkCount; i++) stacks.addAll(assembly.chunks.get(i));
            PlayerInventoryData data = context.player().getData(ModAttachments.PLAYER_DATA);
            data.clientSync().snapshot(data.inventory(), assembly.revision, stacks);
            data.setMigratedVanillaInventory();
            com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows.inventorySynced();
            CLIENT_SNAPSHOTS.remove(payload.snapshotId());
        }
    }

    private static void receiveDelta(InventoryDeltaPayload payload, IPayloadContext context) {
        PlayerInventoryData data = context.player().getData(ModAttachments.PLAYER_DATA);
        boolean applied = data.clientSync().delta(data.inventory(), payload);
        if (applied) {
            data.syncVanillaCompatibilityView();
            com.cappleapple.bundlednotsiloed.client.ClientInventoryWindows.inventorySynced();
        }
        if (!applied && data.clientSync().requestRecovery(System.nanoTime())) {
            PacketDistributor.sendToServer(new RequestFullSyncPayload());
        }
    }

    private static void receiveMetadata(PlayerMetadataPayload payload, IPayloadContext context) {
        PlayerInventoryData data = context.player().getData(ModAttachments.PLAYER_DATA);
        data.loadMetadata(context.player().registryAccess(), payload.data());
        ClientSaveState.receiveMetadata(context.player(), data);
    }

    private static void updatePlayerCustomization(PlayerCustomizationPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        if (!validCustomization(payload.data(), player)) {
            BundledNotSiloed.LOGGER.warn("Rejected invalid client customization from {}", player.getGameProfile().getName());
            sendMetadata(player);
            return;
        }
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        restorePlayerCustomization(data, player.registryAccess(), payload.data());
        sendMetadata(player);
        player.inventoryMenu.broadcastChanges();
    }

    /** Restores client-owned preferences without replaying the one-shot visible-inventory arrangement. */
    static void restorePlayerCustomization(
            PlayerInventoryData data, HolderLookup.Provider provider, CompoundTag customization
    ) {
        data.loadCustomization(provider, customization);
        CategoryPresetManager.upgradeLegacyDefaults(data.categories());
    }

    private static boolean validCustomization(net.minecraft.nbt.CompoundTag root, ServerPlayer player) {
        var categoryTag = root.getCompound("Categories");
        int encodedCategoryCount = categoryTag.getList("Categories", Tag.TAG_COMPOUND).size();
        if (encodedCategoryCount > MAX_PLAYER_CATEGORIES) return false;

        PlayerCategoryData categories = new PlayerCategoryData();
        categories.load(categoryTag);
        if (categories.categories().size() != encodedCategoryCount) return false;
        java.util.HashSet<ResourceLocation> categoryIds = new java.util.HashSet<>();
        for (CategoryDefinition category : categories.categories()) {
            if (!categoryIds.add(category.id()) || !validCategory(category, categories)) return false;
        }

        HotbarBindings hotbar = new HotbarBindings();
        var hotbarTag = root.getCompound("Hotbar");
        if (hotbarTag.getList("Bindings", Tag.TAG_COMPOUND).size() > HotbarBindings.SLOT_COUNT) return false;
        hotbar.load(player.registryAccess(), hotbarTag);
        for (int slot = 0; slot < HotbarBindings.SLOT_COUNT; slot++) {
            HotbarBinding binding = hotbar.get(slot);
            if (binding.type() == BindingType.CATEGORY && categories.find(binding.target()) == null) return false;
        }

        try {
            SortMode.valueOf(root.getString("InventorySortPreference"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        String selectedValue = root.getString("SelectedCategoryPreference");
        ResourceLocation selected = selectedValue.isBlank() ? null : ResourceLocation.tryParse(selectedValue);
        if (!selectedValue.isBlank() && (selected == null || categories.find(selected) == null)) return false;
        return true;
    }

    private static void requestFullSync(RequestFullSyncPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) FULL_SYNC_REQUESTS.add(player.getUUID());
    }

    private static void inventoryAction(InventoryActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        DynamicCapacityInventory inventory = player.getData(ModAttachments.PLAYER_DATA).inventory();
        ItemStack prototype = payload.prototype();
        int firstBrowserSlot = isBrowserOpen(player) ? 9 : 36;
        int amount;
        switch (payload.action()) {
            case DROP_ONE -> amount = 1;
            case DROP_STACK -> amount = prototype.getMaxStackSize();
            case TAKE_STACK, TAKE_HALF -> {
                ItemStack carried = player.containerMenu.getCarried();
                if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, prototype)) return;
                int space = carried.isEmpty() ? prototype.getMaxStackSize() : carried.getMaxStackSize() - carried.getCount();
                if (space <= 0) return;
                ItemStack extracted = InventoryCursorTransactions.takeFromRange(
                        inventory, prototype, space, payload.action() == InventoryActionPayload.Action.TAKE_HALF,
                        firstBrowserSlot);
                if (extracted.isEmpty()) return;
                if (carried.isEmpty()) player.containerMenu.setCarried(extracted);
                else carried.grow(extracted.getCount());
                player.containerMenu.broadcastChanges();
                return;
            }
            default -> throw new IllegalStateException("Unhandled inventory action");
        }
        ExtractionResult extraction = inventory.extractAtOrAfter(prototype, amount, firstBrowserSlot, false);
        extraction.extractedStacks().forEach(stack -> player.drop(stack, false));
    }

    private static void cycleHotbar(HotbarCyclePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        if (com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard.blocksArrangement(player)) return;
        if (payload.slot() < 0 || payload.slot() >= 9 || Math.abs(payload.direction()) != 1) return;
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        ItemStack selected = data.hotbar().cycle(payload.slot(), payload.direction(), data.inventory(), data.categories());
        CategoryDefinition category = data.hotbar().get(payload.slot()).target() == null ? null : data.categories().find(data.hotbar().get(payload.slot()).target());
        sendMetadata(player);
        PacketDistributor.sendToPlayer(player, new HotbarCycleResultPayload(category == null ? "Hotbar" : category.displayName(), selected));
        player.inventoryMenu.broadcastChanges();
    }

    private static void cycleHotbarResult(HotbarCycleResultPayload payload, IPayloadContext context) {
        ClientTransientState.showCycleOverlay(payload.bindingName(), payload.selected());
    }

    private static boolean allowAction(ServerPlayer player) {
        if (isInventorySyncBlocked(player)) return false;
        long second = System.currentTimeMillis() / 1000L;
        ActionRate rate = ACTION_RATES.computeIfAbsent(player.getUUID(), ignored -> new ActionRate());
        if (rate.second != second) { rate.second = second; rate.count = 0; }
        return ++rate.count <= MAX_CLIENT_ACTIONS_PER_SECOND;
    }

    private static void editCategory(CategoryEditPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        PlayerCategoryData categories = player.getData(ModAttachments.PLAYER_DATA).categories();
        switch (payload.operation()) {
            case RESET -> CategoryPresetManager.reset(categories);
            case DELETE -> {
                String encodedId = payload.category().getString("Id");
                var id = net.minecraft.resources.ResourceLocation.tryParse(encodedId);
                if (id != null) categories.remove(id);
            }
            case UPSERT -> {
                CategoryDefinition definition = PlayerCategoryData.loadCategory(payload.category());
                if (definition == null || !validCategory(definition, categories)) return;
                categories.upsert(definition);
            }
        }
        sendMetadata(player);
    }

    private static boolean validCategory(CategoryDefinition category, PlayerCategoryData existing) {
        boolean mayAdd = existing.find(category.id()) != null || existing.categories().size() < MAX_PLAYER_CATEGORIES;
        return mayAdd
                && !category.displayName().isBlank() && category.displayName().length() <= MAX_CATEGORY_NAME_LENGTH
                && category.includes().size() <= MAX_CATEGORY_RULES_PER_SIDE && category.excludes().size() <= MAX_CATEGORY_RULES_PER_SIDE
                && category.includes().stream().allMatch(ModNetwork::validCategoryRule)
                && category.excludes().stream().allMatch(ModNetwork::validCategoryRule)
                && category.pickupLimit() >= -1 && category.pickupLimit() <= Integer.MAX_VALUE;
    }

    private static boolean validCategoryRule(CategoryRule rule) {
        return rule.type() != CategoryRule.Type.REGEX
                || rule.expression() != null && rule.expression().length() <= MAX_CATEGORY_REGEX_LENGTH;
    }

    private static void bindHotbar(HotbarBindPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player) || payload.slot() < 0 || payload.slot() >= 9) return;
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        if (payload.bindingType() == BindingType.CATEGORY && data.categories().find(payload.target()) == null) return;
        if (payload.bindingType() != BindingType.EMPTY && payload.bindingType() != BindingType.CATEGORY) return;
        data.hotbar().set(payload.slot(), payload.bindingType() == BindingType.EMPTY
                ? HotbarBinding.empty()
                : new HotbarBinding(payload.bindingType(), payload.target(), null));
        sendMetadata(player);
        player.inventoryMenu.broadcastChanges();
    }

    private static void updateViewPreferences(InventoryViewPreferencesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        if (payload.selectedCategory() != null && data.categories().find(payload.selectedCategory()) == null) return;
        data.setInventorySortPreference(payload.sortMode());
        data.setSelectedCategoryPreference(payload.selectedCategory());
        if (!com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard.blocksArrangement(player)) InventoryProjection.applyExplicitView(data);
        sendMetadata(player);
        player.inventoryMenu.broadcastChanges();
    }

    private static void updateInventoryWindow(InventoryWindowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BrowserStatePayload session = BROWSER_SESSIONS.get(player.getUUID());
        if (session == null || session.session() != payload.session()
                || session.containerId() != payload.containerId() || player.containerMenu.containerId != payload.containerId()) return;
        // Coalesce navigation separately from gameplay actions; never silently drop the final page.
        PENDING_WINDOWS.put(player.getUUID(), payload);
    }

    private static void applyInventoryWindow(ServerPlayer player, InventoryWindowPayload payload) {
        BrowserStatePayload session = BROWSER_SESSIONS.get(player.getUUID());
        if (session == null || session.session() != payload.session()
                || player.containerMenu.containerId != payload.containerId()) return;
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        long maximum = (long)Math.max(InventorySlotWindow.MAIN_START, data.inventory().syntheticSlotCount())
                + InventorySlotWindow.VISIBLE_SLOTS;
        List<Integer> slots = payload.slots();
        if (slots.stream().anyMatch(index -> index >= maximum)) {
            // A shrink can invalidate a requested page. Return an explicit correction.
            int first = InventorySlotWindow.maximumRangeStart(data.inventory().syntheticSlotCount());
            slots = java.util.stream.IntStream.range(first, first + InventorySlotWindow.VISIBLE_SLOTS).boxed().toList();
        }
        if (!sendDeltaOrSnapshot(player)) {
            PENDING_WINDOWS.put(player.getUUID(), payload);
            return;
        }
        data.showInventorySlots(slots, payload.identities());
        PacketDistributor.sendToPlayer(player, new InventoryWindowResultPayload(new InventoryWindowPayload(
                payload.session(), payload.request(), payload.containerId(), payload.identities(), slots), data.inventory().revision()));
        player.containerMenu.broadcastChanges();
    }

    private static void stowSlot(StowSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)
                || payload.slot() < -1 || payload.slot() >= 36) return;
        if (com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard.blocksArrangement(player)) return;
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        if (payload.slot() >= 0) {
            if (data.inventory().stowSyntheticSlot(payload.slot())) player.containerMenu.broadcastChanges();
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) return;
        var insertion = InventoryTransactions.insertIntoBackend(player, carried, false);
        if (!insertion.acceptedAnything()) return;
        carried.shrink(insertion.acceptedAmount());
        if (carried.isEmpty()) player.containerMenu.setCarried(ItemStack.EMPTY);
        player.containerMenu.broadcastChanges();
    }

    private static void stowCreativeInventoryCursor(CreativeInventoryStowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.gameMode.isCreative() || !allowAction(player)) return;
        ItemStack carried = payload.carried();
        if (carried.isEmpty() || carried.getCount() > carried.getMaxStackSize()
                || !carried.isItemEnabled(player.level().enabledFeatures())) return;
        var insertion = InventoryTransactions.insertIntoBackend(player, carried, false);
        PacketDistributor.sendToPlayer(player, new CreativeInventoryCursorPayload(insertion.remainder()));
        player.inventoryMenu.broadcastChanges();
    }

    private static void takeCreativeInventoryCursor(CreativeInventoryTakePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.gameMode.isCreative() || !allowAction(player)) return;
        ItemStack prototype = payload.prototype();
        ItemStack extracted = InventoryCursorTransactions.takeFromBackend(
                player.getData(ModAttachments.PLAYER_DATA).inventory(),
                prototype,
                prototype.getMaxStackSize(),
                payload.takeHalf());
        if (extracted.isEmpty()) return;
        PacketDistributor.sendToPlayer(player, new CreativeInventoryCursorPayload(extracted));
        player.inventoryMenu.broadcastChanges();
    }

    private static void receiveCreativeInventoryCursor(CreativeInventoryCursorPayload payload, IPayloadContext context) {
        CreativeInventoryCursor.replace(payload.carried());
    }

    private static void updateNewItemDestination(NewItemDestinationPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setNewItemDestination(payload.destination());
        sendMetadata(player);
    }

    private static void updateAutoRefill(AutoRefillPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        PlayerInventoryData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setAutoRefill(payload.enabled());
        sendMetadata(player);
    }

    private static void transferRecipe(RecipeTransferPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        if (com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard.blocksArrangement(player)) return;
        RecipeTransferService.transfer(player, payload.recipeId(), payload.placeAll(), payload.destination());
    }

    private static void stowMainGrid(StowMainGridPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        if (com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard.blocksArrangement(player)) return;
        if (player.getData(ModAttachments.PLAYER_DATA).inventory().stowMainGrid()) player.containerMenu.broadcastChanges();
    }

    private static void clearCreativeInventory(ClearCreativeInventoryPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.gameMode.isCreative() || !allowAction(player)) return;
        if (com.cappleapple.bundlednotsiloed.compat.OpenBackpackGuard.blocksArrangement(player)) return;
        player.getData(ModAttachments.PLAYER_DATA).inventory().clear();
        player.inventoryMenu.broadcastChanges();
    }

    private static void transferBrowserEntry(BrowserTransferPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        ContainerTransfers.moveBrowserEntryToMenu(player, payload.prototype(), payload.mode());
    }

    private static void browserState(BrowserStatePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BrowserStatePayload current = BROWSER_SESSIONS.get(player.getUUID());
        if (payload.open()) {
            if (player.containerMenu.containerId != payload.containerId()
                    || current != null && current.session() >= payload.session()) return;
            BROWSER_SESSIONS.put(player.getUUID(), payload);
            OPEN_BROWSERS.add(player.getUUID());
            PENDING_WINDOWS.remove(player.getUUID());
        } else if (current != null && current.session() == payload.session()) {
            BROWSER_SESSIONS.remove(player.getUUID());
            OPEN_BROWSERS.remove(player.getUUID());
            PENDING_WINDOWS.remove(player.getUUID());
            player.getData(ModAttachments.PLAYER_DATA).resetInventoryWindow();
            player.containerMenu.broadcastChanges();
        }
    }

    private static void bulkTransfer(BulkTransferPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !allowAction(player)) return;
        List<ContainerTransfers.TransferredStack> moved = ContainerTransfers.bulk(player, payload);
        if (payload.target() == BulkTransferPayload.Target.LOOKED_AT && !moved.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new BulkTransferResultPayload(payload.direction(), moved));
        }
        player.containerMenu.broadcastChanges();
    }

    private static void bulkTransferResult(BulkTransferResultPayload payload, IPayloadContext context) {
        if (!ClientConfig.BULK_TRANSFER_OVERLAY.getAsBoolean()) return;
        ClientTransientState.showTransferOverlay(payload.direction(), payload.stacks(),
                ClientConfig.BULK_TRANSFER_OVERLAY_SECONDS.get());
    }

    private static void pickupFeedback(PickupFeedbackPayload payload, IPayloadContext context) {
        ClientConfig.PickupNotification mode = ClientConfig.PICKUP_NOTIFICATION.get();
        if (mode == ClientConfig.PickupNotification.NONE) return;
        Component message = payload.reason() == com.cappleapple.stacksnotslots.api.InsertionRejection.CATEGORY_LIMIT
                ? Component.translatable("message.bundlednotsiloed.category_limit")
                : Component.translatable("message.bundlednotsiloed.inventory_full", payload.usedCapacity(), payload.capacity());
        if (mode == ClientConfig.PickupNotification.HUD || mode == ClientConfig.PickupNotification.ACTION_BAR || mode == ClientConfig.PickupNotification.HUD_AND_SOUND) {
            context.player().displayClientMessage(message, true);
        }
        if (mode == ClientConfig.PickupNotification.SOUND || mode == ClientConfig.PickupNotification.HUD_AND_SOUND) {
            context.player().playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.7F);
        }
    }

    private record SentState(long revision, List<ItemStack> stacks) {
        private SentState { stacks = stacks.stream().map(ItemStack::copy).toList(); }
    }

    private static final class SnapshotAssembly {
        private final long revision;
        private final int chunkCount;
        private final long createdAtNanos = System.nanoTime();
        private final Map<Integer, List<ItemStack>> chunks = new HashMap<>();
        private SnapshotAssembly(long revision, int chunkCount) { this.revision = revision; this.chunkCount = chunkCount; }
    }

    private static final class ActionRate { private long second; private int count; }
}
