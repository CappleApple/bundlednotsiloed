package com.cappleapple.bundlednotsiloed.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Registers codecs on both sides and installs client receivers only in the client entrypoint. */
public final class PayloadRegistrar {
    private static final List<Runnable> CLIENT_RECEIVERS = new ArrayList<>();
    public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf,T> codec, BiConsumer<T,PayloadContext> receiver) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> receiver.accept(payload, new PayloadContext(context.player())));
    }
    public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf,T> codec, BiConsumer<T,PayloadContext> receiver) {
        PayloadTypeRegistry.playS2C().register(type, codec);
        CLIENT_RECEIVERS.add(() -> ClientReceivers.register(type, receiver));
    }
    public static void registerClient() { CLIENT_RECEIVERS.forEach(Runnable::run); }
    private static final class ClientReceivers {
        static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, BiConsumer<T,PayloadContext> receiver) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type,
                    (payload, context) -> receiver.accept(payload, new PayloadContext(context.player())));
        }
    }
}
