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

//    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
//            .comment("Whether to log the dirt block on common setup")
//            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue PRIORITY = BUILDER
            .comment("The priority of hand crank tasks\nThe task needs to be reset to take effect")
            .translation(translateKey("priority"))
            .defineInRange("priority", 5, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SEARCH_RADIUS = BUILDER
            .comment("The search distance of the hand crank\nThe task needs to be reset to take effect")
            .translation(translateKey("searchRadius"))
            .defineInRange("searchRadius", 8, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue REACH_RADIUS = BUILDER
            .comment("Interaction with the hand crank will only start when the distance is less than this\nThe task needs to be reset to take effect")
            .translation(translateKey("reachRadius"))
            .defineInRange("reachRadius", 4, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue OPERATION_INTERVAL = BUILDER
            .comment("Lower values mean faster speed")
            .translation(translateKey("operationInterval"))
            .defineInRange("operationInterval", 10, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue BUBBLE_INTERVAL = BUILDER
            .comment("How often to say something while cranking")
            .translation(translateKey("bubbleInterval"))
            .defineInRange("bubbleInterval", 600, 1, Integer.MAX_VALUE);
//
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
