package com.cappleapple.bundlednotsiloed.platform;
import net.minecraft.server.level.ServerPlayer;
public final class PacketDistributor {
 public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
  PayloadRegistrar.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),payload);
 }
 public static void sendToServer(CustomPacketPayload payload) { PayloadRegistrar.CHANNEL.sendToServer(payload); }
}
