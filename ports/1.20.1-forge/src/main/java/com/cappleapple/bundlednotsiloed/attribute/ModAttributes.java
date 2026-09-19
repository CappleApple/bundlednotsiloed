package com.cappleapple.bundlednotsiloed.attribute;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.inventory.PlayerInventoryDefaults;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModAttributes {
    public static final double DEFAULT_CAPACITY = PlayerInventoryDefaults.CAPACITY_UNITS;
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, BundledNotSiloed.MOD_ID);
    public static final RegistryObject<Attribute> INVENTORY_CAPACITY = ATTRIBUTES.register(
            "inventory_capacity",
            () -> new RangedAttribute("attribute.name.bundlednotsiloed.inventory_capacity", DEFAULT_CAPACITY, 0, Integer.MAX_VALUE).setSyncable(true)
    );

    private ModAttributes() {}

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, INVENTORY_CAPACITY.get());
    }
}
