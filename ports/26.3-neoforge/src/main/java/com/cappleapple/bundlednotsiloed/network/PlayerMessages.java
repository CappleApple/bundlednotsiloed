package com.cappleapple.bundlednotsiloed.network;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
public final class PlayerMessages {
    private PlayerMessages() {}
    public static void send(Player player, Component message, boolean overlay) {
        if (player instanceof ServerPlayer server) server.sendSystemMessage(message, overlay);
        else com.cappleapple.bundlednotsiloed.client.ClientPlayerMessages.send(message, overlay);
    }
}
