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
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public class UseHandCrank extends MaidCheckRateTask implements IUniPosOwner {
    private static final int MAX_DELAY_TIME = 60;

    private final float speed;
    private int operationTimer = 0;
    private int bubbleTimer = 0;
    private final RandomSource random = RandomSource.create();
    private BlockPos crankPos;
    private boolean offHand = false;

    public UseHandCrank(float speed) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_ABSENT));
        this.speed = speed;
        this.setMaxCheckRate(MAX_DELAY_TIME);
        this.bubbleTimer = getRandomBubbleTimer();
    }

    protected boolean outOfReachRange(EntityMaid maid, BlockPos pos) {
        int reachRadius = Config.REACH_RADIUS.get();
        return pos.distToCenterSqr(maid.position()) > reachRadius * reachRadius;
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
                crankPos = null;
            }
            return false;
        }
        MaidUseHandCrank.LOGGER.debug("寻找到的手摇曲柄位置: {}", pos);

        // 若方块不是自己独占的
        if (crankPos != pos && !pos.equals(crankPos)) {
            if (crankPos != null) {
                // 确保只持有一个锁
                unLock(level, crankPos);
            }
            crankPos = pos;
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

        if (outOfReachRange(maid, crankPos)) {
            MaidUseHandCrank.LOGGER.debug("发现手摇曲柄，但距离太远，开始移动...");
            BehaviorUtils.setWalkAndLookTargetMemories(maid, getFrontPos(level, crankPos), speed, 0);
//            BehaviorUtils.setWalkAndLookTargetMemories(maid, crankPos, speed, Config.REACH_RADIUS.get());
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
     * @param pos   输入的坐标
     * @return frontPos 手摇曲柄前方的坐标，若当前不是手摇曲柄，返回原坐标
     */
    private BlockPos getFrontPos(@Nonnull ServerLevel level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getBlock() instanceof HandCrankBlock) {
            return pos.relative(blockState.getValue(HandCrankBlock.FACING));
        }
        return pos;
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

        // 下班！
        if (maid.getScheduleDetail() != Activity.WORK) {
            return false;
        }

        if (crankPos == null) {
            MaidUseHandCrank.LOGGER.debug("位置已丢失，停止操作");
            return false;
        }

        if (!(level.getBlockEntity(crankPos) instanceof HandCrankBlockEntity)) {
            MaidUseHandCrank.LOGGER.debug("方块验证失败，停止操作");
            return false;
        }

        if (outOfReachRange(maid, crankPos)) {
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

        // 持续续租
        refreshLock(level, crankPos);

        // 开始操作状态
        int maxOperationTime = Config.OPERATION_INTERVAL.get();
        maid.getLookControl().setLookAt(crankPos.getCenter());
        if (operationTimer == 0) {
            // MaidUseHandCrank.LOGGER.debug("执行一次曲柄操作");

            // 尝试重新锁定最近的曲柄
            refreshLockToNearestAvailable(level, maid);

            operateReachableFreeCrankHandle(level, maid);

        } else if (Config.TWO_HANDED_OPERATION.get()
                && operationTimer == maxOperationTime / 2
                && maid.getFavorability() == 384) {
            // 满级好感速度*2
            offHand = !offHand;
            operateReachableFreeCrankHandle(level, maid);
            offHand = !offHand;
        }

        operationTimer = (operationTimer + 1) % maxOperationTime;

        handleChatBubbles(maid);
    }

    private void refreshLockToNearestAvailable(@Nonnull ServerLevel level, @Nonnull EntityMaid maid) {
        BlockPos pos = getNearestReachableCrankPosition(maid, level,
                p -> level.getBlockEntity(p) instanceof HandCrankBlockEntity
                        && canLock(level, p));
        if (pos != null && crankPos != pos && !pos.equals(crankPos) && tryLock(level, pos)) {
            if (crankPos != null) {
                // 确保只持有一个锁
                unLock(level, crankPos);
            }
            crankPos = pos;
        }
    }

//    private boolean shouldOperate(int favorability, int maxOperationTime) {
//        if (favorability >= 64 && favorability < 192) {
//            return operationTimer == maxOperationTime / 2;
//        } else if (favorability >= 192 && favorability < 384) {
//            int third = maxOperationTime / 3;
//            return (operationTimer == third || operationTimer == 2 * third);
//        } else if (favorability == 384) {
//            int quarter = maxOperationTime / 4;
//            return (operationTimer == quarter || operationTimer == 2 * quarter || operationTimer == 3 * quarter);
//        }
//        return false;
//    }

    private void operateReachableFreeCrankHandle(@Nonnull ServerLevel level, @Nonnull EntityMaid maid) {
        int halfTime = Config.OPERATION_DURATION.get() / 2;
        BlockPos pos = getNearestReachableCrankPosition(maid, level,
                p -> level.getBlockEntity(p) instanceof HandCrankBlockEntity handCrank
                        && handCrank.inUse <= halfTime);
        if (pos != null) {
            operateCrankHandle(level, maid, pos);
        }
    }

    private void handleChatBubbles(@Nonnull EntityMaid maid) {
        if (Config.RANDOM_WALK.get() && maid.canBrainMoving()) {
            return; // 允许到处走且能移动时不显示气泡
        }

        if (--bubbleTimer <= 0) {
            bubbleTimer = getRandomBubbleTimer();
            String[] chatBubbles = DynamicLangKeys.getChatBubbles();
            Component component = getComponent(maid, chatBubbles[random.nextInt(chatBubbles.length)]);
            maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(component));
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
            // 车万女仆的实际跟随距离就是比设置距离小，这可能是原 mod 的 bug
            centerRadius = maid.isHomeModeEnable()
                    ? (int) maid.getRestrictRadius() - 1
                    : (int) maid.getRestrictRadius() - 2;
        }
        Vec3 centerPos = maid.hasRestriction()
                ? maid.getRestrictCenter().getCenter()
                : Optional.ofNullable(maid.getOwner())
                .map(e -> new Vec3(e.getX(), e.getY(), e.getZ()))
                .orElse(maid.position());

        int maidRadius = Config.MAID_SEARCH_RADIUS.get();
        if (maidRadius == 0) {
            maidRadius = Config.REACH_RADIUS.get();
        }
        Vec3 maidPos = maid.position();

        MaidUseHandCrank.LOGGER.debug("default: 开始在 {} 周围 {} 格，以及 {} 周围 {} 格内搜索手摇曲柄POI...", centerPos, centerRadius, maidPos, maidRadius);
        BlockPos result = getCrankInDoubleCircleUnion(level, centerPos, centerRadius, maidPos, maidRadius)
                .map(PoiRecord::getPos)
                .filter(pos -> level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank
                        && handCrank.inUse == 0
                        && canLock(level, pos))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(maid.blockPosition())))
                .orElse(null);

        MaidUseHandCrank.LOGGER.debug("手摇曲柄搜索完成，结果: {}", result);
        return result;
    }

    public Stream<PoiRecord> getCrankInDoubleCircleUnion(ServerLevel level,
            Vec3 centerPos, int centerRadius,
            Vec3 maidPos, int maidRadius) {
        // 计算包含两个圆的最小方形区域
        int minX = Math.min((int) centerPos.x() - centerRadius, (int) maidPos.x() - maidRadius);
        int maxX = Math.max((int) centerPos.x() + centerRadius, (int) maidPos.x() + maidRadius);
        int minZ = Math.min((int) centerPos.z() - centerRadius, (int) maidPos.z() - maidRadius);
        int maxZ = Math.max((int) centerPos.z() + centerRadius, (int) maidPos.z() + maidRadius);

        // 计算需要检查的区块范围
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);

        // 预计算距离平方以避免重复计算
        int centerRadiusSqr = centerRadius * centerRadius;
        int maidRadiusSqr = maidRadius * maidRadius;

        PoiManager poiManager = level.getPoiManager();

        return ChunkPos.rangeClosed(new ChunkPos(minChunkX, minChunkZ), new ChunkPos(maxChunkX, maxChunkZ))
                .flatMap(chunkPos -> poiManager.getInChunk(
                        type -> type.value().equals(InitPoi.HAND_CRANK.get()),
                        chunkPos, PoiManager.Occupancy.ANY))
                .filter(poiRecord -> {
                    BlockPos pos = poiRecord.getPos();
                    // 需要能走到正前方，要么能直接碰到
                    return getFrontPos(level, pos).distToCenterSqr(centerPos) <= centerRadiusSqr ||
                            pos.distToCenterSqr(maidPos) <= maidRadiusSqr;
                });
    }

    @javax.annotation.Nullable
    private BlockPos getNearestReachableCrankPosition(EntityMaid maid, ServerLevel level, Predicate<BlockPos> blockPredicate) {
        // 使用注册的POI类型查找手摇曲柄
        return level.getPoiManager()
                .getInRange(
                        type -> type.value().equals(InitPoi.HAND_CRANK.get()),
                        maid.blockPosition(),
                        Config.REACH_RADIUS.get(),
                        PoiManager.Occupancy.ANY
                )
                .map(PoiRecord::getPos)
                .filter(blockPredicate)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(maid.blockPosition())))
                .orElse(null);
    }

    private float lastStress = -1;

    private void operateCrankHandle(ServerLevel level, EntityMaid maid, @Nonnull BlockPos pos) {
        // MaidUseHandCrank.LOGGER.debug("女仆 {} 正在操作位于 {} 的手摇曲柄",
        // maid.getName().getString(), pos);

        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity handCrank)) {
            MaidUseHandCrank.LOGGER.debug("未找到手摇曲柄的方块实体");
            return;
        }

        // ------------ 是否反转 ------------

        // 若位置有物品展示框则反转
        boolean back = false;
        if (Config.ITEM_FRAME_INTERACTION.get()) {
            back = !level.getEntitiesOfClass(ItemFrame.class, new AABB(pos)).isEmpty();
        }

        if (handCrank.getSpeed() != 0.0F) {
            // 如果检测到旋转，那么会更新旋转方向
            back = handCrank.getBlockState()
                    .getValue(HandCrankBlock.FACING).getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? handCrank.getSpeed() < 0
                    : handCrank.getSpeed() > 0;
        }

        MaidUseHandCrank.LOGGER.debug("{} 当前方向 {}", maid.getName().getString(), back ? "逆向" : "正向");

        // ------------ 好感度加成 ------------

        int favorability = maid.getFavorability();

        // 好感度加应力
        float speed = 32;
        float baseStress = ((float) Config.BASE_STRESS.get()) / speed;
        float extraStress = ((float) Config.STRESS_PER_FAVORABILITY.get()) / speed;
        float stress = (baseStress + favorability * extraStress);

        // 好感度增加应力时间
        int tick = Config.OPERATION_DURATION.get();
        if (Config.ENDURANCE_OPERATION.get() && favorability >= 192) {
            tick *= 2;
        }

        if (handCrank instanceof IMaidHandCrank maidHandCrank) {
            maidHandCrank.muhc$setStress(stress, tick);
        }

        // ------------ 原版turn ------------

        boolean update = handCrank.getGeneratedSpeed() == 0.0F
                || back != handCrank.backwards
                || lastStress != stress;
        lastStress = stress;

        handCrank.inUse = tick;
        handCrank.backwards = back;
        if (update && !level.isClientSide) {
            handCrank.updateGeneratedRotation();
        }

        // ------------ 女仆动画 ------------

        maid.swing(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

}