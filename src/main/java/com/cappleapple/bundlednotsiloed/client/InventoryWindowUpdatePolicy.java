package com.cappleapple.bundlednotsiloed.client;

/** Decides when the transient real-slot window may be remapped around a cursor-held stack. */
public final class InventoryWindowUpdatePolicy {
    private InventoryWindowUpdatePolicy() {}

    public static boolean shouldApply(
            boolean cursorCarrying,
            boolean navigationDirty,
            boolean windowPreviouslyApplied
    ) {
        return !cursorCarrying || navigationDirty || !windowPreviouslyApplied;
    }
}
