package com.cappleapple.bundlednotsiloed.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;


public final class ClientKeyMappings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(com.cappleapple.bundlednotsiloed.BundledNotSiloed.id("bundlednotsiloed"));
    public static final KeyMapping SEARCH_BROWSER = new KeyMapping("key.bundlednotsiloed.search_browser",
            KeyConflictContext.GUI, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, com.mojang.blaze3d.platform.InputConstants.KEY_F, CATEGORY);
    // Avoid Sophisticated Core's default [ and ] bulk-transfer bindings.
    public static final KeyMapping CYCLE_FORWARD = new KeyMapping("key.bundlednotsiloed.cycle_forward", InputConstants.Type.MOUSE, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_4, CATEGORY);
    public static final KeyMapping CYCLE_BACKWARD = new KeyMapping("key.bundlednotsiloed.cycle_backward", InputConstants.Type.MOUSE, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_5, CATEGORY);
    public static final KeyMapping DUMP_TO_CONTAINER = new KeyMapping("key.bundlednotsiloed.dump_to_container",
            KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, com.mojang.blaze3d.platform.InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping EXTRACT_FROM_CONTAINER = new KeyMapping("key.bundlednotsiloed.extract_from_container",
            KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, com.mojang.blaze3d.platform.InputConstants.KEY_H, CATEGORY);
    public static final KeyMapping TOGGLE_AUTO_REFILL = new KeyMapping("key.bundlednotsiloed.toggle_auto_refill",
            KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private ClientKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(SEARCH_BROWSER);
        event.register(CYCLE_FORWARD);
        event.register(CYCLE_BACKWARD);
        event.register(DUMP_TO_CONTAINER);
        event.register(EXTRACT_FROM_CONTAINER);
        event.register(TOGGLE_AUTO_REFILL);
    }
}
