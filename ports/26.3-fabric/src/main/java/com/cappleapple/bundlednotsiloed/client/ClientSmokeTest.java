package com.cappleapple.bundlednotsiloed.client;
import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.client.Minecraft;
/** Opt-in development gate for client startup and target mixin application. */
public final class ClientSmokeTest {
    private static boolean finished;
    public static void tick(Minecraft client) {
        if (!Boolean.getBoolean("bundlednotsiloed.clientSmoke") || finished) return;
        org.lwjgl.sdl.SDLVideo.SDL_HideWindow(client.getWindow().handle());
        client.mouseHandler.releaseMouse();
        if (client.gui.overlay() != null || client.gui.screen() == null) return;
        for (String name : new String[]{
                "com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen",
                "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "net.minecraft.client.multiplayer.MultiPlayerGameMode",
                "net.minecraft.client.MouseHandler", "net.minecraft.client.KeyMapping"}) {
            try { Class.forName(name); } catch (ClassNotFoundException error) { throw new IllegalStateException(error); }
        }
        finished = true;
        BundledNotSiloed.LOGGER.info("BNS_CLIENT_SMOKE_PASSED: title screen ready and client mixins loaded");
        client.stop();
    }
}
