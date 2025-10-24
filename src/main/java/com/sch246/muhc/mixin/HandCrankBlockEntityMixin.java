package com.sch246.muhc.mixin;

import com.sch246.muhc.util.IMaidHandCrank;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandCrankBlockEntity.class)
public abstract class HandCrankBlockEntityMixin extends GeneratingKineticBlockEntity implements IMaidHandCrank {

    @Unique
    private float muhc$stress = 0;

    @Unique
    private int muhc$tick = 0;

    @Shadow(remap = false) public abstract float getGeneratedSpeed();

    public HandCrankBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    public void muhc$setStress(float stress, int tick) {
        muhc$stress = stress;
        muhc$tick = tick;
        this.sendData(); // 发送数据包到客户端
    }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("TAIL"), remap = false)
    private void onWrite(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        compound.putFloat("muhc_stress", muhc$stress);
        compound.putInt("muhc_tick", muhc$tick);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("TAIL"), remap = false)
    private void onRead(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        muhc$stress = compound.getFloat("muhc_stress");
        muhc$tick = compound.getInt("muhc_tick");
    }

    @Inject(method = "tick()V", at = @At("TAIL"), remap = false)
    private void onTick(CallbackInfo ci) {
        if (muhc$tick > 0) {
            --muhc$tick;
            if (muhc$tick == 0) {
                muhc$stress = 0;
            }
        }
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (muhc$tick > 0) {
            float capacity = muhc$stress;
            this.lastCapacityProvided = capacity;
            return capacity;
        }
        return super.calculateAddedStressCapacity();
    }
}