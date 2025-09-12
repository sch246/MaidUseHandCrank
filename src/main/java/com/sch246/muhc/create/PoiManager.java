package com.sch246.muhc.create;

import java.util.Set;

import com.simibubi.create.AllBlocks;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

public final class PoiManager {
    private static final Set<BlockState> HAND_CRANK_STATES;

    public PoiManager() {
    }

    public static PoiType getHandCrank() {
        return new PoiType(HAND_CRANK_STATES, 1, 1);
    }

    static {
        HAND_CRANK_STATES = AllBlocks.HAND_CRANK.get()
                .getStateDefinition()
                .getPossibleStates()
                .stream()
                .collect(com.google.common.collect.ImmutableSet.toImmutableSet());
    }
}
