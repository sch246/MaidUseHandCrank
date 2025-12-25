package com.sch246.muhc;

import com.mojang.logging.LogUtils;
import com.sch246.muhc.create.InitPoi;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MaidUseHandCrank.MODID)
public class MaidUseHandCrank {
    public static final String MODID = "muhc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidUseHandCrank() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        InitPoi.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
