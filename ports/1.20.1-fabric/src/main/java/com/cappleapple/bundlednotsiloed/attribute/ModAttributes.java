package com.cappleapple.bundlednotsiloed.attribute;
import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.inventory.PlayerInventoryDefaults;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
public final class ModAttributes {
 public static final double DEFAULT_CAPACITY=PlayerInventoryDefaults.CAPACITY_UNITS;
 public static final Attribute INVENTORY_CAPACITY=Registry.register(BuiltInRegistries.ATTRIBUTE,
  BundledNotSiloed.id("inventory_capacity"),new RangedAttribute("attribute.name.bundlednotsiloed.inventory_capacity",DEFAULT_CAPACITY,0,Integer.MAX_VALUE).setSyncable(true));
 private ModAttributes() {}
 public static void register() {}
}
