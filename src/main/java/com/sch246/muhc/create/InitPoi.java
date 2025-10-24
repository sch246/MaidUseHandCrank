// InitPoi.java (Forge)

package com.sch246.muhc.create;

import com.sch246.muhc.MaidUseHandCrank;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class InitPoi {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MaidUseHandCrank.MODID);

    public static final RegistryObject<PoiType> HAND_CRANK =
            POI_TYPES.register("hand_crank", PoiManager::getCrankPoiType);

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}