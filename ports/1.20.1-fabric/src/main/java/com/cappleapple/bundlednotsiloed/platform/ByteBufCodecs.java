package com.cappleapple.bundlednotsiloed.platform;
public final class ByteBufCodecs {
 public static final StreamCodec<RegistryFriendlyByteBuf,Long> VAR_LONG=new StreamCodec<>() {
  public Long decode(RegistryFriendlyByteBuf b) { return b.readVarLong(); }
  public void encode(RegistryFriendlyByteBuf b,Long v) { b.writeVarLong(v); }
 };
}
