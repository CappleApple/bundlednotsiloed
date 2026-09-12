# Bundled Not Siloed

Bundled Not Siloed is an inventory overhaul for NeoForge 1.21.1 built on top of **Stacks Not Slots**.

Instead of treating the player's inventory as a hard set of storage slots, BNS gives the player one capacity-limited inventory and keeps the familiar Minecraft grid as the way you interact with it. The normal 27-slot inventory and 9-slot hotbar still exist visually, but storage can extend beyond them when the player has enough capacity.

The default capacity is **2,304 units**, which is equivalent to 36 normal stacks. A full stack always costs 64 capacity, so stack size matters: a normal 64-stack item costs 1 per item, a 16-stack item costs 4, and a non-stackable item costs 64.

## Main features

- Capacity-based player inventory instead of a fixed number of storage slots.
- Scrollable extra inventory rows when more space is needed.
- Search, categories, sorting, and configurable tabs built into the normal inventory screen.
- Nine normal hotbar positions with optional category-based cycling.
- Pickup limits per category.
- Bulk dump/extract controls for containers.
- Support for normal Minecraft inventory interactions such as shift-clicking, number keys, drag splitting, double-click collection, and dropping items.
- NeoForge item-handler support for mods that interact with player inventories.
- JEI and EMI recipe-transfer integration.
- Sophisticated Backpacks integration.
- Public API for inventory and capacity-related integrations.

Armor and offhand items continue to use their normal equipment slots and do not consume BNS inventory space.

## Using the inventory

Open your normal player inventory. The usual Minecraft layout remains in place, with a small control rail for search, categories, sorting, and settings.

The first 27 inventory positions behave like the normal main inventory. If your stored items extend beyond that, scroll over the grid to reach additional rows.

The hotbar is still a separate nine-position access bar. Items stored deeper in the inventory can be swapped into a hotbar position manually or through the optional category-cycle keybinds.

### Search

Search checks the complete BNS inventory, not just the currently visible row.

Useful prefixes:

- `@modid` - items from a mod
- `#tag` - item or represented block tags
- `^text` - cached tooltip text
- `/pattern` - regular expression

An empty search returns to the normal inventory view.

### Categories

Categories are filters over the same inventory; they do not create separate storage compartments.

A category can match exact items, tags, mod IDs, or regular expressions, and can also exclude matches. Items are allowed to appear in more than one category.

Pack defaults live in:

```text
config/bundlednotsiloed/default_categories.json
```

Players can also create and rearrange their own categories in-game.

### Sorting

Sorting is a one-time action rather than a permanent rule. After sorting, you can move items around normally without the mod immediately rearranging them again.

Available sort modes include name, quantity, recently modified, registry ID, and mod namespace.

## Capacity

The player's capacity comes from the attribute:

```text
bundlednotsiloed:inventory_capacity
```

That means other mods, equipment, effects, or commands can change inventory capacity using normal Minecraft attribute modifiers.

Example:

```mcfunction
/attribute @s bundlednotsiloed:inventory_capacity modifier add bundlednotsiloed:demo_pack 768 add_value
```

If capacity is reduced below what the player is already carrying, items are **not** deleted. The player simply cannot add more positive-cost inventory contents until enough space is freed or capacity is restored.

## Container support

BNS keeps the standard player inventory slots used by vanilla and most modded menus, so ordinary container interactions continue to work.

When a container screen exposes the usual player inventory grid, BNS can make that section searchable and scrollable without replacing the container's own slots or controls.

Bulk dump/extract actions are also available for supported open containers and item handlers.

## Configuration

Common settings are in:

```text
config/bundlednotsiloed-common.toml
```

Important options include base inventory capacity, partial pickups, over-capacity pickup handling, and whether category limits affect world pickups or manual transfers.

Client defaults are in:

```text
config/bundlednotsiloed-client.toml
```

Player-specific UI/category settings are stored separately in `BNS-SaveState.json` so modpack config updates do not overwrite personal layouts.

## Commands

```text
/bundlednotsiloed capacity [player]
/bundlednotsiloed inventory debug
/bundlednotsiloed inventory validate
/bundlednotsiloed categories reset
/bundlednotsiloed categories reload
```

Use vanilla `/attribute` commands when changing capacity through modifiers.

## Compatibility notes

Most mods that use normal inventory methods or NeoForge item handlers should see the BNS inventory correctly.

Mods that directly access Minecraft's public `Inventory.items` field can bypass those adapters and may need special handling. See [docs/VANILLA_HOOKS.md](docs/VANILLA_HOOKS.md) for the technical details.

For API usage, see [docs/API.md](docs/API.md).

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.244
- Java 21
- Stacks Not Slots

Install BNS on both the server and connecting clients.

## Building from source

The development setup expects a sibling Stacks Not Slots checkout.

```bash
./gradlew build
./gradlew test
```

Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

The built jar is written to `build/libs/`.
