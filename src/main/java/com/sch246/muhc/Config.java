package com.sch246.muhc;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final String TRANSLATE_KEY = "muhc.configuration";
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static String translateKey(String key) {
        return TRANSLATE_KEY + "." + key;
    }

    static {
        // 基础设置
        BUILDER.push("basic");
        PRIORITY = BUILDER
                .translation(translateKey("priority"))
                .defineInRange("priority", 5, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        // 搜索范围设置
        BUILDER.push("range");
        CENTER_SEARCH_RADIUS = BUILDER
                .translation(translateKey("centerSearchRadius"))
                .defineInRange("centerSearchRadius", 0, 0, Integer.MAX_VALUE);
        MAID_SEARCH_RADIUS = BUILDER
                .translation(translateKey("maidSearchRadius"))
                .defineInRange("maidSearchRadius", 0, 0, Integer.MAX_VALUE);
        REACH_RADIUS = BUILDER
                .translation(translateKey("reachRadius"))
                .defineInRange("reachRadius", 4, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        // 行为设置
        BUILDER.push("behavior");
        CLAIMS_BEFOREHAND = BUILDER
                .translation(translateKey("claimsBeforehand"))
                .define("claimsBeforehand", true);
        RANDOM_WALK = BUILDER
                .translation(translateKey("randomWalk"))
                .define("randomWalk", true);
        BUILDER.pop();

        // 操作时间设置
        BUILDER.push("timing");
        BUBBLE_INTERVAL = BUILDER
                .translation(translateKey("bubbleInterval"))
                .defineInRange("bubbleInterval", 600, 1, Integer.MAX_VALUE);
        OPERATION_INTERVAL = BUILDER
                .translation(translateKey("operationInterval"))
                .defineInRange("operationInterval", 9, 1, Integer.MAX_VALUE);
        OPERATION_DURATION = BUILDER
                .translation(translateKey("operationDuration"))
                .defineInRange("operationDuration", 11, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        // 压力和好感度设置
        BUILDER.push("stress");
        BASE_STRESS = BUILDER
                .translation(translateKey("baseStress"))
                .defineInRange("baseStress", 256, 1, Integer.MAX_VALUE);
        STREES_PER_FAVORABILITY = BUILDER
                .translation(translateKey("stressPerFavorability"))
                .defineInRange("stressPerFavorability", 10, 0, Integer.MAX_VALUE);
        BUILDER.pop();
    }

    // 声明配置项
    public static final ModConfigSpec.IntValue PRIORITY;
    public static final ModConfigSpec.IntValue CENTER_SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue MAID_SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue REACH_RADIUS;
    public static final ModConfigSpec.IntValue BUBBLE_INTERVAL;
    public static final ModConfigSpec.BooleanValue CLAIMS_BEFOREHAND;
    public static final ModConfigSpec.BooleanValue RANDOM_WALK;
    public static final ModConfigSpec.IntValue OPERATION_INTERVAL;
    public static final ModConfigSpec.IntValue OPERATION_DURATION;
    public static final ModConfigSpec.IntValue BASE_STRESS;
    public static final ModConfigSpec.IntValue STREES_PER_FAVORABILITY;

//    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
//            .comment("What you want the introduction message to be for the magic number")
//            .define("magicNumberIntroduction", "The magic number is... ");
//
//    // a list of strings that are treated as resource locations for items
//    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
//            .comment("A list of items to log on common setup.")
//            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

//    private static boolean validateItemName(final Object obj) {
//        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
//    }
}
