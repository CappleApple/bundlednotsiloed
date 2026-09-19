package com.cappleapple.bundlednotsiloed.client;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public final class ClientPlayerMessages {
    private ClientPlayerMessages() {}
    public static void send(Component message, boolean overlay) {
        var player = Minecraft.getInstance().player;
        if (player != null) { if (overlay) player.sendOverlayMessage(message); else player.sendSystemMessage(message); }
    }
}
