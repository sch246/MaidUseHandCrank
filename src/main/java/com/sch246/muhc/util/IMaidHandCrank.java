package com.sch246.muhc.util;

import org.spongepowered.asm.mixin.Unique;

/*
 * 参见 com.sch246.muhc.create.HandCrankBlockEntityMixin
 */
public interface IMaidHandCrank {
    void muhc$setStress(float stress, int tick);
    float muhc$getStress();
    int muhc$getTick();
}
