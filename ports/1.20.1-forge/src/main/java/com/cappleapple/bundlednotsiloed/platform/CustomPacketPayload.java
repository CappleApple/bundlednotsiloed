package com.cappleapple.bundlednotsiloed.platform;
import net.minecraft.resources.ResourceLocation;
public interface CustomPacketPayload {
 Type<? extends CustomPacketPayload> type();
 record Type<T extends CustomPacketPayload>(ResourceLocation id, Class<T> payloadClass) {}
}
