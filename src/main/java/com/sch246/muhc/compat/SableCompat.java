package com.sch246.muhc.compat;

import com.sch246.muhc.create.InitPoi;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;

import java.util.stream.Stream;

public class SableCompat {

    public static Stream<PoiRecord> getSableCrank(ServerLevel level, BlockPos maidPos, double searchRadiusSqr) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return Stream.empty();

        return container.getAllSubLevels().stream()
                .filter(subLevel -> subLevel instanceof ServerSubLevel)
                .flatMap(subLevel -> {
                    double distSq = subLevel.logicalPose().position().distanceSquared(maidPos.getX(), maidPos.getY(), maidPos.getZ());
                    if (distSq > Math.pow(Math.sqrt(searchRadiusSqr) + 100, 2)) {
                        return Stream.empty();
                    }

                    LevelPlot plot = subLevel.getPlot();
                    if (plot == null) return Stream.empty();

                    BoundingBox3ic box = plot.getBoundingBox();
                    if (box == null) return Stream.empty();

                    int minChunkX = box.minX() >> 4;
                    int maxChunkX = box.maxX() >> 4;
                    int minChunkZ = box.minZ() >> 4;
                    int maxChunkZ = box.maxZ() >> 4;

                    return ChunkPos.rangeClosed(new ChunkPos(minChunkX, minChunkZ), new ChunkPos(maxChunkX, maxChunkZ))
                            .flatMap(chunkPos -> level.getPoiManager().getInChunk(
                                    type -> type.value().equals(InitPoi.HAND_CRANK.get()),
                                    chunkPos, PoiManager.Occupancy.ANY));
                });
    }

    public static double getDistanceSqr(ServerLevel level, BlockPos pos, Vec3 target) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel != null) {
            Vector3dc globalPos = subLevel.logicalPose().transformPosition(JOMLConversion.atCenterOf(pos));
            return globalPos.distanceSquared(target.x, target.y, target.z);
        }
        return pos.distToCenterSqr(target);
    }

    public static BlockPos toGlobalBlockPos(ServerLevel level, BlockPos pos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel != null) {
            Vector3dc globalPos = subLevel.logicalPose().transformPosition(JOMLConversion.atCenterOf(pos));
            return BlockPos.containing(globalPos.x(), globalPos.y(), globalPos.z());
        }
        return pos;
    }
}
