package com.sch246.muhc;

import com.mojang.logging.LogUtils;
import com.sch246.muhc.create.InitPoi;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(MaidUseHandCrank.MODID)
public class MaidUseHandCrank {
    public static final String MODID = "muhc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidUseHandCrank(IEventBus modEventBus, ModContainer modContainer) {
        InitPoi.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
