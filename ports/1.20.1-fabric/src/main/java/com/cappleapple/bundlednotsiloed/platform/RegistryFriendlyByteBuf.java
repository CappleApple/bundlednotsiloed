package com.cappleapple.bundlednotsiloed.platform;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
public final class RegistryFriendlyByteBuf extends FriendlyByteBuf {
 public RegistryFriendlyByteBuf(ByteBuf buffer) { super(buffer); }
 public RegistryFriendlyByteBuf(ByteBuf buffer, RegistryAccess ignored) { super(buffer); }
}
