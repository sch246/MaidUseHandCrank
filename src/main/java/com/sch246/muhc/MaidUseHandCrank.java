package com.sch246.muhc;

import com.sch246.muhc.config.ClothConfigIntegration;
import com.sch246.muhc.create.InitPoi;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MaidUseHandCrank.MODID)
public class MaidUseHandCrank {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "muhc";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidUseHandCrank() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        InitPoi.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // 注册 Mods 界面的“设置”按钮与界面
        if (!FMLEnvironment.production) {
            LOGGER.info("[{}] Registering config screen...", MODID);
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClothConfigIntegration.registerConfigScreen();
        }
    }

}
