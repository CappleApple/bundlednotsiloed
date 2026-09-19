package com.cappleapple.bundlednotsiloed.client;
import com.cappleapple.bundlednotsiloed.config.ClientConfig;
import com.cappleapple.bundlednotsiloed.platform.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
public final class BundledNotSiloedClient implements ClientModInitializer {
 @Override public void onInitializeClient() {
  ClientConfig.SPEC.load(FabricLoader.getInstance().getConfigDir().resolve("bundlednotsiloed-client.toml"));
  ClientKeyMappings.register(); PayloadRegistrar.registerClient();
  ClientTickEvents.END_CLIENT_TICK.register(ClientSmokeTest::tick);
  ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.clientTick());
  ClientPlayConnectionEvents.JOIN.register((handler,sender,client) -> ClientEvents.playerLoggingIn(client.player));
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> ClientEvents.playerLoggingOut());
  ItemTooltipCallback.EVENT.register((stack,context,type,tooltip) -> ClientEvents.appendExactInventoryCount(stack,tooltip));
  HudRenderCallback.EVENT.register((graphics,tick) -> ClientEvents.renderHud(new UiEvent(null,graphics,0,0,0,0)));
  ScreenEvents.AFTER_INIT.register((client,screen,width,height) -> {
   ClientEvents.initializeContainerOverlay(UiEvent.screen(screen));
   ScreenEvents.remove(screen).register(old -> ClientEvents.closeContainerOverlay(UiEvent.screen(old)));
   ScreenEvents.afterRender(screen).register((current,graphics,x,y,delta) -> ClientEvents.renderContainerTooltips(new UiEvent(current,graphics,x,y,0,0)));
   ScreenKeyboardEvents.allowKeyPress(screen).register((current,key,scan,mods) -> {
    UiEvent event=new UiEvent(current,null,0,0,key,scan);
    ClientEvents.awaitWindowKey(event);
    if (!event.isCanceled()) ClientEvents.keyPlayerInventorySearch(event);
    if (!event.isCanceled()) ClientEvents.keyContainerOverlay(event);
    return !event.isCanceled();
   });
   ScreenKeyboardEvents.afterKeyRelease(screen).register((current,key,scan,mods) -> ClientEvents.releaseKeyContainerOverlay(new UiEvent(current,null,0,0,key,scan)));
   ScreenMouseEvents.allowMouseRelease(screen).register((current,x,y,button) -> {
    ClientEvents.releaseFullInventoryPlacement(new UiEvent(current,null,(int)x,(int)y,button,0)); return true;
   });
  });
 }
}
