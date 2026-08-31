# Vanilla inventory compatibility hooks

Minecraft 1.21.1 assumes 36 player item indices in menus and several direct code paths. Bundled Not Siloed keeps those indices as a bounded *view* while storage and NeoForge capability enumeration remain dynamic.

## `InventoryMixin`

The mixin activates only after the one-time vanilla inventory migration marker is set.

- `getItem`, `getSelected`, `setItem`, and removal methods map vanilla indices 0-35 to sparse backing positions. The browser's normal first page maps directly to logical indices 9-35 even if trailing empty cells are absent from serialized sparse storage. Scrolling advances that 27-slot range by rows; search may instead map indices 9-35 to requested identities. Category and sort clicks rearrange storage once and remain on the native range mapping. The hotbar remains fixed.
- `setPickedItem` and `pickSlot` replace vanilla's direct `Inventory#items` writes so creative pick block and survival pick-item swaps update logical storage instead of creating client-only ghosts.
- Creative players bypass capacity-based slot limits. Browser stow/take actions explicitly synchronize the cursor owned by the creative screen's client-only menu, while shift-clicking the creative trash slot sends an authorized request to clear hidden storage as well as vanilla menu slots.
- both `add` overloads call the centralized capacity transaction; pickup, commands, rewards, trading, crafting remainders, and most modded vanilla-style insertion therefore share one enforcement path.
- `getFreeSlot`, stack matching, and remaining-space queries describe only the projection and never define logical carrying capacity.
- `tick` ticks every actual backing stack; changes made through a live held-item reference are reconciled by identity/count hash.
- `fillStackedContents` accounts for the complete logical collection for recipe matching.
- `dropAll` drains and drops the complete logical collection. Armor/offhand continue through vanilla.
- `clearContent` clears logical ownership without affecting category/hotbar metadata, and targeted `/clear` operations continue past the 36-slot compatibility view into every matching backend stack.

The mixin deliberately leaves `getContainerSize()` at 41 so vanilla armor indices 36–39 and offhand index 40 do not shift as the collection grows.

## `ItemEntityMixin`

The redirect wraps only the call from `ItemEntity.playerTouch` to `Inventory.add`. It adds world-pickup category policies before the common global-capacity transaction while retaining vanilla pickup statistics, animation, partial-stack remainder, and post-event behavior.

## `SlotMixin`

Vanilla menu transfer logic splits source stacks before calling `Inventory.setItem`. The slot mixin reports a capacity-adjusted maximum for player item slots so the source is split by exactly the amount global capacity can accept. Existing stack capacity is credited when a slot is being replaced. Armor/offhand and non-player containers are untouched.

## Menu shift-click and equipment mixins

`AbstractContainerMenuMixin` recognizes destination ranges made entirely of vanilla player-storage slots and routes the source directly through the central insertion transaction. Native transfers fill the main grid in display order, then the hotbar, then the backend; a recognized integrated player-grid screen routes container-to-player transfers directly to the backend. This allows chest, furnace, crafting-result, and modded-menu shift-clicks to append beyond the 36-slot projection whenever capacity remains.

Menus that bypass vanilla's `moveItemStackTo` helper are reconciled after their native quick-move finishes. Only positive changes relative to the pre-click player view are relocated, so custom menu bookkeeping runs normally and existing player-slot placement is preserved. Browser-to-container transfers expose one logical stack through a real mapped player slot and call the active menu's own `quickMoveStack`, allowing virtual terminals and custom storage merge logic to select their proper destination without per-mod dependencies.

When the source is already owned by the player, the general container mixin leaves Shift-left-click to the menu's native quick-move, transferring one legal stack. Shift-right-click is an explicit browser gesture that repeatedly refreshes the mapped source slot and invokes that same native quick-move until the destination accepts no more matching items. Normal clicks, dragging, hotkeys, tooltips, and mod hooks continue through real `Slot` and menu paths.

Category and sort controls rearrange the logical inventory only when clicked and never install a live comparator. The player can immediately rearrange the same native slots, and that authoritative sparse placement persists.

`PlayerMixin` redirects the direct main-hand `NonNullList.set` inside `Player#setItemSlot` into the logical inventory. This covers the vanilla swap-to-offhand action without duplicating a stale visual stack in the selected hotbar slot.

## Dynamic NeoForge view

`DynamicItemHandler` is independent of vanilla's 36-index projection. It reports the complete sparse indexed extent plus an append slot. Empty positions remain addressable, and insertion/replacement targets the requested index instead of silently merging elsewhere. Inserting through the final slot may grow the view, so the synthetic slot count can represent as many distinct stacks as capacity and JVM memory/indexing allow.

## Client projection and recipe-view compatibility

`GuiMixin` changes only vanilla HUD hotbar item lookup, routing its direct `Inventory.items` read through the live `Inventory.getItem` projection. This fixes immediate visual synchronization without turning the fixed vanilla field into storage.

The exact vanilla inventory screen is replaced client-side with a native-texture extension that shifts the main grid downward beneath an integrated toolbar. On other vanilla or modded container screens, integration is deliberately conservative: it activates only when exactly 27 player-owned main-inventory slots form the standard 9-by-3 grid. The existing slots stay present and become the searchable mapped viewport, while container-owned slots and menu behavior remain untouched. Expanded category/settings menus render above slot contents. JEI and EMI recipe-transfer support remains active without a floating exclusion area.

## Remaining direct-field risk

`Inventory.items` is a public final `NonNullList` in vanilla. Changing its shape would break fixed armor/offhand index assumptions, and replacing every external direct field access is not possible without broad bytecode changes. Bundled Not Siloed republishes hotbar references plus the currently mapped 27-slot page into that fixed list and reconciles direct replacements, but it still cannot expose more than one page simultaneously. Integrations should use:

1. the public capacity-inventory API,
2. NeoForge's player entity item-handler capability, or
3. vanilla `Inventory` methods for the 36-entry projection.

Mods that directly iterate or mutate `player.getInventory().items` will see only that compatibility field and need an integration patch. This limitation does not cap storage or the item-handler slot view.
