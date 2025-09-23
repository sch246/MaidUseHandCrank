package com.sch246.muhc.mixin;

import com.sch246.muhc.Config;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrame.class)
public class ItemFrameMixin {
    @Redirect(method = "survives",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean allowCollisionWithCrank(Level level, Entity entity) {
        // 如果与手摇曲柄碰撞，直接返回 true
        if (level.getBlockEntity(entity.blockPosition()) instanceof HandCrankBlockEntity) {
            return true;
        }
        // 否则使用原版逻辑
        return level.noCollision(entity);
    }

}
