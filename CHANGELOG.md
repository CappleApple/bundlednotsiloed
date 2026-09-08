# Changelog

## 1.4.3 - 2026-09-08

### Fixed

- Predicted inventory clicks and vanilla slot updates no longer invalidate the server's delta baseline or trigger repeated full-inventory synchronization.
- Search and scrolling now use acknowledged logical slot references, preventing changing item metadata, delayed updates, and dropped navigation from mapping clicks to different slots on the client and server.
- Hover-opening Sophisticated Backpacks now retains the correct logical backpack slot across screen changes. Open backpacks are protected from pickup, swapping, extraction, and bulk transfer; inventory arrangement requires closing the backpack first.
- Inventory snapshots now split by encoded data size as well as item count. BNS pauses synchronization of invalid or oversized item data before sending a failing inventory packet, preserves server-side items, and reports the affected item and logical slot.

## 1.4.2 - 2026-09-03

### Changed

- Stowed-stack auto-refill now tops up only hotbar stacks and leaves the 27-slot main inventory untouched.

## 1.4.1 - 2026-09-03

### Fixed

- Shift-clicking the final stowed stack with no valid destination no longer repeats the same quick-move indefinitely and freezes the game.
- Shift-clicking a stowed stack now falls back from a full hotbar to matching or empty slots in the visible main inventory, and remains in place when neither region can accept it.

## 1.4 - 2026-09-03

### Added

- Added one attached creative-tab-style rail with vertically stacked search, category, and settings controls to the player inventory and compatible container screens.

### Changed

- Search is now activated from an icon button, expands its translucent query display to the right while typing, retains focus across empty GUI space, resumes typing when an active-query button is hovered, blinks while a query remains active, clears on right-click, and resets when the inventory or container closes.
- The player inventory keeps its vanilla dimensions and slot margins, while capacity changes briefly display a centered two-pixel green-to-orange-to-red gradient between the main inventory and hotbar.
- The attached control rail sits flush with the inventory panel, while the conditional scrollbar and transient capacity overlay are visually centered in their native margins.
- Compact control-button tooltips now open to the cursor's left, while expanded Shift help retains Minecraft's normal tooltip placement.

### Fixed

- Sophisticated Backpacks now discovers backpacks throughout the complete logical inventory and keeps an opened backpack bound to its stable logical slot while the inventory browser scrolls.
- Container player-inventory discovery now recognizes standard NeoForge item-handler wrappers generically, restoring the complete BNS interface in Iron Furnaces and other wrapped 9-by-3 inventories.
- Container category and settings menus now share the player-grid left edge and render above container items, held stacks, and native tooltips.
- Removed the visible seam between the attached control rail and the inventory panel.
- Active search typing now captures keyboard input and suppresses unrelated keybind activation until typing focus ends.
- Escape now exits search typing without immediately reactivating it while the cursor remains over the search button.
- Search-filtered inventory items now use Minecraft's native tooltip pass without rendering a duplicate tooltip on top.

## 1.3.5 - 2026-09-01

### Fixed

- EMI Shift-click fill and craft-all interactions now retain their native one/all and cursor/inventory destinations while sourcing ingredients from the complete logical inventory through vanilla recipe-placement hooks.

## 1.3.4 - 2026-09-01

### Fixed

- EMI recipe availability and autofill now use the complete logical inventory in Visual Workbench crafting tables instead of only the currently visible player rows.

## 1.3.3 - 2026-09-01

### Fixed

- EMI now recognizes JEMI-wrapped crafting recipes across the complete logical inventory instead of checking only the currently visible player rows.

## 1.3.2 - 2026-08-31

### Added

- Added a Recently Modified sort mode that persists exact item-identity changes while ignoring the rearrangement performed by the sort itself.
- Item tooltips now append the exact quantity owned across the unified inventory.

### Changed

- The default inventory-search shortcut is now Shift+F instead of F.

### Fixed

- Inventory scrolling now changes the visible slot window while carrying an item, allowing it to be placed in another part of the inventory.
- Integrated container search bars and controls now render beneath native item tooltips and the cursor-held stack.
- External-container Shift-clicks now join an existing item identity or follow the selected New items destination and fallback order instead of being forced into stowed storage while the inventory browser is open.
- Respawning now immediately republishes the logical inventory and sends a full client snapshot, restoring stowed items and the search-bar capacity fill without requiring an inventory click.
- Expanded category and settings menus now own their popup hover area, preventing covered inventory-item tooltips from rendering or merging with menu tooltips.
- Player-inventory Shift-clicks now move hotbar stacks into the first available main-inventory slot, falling back to the first available stowed slot; other source stacks still try the hotbar first and then the logical-storage tail.
- Exact item-count tooltips now label the unified quantity as **Total** instead of **Owned**.
- Exact item-count tooltips now appear only for items hovered in player main-inventory or hotbar slots, not external containers or other tooltip sources.
- JEI and EMI recipe autofill now extracts complete stacks from stowed storage without losing the placement count, and remains anchored to logical slots while the inventory is scrolled or filtered.

## 1.3.1 - 2026-08-31

### Fixed

- Clicking outside an integrated inventory search bar now releases keyboard focus so normal inventory input resumes immediately.

## 1.3 - 2026-08-31

### Added

- Added a native-texture inventory-screen replacement with a compact separator containing search, category, and settings controls.
- Added a fixed 27-cell logical inventory window with conditional row scrolling and a scrollbar that appears only when more rows are available.
- Added conservative vanilla and modded container-screen integration for standard 9-by-3 player main-inventory grids, including an in-margin search/category/settings toolbar and conditional scrollbar.
- Added expandable category and settings menus; categories can also be changed by scrolling over their button.
- Added a muted green/orange/red capacity fill behind every integrated search field, with current/maximum stack capacity on normal hover and three-line search help while Shift is held.
- Added a synchronized real-slot viewport: the menu's 27 vanilla player-main slots map onto additional logical inventory rows as needed while retaining native slot interaction paths.
- Added asymmetric player-to-container transfer controls: Shift-left moves one legal stack through the active menu, while Shift-right moves as much matching quantity as that menu can accept.
- Added Shift-click on the category selector to move the main 27 inventory slots into stowed storage while leaving the hotbar unchanged; holding Shift changes the selector icon to a sticky piston and reveals its cleanup tooltip.

### Changed

- Search combines exact item-and-component identities from player main storage and the dynamic backend into one displayed quantity; category and sort actions keep the native real-slot grid.
- The vanilla player profile, crafting area, armor, offhand, recipe-book control, and hotbar remain in the familiar native layout while the main inventory shifts downward.
- Replaced the floating Panels Not Screens browser and removed that project dependency.
- Category and sort controls now apply a one-shot arrangement without rearranging the hotbar; subsequent manual slot placement remains under the player's control and persists.
- Category and settings controls use compact 13-pixel Minecraft button textures, while icons in their expanded menus render at the full 16-pixel item size.
- The manual inventory view always retains all 27 vanilla main-grid cells, including empty placement cells, and adds nine-cell scroll rows only when storage extends beyond them.
- Quantity sort labels now read `1-9` and `9-1`.

### Fixed

- Fixed the real All category being accompanied by a second synthetic All entry and being visited twice when scrolling categories.
- Fixed expanded category and settings menu icons rendering beneath container inventory items.
- Fixed the integrated container overlay consuming Shift-clicks instead of preserving the active menu's native transfer behavior.
- Fixed category and sort selection temporarily using custom-rendered entries with small counts, missing durability bars, and a scrollbar sized only to the selected category.
- Fixed category results staying stale when saved tabs arrived after the inventory-entry cache was populated.
- Fixed clicking an already-open category or settings button reopening its menu instead of closing it.

## 1.2.5 - 2026-08-26

### Fixed

- Armor and offhand items now keep receiving vanilla inventory ticks, preventing their slot pop animation from shaking and stretching indefinitely after a hand swap.

## 1.2.4 - 2026-08-26

### Fixed

- The simulated vanilla inventory now preserves its visible slot arrangement when leaving and rejoining a world.

## 1.2.3 - 2026-08-22

### Fixed

- `/clear` now applies its item filter and count across the complete logical player inventory, including stowed storage.
- Creative pick block and creative inventory slot edits now update authoritative logical storage instead of creating ghost items.
- Creative inventory browser actions now stow the mouse-held stack and place extracted stowed items directly on the creative cursor.
- Mouse-held stack amounts now retain their normal white text while rendering above inventory browser panels.

## 1.2.2 - 2026-08-20

### Fixed

- Fixed dump-to-container actions transferring hotbar items instead of leaving the hotbar untouched.

## 1.2.1 - 2026-08-13

### Added

- Added a configurable sound for cursor placement attempts that fail because the unified player inventory has no remaining capacity.
- Empty projected player slots can now show visual-only barrier icons while the cursor-held stack cannot fit; this display is configurable.
- The inventory-browser handle now changes to a barrier icon while the cursor-held stack cannot fit in the player inventory.

## 1.2 - 2026-08-11

### Added

- Added a browser-settings option for routing brand-new item types to the hotbar first, the simulated 27-slot main inventory first, or stowed storage first. Existing matching visible and stowed stacks still take priority.
- Clicking the browser panel's category control now opens a scrollable grid of category icons with name tooltips and direct selection.

### Changed

- Scrolling over the browser handle now switches categories whether the panel is open or closed and temporarily previews the selected category icon and name.
- Holding Shift changes the browser handle into a `Stow All` sticky-piston action.
- The open-container bulk-transfer control is now hidden in the player's own inventory and remains available in container interfaces.
- Removed the separate floating inventory category and `Stow All` selector; its functionality now lives on the browser handle and panel.

## 1.1 - 2026-08-11

### Added

- Added default-on automatic refilling of existing hotbar and main-inventory stacks from matching stowed items.
- Added a persistent auto-refill toggle to the inventory-browser settings and an unbound-by-default keybind under Controls.

### Changed

- JEI and EMI recipe transfer now count ingredients across the complete unified inventory, including stowed items, and request server-authoritative vanilla recipe placement.
- World pickups now top up matching visible stacks first. New item types enter the main inventory from left to right and top to bottom, while items already present only in stowed storage join their stowed stack.
- Pickup overflow from a matching visible stack is stowed instead of occupying an unrelated empty visible slot.

### Fixed

- Fixed JEI and EMI reporting that recipes could not be autofilled when the required ingredients were stowed.
- Fixed newly encountered item types entering an empty hotbar before available main-inventory slots.

## 1.0 - 2026-08-10

- Promoted the integrated player-facing mod and its API dependencies to version 1.0 with no functional changes.

## 0.6.5 - 2026-08-10

### Changed

- Capacity accounting now supports every positive item max-stack size with exact proportional costs. A complete legal stack always costs 64 units, so individual 128-stackable items cost 0.5 units and 96-stackable items cost exactly 2/3 of a unit.
- Capacity acceptance, category pickup limits, slot replacement, invariant repair, commands, percentages, and the browser capacity display now use exact rational arithmetic without floating-point drift.
- Added exact capacity values to the public API while retaining conservative rounded whole-unit methods for source compatibility.

## 0.6.4 - 2026-08-09

### Changed

- Increased the default base inventory capacity from 1,728 to 2,304 units so it represents all 36 ordinary player slots, including the nine-slot hotbar.

### Fixed

- Fixed shift-clicking browser entries into virtual storage terminals and menus with custom transfer logic, including Tom's Simple Storage and Sophisticated Backpacks.
- Fixed closed-browser container transfers filling the player inventory from right to left and bottom to top instead of main-grid order.
- Fixed custom storage menus bypassing backend stow when shift-clicking their contents while the inventory browser is open.

## 0.6.3 - 2026-08-09

### Added

- Added `/pattern` regular-expression searches to the inventory browser and category rule editor.
- Added explicit `^text` tooltip searches and `^/pattern` tooltip regular-expression searches.
- Added the project logo to the NeoForge Mods screen and made it the default inventory-browser handle icon.
- Added `SNS-SaveState.json` in the game directory for client-owned settings, per-screen browser placement, tabs, hotbar category bindings, and view preferences.
- Added durable `/regex` category include/exclude rules. Regex rules match names, registry IDs, namespaces, item/block tags, and `block:<id>` for block items.
- Added block-tag support for `BlockItem`s, including vanilla `minecraft:mineable/*` tags.

### Changed

- Normal unprefixed searches no longer inspect tooltip text; tooltip indexing only runs for explicit `^` searches.
- Player customization is now keyed by player UUID on the client and validated/synchronized to the server at login. Actual inventory contents and capacity remain server/world-owned.
- Existing client TOML placement/settings and legacy world-saved tab data migrate automatically on first use.
- Restored the spyglass as the default browser handle while retaining the project logo on the Mods screen and as a configurable handle option.
- Updated the bundled category presets to use valid NeoForge conventional tags and dynamic regex rules. Unedited legacy defaults upgrade automatically; customized presets are preserved.

### Fixed

- Search syntax help now appears only while Shift is held over the search field.
- The browser handle normally shows only `Inventory Browser`; holding Shift adds the drag hint.
- Fixed block-only tags appearing invalid in the category editor and never matching their block items.

## 0.6.2 - 2026-08-09

Changes since 0.6.0:

### Added

- Added independently persisted browser position, docking direction, open state, and visibility for each container-screen type.
- Added a configurable default browser-handle anchor. New interfaces default to the bottom-right beside the player hotbar.
- Added the `Start Inventory Search` keybind, bound to `F` by default. It opens the browser, clears the previous query, and focuses search without entering the shortcut character.
- Added right-click-to-clear and Control-A selection to the browser search field.

### Changed

- Rebuilt the inventory browser as a responsive four-direction layout. It shrinks its visible rows or columns to available space, retains at least one result row or column, and stays between its handle and the screen edge.
- Top and bottom docking now use horizontal results with controls on side rails while keeping search at the top.
- Added a drag threshold so clicking the browser handle no longer nudges its saved position.
- Browser placement is now stored relative to the current container GUI instead of as absolute screen pixels. It follows GUI repositioning, resolution changes, GUI-scale changes, and ultrawide layouts.
- Reduced the overlay's input ownership to its visible bounds so underlying vanilla and modded container interfaces remain interactive.
- Simplified production build metadata and optional compatibility dependency handling.

### Fixed

- Fixed browser panels and controls overlapping each other or extending beyond the available screen area.
- Fixed stale absolute browser coordinates placing the handle far away from the current inventory interface.
- Fixed the search shortcut's key appearing as the first character of a new query.
- Fixed crashes when a modded inventory menu does not expose a safely constructible menu type.
- Fixed browser state leaking between unrelated container-screen types.
