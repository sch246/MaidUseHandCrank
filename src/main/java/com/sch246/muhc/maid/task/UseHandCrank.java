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
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UseHandCrank extends MaidCheckRateTask {
    private static final int MAX_DELAY_TIME = 60;


    private final float speed;
    private final int closeEnoughDist;
    private int operationTimer = 0;
    private int bubbleTimer = 0;
    private final RandomSource random = RandomSource.create();

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
                String[] chatBubbles = DynamicLangKeys.getChatBubbles();
                int i = random.nextInt(chatBubbles.length);
                Component component = getComponent(maid, chatBubbles[i]);
                maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(component));
                bubbleTimer=0;
            }
        });

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
                .filter(pos -> level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank
                        && handCrank.inUse == 0)
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


    private static final Map<BlockPos, WeakReference<UseHandCrank>> REGISTRY = new ConcurrentHashMap<>();
    /**
     * 尝试为一个任务声明（锁定）一个曲柄位置。
     * @param pos  目标方块位置
     * @param task 尝试声明该位置的任务实例
     * @return 如果声明成功，返回 true；如果已被其他活动任务占用，返回 false。
     */
    public static boolean tryClaim(BlockPos pos, UseHandCrank task) {
        // 使用 compute 方法来保证操作的原子性
        return REGISTRY.compute(pos.immutable(), (key, existingRef) -> {
            // 检查是否已有锁
            if (existingRef != null) {
                UseHandCrank existingTask = existingRef.get();
                // 如果旧的锁还活着，并且不是当前这个任务（防止重入），则声明失败
                if (existingTask != null && existingTask != task) {
                    return existingRef; // 返回旧的引用，表示不作修改
                }
            }
            // 如果没有锁，或者旧的锁已经失效(被GC)，则设置新的锁
            return new WeakReference<>(task);
        }).get() == task; // 确认存入 Map 的就是我们自己的任务
    }

    /**
     * 释放一个任务对曲柄位置的声明。
     * 为了安全，只有持有锁的任务自己才能释放它。
     * @param pos  目标方块位置
     * @param task 尝试释放该位置的任务实例
     */
    public static void release(BlockPos pos, UseHandCrank task) {
        // 同样使用 compute 原子操作
        REGISTRY.computeIfPresent(pos.immutable(), (key, existingRef) -> {
            UseHandCrank existingTask = existingRef.get();
            // 只有当 Map 中的任务就是当前任务时，才进行移除（释放锁）
            if (existingTask == task) {
                return null; // 返回 null 会导致 ConcurrentHashMap 移除该键值对
            }
            // 否则，保持不变
            return existingRef;
        });
    }

    /**
     * 检查一个位置当前是否被一个活动的任务占用。
     * @param pos 要检查的位置
     * @return 如果被占用，返回 true
     */
    public static boolean isClaimed(BlockPos pos) {
        WeakReference<UseHandCrank> ref = REGISTRY.get(pos);
        if (ref == null) {
            return false;
        }
        // 如果引用存在，但引用的对象已被GC，说明锁已失效
        if (ref.get() == null) {
            // 可以顺便清理一下这个无效的键
            REGISTRY.remove(pos, ref);
            return false;
        }
        // 引用和对象都存在，说明被占用
        return true;
    }

}