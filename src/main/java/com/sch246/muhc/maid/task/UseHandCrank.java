package com.sch246.muhc.maid.task;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import com.sch246.muhc.Config;
import com.sch246.muhc.MaidUseHandCrank;
import com.sch246.muhc.create.InitPoi;
import com.sch246.muhc.util.DynamicLangKeys;
import com.sch246.muhc.util.IUniPosOwner;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Optional;

public class UseHandCrank extends MaidCheckRateTask implements IUniPosOwner {
    private static final int MAX_DELAY_TIME = 60;


    private final float speed;
    private int operationTimer = 0;
    private int bubbleTimer = 0;
    private final RandomSource random = RandomSource.create();
    private BlockPos crankPos;
    private boolean shouldTurnBackwards = false;

    public UseHandCrank(float speed) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_ABSENT));
        this.speed = speed;
        this.setMaxCheckRate(MAX_DELAY_TIME);
        this.bubbleTimer = getRandomBubbleTimer();
    }

    protected boolean outOfRange(EntityMaid maid, BlockPos pos) {
        return pos.distSqr(maid.blockPosition()) > Math.pow(Config.REACH_RADIUS.get(), 2);
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

        // 每次运行都会重新检查最近的手摇曲柄
        BlockPos pos = findCrankHandle(level, maid);
        if (pos == null) {
            if (crankPos != null) {
                // 存储的pos不可达，释放
                unLock(level, crankPos);
            }
            return false;
        }
        MaidUseHandCrank.LOGGER.debug("寻找到的手摇曲柄位置: {}", pos);

        // 若方块不是自己独占的
        if (crankPos != pos) {
            if (crankPos != null) {
                // 确保只持有一个锁
                unLock(level, crankPos);
            }
            crankPos = pos.immutable();
            if (Config.CLAIMS_BEFOREHAND.get()) {
                MaidUseHandCrank.LOGGER.debug("尝试独占方块");
                if (!tryLock(level, crankPos)) {
                    MaidUseHandCrank.LOGGER.debug("独占方块失败");
                    crankPos = null;
                    return false;
                }
                MaidUseHandCrank.LOGGER.debug("独占方块成功");
            }
        } else {
            MaidUseHandCrank.LOGGER.debug("方块本是独占的");
        }

        if (outOfRange(maid, crankPos)) {
            MaidUseHandCrank.LOGGER.debug("发现手摇曲柄，但距离太远，开始移动...");
            BehaviorUtils.setWalkAndLookTargetMemories(maid, crankPos, speed, Config.REACH_RADIUS.get());
            this.setNextCheckTickCount(5);
            return false;
        }

        MaidUseHandCrank.LOGGER.debug("找到并锁定了手摇曲柄，任务准备启动");
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull EntityMaid maid, long gameTime) {
        MaidUseHandCrank.LOGGER.debug("手摇曲柄任务：开始执行");
        if (crankPos == null) {
            MaidUseHandCrank.LOGGER.debug("start:坐标不存在");
            return;
        }
        // 让女仆面向曲柄
        MaidUseHandCrank.LOGGER.debug("改变女仆的朝向...");
        maid.getLookControl().setLookAt(crankPos.getX() + 0.5, crankPos.getY() + 0.5, crankPos.getZ() + 0.5);
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull EntityMaid maid, long gameTime) {
        // 这段必定在tick前检查
//        MaidUseHandCrank.LOGGER.debug("女仆已在操作状态，重新验证目标...");

        // 女仆无法移动不会导致任务结束，坐下开始任务不是很正常吗
//        if (!maid.canBrainMoving()) {
//            MaidUseHandCrank.LOGGER.info("女仆无法移动，任务结束");
//            return false;
//        }

        if (crankPos == null) {
            MaidUseHandCrank.LOGGER.debug("位置已丢失，停止操作");
            return false;
        }

        if (!(level.getBlockEntity(crankPos) instanceof HandCrankBlockEntity)) {
            MaidUseHandCrank.LOGGER.debug("方块验证失败，停止操作");
            return false;
        }

        if (outOfRange(maid, crankPos)) {
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
    protected void tick(@NotNull ServerLevel level, @NotNull EntityMaid maid, long gameTime) {
//        MaidUseHandCrank.LOGGER.debug("手摇曲柄任务 tick - 处于操作状态");
        if (crankPos == null) {
            MaidUseHandCrank.LOGGER.debug("tick:坐标不存在");
            return;
        }

        // 开始操作状态
        --operationTimer;
        maid.getLookControl().setLookAt(crankPos.getX() + 0.5, crankPos.getY() + 0.5, crankPos.getZ() + 0.5);
        if (operationTimer <= 0) {
//            MaidUseHandCrank.LOGGER.debug("执行一次曲柄操作");
            operationTimer = Config.OPERATION_INTERVAL.get();
            operateCrankHandle(level, maid, crankPos);
        }


        if (!Config.RANDOM_WALK.get() || !maid.canBrainMoving()) {
            // 如果不允许到处走或者不能移动
            --bubbleTimer;
            if (bubbleTimer <= 0) {
                bubbleTimer = getRandomBubbleTimer();
                String[] chatBubbles = DynamicLangKeys.getChatBubbles();
                int i = random.nextInt(chatBubbles.length);
                Component component = getComponent(maid, chatBubbles[i]);
                maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(component));
            }
        }
    }

    private int getRandomBubbleTimer() {
        int r = Config.BUBBLE_INTERVAL.get();
        return (r / 2) + random.nextInt(r);
    }

    private static @NotNull Component getComponent(EntityMaid maid, String messageKey) {
        Component component;
        Component ownerName = (maid.getOwner() != null)
                ? maid.getOwner().getName()
                : Component.literal("Master");

        if (messageKey.contains(".master2.")) {
            component = Component.translatable(messageKey, ownerName, ownerName);
        } else if (messageKey.contains(".master.")) {
            component = Component.translatable(messageKey, ownerName);
        } else {
            component = Component.translatable(messageKey);
        }
        return component;
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull EntityMaid maid, long gameTime) {
        MaidUseHandCrank.LOGGER.debug("手摇曲柄任务停止，清除相关记忆");
        operationTimer = 0;
        // 释放已声明的位置，释放失败也问题不大，因为任务是弱引用
        if (crankPos != null) {
            unLock(level, crankPos);
            crankPos = null;
        }
        maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }


    @javax.annotation.Nullable
    private BlockPos findCrankHandle(ServerLevel level, EntityMaid maid) {
        BlockPos result;

        int searchRadius = Config.SEARCH_RADIUS.get();
        if (searchRadius != 0) {
            BlockPos blockPos = maid.blockPosition();
            MaidUseHandCrank.LOGGER.debug("default: 开始在 {} 周围 {} 格内搜索手摇曲柄POI...", blockPos, searchRadius);
            result = findCrankHandleDefault(level, maid, blockPos, searchRadius);
        } else {
            int range = (int) maid.getRestrictRadius() + Config.REACH_RADIUS.get();
            BlockPos blockPos = maid.hasRestriction()
                    ? maid.getRestrictCenter()
                    : Optional.ofNullable(maid.getOwner())
                    .map(e -> new BlockPos(e.getBlockX(), e.getBlockY(), e.getBlockZ()))
                    .orElse(maid.blockPosition());
            MaidUseHandCrank.LOGGER.debug("auto: 开始在 {} 周围 {} 格内搜索手摇曲柄POI...", blockPos, range);
            result = findCrankHandleAuto(level, maid, blockPos, range);
        }

        MaidUseHandCrank.LOGGER.debug("手摇曲柄搜索完成，结果: {}", result);
        return result;
    }

    @javax.annotation.Nullable
    private BlockPos findCrankHandleDefault(ServerLevel level, EntityMaid maid, BlockPos blockPos, int range) {
        PoiManager poiManager = level.getPoiManager();

        // 使用注册的POI类型查找手摇曲柄
        return poiManager.getInRange(
                        type -> type.value().equals(InitPoi.HAND_CRANK.get()),
                        blockPos,
                        range,
                        PoiManager.Occupancy.ANY
                )
                .map(PoiRecord::getPos)
                .filter(pos -> level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank
                        //inUse有点用，但不多，因为手柄在不使用的时候是没有这个值的
                        && handCrank.inUse == 0
                        && canLock(level, pos.immutable()))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(maid.blockPosition())))
                .orElse(null);
    }

    @javax.annotation.Nullable
    private BlockPos findCrankHandleAuto(ServerLevel level, EntityMaid maid, BlockPos blockPos, int range) {
        PoiManager poiManager = level.getPoiManager();

        // 使用注册的POI类型查找手摇曲柄
        return poiManager.getInRange(
                        type -> type.value().equals(InitPoi.HAND_CRANK.get()),
                        blockPos,
                        range,
                        PoiManager.Occupancy.ANY
                )
                .map(PoiRecord::getPos)
                .filter(pos -> level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank
                        //inUse有点用，但不多，因为手柄在不使用的时候是没有这个值的
                        && handCrank.inUse == 0
                        // 当 searchRadius 为 0 时始终通过，否则进行判断
                        && (pos.closerThan(blockPos, (int) maid.getRestrictRadius()) || !outOfRange(maid, pos))
                        && canLock(level, pos.immutable()))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(maid.blockPosition())))
                .orElse(null);
    }

    private void operateCrankHandle(ServerLevel level, EntityMaid maid, BlockPos pos) {
//        MaidUseHandCrank.LOGGER.debug("女仆 {} 正在操作位于 {} 的手摇曲柄", maid.getName().getString(), pos);

        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank)) {
            MaidUseHandCrank.LOGGER.debug("未找到手摇曲柄的方块实体");
            return;
        }


        if (handCrank.getSpeed() != 0.0F) {
            // 如果检测到旋转，那么会更新旋转方向
            shouldTurnBackwards = shouldTurnBackwardsToContinue(handCrank);
        }

        MaidUseHandCrank.LOGGER.debug("{} 当前方向 {}", maid.getName().getString(), shouldTurnBackwards ? "逆向" : "正向");

        handCrank.turn(shouldTurnBackwards);
        maid.swing(InteractionHand.MAIN_HAND);
    }

    /**
     * 根据手摇曲柄的当前状态，计算出为了让其继续旋转应该传入 turn() 方法的布尔值。
     * <p>
     * 这是因为 Create Mod 的手摇曲柄的旋转方向 (getSpeed()的正负)
     * 取决于它的朝向 (FACING) 和内部的 backwards 状态。
     * 这个方法逆向了这个逻辑，以确保女仆总是顺着当前方向转动。
     *
     * @param handCrank The HandCrankBlockEntity instance.
     * @return true 如果应该调用 turn(true)，否则 false.
     */
    private boolean shouldTurnBackwardsToContinue(HandCrankBlockEntity handCrank) {
        return switch (handCrank.getBlockState().getValue(HandCrankBlock.FACING)) {
            case UP, EAST, SOUTH ->
                // 对于这些方向, speed < 0 意味着 backwards=true, 所以要继续就得传入true
                    handCrank.getSpeed() < 0;
            default -> // NORTH, WEST, DOWN
                // 对于这些方向, speed > 0 意味着 backwards=true, 所以要继续就得传入true
                    handCrank.getSpeed() > 0;
        };
    }
}