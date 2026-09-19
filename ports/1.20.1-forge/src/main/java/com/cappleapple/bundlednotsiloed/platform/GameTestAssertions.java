package com.cappleapple.bundlednotsiloed.platform;
public final class GameTestAssertions {
 public static void equal(net.minecraft.gametest.framework.GameTestHelper helper,Object actual,Object expected,String message) {
  helper.assertTrue(java.util.Objects.equals(actual,expected),message+": expected "+expected+", got "+actual);
 }
}
