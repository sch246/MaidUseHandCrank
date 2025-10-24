package com.sch246.muhc.mixin;

import com.sch246.muhc.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemFrameItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameItem.class)
public class ItemFrameItemMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void allowPlaceOverCrank(Player player, Direction direction,
                                     ItemStack itemStack, BlockPos pos,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (Config.ITEM_FRAME_INTERACTION.get()) {
            cir.setReturnValue(true);
        }
    }
}