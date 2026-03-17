package com.sch246.muhc.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sch246.muhc.util.IMaidHandCrank;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KineticBlockEntity.class)
public abstract class KineticBlockEntityMixin {

    @Shadow(remap = false) protected float lastCapacityProvided;

    @WrapMethod(remap = false, method = "calculateAddedStressCapacity")
    private float wrapCalculateAddedStressCapacity(Operation<Float> original) {
        // 判断当前这个 KineticBlockEntity 是不是手摇曲柄
        if ((Object) this instanceof IMaidHandCrank maidCrank) {
            // 如果女仆正在操作 (tick > 0)
            if (maidCrank.muhc$getTick() > 0) {
                float capacity = maidCrank.muhc$getStress();
                this.lastCapacityProvided = capacity;
                return capacity;
            }
        }
        return original.call();
    }
}
