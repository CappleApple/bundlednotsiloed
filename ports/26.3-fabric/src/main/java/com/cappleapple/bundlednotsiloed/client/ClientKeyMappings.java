package com.cappleapple.bundlednotsiloed.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;


public final class ClientKeyMappings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(com.cappleapple.bundlednotsiloed.BundledNotSiloed.id("bundlednotsiloed"));
    public static final KeyMapping SEARCH_BROWSER = new KeyMapping("key.bundlednotsiloed.search_browser",
            InputConstants.Type.KEYBOARD, com.mojang.blaze3d.platform.InputConstants.KEY_F, CATEGORY);
    // Avoid Sophisticated Core's default [ and ] bulk-transfer bindings.
    public static final KeyMapping CYCLE_FORWARD = new KeyMapping("key.bundlednotsiloed.cycle_forward", InputConstants.Type.MOUSE, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_4, CATEGORY);
    public static final KeyMapping CYCLE_BACKWARD = new KeyMapping("key.bundlednotsiloed.cycle_backward", InputConstants.Type.MOUSE, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_5, CATEGORY);
    public static final KeyMapping DUMP_TO_CONTAINER = new KeyMapping("key.bundlednotsiloed.dump_to_container",
            InputConstants.Type.KEYBOARD, com.mojang.blaze3d.platform.InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping EXTRACT_FROM_CONTAINER = new KeyMapping("key.bundlednotsiloed.extract_from_container",
            InputConstants.Type.KEYBOARD, com.mojang.blaze3d.platform.InputConstants.KEY_H, CATEGORY);
    public static final KeyMapping TOGGLE_AUTO_REFILL = new KeyMapping("key.bundlednotsiloed.toggle_auto_refill",
            InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private ClientKeyMappings() {}

    public static void register() {
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(SEARCH_BROWSER);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(CYCLE_FORWARD);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(CYCLE_BACKWARD);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(DUMP_TO_CONTAINER);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(EXTRACT_FROM_CONTAINER);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(TOGGLE_AUTO_REFILL);
    }
}
