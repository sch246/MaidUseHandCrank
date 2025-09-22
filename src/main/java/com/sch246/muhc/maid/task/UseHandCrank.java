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
import com.sch246.muhc.util.IMaidHandCrank;
import com.sch246.muhc.util.IUniPosOwner;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class UseHandCrank extends MaidCheckRateTask implements IUniPosOwner {
    private static final int MAX_DELAY_TIME = 60;

    private final float speed;
    private int operationTimer = 0;
    private int bubbleTimer = 0;
    private final RandomSource random = RandomSource.create();
    private BlockPos crankPos;
    private boolean back = false;

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
    protected boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull EntityMaid maid) {
        // 这段必定在start前检查
        if (!super.checkExtraStartConditions(level, maid)) {
            return false;
        }

        // 女仆无法移动不会导致任务无法启动，坐着开始很正常
        // if (!maid.canBrainMoving()) {
        // MaidUseHandCrank.LOGGER.info("女仆无法移动，任务无法启动");
        // return false;
        // }

        // 每次运行都会重新检查最近的手摇曲柄
        BlockPos pos = findCrankHandle(maid, level);
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
            BehaviorUtils.setWalkAndLookTargetMemories(maid, getFrontPos(level, crankPos), speed, 1);
            this.setNextCheckTickCount(5);
            return false;
        }

        MaidUseHandCrank.LOGGER.debug("找到并锁定了手摇曲柄，任务准备启动");
        return true;
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
        MaidUseHandCrank.LOGGER.debug("手摇曲柄任务：开始执行");
        if (crankPos == null) {
            MaidUseHandCrank.LOGGER.debug("start:坐标不存在");
            return;
        }
        // 让女仆面向曲柄
        MaidUseHandCrank.LOGGER.debug("改变女仆的朝向...");
        maid.getLookControl().setLookAt(crankPos.getCenter());
    }

    /**
     * 根据手摇曲柄的当前状态，计算其前方方块的坐标
     *
     * @param level 输入的维度
     * @param pos 输入的坐标
     * @return frontPos 手摇曲柄前方的坐标，若当前不是手摇曲柄，返回原坐标
     */
    private BlockPos getFrontPos(@Nonnull ServerLevel level, BlockPos pos) {
        try {
            return pos.relative(level.getBlockState(pos).getValue(HandCrankBlock.FACING));
        } catch (IllegalArgumentException e) {
            return pos;
        }
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
        // 这段必定在tick前检查
        // MaidUseHandCrank.LOGGER.debug("女仆已在操作状态，重新验证目标...");

        // 女仆无法移动不会导致任务结束，坐下开始任务不是很正常吗
        // if (!maid.canBrainMoving()) {
        // MaidUseHandCrank.LOGGER.info("女仆无法移动，任务结束");
        // return false;
        // }

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
    protected void tick(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
        // MaidUseHandCrank.LOGGER.debug("手摇曲柄任务 tick - 处于操作状态");
        if (crankPos == null) {
            MaidUseHandCrank.LOGGER.debug("tick:坐标不存在");
            return;
        }

        // 开始操作状态
        --operationTimer;
        maid.getLookControl().setLookAt(crankPos.getCenter());
        if (operationTimer <= 0) {
            // MaidUseHandCrank.LOGGER.debug("执行一次曲柄操作");
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

    private static @Nonnull Component getComponent(EntityMaid maid, String messageKey) {
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
    protected void stop(@Nonnull ServerLevel level, @Nonnull EntityMaid maid, long gameTime) {
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
    private BlockPos findCrankHandle(EntityMaid maid, ServerLevel level) {

        int centerRadius = Config.CENTER_SEARCH_RADIUS.get();
        if (centerRadius == 0) {
            centerRadius = (int) maid.getRestrictRadius();
        }
        BlockPos centerPos = maid.hasRestriction()
                ? maid.getRestrictCenter()
                : Optional.ofNullable(maid.getOwner())
                        .map(e -> new BlockPos(e.getBlockX(), e.getBlockY(), e.getBlockZ()))
                        .orElse(maid.blockPosition());

        int maidRadius = Config.MAID_SEARCH_RADIUS.get();
        if (maidRadius == 0) {
            maidRadius = Config.REACH_RADIUS.get();
        }
        BlockPos maidPos = maid.blockPosition();

        MaidUseHandCrank.LOGGER.debug("default: 开始在 {} 周围 {} 格，以及 {} 周围 {} 格内搜索手摇曲柄POI...", centerPos, centerRadius, maidPos, maidRadius);
        BlockPos result = getCrankInDoubleCircleUnion(level, centerPos, centerRadius, maidPos, maidRadius)
                .map(PoiRecord::getPos)
                .filter(pos -> level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank
                        && handCrank.inUse == 0
                        && canLock(level, pos.immutable()))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(maid.blockPosition())))
                .orElse(null);

        MaidUseHandCrank.LOGGER.debug("手摇曲柄搜索完成，结果: {}", result);
        return result;
    }

    public Stream<PoiRecord> getCrankInDoubleCircleUnion(ServerLevel level,
            BlockPos center1, int radius1,
            BlockPos center2, int radius2) {
        // 计算包含两个圆的最小方形区域
        int minX = Math.min(center1.getX() - radius1, center2.getX() - radius2);
        int maxX = Math.max(center1.getX() + radius1, center2.getX() + radius2);
        int minZ = Math.min(center1.getZ() - radius1, center2.getZ() - radius2);
        int maxZ = Math.max(center1.getZ() + radius1, center2.getZ() + radius2);

        // 计算需要检查的区块范围
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);

        // 预计算距离平方以避免重复计算
        int radiusSquared1 = radius1 * radius1;
        int radiusSquared2 = radius2 * radius2;

        PoiManager poiManager = level.getPoiManager();

        return ChunkPos.rangeClosed(new ChunkPos(minChunkX, minChunkZ), new ChunkPos(maxChunkX, maxChunkZ))
                .flatMap(chunkPos -> poiManager.getInChunk(
                        type -> type.value().equals(InitPoi.HAND_CRANK.get()),
                        chunkPos, PoiManager.Occupancy.ANY))
                .filter(poiRecord -> {
                    BlockPos pos = poiRecord.getPos();
                    // 检查是否在任一圆内
                    return pos.distSqr(center1) <= radiusSquared1 ||
                            pos.distSqr(center2) <= radiusSquared2;
                });
    }


    private int lastFavorability = -1;

    private void operateCrankHandle(ServerLevel level, EntityMaid maid, BlockPos pos) {
        // MaidUseHandCrank.LOGGER.debug("女仆 {} 正在操作位于 {} 的手摇曲柄",
        // maid.getName().getString(), pos);

        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank)) {
            MaidUseHandCrank.LOGGER.debug("未找到手摇曲柄的方块实体");
            return;
        }

        // ------------ 是否反转 ------------

        if (handCrank.getSpeed() != 0.0F) {
            // 如果检测到旋转，那么会更新旋转方向
            back = handCrank.getBlockState()
                    .getValue(HandCrankBlock.FACING).getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? handCrank.getSpeed() < 0
                    : handCrank.getSpeed() > 0;
        }

        MaidUseHandCrank.LOGGER.debug("{} 当前方向 {}", maid.getName().getString(), back ? "逆向" : "正向");

        // ------------ 好感度加应力 ------------

        int tick = Config.OPERATION_DURATION.get();

        if (handCrank instanceof IMaidHandCrank maidHandCrank) {
            float baseStress = (float)Config.BASE_STRESS.get()/32;
            float extraStress = (float)Config.STREES_PER_FAVORABILITY.get()/32;
            maidHandCrank.muhc$turn(
                    (int) (baseStress + maid.getFavorability() * extraStress),
                    tick
            );
        }

        // ------------ 原版turn ------------

        boolean update = handCrank.getGeneratedSpeed() == 0.0F
                || back != handCrank.backwards
                || lastFavorability != maid.getFavorability();
        lastFavorability = maid.getFavorability();

        handCrank.inUse = tick;
        handCrank.backwards = back;
        if (update && !level.isClientSide) {
            handCrank.updateGeneratedRotation();
        }

        // ------------ 女仆动画 ------------

        maid.swing(InteractionHand.MAIN_HAND);
    }

}