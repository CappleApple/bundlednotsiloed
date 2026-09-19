package com.cappleapple.bundlednotsiloed.platform;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
final class ClientPacketContext { static Player player() { return Minecraft.getInstance().player; } }
