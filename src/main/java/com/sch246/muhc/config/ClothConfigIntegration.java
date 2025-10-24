// file: src/main/java/com/sch246/muhc/client/ClothConfigIntegration.java
package com.sch246.muhc.config;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

public final class ClothConfigIntegration {
    private ClothConfigIntegration() {}

    public static void registerConfigScreen() {
        if (!ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) ->
                            new NoClothConfigScreen(parent)));
        } else {
            ClothConfigScreen.init();
        }

    }
}