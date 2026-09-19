package com.cappleapple.bundlednotsiloed;
import com.cappleapple.bundlednotsiloed.attribute.ModAttributes;
import com.cappleapple.bundlednotsiloed.config.*;
import com.cappleapple.bundlednotsiloed.network.ModNetwork;
import com.cappleapple.bundlednotsiloed.server.ServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
public final class BundledNotSiloed implements ModInitializer {
 public static final String MOD_ID="bundlednotsiloed";
 public static final Logger LOGGER=LogUtils.getLogger();
 @Override public void onInitialize() {
  CommonConfig.SPEC.load(FabricLoader.getInstance().getConfigDir().resolve("bundlednotsiloed-common.toml"));
  ModAttributes.register();
  ModNetwork.registerPayloads();
  ServerEvents.register();
  com.cappleapple.bundlednotsiloed.compat.SophisticatedBackpacksCompatibility.commonSetup();
 }
 public static ResourceLocation id(String path) { return new ResourceLocation(MOD_ID,path); }
}
