package com.sch246.muhc.config;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

public final class ClothConfigIntegration {
    private ClothConfigIntegration() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
                    if (ModList.get().isLoaded("cloth_config")) {
                        // 复用逻辑：创建完整 Screen
                        return ClothConfigScreen.create(parent);
                    } else {
                        // 降级逻辑：提示下载
                        return new NoClothConfigScreen(parent);
                    }
                })
        );
    }
}
