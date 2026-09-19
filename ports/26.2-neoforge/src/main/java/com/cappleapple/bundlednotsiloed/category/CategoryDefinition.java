package com.cappleapple.bundlednotsiloed.category;

import java.util.List;
import net.minecraft.resources.Identifier;

public record CategoryDefinition(
        Identifier id,
        String displayName,
        Identifier icon,
        int order,
        List<CategoryRule> includes,
        List<CategoryRule> excludes,
        long pickupLimit,
        SortMode sortMode,
        boolean enabled,
        boolean allItems
) {
    public CategoryDefinition {
        includes = List.copyOf(includes);
        excludes = List.copyOf(excludes);
        if (pickupLimit < -1) throw new IllegalArgumentException("pickupLimit must be -1 or non-negative");
    }
}
