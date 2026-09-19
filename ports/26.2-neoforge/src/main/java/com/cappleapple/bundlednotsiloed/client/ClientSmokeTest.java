package com.cappleapple.bundlednotsiloed.client;
import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
/** Opt-in development gate for client startup and target mixin application. */
@EventBusSubscriber(modid = BundledNotSiloed.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static boolean finished;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws ClassNotFoundException {
        if (!Boolean.getBoolean("bundlednotsiloed.clientSmoke") || finished) return;
        Minecraft client = Minecraft.getInstance();
        org.lwjgl.glfw.GLFW.glfwHideWindow(client.getWindow().handle());
        client.mouseHandler.releaseMouse();
        if (client.gui.overlay() != null || client.gui.screen() == null) return;
        for (String name : new String[]{
                "com.cappleapple.bundlednotsiloed.client.screen.BundledInventoryScreen",
                "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "net.minecraft.client.multiplayer.MultiPlayerGameMode",
                "net.minecraft.client.MouseHandler", "net.minecraft.client.KeyMapping"}) Class.forName(name);
        finished = true;
        BundledNotSiloed.LOGGER.info("BNS_CLIENT_SMOKE_PASSED: title screen ready and client mixins loaded");
        client.stop();
    }
}
