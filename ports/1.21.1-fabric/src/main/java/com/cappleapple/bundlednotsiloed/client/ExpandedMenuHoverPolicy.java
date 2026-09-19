package com.cappleapple.bundlednotsiloed.client;

/** Decides whether an expanded inventory menu owns the pointer instead of a covered item slot. */
public final class ExpandedMenuHoverPolicy {
    private ExpandedMenuHoverPolicy() {}

    public static boolean blocksUnderlyingSlot(
            boolean categoryMenuOpen,
            boolean insideCategoryMenu,
            boolean settingsMenuOpen,
            boolean insideSettingsMenu
    ) {
        return categoryMenuOpen && insideCategoryMenu
                || settingsMenuOpen && insideSettingsMenu;
    }
}
