package com.sch246.muhc.util;


import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.sch246.muhc.MaidUseHandCrank;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class HandCrankHelper {
    private static final int SEARCH_RADIUS = 16;

    public static boolean canUseHandCrank(EntityMaid maid) {
        MaidUseHandCrank.LOGGER.info("检查女仆是否可以使用手摇曲柄 - 女仆: " + maid.getName().getString());
        MaidUseHandCrank.LOGGER.info("女仆位置: " + maid.blockPosition());
        MaidUseHandCrank.LOGGER.info("女仆所在维度: " + maid.level().dimension().location());

        // 在这里添加条件检查
        // 比如检查女仆是否有权限使用手摇曲柄等
        boolean canUse = true; // 暂时返回true
        MaidUseHandCrank.LOGGER.info("女仆可以使用手摇曲柄: " + canUse);
        return canUse;
    }

    public static Optional<HandCrankBlockEntity> findNearestHandCrank(EntityMaid maid) {
        MaidUseHandCrank.LOGGER.info("=== 开始寻找最近的手摇曲柄 ===");
        ServerLevel level = (ServerLevel) maid.level();
        BlockPos maidPos = maid.blockPosition();
        HandCrankBlockEntity nearestHandCrank = null;
        double nearestDistance = Double.MAX_VALUE;

        MaidUseHandCrank.LOGGER.info("女仆位置: " + maidPos + ", 搜索半径: " + SEARCH_RADIUS);

        int totalBlocks = 0;
        int handCrankBlocks = 0;
        int availableHandCranks = 0;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos pos = maidPos.offset(x, y, z);
                    totalBlocks++;

                    if (!maid.isWithinRestriction(pos)) {
                        continue;
                    }

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof HandCrankBlockEntity handCrank) {
                        handCrankBlocks++;
                        MaidUseHandCrank.LOGGER.info("找到手摇曲柄在位置: " + pos);

                        if (isHandCrankAvailable(handCrank)) {
                            availableHandCranks++;
                            double distance = maidPos.distSqr(pos);
                            MaidUseHandCrank.LOGGER.info("可用的手摇曲柄，距离: " + Math.sqrt(distance));

                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                nearestHandCrank = handCrank;
                                MaidUseHandCrank.LOGGER.info("更新最近的手摇曲柄: " + pos);
                            }
                        } else {
                            MaidUseHandCrank.LOGGER.info("手摇曲柄不可用: " + pos);
                        }
                    }
                }
            }
        }

        MaidUseHandCrank.LOGGER.info("搜索完成 - 总方块数: " + totalBlocks +
                ", 手摇曲柄数: " + handCrankBlocks +
                ", 可用手摇曲柄数: " + availableHandCranks);

        if (nearestHandCrank != null) {
            MaidUseHandCrank.LOGGER.info("找到最近的手摇曲柄: " + nearestHandCrank.getBlockPos() +
                    ", 距离: " + Math.sqrt(nearestDistance));
        } else {
            MaidUseHandCrank.LOGGER.info("未找到可用的手摇曲柄");
        }

        return Optional.ofNullable(nearestHandCrank);
    }

    private static boolean isHandCrankAvailable(HandCrankBlockEntity handCrank) {
        boolean available = !handCrank.isRemoved();
        MaidUseHandCrank.LOGGER.info("检查手摇曲柄可用性: " + available + " (位置: " + handCrank.getBlockPos() + ")");
        return available;
    }
}