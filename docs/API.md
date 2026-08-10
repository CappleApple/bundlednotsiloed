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

## Panels

The inventory browser is a Panels Not Screens `Panel`. Bundled Not Siloed supplies inventory-specific content and persists per-screen placement; Panels Not Screens owns procedural chrome, handle dragging, docking, clipping, and topmost panel input. Other mods should depend on Panels Not Screens directly rather than reaching into `ContainerInventoryOverlay`.

## NeoForge player capability

Query `Capabilities.ItemHandler.ENTITY` on a player for the dynamically growing compatibility view. Its last index is an append position, not a capacity ceiling. Bundled Not Siloed applies player category and transfer policy around this backend where appropriate.

## Authority

Inventory mutation requests are validated and committed on the server. Client snapshots and deltas update display state only. Add-ons should never mutate a client-side player inventory and treat it as authoritative.
