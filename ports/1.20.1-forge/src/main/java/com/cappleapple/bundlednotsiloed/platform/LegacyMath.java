package com.cappleapple.bundlednotsiloed.platform;
public final class LegacyMath {
 private LegacyMath() {}
 public static int ceilDiv(int x,int y) { return -Math.floorDiv(-x,y); }
 public static long ceilDiv(long x,long y) { return -Math.floorDiv(-x,y); }
}
