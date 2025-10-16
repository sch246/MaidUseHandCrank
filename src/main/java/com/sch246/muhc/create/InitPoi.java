package com.sch246.muhc.create;

import com.sch246.muhc.MaidUseHandCrank;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;


public class InitPoi {
    public static final DeferredRegister<PoiType> POI_TYPES;
    public static final DeferredHolder<PoiType, PoiType> HAND_CRANK;

    public InitPoi() {
    }

    static {
        POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MaidUseHandCrank.MODID);
        HAND_CRANK = POI_TYPES.register("hand_crank", PoiManager::getCrankPoiType);
    }

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
