package com.sch246.muhc.util;

import com.google.common.collect.MapMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Optional;

/**
 * 一个对象接口，该对象可声称对特定维度中的 BlockPos 拥有唯一所有权。
 * 这个系统被称为 "uniPos"，可确保（Level、BlockPos）对在同一时间只能被一个对象 "拥有"。
 * 一次只能被一个对象 "拥有"。
 *
 * <p><b> 重要事项：</b>要使自动内存管理（弱键）正常工作、
 * 实现类必须持有对其用于锁定的 BlockPos 实例的强引用。
 * 的强引用。通常，这意味着要将 BlockPos 作为最终字段存储在类中。
 * An interface for objects that can claim unique ownership of a BlockPos in a specific Level.
 * This system, called "uniPos", ensures that a (Level, BlockPos) pair can only be "owned"
 * by one object at a time.
 *
 * <p><b>IMPORTANT:</b> For the automatic memory management (weak keys) to work correctly,
 * the implementing class MUST hold a strong reference to the exact BlockPos instance it uses
 * to lock. Typically, this means storing the BlockPos as a final field in the class.</p>
 */
public interface IUniPosOwner {

    /**
     * 检查特定位置当前是否被 *任意* 对象拥有。
     * Checks if a specific position is currently owned by *any* object.
     *
     * @param level 维度 The level (dimension) of the position.
     * @param pos   坐标 The position to lock.
     * @return true if the position has an owner, false otherwise.
     */
    default boolean isLocked(Level level, BlockPos pos) {
        return UniPosManager.INSTANCE.getOwner(level, pos) != null;
    }

    /**
     * 获取位置的当前所有者（如果存在的话）。
     * Gets the current owner of a position, if one exists.
     *
     * @param level 维度 The level (dimension) of the position.
     * @param pos   坐标 The position to lock.
     * @return An Optional containing the owner, or an empty Optional if the position is not owned.
     */
    default Optional<IUniPosOwner> getOwner(Level level, BlockPos pos) {
        return Optional.ofNullable(UniPosManager.INSTANCE.getOwner(level, pos));
    }

    /**
     * 检查该对象是否可以锁定指定位置。
     * 如果该位置当前未锁定，或者该对象已经是所有者，则该值为 true。
     * Checks if this object can lock the specified position.
     * This is true if the position is currently unlocked, OR if this object is already the owner.
     *
     * @param level 维度 The level (dimension) of the position.
     * @param pos   坐标 The position to lock.
     * @return true if this object can claim ownership.
     */
    default boolean canLock(Level level, BlockPos pos) {
        IUniPosOwner owner = UniPosManager.INSTANCE.getOwner(level, pos);
        return owner == null || owner == this;
    }

    /**
     * 尝试对该对象的指定位置声明所有权。
     * 此操作是原子操作。
     * Attempts to claim ownership of the specified position for this object.
     * This operation is atomic.
     *
     * @param level 维度 The level (dimension) of the position.
     * @param pos   坐标 The position to lock.
     * @return true if ownership was successfully claimed or was already held by this object,
     * false if the position is owned by another object.
     */
    default boolean tryLock(Level level, BlockPos pos) {
        return UniPosManager.INSTANCE.tryLock(level, pos, this);
    }

    /**
     * 释放指定位置的所有权。
     * 只有当该对象是当前所有者时，此操作才会成功。
     * Releases ownership of the specified position.
     * This will only succeed if this object is the current owner.
     *
     * @param level 位置的级别（维度） The level (dimension) of the position.
     * @param pos   要解锁的位置 The position to unlock.
     * @return true if the value was replaced.
     */
    default boolean unLock(Level level, BlockPos pos) {
        return UniPosManager.INSTANCE.unLock(level, pos, this);
    }
}

/**
 * 管理 IUniPosOwner 系统的单例存储。
 * 该类是后台实现，大多数类都不应直接使用。
 * 确保每个维度的 BlockPos 都有唯一的所有者。
 * 它为 BlockPos 使用弱键，以防止在不再引用某个位置时出现内存泄漏。
 * Manages the singleton storage for the IUniPosOwner system.
 * This class is the backend implementation and should not be used directly by most classes.
 * It ensures that for each dimension, a given BlockPos has a unique owner.
 * It uses weak keys for BlockPos to prevent memory leaks when a pos is no longer referenced.
 */
final class UniPosManager {

    public static final UniPosManager INSTANCE = new UniPosManager();

    // 顶层地图：Dimension -> Map of Positions.
    // 这是线程安全的。
    // The top-level map: Dimension -> Map of Positions.
    // This is thread-safe.
    private final ConcurrentMap<ResourceKey<Level>, ConcurrentMap<BlockPos, IUniPosOwner>> dimensionLocks;

    private UniPosManager() {
        this.dimensionLocks = new ConcurrentHashMap<>();
    }

    /**
     * 获取或创建特定维度的映射。
     * 返回的映射使用 BlockPos 实例的弱键。
     * Gets or creates the map for a specific dimension.
     * The returned map uses weak keys for BlockPos instances.
     */
    private ConcurrentMap<BlockPos, IUniPosOwner> getDimensionMap(Level level) {
        return dimensionLocks.computeIfAbsent(level.dimension(), k ->
                new MapMaker()
                        .weakKeys() // Use weak references for keys (BlockPos)
                        .makeMap()
        );
    }

    @Nullable
    public IUniPosOwner getOwner(Level level, BlockPos pos) {
        return getDimensionMap(level).get(pos);
    }

    /**
     * 尝试为指定位置设置所有者。
     * 如果锁已被同一所有者获得或持有
     * Attempts to set the owner for a given position.
     *
     * @return true if the lock was acquired or already held by the same owner, false otherwise.
     */
    public boolean tryLock(Level level, BlockPos pos, IUniPosOwner owner) {
        // 所有者必须持有对 pos 实例的强引用，弱密钥才能正常工作。
        // The owner must hold a strong reference to the pos instance for the weak key to work.
        IUniPosOwner currentOwner = getDimensionMap(level).putIfAbsent(pos, owner);
        return currentOwner == null || currentOwner == owner;
    }

    /**
     * 解锁一个位置，但前提是所提供的所有者是当前所有者。
     * Unlocks a position, but only if the provided owner is the current owner.
     *
     * @return true if the value was replaced.
     */
    public boolean unLock(Level level, BlockPos pos, IUniPosOwner owner) {
        // 仅当键映射到指定的所有者时，才以原子方式删除。
        // Atomically removes only if the key is mapped to the specified owner.
        return getDimensionMap(level).remove(pos, owner);
    }
}