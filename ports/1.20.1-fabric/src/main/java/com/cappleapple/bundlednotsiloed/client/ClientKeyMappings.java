package com.cappleapple.bundlednotsiloed.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyMappings {
    public static final String CATEGORY = "key.categories.bundlednotsiloed";
    public static final KeyMapping SEARCH_BROWSER = new KeyMapping("key.bundlednotsiloed.search_browser",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);
    // Avoid Sophisticated Core's default [ and ] bulk-transfer bindings.
    public static final KeyMapping CYCLE_FORWARD = new KeyMapping("key.bundlednotsiloed.cycle_forward", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_4, CATEGORY);
    public static final KeyMapping CYCLE_BACKWARD = new KeyMapping("key.bundlednotsiloed.cycle_backward", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, CATEGORY);
    public static final KeyMapping DUMP_TO_CONTAINER = new KeyMapping("key.bundlednotsiloed.dump_to_container",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping EXTRACT_FROM_CONTAINER = new KeyMapping("key.bundlednotsiloed.extract_from_container",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping TOGGLE_AUTO_REFILL = new KeyMapping("key.bundlednotsiloed.toggle_auto_refill",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private ClientKeyMappings() {}

    public static void register() {
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(SEARCH_BROWSER);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(CYCLE_FORWARD);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(CYCLE_BACKWARD);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(DUMP_TO_CONTAINER);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(EXTRACT_FROM_CONTAINER);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(TOGGLE_AUTO_REFILL);
    }
}
