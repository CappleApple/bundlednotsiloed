package com.cappleapple.bundlednotsiloed.platform;
import java.util.*;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
public final class PayloadRegistrar {
 private static final List<Runnable> CLIENT_RECEIVERS=new ArrayList<>();
 private static final Map<ResourceLocation,StreamCodec<RegistryFriendlyByteBuf,?>> CODECS=new HashMap<>();
 public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec,BiConsumer<T,PayloadContext> handler) {
  CODECS.put(type.id(),codec);
  ServerPlayNetworking.registerGlobalReceiver(type.id(),(server,player,network,buffer,sender)-> {
   T payload=codec.decode(new RegistryFriendlyByteBuf(buffer));
   server.execute(()->handler.accept(payload,new PayloadContext(player)));
  });
 }
 public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec,BiConsumer<T,PayloadContext> handler) {
  CODECS.put(type.id(),codec);CLIENT_RECEIVERS.add(()->ClientReceivers.register(type,codec,handler));
 }
 public static void registerClient() { CLIENT_RECEIVERS.forEach(Runnable::run); }
 @SuppressWarnings("unchecked")
 public static net.minecraft.network.FriendlyByteBuf encode(CustomPacketPayload payload) {
  var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
  ((StreamCodec<RegistryFriendlyByteBuf,CustomPacketPayload>)CODECS.get(payload.type().id())).encode(buffer,payload);
  return buffer;
 }
 private static final class ClientReceivers {
  static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec,BiConsumer<T,PayloadContext> handler) {
   net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type.id(),(client,network,buffer,sender)-> {
    T payload=codec.decode(new RegistryFriendlyByteBuf(buffer));
    client.execute(()-> { if(client.player!=null) handler.accept(payload,new PayloadContext(client.player)); });
   });
  }
 }
}
