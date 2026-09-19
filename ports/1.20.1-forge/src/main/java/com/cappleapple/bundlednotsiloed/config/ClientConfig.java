package com.cappleapple.bundlednotsiloed.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public enum CapacityDisplayMode { CAPACITY, STACK_EQUIVALENTS, BOTH }
    public enum PickupNotification { NONE, HUD, ACTION_BAR, SOUND, HUD_AND_SOUND }
    public enum ItemCountMode { EXACT, COMPACT, STACKS, STACKS_REMAINDER, PERCENTAGE }
    public enum OverallCountMode { EXACT, COMPACT, STACKS, PERCENTAGE }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<CapacityDisplayMode> CAPACITY_DISPLAY_MODE;
    public static final ForgeConfigSpec.EnumValue<PickupNotification> PICKUP_NOTIFICATION;
    public static final ForgeConfigSpec.BooleanValue TOOLTIP_INDEXING;
    public static final ForgeConfigSpec.BooleanValue HOTBAR_CYCLE_OVERLAY;
    public static final ForgeConfigSpec.EnumValue<ItemCountMode> ITEM_COUNT_MODE;
    public static final ForgeConfigSpec.EnumValue<OverallCountMode> OVERALL_COUNT_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> MANAGE_TABS_ICON;
    public static final ForgeConfigSpec.ConfigValue<String> SETTINGS_ICON;
    public static final ForgeConfigSpec.BooleanValue BULK_TRANSFER_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue BULK_TRANSFER_OVERLAY_SECONDS;
    public static final ForgeConfigSpec.BooleanValue FULL_INVENTORY_BARRIER_ICONS;
    public static final ForgeConfigSpec.ConfigValue<String> INVENTORY_FULL_SOUND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CAPACITY_DISPLAY_MODE = builder.defineEnum("capacityDisplayMode", CapacityDisplayMode.BOTH);
        PICKUP_NOTIFICATION = builder.defineEnum("pickupLimitNotification", PickupNotification.HUD);
        TOOLTIP_INDEXING = builder.define("enableSearchTooltipIndexing", true);
        HOTBAR_CYCLE_OVERLAY = builder.define("enableHotbarCycleOverlay", true);
        ITEM_COUNT_MODE = builder.defineEnum("browserItemCountMode", ItemCountMode.COMPACT);
        OVERALL_COUNT_MODE = builder.defineEnum("browserOverallCountMode", OverallCountMode.STACKS);
        MANAGE_TABS_ICON = builder.define("manageTabsIcon", "minecraft:name_tag");
        SETTINGS_ICON = builder.define("settingsIcon", "minecraft:redstone");
        BULK_TRANSFER_OVERLAY = builder.define("showBulkTransferOverlay", true);
        BULK_TRANSFER_OVERLAY_SECONDS = builder.defineInRange("bulkTransferOverlaySeconds", 2.5D, 0.25D, 30.0D);
        FULL_INVENTORY_BARRIER_ICONS = builder.comment("Show barrier icons in empty projected player slots while the cursor-held stack has no capacity available")
                .define("showFullInventoryBarrierIcons", true);
        INVENTORY_FULL_SOUND = builder.comment("Sound event played after a cursor placement fails for lack of player-inventory capacity; leave blank to disable")
                .define("inventoryFullSound", "minecraft:block.note_block.bass");
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
