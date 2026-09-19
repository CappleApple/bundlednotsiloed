package com.cappleapple.bundlednotsiloed.platform;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/** TOML-backed values owned by this mod; load before registering gameplay callbacks. */
public final class ModConfigSpec {
    private final Map<String, ConfigValue<?>> values;
    private CommentedFileConfig file;
    private ModConfigSpec(Map<String, ConfigValue<?>> values) { this.values = Map.copyOf(values); }
    public void load(Path path) {
        file = CommentedFileConfig.builder(path).sync().build();
        file.load();
        values.forEach((key, value) -> {
            Object loaded = file.get(key);
            if (loaded != null) value.read(loaded);
            file.set(key, value.serialized());
            value.owner = this;
            value.path = key;
        });
        file.save();
    }
    public void save() { if (file != null) file.save(); }
    public static class ConfigValue<T> {
        protected T value;
        private final Function<Object, T> parser;
        private ModConfigSpec owner;
        private String path;
        ConfigValue(T value, Function<Object, T> parser) { this.value = value; this.parser = parser; }
        public T get() { return value; }
        public void set(T next) {
            value = parser.apply(next);
            if (owner != null) { owner.file.set(path, serialized()); owner.save(); }
        }
        void read(Object raw) {
            try { value = parser.apply(raw); }
            catch (IllegalArgumentException ignored) { /* Keep the declared default for invalid values. */ }
        }
        Object serialized() { return value instanceof Enum<?> e ? e.name() : value; }
    }
    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(boolean value) { super(value, raw -> { if (raw instanceof Boolean b) return b; throw new IllegalArgumentException(); }); }
        public boolean getAsBoolean() { return get(); }
    }
    public static final class IntValue extends ConfigValue<Integer> {
        IntValue(int value, int min, int max) { super(value, raw -> { if (!(raw instanceof Number n) || n.longValue() < min || n.longValue() > max) throw new IllegalArgumentException(); return n.intValue(); }); }
        public int getAsInt() { return get(); }
    }
    public static final class DoubleValue extends ConfigValue<Double> {
        DoubleValue(double value, double min, double max) { super(value, raw -> { if (!(raw instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() < min || n.doubleValue() > max) throw new IllegalArgumentException(); return n.doubleValue(); }); }
        public double getAsDouble() { return get(); }
    }
    public static final class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(T value) { super(value, raw -> Enum.valueOf(value.getDeclaringClass(), raw.toString())); }
    }
    public static final class Builder {
        private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();
        private final List<String> sections = new ArrayList<>();
        public Builder push(String section) { sections.add(section); return this; }
        public Builder pop() { sections.remove(sections.size() - 1); return this; }
        public Builder comment(String comment) { return this; }
        private <V extends ConfigValue<?>> V add(String key, V value) { values.put(String.join(".", sections) + (sections.isEmpty() ? "" : ".") + key, value); return value; }
        public BooleanValue define(String key, boolean value) { return add(key, new BooleanValue(value)); }
        public ConfigValue<String> define(String key, String value) { return add(key, new ConfigValue<>(value, Object::toString)); }
        public IntValue defineInRange(String key, int value, int min, int max) { return add(key, new IntValue(value, min, max)); }
        public DoubleValue defineInRange(String key, double value, double min, double max) { return add(key, new DoubleValue(value, min, max)); }
        public <T extends Enum<T>> EnumValue<T> defineEnum(String key, T value) { return add(key, new EnumValue<>(value)); }
        public ModConfigSpec build() { return new ModConfigSpec(values); }
    }
}
