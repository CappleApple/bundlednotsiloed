package com.cappleapple.bundlednotsiloed.client;
public final class BundledNotSiloedClient {
 public static void initialize() {
  net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> {
   if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) ClientSmokeTest.tick(net.minecraft.client.Minecraft.getInstance());
  });
  net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientKeyMappings::register);
  net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ClientEvents.class);
 }
}
