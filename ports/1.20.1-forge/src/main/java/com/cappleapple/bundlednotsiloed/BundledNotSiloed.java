package com.cappleapple.bundlednotsiloed;

import com.mojang.logging.LogUtils;
import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.compat.SophisticatedBackpacksCompatibility;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.config.CommonConfig;
import com.cappleapple.bundlednotsiloed.network.ModNetwork;
import com.cappleapple.bundlednotsiloed.server.ServerEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

@Mod(BundledNotSiloed.MOD_ID)
public final class BundledNotSiloed {
    public static final String MOD_ID = "bundlednotsiloed";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BundledNotSiloed() {
        IEventBus modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        var container = net.minecraftforge.fml.ModLoadingContext.get();
        ModAttributes.ATTRIBUTES.register(modBus);
        modBus.addListener(ModAttributes::addPlayerAttributes);
        modBus.addListener(SophisticatedBackpacksCompatibility::commonSetup);
        ModNetwork.registerPayloads();

        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "bundlednotsiloed-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "bundlednotsiloed-client.toml");
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
        net.minecraftforge.fml.DistExecutor.safeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> com.cappleapple.bundlednotsiloed.client.BundledNotSiloedClient::initialize);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
