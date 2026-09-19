package com.cappleapple.bundlednotsiloed.api;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** Immutable public description of a category rule. Regex rules use expression instead of target. */
public record CategoryRuleView(Type type, @Nullable Identifier target, @Nullable String expression) {
    public CategoryRuleView(Type type, Identifier target) {
        this(type, target, null);
    }

    public enum Type { ITEM, TAG, MOD_ID, REGEX }
}
