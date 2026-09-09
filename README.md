# Bundled Not Siloed

Bundled Not Siloed is the player-facing NeoForge 1.21.1 inventory overhaul. It uses **Stacks Not Slots** for authoritative capacity storage and integrates its browser directly into Minecraft's inventory screens. The nine-position hotbar and vanilla inventory indices are access views over the logical collection; neither grants storage nor limits how many distinct entries can exist.

The default capacity is 2,304 units: 36 stack-equivalents covering the vanilla 27-slot main grid plus 9-slot hotbar. Every complete legal stack costs exactly 64 units: a 64-stackable item costs one unit each, a 16-stackable item costs four, a non-stackable item costs 64, and a 128-stackable item costs one half. Exact rational accounting supports any positive max-stack size without floating-point drift. Capacity includes hotbar-accessed items and is controlled live by the `bundlednotsiloed:inventory_capacity` player attribute.

There is no hidden compatibility-slot ceiling below capacity. If a player has capacity `N`, the backend and NeoForge item-handler view can grow to represent `N` distinct quantity-one 64-stackable identities (subject only to Java's practical integer/memory limits). Snapshot chunking and the vanilla 36-index projection are transport/access details, never carrying limits.

## Implementation status

Implemented:

- Gradle dependency on the local Stacks Not Slots project
- Dynamic logical inventory with no configured backing-slot maximum
- Sparse, dynamically indexed compatibility slots that retain explicit placement, plus an append slot
- Runtime capacity attribute and normal Minecraft attribute-modifier support
- Exact proportional capacity costs with conservative whole-unit compatibility views and a public override registry
- Transactional simulated/real partial insertion and extraction
- Automatic consolidation that respects data components
- Persistent NeoForge player attachment, lossless vanilla-inventory migration, and unresolved-entry preservation
- Over-capacity retention when attribute capacity falls
- Chunked initial sync and revisioned delta sync
- Player-owned category definitions, exact item/tag/mod/regex include/exclude rules, item-and-block-tag matching, ordering, dynamic or fixed icons, sorting, enablement, and pickup limits
- Separate `config/bundlednotsiloed/default_categories.json` preset file
- In-game searchable category editor and reset-to-defaults operation
- Most-restrictive overlapping world-pickup limits and rate-limited feedback
- Category-only hotbar cycle bindings, remembered selections, cycling keybinds, and HUD feedback
- Native-style player inventory replacement with a left-side search/category/sort/settings rail, a synchronized real-slot 9-by-3 viewport, transient capacity feedback, and conditional row scrolling
- Generic vanilla and modded container integration that retains the menu's standard 27 real player-main `Slot` objects and remaps them onto additional logical rows as needed
- JEI and EMI recipe transfer integration without floating GUI exclusion areas
- Native quick-move integration for vanilla, Mouse Tweaks-style repeated clicks, and modded container screens
- Bulk dump/extract controls for open menus and looked-at item-handler containers, with an optional world feedback grid
- Server-authoritative inventory/category/hotbar packets with input validation and action rate limiting
- NeoForge entity item-handler capability and public Java API
- Capacity/debug/validation/category commands
- Automated tests, including 1,000 distinct quantity-one 64-stackable entries at capacity 1,000
- Successful dedicated NeoForge server smoke startup

The vanilla 36 item indices remain a compatibility projection. Mods that call `Inventory` methods see this projection, while NeoForge item-handler users see the dynamically growing complete view. A mod that directly reads the public `Inventory.items` field bypasses both adapters and is a known compatibility risk; see [docs/VANILLA_HOOKS.md](docs/VANILLA_HOOKS.md).

## Build and development environment

Requirements:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.244
- sibling `../stacks-not-slots` checkout (a Gradle composite build provides the local library artifact)

Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Linux/macOS:

```bash
./gradlew build
./gradlew test
./gradlew runClient
./gradlew runServer
```

The built mod is written to `build/libs/bundlednotsiloed-1.4.5.jar`. The project uses official Mojang mappings with Parchment parameter names and ModDevGradle's Minecraft-aware JUnit support.

## Player usage

Open the normal inventory to see Minecraft's familiar player model, armor, crafting, recipe-book, hotbar, dimensions, and slot margins. A creative-tab-style rail attached to the left of the main grid contains vertically stacked search, category, sort, and settings buttons. The normal view always exposes the complete 27-cell vanilla main grid, including empty cells, and preserves manual placement. It gains additional nine-cell scroll rows only when occupied logical storage extends past that base. Search combines exact item-and-component identities into one displayed quantity, while category and sort clicks perform one arrangement and keep the real native slots active. Tooltips for items hovered in the player inventory or hotbar append the exact quantity of that item-and-components identity owned across the unified inventory; other tooltip sources do not. The hotbar remains a separate nine-position access view.

- Use normal left/right clicks, drag splitting, double-click collection, number keys, and compatible mod interactions on the real player slots.
- In the open player inventory, Shift-clicking a hotbar stack first merges into or fills the 27-slot main inventory, then uses the first available stowed slot. A main-inventory stack tries the hotbar first, then appends any remainder after the currently necessary logical storage slots. A stowed stack tries the hotbar, then the visible main inventory, and stays in place when neither region can accept it.
- In an opened container, Shift-left-click a player stack to let that menu transfer one legal stack. Shift-right-click to repeat the same native transfer for as much of that exact item-and-component identity as the container can accept.
- Press the normal drop key while hovering an entry to drop one; hold Control to drop a stack.
- Scroll over the 27-cell grid to move through additional logical rows. The narrow scrollbar appears only when the inventory or active search has more than 27 visible positions.
- Left-click the search button or press Shift+F to focus search; while focused, the translucent query display expands to the button's right and F or Shift+F types normally. Focus survives movement across empty GUI space and collapses only when the pointer reaches a slot or another control. Hovering the blinking button for an existing query resumes typing without another click. Right-click the button to clear it, Control-A selects the complete query, and closing the inventory or container resets it. Hover the button for current/maximum stack-equivalent capacity; hold Shift for the three-line search-syntax help. Adding or removing inventory contents briefly shows a thin capacity overlay centered between the main grid and hotbar with a continuous green-to-orange-to-red gradient.
- While search is accepting text, unrelated key mappings are suppressed so typing cannot open another mod's interface. Escape exits typing mode; hover-to-resume is rearmed only after the pointer leaves the search button. Compact button tooltips appear to the cursor's left; holding Shift keeps expanded help in Minecraft's normal tooltip position.
- Click the compact category button to open a scrollable category menu, or scroll while hovering the closed button to change categories directly. Click the open button again to close it. Hold Shift to change the selector icon to a sticky piston and reveal its cleanup hint; Shift-click then moves all occupied main-grid cells into stowed rows while leaving the hotbar unchanged. The real **All** preset is shown once, with a fallback entry only when that preset is unavailable.
- Click **Sort inventory**, between the category and settings buttons, to reapply the active category exactly like selecting it again and return the inventory view to its first row.
- Click the compact settings button for **Manage Tabs**, **Sort Order**, and **Settings**. The sort entry cycles the active order and also responds to the mouse wheel; quantity order is labeled **1-9** or **9-1**, and **Recently Modified** places the last changed identities first.
- Use **Manage Tabs** to add/edit/delete/reorder categories and assign a cycle category independently to each of the nine hotbar positions. Under **Settings**, choose whether brand-new pickups and external-container Shift-clicks try the hotbar, main inventory, or stowed storage first.
- Selecting a category or sort mode arranges the inventory once without rearranging the hotbar. The grid remains the same 27 native player slots throughout, so normal counts, durability bars, mod interactions, scrolling through the rest of the inventory, and subsequent manual placement remain available immediately.
- Hotbar bindings never restrict placement or rearrange items automatically. The configurable forward/backward cycle keys explicitly swap the selected position with the next owned item in its assigned category.
- When Sophisticated Backpacks is installed, its open-backpack action searches the complete logical inventory and keeps item-backed backpack screens attached to the same logical stack while the BNS inventory window scrolls.
- In a vanilla or modded container screen that exposes the standard 9-by-3 player main-inventory grid, including grids exposed through standard NeoForge item-handler wrappers, those same real player slots become the combined, searchable, row-scrollable viewport. The attached left-side control rail and transient capacity overlay follow that grid without changing the container's layout; its scrollbar appears only when needed, and the container's own slots and controls remain native.
- Control-G and Control-H perform bulk dump/extract against the open menu or the item-handler container being looked at.

An empty search shows the complete manually placeable inventory in its current order. A non-empty search spans every category and matches display names and full registry IDs. Prefix with `@` for mod namespaces, `#` for item or represented-block tags, `^` for cached tooltip text, or `/` for a case-insensitive regular expression across names, IDs, namespaces, and tags. Use `^/pattern` for a tooltip regular expression. A closing slash is optional, so both `/pattern` and `/pattern/` work. Invalid regular expressions are shown in red and return no results. Category rules accept exact items, `#tags`, `@modid` namespaces, and durable `/regex` predicates; `/sword` dynamically includes every matching current or future item. Regex rules also see `block:<registry-id>` for every `BlockItem`, allowing `/^block:` to select all blocks. Tooltip searches in the category editor add the selected exact item. Sort modes cover name, quantity, recently modified, registry ID, and namespace. The selected category and sort preference persist with player data, but both controls are one-shot arrangement actions: later manual item placement is not continuously reordered.

The integrated browser always uses nine columns and three visible rows. The manual view never shrinks below 27 addressable cells and only adds scroll rows beyond them when necessary. Search filtering can remove the need to scroll; the row offset and scrollbar are clamped immediately whenever the result count changes.

## Capacity and over-capacity behavior

The effective limit is the floored, non-negative value of the player's `bundlednotsiloed:inventory_capacity` attribute. Attribute modifiers take effect immediately. For example:

```mcfunction
/attribute @s bundlednotsiloed:inventory_capacity modifier add bundlednotsiloed:demo_pack 768 add_value
```

If capacity falls below current usage, no item is deleted or moved. New positive-cost insertion is rejected until enough capacity is restored or items are removed. Dropping, consuming, crafting with, and transferring items out remain valid.

Empty projected player slots can show visual-only barriers while the cursor holds a stack that cannot add even one item. Attempting a capacity-blocked placement plays the configured client sound. Valid replacements that reclaim an occupied position's capacity remain available.

If a committed held-item transformation produces a higher-cost result, the result is preserved and the player enters the normal over-capacity state rather than losing the item or crashing the operation.

Equipped armor and offhand items retain vanilla equipment storage and are not duplicated in the logical collection.

## Categories and pickup rules

Categories are predicates over the unified collection; they never own items or reserve capacity. Matching is:

```text
(allItems OR any include rule) AND no exclude rule
```

Exclusions win. An item may appear in multiple categories. During world pickup, every matching enabled finite limit must allow the accepted amount, so the most restrictive remaining allowance wins. Manual container transfers do not use category limits by default, while global capacity always applies.

Player customizations are persisted by UUID in the client-owned `BNS-SaveState.json` and are not overwritten when server defaults change. An existing `SNS-SaveState.json` is imported once when the new file does not yet exist. **Reset to Defaults** is explicit.

## Configuration

`config/bundlednotsiloed-common.toml`:

- `inventory.baseInventoryCapacity` - initial base attribute value, default `2304` (36 stack-equivalents: main grid plus hotbar)
- `inventory.overCapacityBlocksPickup` - enables the dedicated over-capacity pickup short-circuit; disabling it never permits positive growth beyond global capacity
- `inventory.allowPartialPickup` - accept only the legal portion of a ground stack
- `categories.categoryLimitsAffectWorldPickup` - default `true`
- `categories.categoryLimitsAffectManualTransfers` - default `false`

`config/bundlednotsiloed-client.toml` supplies initial/default values. Once the client runs, user changes are written to `BNS-SaveState.json` in the game directory instead, along with UUID-keyed tab, hotbar, and view preferences. This keeps player customizations outside the config directory used by modpack updates.

Client defaults/settings:

- `capacityDisplayMode` - `CAPACITY`, `STACK_EQUIVALENTS`, or `BOTH`
- `pickupLimitNotification` - `NONE`, `HUD`, `ACTION_BAR`, `SOUND`, or `HUD_AND_SOUND`
- `enableSearchTooltipIndexing`
- `enableHotbarCycleOverlay`
- `browserItemCountMode` - `EXACT`, `COMPACT` (default), `STACKS`, `STACKS_REMAINDER`, or `PERCENTAGE`
- `browserOverallCountMode` - `EXACT`, `COMPACT`, `STACKS` (default), or `PERCENTAGE`
- `manageTabsIcon` and `settingsIcon` - configurable item IDs for the square controls
- `showBulkTransferOverlay` / `bulkTransferOverlaySeconds` - in-world bulk-transfer feedback and duration
- `showFullInventoryBarrierIcons` - default `true`; when a cursor-held stack cannot add even one item, empty projected player slots display visual-only barrier icons
- `inventoryFullSound` - sound event played after a cursor placement fails for lack of capacity; default `minecraft:block.note_block.bass`, or blank to disable

## Default preset schema

The bundled file is copied once to `config/bundlednotsiloed/default_categories.json`. Pack authors can redistribute a replacement. A category object supports:

```json
{
  "id": "ores",
  "name": "Ores",
  "icon": "minecraft:raw_iron",
  "order": 20,
  "include": ["#c:ores", "minecraft:ancient_debris", "@examplemod", "/raw_.*_ore"],
  "exclude": ["#example:ignored_ores"],
  "pickupLimit": -1,
  "sort": "name",
  "enabled": true,
  "allItems": false
}
```

Unqualified category IDs use the `bundlednotsiloed` namespace. Unqualified item/tag IDs use `minecraft`; `@modid` matches every item registered by that namespace. A `#tag` can be an item tag or a block tag represented by a `BlockItem`. `/regex` is case-insensitive and matches item names/IDs/namespaces, item and block tags, plus the synthetic `block:<id>` field. `pickupLimit: -1` means unlimited. Supported sort strings are `name`, `name_descending`, `quantity_ascending`, `quantity_descending`, `recently_modified`, `registry_id`, and `mod_namespace`.

## Commands

```text
/bundlednotsiloed capacity [player]
/bundlednotsiloed inventory debug
/bundlednotsiloed inventory validate
/bundlednotsiloed categories reset
/bundlednotsiloed categories reload
```

Use vanilla `/attribute` commands for capacity modifiers.

## API

The player integration entry point is `com.cappleapple.bundlednotsiloed.api.BundledNotSiloedApi`. General inventory creation, transactions, serialization, transfers, and cost-provider registration are supplied by `com.cappleapple.stacksnotslots.api.StacksNotSlotsApi`. See [docs/API.md](docs/API.md).

## Compatibility notes

- Install BNS 1.4.3 or newer on both the server and every client: the acknowledged inventory-window protocol is incompatible with earlier versions.
- Navigation sends logical slot references and waits for server confirmation before accepting inventory interactions. Inventory deltas use a separate server baseline, so client prediction and late vanilla slot echoes cannot invalidate or overwrite it.
- Sophisticated Backpacks can be opened from the hovered inventory slot or through its normal discovery hotkey. The open root backpack is protected while its menu is open, including when viewing a nested backpack; close it before sorting, stowing the grid, or cycling the hotbar.
- BNS validates inventory item codecs before sending a snapshot or delta and splits snapshots around a 256 KiB item-data target (a single valid item can exceed that target). If an item fails validation, synchronization pauses, the player sees a diagnostic, and the server log identifies the player, logical slot, item ID, and codec error. Items and components remain in server storage; synchronization retries automatically after the item is repaired. This protects BNS inventory packets and does not override vanilla or other mods' packet codecs.

- The complete dynamic inventory is exposed through NeoForge's player entity item-handler capabilities.
- Empty compatibility positions are retained as sparse holes, so explicit vanilla/API slot placement remains stable across inventory changes and persistence.
- Vanilla menus retain 36 projected item indices and real armor/offhand indices. While an integrated screen is open, indices 9-35 are a synchronized viewport onto the current logical rows, providing native `Slot` interaction with entries outside the initial projection.
- Shift-clicks from external containers join an existing matching identity wherever it already lives. Brand-new identities honor the selected **New items** destination: inventory-first falls back to the hotbar, hotbar-first falls back to the main inventory, and stowed-first bypasses both; only the explicit bulk transfer is always backend-only. Player-to-container Shift-left keeps the active menu's one-stack native quick-move behavior; Shift-right repeats it until no more matching quantity can be accepted.
- The public vanilla `Inventory.items` list is maintained as a live compatibility view of the hotbar plus the currently mapped 27-slot page while the browser is active; direct replacements and stack mutations are reconciled into logical storage.
- The HUD hotbar and accessor APIs read the same live projection.
- JEI and EMI retain recipe-transfer integration; the inventory browser no longer creates a floating exclusion area.
- Recipe matching accounts for all logical stacks. Code that directly indexes `Inventory.items` observes the live first-36 view, while capability/API integrations can enumerate the complete dynamic backend.
- Client UI classes are isolated behind the client-only mod entry point; the dedicated server smoke run loads no client package.

## Roadmap

- Broader compatibility fixtures for popular menu/recipe implementations that directly access vanilla fields
- Datapack reload merging for server preset contributions
- Rich tooltip text indexing cache for search
- More customization features

- Continue moving player-specific compatibility policy behind intentional Bundled Not Siloed APIs without duplicating either library.

## License

MIT. Minecraft and NeoForge remain subject to their respective licenses.
