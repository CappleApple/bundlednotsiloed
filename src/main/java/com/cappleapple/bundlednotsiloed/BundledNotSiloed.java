package com.cappleapple.bundlednotsiloed;

import com.mojang.logging.LogUtils;
import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.compat.PlayerItemHandlerProvider;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.config.CommonConfig;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.ModNetwork;
import com.cappleapple.bundlednotsiloed.server.ServerEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(BundledNotSiloed.MOD_ID)
public final class BundledNotSiloed {
    public static final String MOD_ID = "bundlednotsiloed";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BundledNotSiloed(IEventBus modBus, ModContainer container) {
        ModAttributes.ATTRIBUTES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(ModAttributes::addPlayerAttributes);
        modBus.addListener(PlayerItemHandlerProvider::registerCapabilities);
        modBus.addListener(ModNetwork::registerPayloads);

        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "bundlednotsiloed-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "bundlednotsiloed-client.toml");
        NeoForge.EVENT_BUS.register(ServerEvents.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
