package com.sch246.muhc.maid.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.sch246.muhc.Config;
import com.sch246.muhc.MaidUseHandCrank;
import com.sch246.muhc.create.InitPoi;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.sounds.SoundEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class HandCrankTask implements IMaidTask {
    private static  final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MaidUseHandCrank.MODID, "hand_crank_task");

    private static ItemStack ICON = null;

    @Override
    public @NotNull ResourceLocation getUid() {
        return UID;
    }

    @Override
    public @NotNull ItemStack getIcon() {
         //延迟加载图标，确保 Create Mod 的方块已经注册完毕
        if (ICON == null) {
            // 获取 HandCrankBlock 的物品形式，并创建 ItemStack
            ICON = AllBlocks.HAND_CRANK.asStack();
        }
        return ICON;
    }

    @Override
    @Nullable
    public SoundEvent getAmbientSound(@NotNull EntityMaid maid) {
        return SoundUtil.environmentSound(maid, InitSounds.MAID_IDLE.get(), 0.3f);
    }

    @Override
    public @NotNull List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(@NotNull EntityMaid maid) {
        MaidUseHandCrank.LOGGER.debug("为女仆创建手摇曲柄任务");
        UseHandCrank crankHandleTask = new UseHandCrank(0.5f, Config.REACH_RADIUS.get());
        return Lists.newArrayList(Pair.of(Config.PRIORITY.get(), crankHandleTask));
    }

    @Override
    public boolean enableLookAndRandomWalk(@NotNull EntityMaid maid) {
        return false;
    }

    @Override
    public boolean workPointTask(@NotNull EntityMaid maid) {
        return true;
    }

    @Override
    public float searchRadius(@NotNull EntityMaid maid) {
        return Config.SEARCH_RADIUS.get();
    }
}

class UseHandCrank extends MaidCheckRateTask {
    private static final int MAX_DELAY_TIME = 60;


    private final float speed;
    private final int closeEnoughDist;
    private int operationTimer = 0;
    private int bubbleTimer = 0;
    private final RandomSource random = RandomSource.create();
    private static final String[] CHAT_BUBBLES = new String[]{
            "message." + MaidUseHandCrank.MODID + ".working.1",
            "message." + MaidUseHandCrank.MODID + ".working.2",
            "message." + MaidUseHandCrank.MODID + ".working.3",
            "message." + MaidUseHandCrank.MODID + ".working.4",
            "message." + MaidUseHandCrank.MODID + ".working.5",
            "message." + MaidUseHandCrank.MODID + ".master.1",
            "message." + MaidUseHandCrank.MODID + ".master.2",
            "message." + MaidUseHandCrank.MODID + ".master.3",
            "message." + MaidUseHandCrank.MODID + ".master.4",
            "message." + MaidUseHandCrank.MODID + ".master.5",
            "message." + MaidUseHandCrank.MODID + ".toxic_positivity.1",
            "message." + MaidUseHandCrank.MODID + ".toxic_positivity.2",
            "message." + MaidUseHandCrank.MODID + ".toxic_positivity.3",
            "message." + MaidUseHandCrank.MODID + ".toxic_positivity.4",
            "message." + MaidUseHandCrank.MODID + ".toxic_positivity.5",
    };

    public UseHandCrank(float speed, int closeEnoughDist) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_ABSENT));
        this.speed = speed;
        this.closeEnoughDist = closeEnoughDist;
        this.setMaxCheckRate(MAX_DELAY_TIME);
    }

    protected boolean canReachPos(EntityMaid maid, BlockPos pos) {
        return pos.distToCenterSqr(maid.position()) < Math.pow(this.closeEnoughDist, 2);
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull EntityMaid maid) {
        // 这段必定在start前检查
        if (!super.checkExtraStartConditions(level, maid)) {
            return false;
        }

        // 女仆无法移动不会导致任务无法启动，坐着开始很正常
//        if (!maid.canBrainMoving()) {
//            MaidUseHandCrank.LOGGER.info("女仆无法移动，任务无法启动");
//            return false;
//        }

        BlockPos crankPos = findCrankHandle(level, maid);
        MaidUseHandCrank.LOGGER.debug("寻找到的手摇曲柄位置: {}", crankPos);
        if (crankPos == null || !maid.isWithinRestriction(crankPos)) {
            return false;
        }

        if (!canReachPos(maid, crankPos)) {
            MaidUseHandCrank.LOGGER.debug("发现手摇曲柄，但距离太远，开始移动...");
            BehaviorUtils.setWalkAndLookTargetMemories(maid, crankPos, speed, closeEnoughDist);
            this.setNextCheckTickCount(5);
            return false;
        }

        MaidUseHandCrank.LOGGER.debug("女仆已到达手摇曲柄附近，设置目标，准备开始操作");
        maid.getBrain().setMemory(InitEntities.TARGET_POS.get(), new BlockPosTracker(crankPos));
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, EntityMaid maid, long gameTime) {
        MaidUseHandCrank.LOGGER.debug("手摇曲柄任务：开始执行");
        maid.getBrain().getMemory(InitEntities.TARGET_POS.get()).ifPresent(target -> {
            BlockPos crankPos = target.currentBlockPosition();
            // 让女仆面向曲柄
            MaidUseHandCrank.LOGGER.debug("改变女仆的朝向...");
            maid.getLookControl().setLookAt(crankPos.getX() + 0.5, crankPos.getY() + 0.5, crankPos.getZ() + 0.5);
        });
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, EntityMaid maid, long gameTime) {
        // 这段必定在tick前检查
        MaidUseHandCrank.LOGGER.debug("女仆已在操作状态，重新验证目标...");

        // 女仆无法移动不会导致任务结束，坐下开始任务不是很正常吗
//        if (!maid.canBrainMoving()) {
//            MaidUseHandCrank.LOGGER.info("女仆无法移动，任务结束");
//            return false;
//        }

        Optional<PositionTracker> posTracker = maid.getBrain().getMemory(InitEntities.TARGET_POS.get());
        Optional<BlockPos> nullablePos = posTracker.map(PositionTracker::currentBlockPosition);
        if (nullablePos.isEmpty()) {
            MaidUseHandCrank.LOGGER.debug("位置已丢失，停止操作");
            return false;
        }
        BlockPos pos = nullablePos.get();

        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity)){
            MaidUseHandCrank.LOGGER.debug("方块验证失败，停止操作");
            return false;
        }

        if (!canReachPos(maid, pos)) {
            MaidUseHandCrank.LOGGER.debug("距离太远，停止操作");
            return false;
        }

        return true;
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected void tick(@NotNull ServerLevel level, EntityMaid maid, long gameTime) {
        MaidUseHandCrank.LOGGER.debug("手摇曲柄任务 tick - 处于操作状态");

        maid.getBrain().getMemory(InitEntities.TARGET_POS.get()).ifPresent(target -> {
            BlockPos crankPos = target.currentBlockPosition();
            // 开始操作状态
            operationTimer++;
            maid.getLookControl().setLookAt(crankPos.getX() + 0.5, crankPos.getY() + 0.5, crankPos.getZ() + 0.5);
            if (operationTimer >= Config.OPERATION_INTERVAL.get()) {
                MaidUseHandCrank.LOGGER.debug("执行一次曲柄操作");
                operateCrankHandle(level, maid, crankPos);
                operationTimer = 0;
            }

            bubbleTimer++;
            if (bubbleTimer >= Config.BUBBLE_INTERVAL.get()){
                int i = random.nextInt(CHAT_BUBBLES.length);
                String messageKey = CHAT_BUBBLES[i];
                Component component = messageKey.contains(".master.")
                        ? Component.translatable(
                                messageKey,
                                (maid.getOwner() != null)
                                        ? maid.getOwner().getName()
                                        : "Master")
                        : Component.translatable(messageKey);
                maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(component));
                bubbleTimer=0;
            }
        });

    }

    @Override
    protected void stop(@NotNull ServerLevel level, EntityMaid maid, long gameTime) {
        MaidUseHandCrank.LOGGER.debug("手摇曲柄任务停止，清除相关记忆");
        operationTimer = 0;
        maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }


    @javax.annotation.Nullable
    private BlockPos findCrankHandle(ServerLevel level, EntityMaid maid) {
        BlockPos blockPos = maid.getBrainSearchPos();
        PoiManager poiManager = level.getPoiManager();
        int range = (int) maid.searchRadius();
        MaidUseHandCrank.LOGGER.debug("开始在女仆周围 {} 格内搜索手摇曲柄POI...", range);

        // 使用注册的POI类型查找手摇曲柄
        BlockPos result = poiManager.getInRange(
                        type -> type.value().equals(InitPoi.HAND_CRANK.get()),
                        blockPos,
                        range,
                        PoiManager.Occupancy.ANY
                )
                .map(PoiRecord::getPos)
                .filter(pos -> level.getBlockEntity(pos) instanceof HandCrankBlockEntity)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(maid.blockPosition())))
                .orElse(null);
        MaidUseHandCrank.LOGGER.debug("手摇曲柄搜索完成，结果: {}", result);
        return result;
    }

    private void operateCrankHandle(ServerLevel level, EntityMaid maid, BlockPos pos) {
        MaidUseHandCrank.LOGGER.debug("女仆 {} 正在操作位于 {} 的手摇曲柄", maid.getName().getString(), pos);
        BlockState state = level.getBlockState(pos);

        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank)) {
            MaidUseHandCrank.LOGGER.debug("未找到手摇曲柄的方块实体");
            return;
        }

        handCrank.turn(false); // false表示不是潜行操作
//
//        // 播放操作音效
//        level.playSound(null, crankPos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.0F);

        // 女仆挥手动作
        maid.swing(InteractionHand.MAIN_HAND);

    }
}