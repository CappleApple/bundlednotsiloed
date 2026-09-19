package com.cappleapple.bundlednotsiloed.config;

import com.cappleapple.bundlednotsiloed.inventory.PlayerInventoryDefaults;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue BASE_CAPACITY;
    public static final ForgeConfigSpec.BooleanValue CATEGORY_LIMITS_WORLD_PICKUP;
    public static final ForgeConfigSpec.BooleanValue CATEGORY_LIMITS_MANUAL_TRANSFERS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_PARTIAL_PICKUP;
    public static final ForgeConfigSpec.BooleanValue OVER_CAPACITY_BLOCKS_PICKUP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("inventory");
        BASE_CAPACITY = builder.comment("Base value applied to the inventory_capacity attribute for a new player; includes the 27-slot main grid and 9-slot hotbar.")
                .defineInRange("baseInventoryCapacity", PlayerInventoryDefaults.CAPACITY_UNITS, 0, Integer.MAX_VALUE);
        OVER_CAPACITY_BLOCKS_PICKUP = builder.define("overCapacityBlocksPickup", true);
        ALLOW_PARTIAL_PICKUP = builder.define("allowPartialPickup", true);
        builder.pop().push("categories");
        CATEGORY_LIMITS_WORLD_PICKUP = builder.define("categoryLimitsAffectWorldPickup", true);
        CATEGORY_LIMITS_MANUAL_TRANSFERS = builder.define("categoryLimitsAffectManualTransfers", false);
        builder.pop();
        SPEC = builder.build();
    }

    private CommonConfig() {}
}
