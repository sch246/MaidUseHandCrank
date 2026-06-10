package com.sch246.muhc.compat;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * Sable 兼容层。
 * <p>
 * 所有依赖 Sable API 的代码集中在静态内部类 {@code Internal} 中。
 * Java 的静态内部类是延迟加载的 —— {@code SableCompat$Internal.class}
 * 只有在外层方法真正执行到 {@code Internal.xxx()} 时才会被 JVM 加载。
 * 由于外层有 {@code if (IS_SABLE_LOADED)} 守卫，Sable 不存在时
 * {@code Internal} 永远不会被触碰，从而避免 {@link NoClassDefFoundError}。
 */
public final class SableCompat {
    private SableCompat() {}

    private static final boolean IS_SABLE_LOADED = ModList.get().isLoaded("sable");

    // ── Public API（签名只使用原版类型） ──────────────────────────

    public static Stream<PoiRecord> getSableCrank(ServerLevel level, BlockPos maidPos, double searchRadiusSqr) {
        if (IS_SABLE_LOADED) {
            return Internal.getSableCrank(level, maidPos, searchRadiusSqr);
        }
        return Stream.empty();
    }

    public static double getDistanceSqr(ServerLevel level, BlockPos pos, @Nonnull Vec3 target) {
        if (IS_SABLE_LOADED) {
            return Internal.getDistanceSqr(level, pos, target);
        }
        return pos.distToCenterSqr(target);
    }

    public static BlockPos toGlobalBlockPos(ServerLevel level, BlockPos pos) {
        if (IS_SABLE_LOADED) {
            return Internal.toGlobalBlockPos(level, pos);
        }
        return pos;
    }

    // ════════════════════════════════════════════════════════════════
    //  所有引用 Sable 类型的代码集中在此类中
    //  使用完全限定名，不在文件顶部 import Sable 任何类
    // ════════════════════════════════════════════════════════════════

    private static final class Internal {

        static Stream<PoiRecord> getSableCrank(ServerLevel level, BlockPos maidPos, double searchRadiusSqr) {
            dev.ryanhcode.sable.api.sublevel.SubLevelContainer container =
                    dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
            if (container == null) return Stream.empty();

            double maxDist = Math.sqrt(searchRadiusSqr) + 100;
            double maxDistSqr = maxDist * maxDist;

            return container.getAllSubLevels().stream()
                    .filter(sub -> sub instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel)
                    .flatMap(subLevel -> {
                        double distSq = subLevel.logicalPose().position()
                                .distanceSquared(maidPos.getX(), maidPos.getY(), maidPos.getZ());
                        if (distSq > maxDistSqr) return Stream.empty();

                        dev.ryanhcode.sable.sublevel.plot.LevelPlot plot = subLevel.getPlot();
                        if (plot == null) return Stream.empty();

                        dev.ryanhcode.sable.companion.math.BoundingBox3ic box = plot.getBoundingBox();
                        if (box == null) return Stream.empty();

                        return ChunkPos.rangeClosed(
                                        new ChunkPos(box.minX() >> 4, box.minZ() >> 4),
                                        new ChunkPos(box.maxX() >> 4, box.maxZ() >> 4))
                                .flatMap(chunkPos -> level.getPoiManager().getInChunk(
                                        type -> type.value().equals(
                                                com.sch246.muhc.create.InitPoi.HAND_CRANK.get()),
                                        chunkPos, PoiManager.Occupancy.ANY));
                    });
        }

        static double getDistanceSqr(ServerLevel level, BlockPos pos, @Nonnull Vec3 target) {
            dev.ryanhcode.sable.sublevel.SubLevel subLevel =
                    dev.ryanhcode.sable.Sable.HELPER.getContaining(level, pos);
            if (subLevel != null) {
                org.joml.Vector3dc globalPos = subLevel.logicalPose()
                        .transformPosition(dev.ryanhcode.sable.companion.math.JOMLConversion.atCenterOf(pos));
                return globalPos.distanceSquared(target.x, target.y, target.z);
            }
            return pos.distToCenterSqr(target);
        }

        static BlockPos toGlobalBlockPos(ServerLevel level, BlockPos pos) {
            dev.ryanhcode.sable.sublevel.SubLevel subLevel =
                    dev.ryanhcode.sable.Sable.HELPER.getContaining(level, pos);
            if (subLevel != null) {
                org.joml.Vector3dc globalPos = subLevel.logicalPose()
                        .transformPosition(dev.ryanhcode.sable.companion.math.JOMLConversion.atCenterOf(pos));
                return BlockPos.containing(globalPos.x(), globalPos.y(), globalPos.z());
            }
            return pos;
        }
    }
}
