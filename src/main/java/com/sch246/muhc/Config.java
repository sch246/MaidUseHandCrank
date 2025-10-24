// file: src/main/java/com/sch246/muhc/Config.java
package com.sch246.muhc;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

/**
 * 单一来源定义：动态生成 ForgeConfigSpec 与 Cloth Config 元数据。
 * 翻译键修正：使用 "muhc.configuration.<key>" 与 "muhc.configuration.<key>.tooltip"
 * 类别标题使用 "muhc.configuration.<category>"
 */
public class Config {
    public static final String MODID = MaidUseHandCrank.MODID;
    // 翻译键根：与你的语言文件一致
    private static final String TRANSLATE_ROOT = "muhc.configuration";

    public enum Type {
        INT, BOOLEAN, DOUBLE, STRING_LIST
    }

    public static final class NumericRange<T extends Number> {
        public final T min;
        public final T max;
        public final boolean slider;

        public NumericRange(T min, T max, boolean slider) {
            this.min = min;
            this.max = max;
            this.slider = slider;
        }

        public static NumericRange<Integer> ofInt(int min, int max, boolean slider) {
            return new NumericRange<>(min, max, slider);
        }

        public static NumericRange<Double> ofDouble(double min, double max, boolean slider) {
            return new NumericRange<>(min, max, slider);
        }
    }

    public static final class Entry<T> {
        public final String category;          // 例如 "basic"
        public final String key;               // 例如 "priority"
        public final Type type;
        public final String labelKey;          // muhc.configuration.priority
        public final String tooltipKey;        // muhc.configuration.priority.tooltip
        public final T defaultValue;
        public final NumericRange<?> range;
        public Object valueHandle;             // IntValue/BooleanValue/DoubleValue/ConfigValue<List<String>>

        private Entry(String category, String key, Type type,
                      String labelKey, String tooltipKey,
                      T defaultValue, NumericRange<?> range) {
            this.category = category;
            this.key = key;
            this.type = type;
            this.labelKey = labelKey;
            this.tooltipKey = tooltipKey;
            this.defaultValue = defaultValue;
            this.range = range;
        }

        // 按你的语言文件规则生成键：不带 category 前缀
        private static String labelOf(String key) {
            return TRANSLATE_ROOT + "." + key;
        }
        private static String tooltipOf(String key) {
            return TRANSLATE_ROOT + "." + key + ".tooltip";
        }

        public static Entry<Integer> intEntry(String category, String key, int defaultValue, NumericRange<Integer> range) {
            return new Entry<>(category, key, Type.INT, labelOf(key), tooltipOf(key), defaultValue, range);
        }

        public static Entry<Boolean> boolEntry(String category, String key, boolean defaultValue) {
            return new Entry<>(category, key, Type.BOOLEAN, labelOf(key), tooltipOf(key), defaultValue, null);
        }

        public static Entry<Double> doubleEntry(String category, String key, double defaultValue, NumericRange<Double> range) {
            return new Entry<>(category, key, Type.DOUBLE, labelOf(key), tooltipOf(key), defaultValue, range);
        }

        public static Entry<List<String>> strListEntry(String category, String key, List<String> defaultList) {
            return new Entry<>(category, key, Type.STRING_LIST, labelOf(key), tooltipOf(key), defaultList, null);
        }

        @SuppressWarnings("unchecked")
        private void attachSpec(ForgeConfigSpec.Builder b) {
            switch (type) {
                case INT -> {
                    NumericRange<Integer> r = (NumericRange<Integer>) range;
                    ForgeConfigSpec.IntValue v = b.comment(tooltipKey)
                            .translation(labelKey)
                            .defineInRange(key, (Integer) defaultValue, r.min, r.max);
                    this.valueHandle = v;
                }
                case BOOLEAN -> {
                    boolean def = ((Boolean) defaultValue).booleanValue();
                    ForgeConfigSpec.BooleanValue v = b.comment(tooltipKey)
                            .translation(labelKey)
                            .define(key, def);
                    this.valueHandle = v;
                }
                case DOUBLE -> {
                    NumericRange<Double> r = (NumericRange<Double>) range;
                    ForgeConfigSpec.DoubleValue v = b.comment(tooltipKey)
                            .translation(labelKey)
                            .defineInRange(key, (Double) defaultValue, r.min, r.max);
                    this.valueHandle = v;
                }
                case STRING_LIST -> {
                    ForgeConfigSpec.ConfigValue<List<String>> v = b.comment(tooltipKey)
                            .translation(labelKey)
                            .define(key, (List<String>) defaultValue);
                    this.valueHandle = v;
                }
            }
        }
    }

    public static final class Category {
        public final String key;        // 例如 "basic"
        public final String labelKey;   // muhc.configuration.basic
        public final List<Entry<?>> entries;

        public Category(String key, List<Entry<?>> entries) {
            this.key = key;
            this.labelKey = TRANSLATE_ROOT + "." + key; // 使用你的分类键
            this.entries = entries;
        }
    }

    private static final List<Category> CATEGORIES = new ArrayList<>();

    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.IntValue PRIORITY;
    public static ForgeConfigSpec.IntValue CENTER_SEARCH_RADIUS;
    public static ForgeConfigSpec.IntValue MAID_SEARCH_RADIUS;
    public static ForgeConfigSpec.IntValue REACH_RADIUS;
    public static ForgeConfigSpec.BooleanValue CLAIMS_BEFOREHAND;
    public static ForgeConfigSpec.BooleanValue RANDOM_WALK;
    public static ForgeConfigSpec.BooleanValue ITEM_FRAME_INTERACTION;
    public static ForgeConfigSpec.IntValue BUBBLE_INTERVAL;
    public static ForgeConfigSpec.IntValue OPERATION_INTERVAL;
    public static ForgeConfigSpec.IntValue OPERATION_DURATION;
    public static ForgeConfigSpec.IntValue BASE_STRESS;
    public static ForgeConfigSpec.IntValue STRESS_PER_FAVORABILITY;
    public static ForgeConfigSpec.BooleanValue ENDURANCE_OPERATION;
    public static ForgeConfigSpec.BooleanValue TWO_HANDED_OPERATION;

    static {
        // basic
        List<Entry<?>> basic = new ArrayList<>();
        basic.add(Entry.intEntry("basic", "priority", 5, NumericRange.ofInt(0, Integer.MAX_VALUE, false)));

        // behavior
        List<Entry<?>> behavior = new ArrayList<>();
        behavior.add(Entry.intEntry("behavior", "centerSearchRadius", 0, NumericRange.ofInt(0, Integer.MAX_VALUE, false)));
        behavior.add(Entry.intEntry("behavior", "maidSearchRadius", 0, NumericRange.ofInt(0, Integer.MAX_VALUE, false)));
        behavior.add(Entry.intEntry("behavior", "reachRadius", 4, NumericRange.ofInt(0, Integer.MAX_VALUE, false)));
        behavior.add(Entry.boolEntry("behavior", "claimsBeforehand", true));
        behavior.add(Entry.boolEntry("behavior", "randomWalk", true));
        behavior.add(Entry.boolEntry("behavior", "itemFrameInteraction", true));

        // work
        List<Entry<?>> work = new ArrayList<>();
        work.add(Entry.intEntry("work", "bubbleInterval", 600, NumericRange.ofInt(1, Integer.MAX_VALUE, false)));
        work.add(Entry.intEntry("work", "operationInterval", 8, NumericRange.ofInt(1, Integer.MAX_VALUE, false)));
        work.add(Entry.intEntry("work", "operationDuration", 10, NumericRange.ofInt(1, Integer.MAX_VALUE, false)));
        work.add(Entry.intEntry("work", "baseStress", 256, NumericRange.ofInt(0, Integer.MAX_VALUE, false)));
        work.add(Entry.intEntry("work", "stressPerFavorability", 10, NumericRange.ofInt(0, Integer.MAX_VALUE, false)));
        work.add(Entry.boolEntry("work", "enduranceOperation", true));
        work.add(Entry.boolEntry("work", "twoHandedOperation", true));

        CATEGORIES.add(new Category("basic", basic));
        CATEGORIES.add(new Category("behavior", behavior));
        CATEGORIES.add(new Category("work", work));

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        for (Category cat : CATEGORIES) {
            builder.push(cat.key);
            for (Entry<?> e : cat.entries) {
                e.attachSpec(builder);
            }
            builder.pop();
        }
        SPEC = builder.build();

        PRIORITY = (ForgeConfigSpec.IntValue) find("basic", "priority");

        CENTER_SEARCH_RADIUS = (ForgeConfigSpec.IntValue) find("behavior", "centerSearchRadius");
        MAID_SEARCH_RADIUS = (ForgeConfigSpec.IntValue) find("behavior", "maidSearchRadius");
        REACH_RADIUS = (ForgeConfigSpec.IntValue) find("behavior", "reachRadius");
        CLAIMS_BEFOREHAND = (ForgeConfigSpec.BooleanValue) find("behavior", "claimsBeforehand");
        RANDOM_WALK = (ForgeConfigSpec.BooleanValue) find("behavior", "randomWalk");
        ITEM_FRAME_INTERACTION = (ForgeConfigSpec.BooleanValue) find("behavior", "itemFrameInteraction");

        BUBBLE_INTERVAL = (ForgeConfigSpec.IntValue) find("work", "bubbleInterval");
        OPERATION_INTERVAL = (ForgeConfigSpec.IntValue) find("work", "operationInterval");
        OPERATION_DURATION = (ForgeConfigSpec.IntValue) find("work", "operationDuration");
        BASE_STRESS = (ForgeConfigSpec.IntValue) find("work", "baseStress");
        STRESS_PER_FAVORABILITY = (ForgeConfigSpec.IntValue) find("work", "stressPerFavorability");
        ENDURANCE_OPERATION = (ForgeConfigSpec.BooleanValue) find("work", "enduranceOperation");
        TWO_HANDED_OPERATION = (ForgeConfigSpec.BooleanValue) find("work", "twoHandedOperation");
    }

    public static List<Category> getCategories() {
        return Collections.unmodifiableList(CATEGORIES);
    }

    private static Object find(String category, String key) {
        for (Category cat : CATEGORIES) {
            if (!cat.key.equals(category)) continue;
            for (Entry<?> e : cat.entries) {
                if (e.key.equals(key)) {
                    return e.valueHandle;
                }
            }
        }
        throw new NoSuchElementException("Config entry not found: " + category + "." + key);
    }
}