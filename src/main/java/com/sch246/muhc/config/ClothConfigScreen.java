// file: src/main/java/com/sch246/muhc/client/ClothConfigIntegration.java
package com.sch246.muhc.config;

import com.sch246.muhc.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ClothConfigScreen {
    private ClothConfigScreen() {
    }

    @SuppressWarnings("unchecked")
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        var main = root.getOrCreateCategory(Component.translatable("muhc.configuration.title"));
        for (Config.Category cat : Config.getCategories()) {
            // 类别标题键：muhc.configuration.<category>
            var cc = entryBuilder.startSubCategory(Component.translatable(cat.labelKey)).setExpanded(true);
            for (Config.Entry<?> e : cat.entries) {
                switch (e.type) {
                    case INT -> {
                        ForgeConfigSpec.IntValue h = (ForgeConfigSpec.IntValue) e.valueHandle;
                        int cur = h.get();
                        var r = (Config.NumericRange<Integer>) e.range;
                        var ent = entryBuilder.startIntField(Component.translatable(e.labelKey), cur)
                                .setDefaultValue((Integer) e.defaultValue)
                                .setMin(r.min)
                                .setMax(r.max)
                                .setTooltip(Component.translatable(e.tooltipKey))
                                .setSaveConsumer(h::set)
                                .build();
                        cc.add(ent);
                    }
                    case BOOLEAN -> {
                        ForgeConfigSpec.BooleanValue h = (ForgeConfigSpec.BooleanValue) e.valueHandle;
                        boolean cur = h.get();
                        var ent = entryBuilder.startBooleanToggle(Component.translatable(e.labelKey), cur)
                                .setDefaultValue((Boolean) e.defaultValue)
                                .setTooltip(Component.translatable(e.tooltipKey))
                                .setSaveConsumer(h::set)
                                .build();
                        cc.add(ent);
                    }
                    case DOUBLE -> {
                        ForgeConfigSpec.DoubleValue h = (ForgeConfigSpec.DoubleValue) e.valueHandle;
                        double cur = h.get();
                        var r = (Config.NumericRange<Double>) e.range;
                        var ent = entryBuilder.startDoubleField(Component.translatable(e.labelKey), cur)
                                .setDefaultValue((Double) e.defaultValue)
                                .setMin(r.min)
                                .setMax(r.max)
                                .setTooltip(Component.translatable(e.tooltipKey))
                                .setSaveConsumer(h::set)
                                .build();
                        cc.add(ent);
                    }
                    case STRING_LIST -> {
                        ForgeConfigSpec.ConfigValue<List<String>> h = (ForgeConfigSpec.ConfigValue<List<String>>) e.valueHandle;
                        List<String> cur = h.get();
                        var ent = entryBuilder.startStrList(Component.translatable(e.labelKey), cur)
                                .setDefaultValue((List<String>) e.defaultValue)
                                .setTooltip(Component.translatable(e.tooltipKey))
                                .setSaveConsumer(h::set)
                                .build();
                        cc.add(ent);
                    }
                }
            }

            main.addEntry(cc.build());
        }
    }
}