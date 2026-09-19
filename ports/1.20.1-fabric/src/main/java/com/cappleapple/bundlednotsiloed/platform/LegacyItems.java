package com.cappleapple.bundlednotsiloed.platform;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
public final class LegacyItems {
 private LegacyItems() {}
 public static int hash(ItemStack stack) { return 31*stack.getItem().hashCode()+java.util.Objects.hashCode(stack.getTag()); }
 public static final StreamCodec<RegistryFriendlyByteBuf,ItemStack> STREAM_CODEC=new StreamCodec<>() {
  public ItemStack decode(RegistryFriendlyByteBuf b) {
   if (!b.readBoolean()) return ItemStack.EMPTY;
   int count=b.readVarInt();
   if(count<1) throw new IllegalArgumentException("Invalid item count");
   CompoundTag tag=b.readNbt();
   if(tag==null) throw new IllegalArgumentException("Missing item NBT");
   ItemStack stack=ItemStack.of(tag);
   if(stack.isEmpty()) throw new IllegalArgumentException("Unknown item");
   stack.setCount(count);return stack;
  }
  public void encode(RegistryFriendlyByteBuf b,ItemStack stack) {
   b.writeBoolean(!stack.isEmpty());if(stack.isEmpty()) return;
   b.writeVarInt(stack.getCount());b.writeNbt(stack.copyWithCount(1).save(new CompoundTag()));
  }
 };
}
