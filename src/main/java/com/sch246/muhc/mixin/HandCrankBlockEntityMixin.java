package com.sch246.muhc.mixin;

//import com.sch246.muhc.MaidUseHandCrank;
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
//    @Shadow
//    public int inUse;

    @Unique
    private float muhc$stress = 0;

    @Unique
    private int muhc$tick = 0;

//    @Shadow
//    public boolean backwards;

    @Shadow public abstract float getGeneratedSpeed();

    public HandCrankBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    public void muhc$setStress(float stress, int tick) {
        muhc$stress = stress;
        muhc$tick = tick;
        this.sendData(); // 发送数据包到客户端
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void onWrite(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        // 将自定义数据写入NBT
        compound.putFloat("muhc_stress", muhc$stress);
        compound.putInt("muhc_tick", muhc$tick);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void onRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        // 从NBT读取自定义数据
        muhc$stress = compound.getFloat("muhc_stress");
        muhc$tick = compound.getInt("muhc_tick");
    }

    @Inject(method = "tick", at = @At("TAIL"))
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
//            if (this.level != null && this.level.isClientSide()) {
//                MaidUseHandCrank.LOGGER.debug("calculateAddedStressCapacity: 直接应力 {}, 直接tick {}, 理论速度 {}, 实际速度 {}, inUse {}", muhc$stress, muhc$tick, speed, getGeneratedSpeed(), this.inUse);
//            }
            float capacity = muhc$stress;
//            MaidUseHandCrank.LOGGER.debug("应力大小: {}", capacity*32);
            this.lastCapacityProvided = capacity;
            return capacity;
        }
        return super.calculateAddedStressCapacity();
    }

}
