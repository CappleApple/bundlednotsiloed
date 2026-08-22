# Changelog

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
