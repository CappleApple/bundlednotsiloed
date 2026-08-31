# Bundled Not Siloed developer API

Bundled Not Siloed exposes player-overhaul integration under `com.cappleapple.bundlednotsiloed.api`. General capacity inventory APIs live in the separate Stacks Not Slots dependency.

## Player inventory and categories

```java
ICapacityInventory inventory = BundledNotSiloedApi.inventory(player);
ResourceLocation attributeId = BundledNotSiloedApi.INVENTORY_CAPACITY_ATTRIBUTE;
List<CategoryView> categories = BundledNotSiloedApi.categories(player);
```

The returned inventory is the same server-authoritative backend used by player hooks, networking, container transfers, and the browser. Entries and representative stacks are defensive views. Category records are immutable player-facing metadata.

Use normal Minecraft attribute modifiers against `bundlednotsiloed:inventory_capacity`; capacity changes take effect immediately and do not destroy items if capacity falls below current usage.

## General storage operations

Depend on Stacks Not Slots directly for standalone machines, backpacks, blocks, vehicles, or NPC inventories:

```java
MutableCapacityInventory storage = StacksNotSlotsApi.createInventory(options);
InsertionResult simulation = storage.insert(stack, true);
ExtractionResult extraction = storage.extract(prototype, amount, false);
InventoryTransfer.Result moved = InventoryTransfer.move(source, destination, prototype, amount, false);
```

Cost-provider registration and exact `CapacityAmount` helpers also belong to Stacks Not Slots. Bundled Not Siloed does not duplicate them.

## Integrated inventory screens

Bundled Not Siloed replaces the exact vanilla player inventory screen with a native-texture extension. The upper player/crafting profile and hotbar remain native. The normal viewport maps the 27 real player-main slots directly to logical indices 9-35, even when those cells are empty, and adds row-scrolled ranges only beyond that base. Search can temporarily map the same slots onto aggregated item identities. Category and sort actions rearrange the authoritative inventory once and keep the native range mapping active.

For other `AbstractContainerScreen` implementations, Bundled Not Siloed integrates only when it can identify exactly the standard 27 player-main slots in a regular 9-by-3 arrangement. It retains those `Slot` instances and their menu topology, temporarily maps vanilla indices 9-35 onto the requested logical page, and adds the same capacity-backed search and compact category/settings toolbar in the intervening texture margin. Container-owned slots, menu logic, and controls remain untouched. This is an implementation detail rather than a public UI API; integrations should use the inventory and category APIs above.

## NeoForge player capability

Query `Capabilities.ItemHandler.ENTITY` on a player for the dynamically growing compatibility view. Its last index is an append position, not a capacity ceiling. Bundled Not Siloed applies player category and transfer policy around this backend where appropriate.

## Authority

Inventory mutation requests are validated and committed on the server. Client snapshots and deltas update display state only. Add-ons should never mutate a client-side player inventory and treat it as authoritative.
