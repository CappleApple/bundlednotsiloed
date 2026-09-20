package com.cappleapple.bundlednotsiloed.client;

import com.cappleapple.bundlednotsiloed.BundledNotSiloed;
import com.cappleapple.bundlednotsiloed.client.screen.*;
import com.cappleapple.bundlednotsiloed.data.ModAttachments;
import com.cappleapple.bundlednotsiloed.network.ModNetwork;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Opt-in real-client regression checks. Only runs in the isolated development audit world. */
public final class ClientGameplayTest {
    private static int stage, ticks;
    private static long started = System.currentTimeMillis();
    private static CompletableFuture<Void> serverWork = CompletableFuture.completedFuture(null);
    private static BundledInventoryScreen inventory;
    private static long total, planksBefore, applesBefore;
    private static net.minecraft.world.SimpleContainer auditChest;
    private static boolean done;
    private static final boolean RESUME = Boolean.getBoolean("bundlednotsiloed.clientGameplayResume");
    private ClientGameplayTest() {}

    public static void tick(Minecraft client) {
        if (done) return;
        try {
            if (System.currentTimeMillis() - started > 180_000) throw new AssertionError("Gameplay timeout at stage " + stage + ": " + screen(client));
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) return;
            if (!serverWork.isDone()) return;
            serverWork.join();
            if (++ticks < 25 || ClientInventoryWindows.pending()) return;
            ticks = 0;
            BundledNotSiloed.LOGGER.info("BNS_GAMEPLAY_STAGE {} resume={}", stage, RESUME);
            if (RESUME) { resume(client); return; }
            switch (stage++) {
                case 0 -> {
                    localization(client);
                    server(client, p -> {
                        p.setGameMode(GameType.SURVIVAL);
                        var data = ModAttachments.get(p);
                        data.inventory().clear();
                        data.setMigratedVanillaInventory();
                        int slot = 9;
                        for (var item : BuiltInRegistries.ITEM) {
                            if (item == Items.AIR || item == Items.DIAMOND || item.getDefaultInstance().getMaxStackSize() != 64) continue;
                            data.inventory().replaceSyntheticSlotFromItemUse(slot++, new ItemStack(item, 4));
                            if (slot == 59) break;
                        }
                        data.inventory().replaceSyntheticSlotFromItemUse(42, new ItemStack(Items.DIAMOND, 16));
                        data.resetInventoryWindow(); data.syncVanillaCompatibilityView();
                        ModNetwork.queueInventorySync(p);
                    });
                }
                case 1 -> {
                    check(count(client, Items.DIAMOND) == 16, "Initial server inventory did not synchronize");
                    total = countAll(client);
                    setScreen(client, new InventoryScreen(client.player));
                }
                case 2 -> {
                    check(screen(client) instanceof BundledInventoryScreen, "Vanilla inventory was not replaced");
                    inventory = (BundledInventoryScreen)screen(client);
                    screenshot(client, "01-inventory");
                    var rail = rail(inventory);
                    click(inventory, rail.searchX()+5, rail.searchY()+5, 0);
                    type(inventory, "diamond");
                }
                case 3 -> {
                    check((boolean)field(inventory,"identityWindow"), "Search did not switch to filtered inventory");
                    check(client.player.getInventory().getItem(9).is(Items.DIAMOND), "Search did not expose the stowed diamond");
                    check(countAll(client) == total, "Search changed inventory contents");
                    screenshot(client, "02-search");
                    client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, 9, 0, ClickType.PICKUP, client.player);
                }
                case 4 -> {
                    check(client.player.containerMenu.getCarried().is(Items.DIAMOND) && client.player.containerMenu.getCarried().getCount()==16, "Filtered slot pickup failed");
                    client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, 36, 0, ClickType.PICKUP, client.player);
                }
                case 5 -> {
                    check(client.player.containerMenu.getCarried().isEmpty(), "Cursor did not clear after placing into hotbar");
                    check(client.player.getInventory().getItem(0).is(Items.DIAMOND), "Hotbar placement failed");
                    check(countAll(client)==total, "Filtered transfer lost or duplicated items");
                    server(client,p -> check(p.getInventory().getItem(0).is(Items.DIAMOND) && p.getInventory().getItem(0).getCount()==16,"Server did not accept filtered transfer"));
                    var rail=rail(inventory); click(inventory,rail.searchX()+5,rail.searchY()+5,1);
                }
                case 6 -> { scroll(inventory, inventory.width/2.0, inventory.height/2.0+20, -1); }
                case 7 -> {
                    check((int)field(inventory,"entryScrollRow") > 0, "Inventory scrolling did not advance");
                    check(countAll(client)==total,"Scrolling changed stored items");
                    screenshot(client,"03-scrolled");
                    var rail=rail(inventory); click(inventory,rail.categoryX()+5,rail.categoryY()+5,0);
                }
                case 8 -> {
                    check((boolean)field(inventory,"categoryMenuOpen"),"Category button did not open");
                    screenshot(client,"04-categories");
                    var rail=rail(inventory); click(inventory,rail.settingsX()+5,rail.settingsY()+5,0);
                }
                case 9 -> {
                    check((boolean)field(inventory,"settingsMenuOpen"),"Settings button did not open");
                    screenshot(client,"05-options");
                    setScreen(client,new InventoryBrowserSettingsScreen(inventory));
                }
                case 10 -> {
                    var settings=(InventoryBrowserSettingsScreen)screen(client);
                    validateWidgets(settings);
                    var button=(Button)field(settings,"autoRefillButton");
                    String before=button.getMessage().getString(); press(button);
                    check(!before.equals(button.getMessage().getString()),"Setting toggle did not update its label");
                    screenshot(client,"06-settings");
                    press(button); pressButton(settings,"gui.done");
                }
                case 11 -> { setScreen(client,new CategoryManagerScreen(inventory,client.player)); }
                case 12 -> {
                    validateWidgets(screen(client)); screenshot(client,"07-tab-manager");
                    pressButton(screen(client),"gui.bundlednotsiloed.add_tab");
                }
                case 13 -> {
                    check(screen(client) instanceof CategoryEditorScreen,"Add Tab did not open the editor");
                    var editor=(CategoryEditorScreen)screen(client);
                    ((EditBox)field(editor,"name")).setValue("Audit \\u03a9".replace("\\u03a9","\u03a9"));
                    ((EditBox)field(editor,"icon")).setValue("minecraft:diamond");
                    ((EditBox)field(editor,"ruleSearch")).setValue("diamond");
                    validateWidgets(editor); screenshot(client,"08-tab-editor");
                }
                case 14 -> { pressButton(screen(client),"gui.done"); }
                case 15 -> {
                    check(ModAttachments.get(client.player).categories().categories().stream().anyMatch(c->c.displayName().startsWith("Audit ")),"Saved category did not synchronize");
                    server(client,p->check(ModAttachments.get(p).categories().categories().stream().anyMatch(c->c.displayName().startsWith("Audit ")),"Server did not store category edit"));
                    setScreen(client,new InventoryScreen(client.player));
                }
                case 16 -> {
                    inventory=(BundledInventoryScreen)screen(client);
                    check(countAll(client)==total,"Opening auxiliary screens lost inventory items");
                    screenshot(client,"09-returned-inventory");
                    server(client,p-> { client.getSingleplayerServer().getPlayerList().saveAll(); });
                }
                case 17 -> {
                    planksBefore=count(client,Items.OAK_PLANKS);
                    server(client,p -> {var d=ModAttachments.get(p);d.inventory().replaceSyntheticSlotFromItemUse(1,new ItemStack(Items.OAK_LOG));d.syncVanillaCompatibilityView();ModNetwork.queueInventorySync(p);});
                }
                case 18 -> nativeClick(client,37,ClickType.PICKUP);
                case 19 -> nativeClick(client,1,ClickType.PICKUP);
                case 20 -> {
                    check(client.player.containerMenu.getSlot(0).getItem().is(Items.OAK_PLANKS),"Crafting result did not synchronize");
                    screenshot(client,"12-crafting"); nativeClick(client,0,ClickType.QUICK_MOVE);
                }
                case 21 -> {
                    check(count(client,Items.OAK_PLANKS)==planksBefore+4,"Shift-click crafting did not return four planks");
                    check(client.player.containerMenu.getCarried().isEmpty(),"Crafting left cursor contents");
                    check(countAll(client)==total+4,"Crafting lost or duplicated contents");
                    nativeClick(client,36,ClickType.PICKUP);
                }
                case 22 -> nativeClick(client,45,ClickType.PICKUP);
                case 23 -> {
                    check(client.player.getOffhandItem().is(Items.DIAMOND) && client.player.getOffhandItem().getCount()==16,"Offhand placement failed");
                    check(count(client,Items.DIAMOND)==0,"Offhand was duplicated in logical inventory");
                    nativeClick(client,45,ClickType.PICKUP);
                }
                case 24 -> nativeClick(client,36,ClickType.PICKUP);
                case 25 -> {
                    check(client.player.getOffhandItem().isEmpty() && count(client,Items.DIAMOND)==16,"Returning offhand to storage lost items");
                    applesBefore=count(client,Items.APPLE);
                    server(client,p -> {
                        auditChest=new net.minecraft.world.SimpleContainer(27); auditChest.setItem(0,new ItemStack(Items.APPLE,8));
                        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->net.minecraft.world.inventory.ChestMenu.threeRows(id,inv,auditChest),Component.literal("Audit Chest")));
                    });
                }
                case 26 -> {
                    check(client.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu,"Server chest did not open on client");
                    screenshot(client,"13-container"); nativeClick(client,0,ClickType.QUICK_MOVE);
                }
                case 27 -> {
                    check(count(client,Items.APPLE)==applesBefore+8,"Chest shift-click did not reach the logical inventory");
                    check(countAll(client)==total+12,"Chest transfer lost or duplicated contents");
                    server(client,p->check(auditChest.isEmpty(),"Chest contents were duplicated"));
                    client.player.closeContainer();
                }
                case 28 -> {
                    setScreen(client,new InventoryScreen(client.player));
                    server(client,p->client.getSingleplayerServer().getPlayerList().saveAll());
                }
                case 29 -> { screenshot(client,"14-final-inventory"); }
                case 30 -> finish(client,"BNS_GAMEPLAY_PASSED");
                default -> throw new AssertionError("Unexpected stage");
            }
        } catch (Throwable error) {
            done=true;
            BundledNotSiloed.LOGGER.error("BNS_GAMEPLAY_FAILED stage="+stage,error);
            try { Files.writeString(client.gameDirectory.toPath().resolve("gameplay-failed.txt"),error.toString()); } catch(Exception ignored) {}
            client.stop();
        }
    }
    private static void resume(Minecraft client) throws Exception {
        switch(stage++) {
            case 0 -> {
                localization(client);
                check(client.player.getInventory().getItem(0).is(Items.DIAMOND) && client.player.getInventory().getItem(0).getCount()==16,"Hotbar did not persist across restart");
                check(ModAttachments.get(client.player).categories().categories().stream().anyMatch(c->c.displayName().startsWith("Audit ")),"Edited category did not persist across restart");
                check(countAll(client)==224,"Stored items did not persist across restart: "+countAll(client));
                setScreen(client,new InventoryScreen(client.player));
            }
            case 1 -> { check(screen(client) instanceof BundledInventoryScreen,"Inventory replacement failed after restart"); screenshot(client,"10-rejoined-localized"); setScreen(client,new InventoryBrowserSettingsScreen(screen(client))); }
            case 2 -> { validateWidgets(screen(client)); screenshot(client,"11-localized-settings"); }
            case 3 -> finish(client,"BNS_GAMEPLAY_RESUME_PASSED");
        }
    }
    private static void finish(Minecraft client,String marker) throws Exception {
        done=true; Files.writeString(client.gameDirectory.toPath().resolve(marker+".txt"),"Passed real client world checks\n");
        BundledNotSiloed.LOGGER.info(marker); client.stop();
    }
    private static void localization(Minecraft client) throws Exception {
        check(client.getLanguageManager().getSelected().equals(RESUME ? "de_de" : "en_us"), "Requested language was not selected");
        var location=new net.minecraft.resources.ResourceLocation("bundlednotsiloed","lang/en_us.json");
        try(var reader=client.getResourceManager().getResource(location).orElseThrow().openAsReader()) {
            var translations=JsonParser.parseReader(reader).getAsJsonObject();
            for(var entry:translations.entrySet()) {
                check(Language.getInstance().has(entry.getKey()),"Missing loaded translation: "+entry.getKey());
                check(!Component.translatable(entry.getKey()).getString().contains(entry.getKey()),"Unresolved translation: "+entry.getKey());
            }
            BundledNotSiloed.LOGGER.info("BNS_LOCALIZATION_PASSED keys={}",translations.size());
        }
    }
    private static void validateWidgets(Screen screen) {
        check(!screen.getTitle().getString().contains("gui.bundlednotsiloed."),"Untranslated screen title");
        for(var child:screen.children()) if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
            String label=widget.getMessage().getString();
            check(!label.contains("gui.bundlednotsiloed.")&&!label.contains("tooltip.bundlednotsiloed."),"Untranslated widget: "+label);
        }
    }
    private static void server(Minecraft client,Consumer<ServerPlayer> task) {
        serverWork=new CompletableFuture<>();
        client.getSingleplayerServer().execute(()-> {
            try { task.accept(client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID())); serverWork.complete(null); }
            catch(Throwable error){serverWork.completeExceptionally(error);}
        });
    }
    private static long count(Minecraft client,net.minecraft.world.item.Item item) {return ModAttachments.get(client.player).inventory().backingStacks().stream().filter(s->s.is(item)).mapToLong(ItemStack::getCount).sum();}
    private static long countAll(Minecraft client) {return ModAttachments.get(client.player).inventory().backingStacks().stream().mapToLong(ItemStack::getCount).sum();}
    private static Object field(Object object,String name) throws Exception {Class<?> type=object.getClass(); while(type!=null){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException e){type=type.getSuperclass();}}throw new NoSuchFieldException(name);}
    private static InventorySideRail.Rail rail(BundledInventoryScreen screen) throws Exception {var m=screen.getClass().getDeclaredMethod("sideRail");m.setAccessible(true);return (InventorySideRail.Rail)m.invoke(screen);}
    private static void pressButton(Screen screen,String key) throws Exception {String label=Component.translatable(key).getString(); for(var child:screen.children()) if(child instanceof Button b && b.getMessage().getString().equals(label)){press(b);return;}throw new AssertionError("Missing button "+label);}
    private static void press(Button button) throws Exception {button.onPress();}
    private static Screen screen(Minecraft client) {return client.screen;}
    private static void setScreen(Minecraft client,Screen value) {client.setScreen(value);}
    private static void click(Screen screen,double x,double y,int button) {screen.mouseClicked(x,y,button);}
    private static void type(Screen screen,String text) {for(int point:text.codePoints().toArray()){screen.charTyped((char)point,0);}}
    private static void scroll(Screen screen,double x,double y,double amount) {screen.mouseScrolled(x,y,amount);}
    private static void screenshot(Minecraft client,String name) throws Exception {Screenshot.grab(client.gameDirectory,name+".png",client.getMainRenderTarget(),message -> BundledNotSiloed.LOGGER.info("BNS_SCREENSHOT {}",message.getString()));}
    private static void nativeClick(Minecraft client,int slot,ClickType kind) {client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId,slot,0,kind,client.player);}
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
}
