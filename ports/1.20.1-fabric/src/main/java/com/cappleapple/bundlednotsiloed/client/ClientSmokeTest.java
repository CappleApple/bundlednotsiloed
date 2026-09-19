package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Opt-in startup check used by the hidden, muted development client run. */
public final class ClientSmokeTest {
    private static boolean finished;
    private ClientSmokeTest() {}

    public static void tick(Minecraft client) {
        if (!Boolean.getBoolean("bundlednotsiloed.clientSmoke") || finished) return;
        GLFW.glfwHideWindow(client.getWindow().getWindow());
        client.mouseHandler.releaseMouse();
        if (client.getOverlay() != null || client.screen == null) return;
        finished = true;
        try {
            String[] targets = {
                "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
                "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
                "net.minecraft.client.multiplayer.MultiPlayerGameMode",
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen",
                "com.cappleapple.bundlednotsiloed.client.screen.CategoryManagerScreen",
                "com.cappleapple.bundlednotsiloed.client.screen.CategoryEditorScreen"
            };
            for (String target : targets) Class.forName(target);
            BundledNotSiloed.LOGGER.info("BNS_CLIENT_SMOKE_PASSED");
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException("Client smoke class failed to load", error);
        } finally {
            client.stop();
        }
    }
}
