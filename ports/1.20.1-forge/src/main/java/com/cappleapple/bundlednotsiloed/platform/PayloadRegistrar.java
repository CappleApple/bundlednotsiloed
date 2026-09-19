package com.cappleapple.bundlednotsiloed.platform;
import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import java.util.function.BiConsumer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
public final class PayloadRegistrar {
 public static final SimpleChannel CHANNEL=NetworkRegistry.ChannelBuilder.named(BundledNotSiloed.id("main"))
  .networkProtocolVersion(()->"12-forge1201").clientAcceptedVersions("12-forge1201"::equals).serverAcceptedVersions("12-forge1201"::equals).simpleChannel();
 private int index;
 public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec,BiConsumer<T,IPayloadContext> handler) { register(type,codec,handler,NetworkDirection.PLAY_TO_CLIENT); }
 public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec,BiConsumer<T,IPayloadContext> handler) { register(type,codec,handler,NetworkDirection.PLAY_TO_SERVER); }
 private <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type,StreamCodec<RegistryFriendlyByteBuf,T> codec,BiConsumer<T,IPayloadContext> handler,NetworkDirection direction) {
  CHANNEL.messageBuilder(type.payloadClass(),index++,direction)
   .encoder((payload,buffer)->codec.encode(new RegistryFriendlyByteBuf(buffer),payload))
   .decoder(buffer->codec.decode(new RegistryFriendlyByteBuf(buffer)))
   .consumerMainThread((payload,supplier)-> {
    var context=supplier.get();
    IPayloadContext target=direction==NetworkDirection.PLAY_TO_SERVER ? context::getSender : ClientPacketContext::player;
    if(target.player()!=null) handler.accept(payload,target);
    context.setPacketHandled(true);
   }).add();
 }
}
