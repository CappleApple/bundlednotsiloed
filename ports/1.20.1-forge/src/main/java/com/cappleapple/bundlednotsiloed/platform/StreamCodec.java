package com.cappleapple.bundlednotsiloed.platform;
import java.util.function.Function;
import java.util.function.BiFunction;
public interface StreamCodec<B,T> {
 T decode(B buffer);
 void encode(B buffer,T value);
 static <B,T> StreamCodec<B,T> unit(T value) { return new StreamCodec<>() { public T decode(B b) { return value; } public void encode(B b,T v) {} }; }
 static <B,A,C,T> StreamCodec<B,T> composite(StreamCodec<B,A> a, Function<T,A> getA, StreamCodec<B,C> c, Function<T,C> getC, BiFunction<A,C,T> factory) {
  return new StreamCodec<>() { public T decode(B b) { return factory.apply(a.decode(b),c.decode(b)); } public void encode(B b,T t) { a.encode(b,getA.apply(t));c.encode(b,getC.apply(t)); } };
 }
}
