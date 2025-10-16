package com.sch246.muhc.create;

import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.simibubi.create.AllBlocks;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

import com.hlysine.create_connected.CCBlocks;


public final class PoiManager {
    private static final Set<BlockState> ALL_CRANK_STATES;

    public PoiManager() {
    }

    public static PoiType getCrankPoiType() {
        return new PoiType(ALL_CRANK_STATES, 1, 1);
    }

    static {
        Stream<BlockState> handCrankStates = AllBlocks.HAND_CRANK.get()
                .getStateDefinition()
                .getPossibleStates()
                .stream();

        // create_connected 的方块
        Stream<BlockState> crankWheelStates = Stream.empty();
        Stream<BlockState> largeCrankWheelStates = Stream.empty();

        // 直接引用，如果mod未安装，下面代码不会被调用（但编译仍需依赖，需加optional runtime依赖）
        if (isCreateConnectedLoaded()) {
            crankWheelStates = CCBlocks.CRANK_WHEEL.get()
                    .getStateDefinition()
                    .getPossibleStates()
                    .stream();
            largeCrankWheelStates = CCBlocks.LARGE_CRANK_WHEEL.get()
                    .getStateDefinition()
                    .getPossibleStates()
                    .stream();
        }

        // 合并所有，收集成Set
        ALL_CRANK_STATES = Stream.of(handCrankStates, crankWheelStates, largeCrankWheelStates)
                .flatMap(s -> s)
                .collect(ImmutableSet.toImmutableSet());

//        HAND_CRANK_STATES = AllBlocks.HAND_CRANK.get()
//                .getStateDefinition()
//                .getPossibleStates()
//                .stream()
//                .collect(com.google.common.collect.ImmutableSet.toImmutableSet());
    }

    private static boolean isCreateConnectedLoaded() {
        return net.neoforged.fml.ModList.get().isLoaded("create_connected");
    }
}
