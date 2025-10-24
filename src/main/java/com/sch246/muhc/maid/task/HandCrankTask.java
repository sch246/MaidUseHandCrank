package com.sch246.muhc.maid.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.sch246.muhc.Config;
import com.sch246.muhc.MaidUseHandCrank;
import com.simibubi.create.AllBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public class HandCrankTask implements IMaidTask {
    private static final ResourceLocation UID = new ResourceLocation(MaidUseHandCrank.MODID, "hand_crank_task");

    private static ItemStack ICON = null;

    @Override
    public @Nonnull ResourceLocation getUid() {
        return UID;
    }

    @Override
    public @Nonnull ItemStack getIcon() {
        //延迟加载图标，确保 Create Mod 的方块已经注册完毕
        if (ICON == null) {
            // 获取 HandCrankBlock 的物品形式，并创建 ItemStack
            ICON = AllBlocks.HAND_CRANK.asStack();
        }
        return ICON;
    }

    @Override
    @Nullable
    public SoundEvent getAmbientSound(@Nonnull EntityMaid maid) {
        return SoundUtil.environmentSound(maid, InitSounds.MAID_IDLE.get(), 0.3f);
    }

    @Override
    public @Nonnull List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(@Nonnull EntityMaid maid) {
        MaidUseHandCrank.LOGGER.debug("为女仆创建手摇曲柄任务");
        UseHandCrank crankHandleTask = new UseHandCrank(0.5f);
        return Lists.newArrayList(Pair.of(Config.PRIORITY.get(), crankHandleTask));
    }

    @Override
    public boolean enableLookAndRandomWalk(@Nonnull EntityMaid maid) {
        return Config.RANDOM_WALK.get();
    }

    @Override
    public boolean workPointTask(@Nonnull EntityMaid maid) {
        return true;
    }

//    @Override
//    public float searchRadius(@Nonnull EntityMaid maid) {
//        return Config.SEARCH_RADIUS.get();
//    }
}
