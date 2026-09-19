package com.cappleapple.bundlednotsiloed.platform;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
public final class PacketDistributor {
 private PacketDistributor() {}
 public static void sendToPlayer(ServerPlayer player,CustomPacketPayload payload) { ServerPlayNetworking.send(player,payload.type().id(),PayloadRegistrar.encode(payload)); }
 public static void sendToServer(CustomPacketPayload payload) { net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload.type().id(),PayloadRegistrar.encode(payload)); }
}
