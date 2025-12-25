package com.sch246.muhc.event;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.sch246.muhc.config.ClothConfigScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClothConfigEvent {
    @SubscribeEvent
    public void onEvent(AddClothConfigEvent event) {
        ClothConfigScreen.init(event.getRoot(), event.getEntryBuilder());
    }
}
