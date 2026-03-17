package com.sch246.muhc.mixin;

import com.sch246.muhc.util.IMaidHandCrank;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KineticBlockEntity.class)
public abstract class KineticBlockEntityMixin {

    @Shadow(remap = false) protected float lastCapacityProvided;

    @Inject(method = "calculateAddedStressCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private void onCalculateAddedStressCapacity(CallbackInfoReturnable<Float> cir) {
        // 判断当前这个 KineticBlockEntity 是不是手摇曲柄
        if ((Object) this instanceof IMaidHandCrank maidCrank) {
            // 如果女仆正在操作 (tick > 0)
            if (maidCrank.muhc$getTick() > 0) {
                float capacity = maidCrank.muhc$getStress();
                this.lastCapacityProvided = capacity;
                cir.setReturnValue(capacity);
            }
        }
    }
}
