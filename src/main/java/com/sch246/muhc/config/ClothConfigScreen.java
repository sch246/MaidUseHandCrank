// file: src/main/java/com/sch246/muhc/client/ClothConfigIntegration.java
package com.sch246.muhc.config;

import com.sch246.muhc.Config;
import com.sch246.muhc.MaidUseHandCrank;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public final class ClothConfigScreen {
    private ClothConfigScreen() {}

    public static void init() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {

                    ConfigBuilder root = ConfigBuilder.create()
                            .setParentScreen(parent)
                            .setTitle(Component.translatable("muhc.configuration.title"));
                    root.setGlobalized(true);
                    root.setGlobalizedExpanded(false);

                    ConfigEntryBuilder entryBuilder = root.entryBuilder();

                    for (Config.Category cat : Config.getCategories()) {
                        // 类别标题键：muhc.configuration.<category>
                        ConfigCategory cc = root.getOrCreateCategory(Component.translatable(cat.labelKey));
                        for (Config.Entry<?> e : cat.entries) {
                            switch (e.type) {
                                case INT -> {
                                    net.minecraftforge.common.ForgeConfigSpec.IntValue h =
                                            (net.minecraftforge.common.ForgeConfigSpec.IntValue) e.valueHandle;
                                    int cur = h.get();
                                    @SuppressWarnings("unchecked")
                                    var r = (Config.NumericRange<Integer>) e.range;
                                    var ent = entryBuilder.startIntField(Component.translatable(e.labelKey), cur)
                                            .setDefaultValue((Integer) e.defaultValue)
                                            .setMin(r.min)
                                            .setMax(r.max)
                                            .setTooltip(Component.translatable(e.tooltipKey))
                                            .setSaveConsumer(h::set)
                                            .build();
                                    cc.addEntry(ent);
                                }
                                case BOOLEAN -> {
                                    net.minecraftforge.common.ForgeConfigSpec.BooleanValue h =
                                            (net.minecraftforge.common.ForgeConfigSpec.BooleanValue) e.valueHandle;
                                    boolean cur = h.get();
                                    var ent = entryBuilder.startBooleanToggle(Component.translatable(e.labelKey), cur)
                                            .setDefaultValue((Boolean) e.defaultValue)
                                            .setTooltip(Component.translatable(e.tooltipKey))
                                            .setSaveConsumer(h::set)
                                            .build();
                                    cc.addEntry(ent);
                                }
                                case DOUBLE -> {
                                    net.minecraftforge.common.ForgeConfigSpec.DoubleValue h =
                                            (net.minecraftforge.common.ForgeConfigSpec.DoubleValue) e.valueHandle;
                                    double cur = h.get();
                                    @SuppressWarnings("unchecked")
                                    var r = (Config.NumericRange<Double>) e.range;
                                    var ent = entryBuilder.startDoubleField(Component.translatable(e.labelKey), cur)
                                            .setDefaultValue((Double) e.defaultValue)
                                            .setMin(r.min)
                                            .setMax(r.max)
                                            .setTooltip(Component.translatable(e.tooltipKey))
                                            .setSaveConsumer(h::set)
                                            .build();
                                    cc.addEntry(ent);
                                }
                                case STRING_LIST -> {
                                    @SuppressWarnings("unchecked")
                                    net.minecraftforge.common.ForgeConfigSpec.ConfigValue<List<String>> h =
                                            (net.minecraftforge.common.ForgeConfigSpec.ConfigValue<List<String>>) e.valueHandle;
                                    List<String> cur = h.get();
                                    @SuppressWarnings("unchecked")
                                    var ent = entryBuilder.startStrList(Component.translatable(e.labelKey), cur)
                                            .setDefaultValue((List<String>) e.defaultValue)
                                            .setTooltip(Component.translatable(e.tooltipKey))
                                            .setSaveConsumer(h::set)
                                            .build();
                                    cc.addEntry(ent);
                                }
                            }
                        }
                    }

                    root.setSavingRunnable(() -> {
                        var cfgs = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON);
                        if (cfgs != null) {
                            cfgs.stream()
                                    .filter(c -> c.getModId().equals(MaidUseHandCrank.MODID))
                                    .forEach(ModConfig::save);
                        }
                    });

                    return root.build();
                }));

    }
}