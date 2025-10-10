package com.sch246.muhc.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;
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
 * <p><b>重要：</b>由于使用了 weakValues()，实现类必须被其他对象强引用，
 * 否则会被 GC 清理导致锁自动释放。通常这意味着将实现类实例存储在
 * 适当的管理器或容器中。
 * An interface for objects that can claim unique ownership of a BlockPos in a specific Level.
 * This system, called "uniPos", ensures that a (Level, BlockPos) pair can only be "owned"
 * by one object at a time.
 *
 * <p><b>IMPORTANT:</b> Since weakValues() is used, implementing classes must be
 * strongly referenced elsewhere, or they will be GC'd and the lock automatically released.
 * Typically this means storing the implementing instance in an appropriate manager or container.
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
     * 注意：此方法不是原子操作，仅用于快速检查。
     * 实际锁定时应使用 tryLock() 并检查返回值。
     * Checks if this object can lock the specified position.
     * This is true if the position is currently unlocked, OR if this object is already the owner.
     * NOTE: This method is not atomic and should only be used for quick checks.
     * Always use tryLock() and check its return value for actual locking.
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
     * @return true if the lock was successfully removed, false otherwise.
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

    // 顶层地图：Dimension -> Cache of Positions.
    // Cache 的键是 BlockPos 的 long 值，值是 IUniPosOwner 的弱引用。
    // 这允许当 IUniPosOwner 被 GC 时，锁自动从缓存中移除。
    // Top-level map: Dimension -> Cache of Positions.
    // The key to the Cache is the long value of BlockPos, and the value is a weak reference to IUniPosOwner.
    // This allows locks to be automatically removed from the cache when the IUniPosOwner is GC'd.
    private final ConcurrentMap<ResourceKey<Level>, Cache<Long, IUniPosOwner>> dimensionLocks;

    private UniPosManager() {
        this.dimensionLocks = new ConcurrentHashMap<>();
    }

    /**
     * 获取或创建特定维度的 Cache。
     * Cache 使用 BlockPos 的 `long` 值作为键，`IUniPosOwner` 的弱引用作为值。
     * 这确保了当 IUniPosOwner 不再被强引用时，它会自动从 Cache 中移除，释放锁。
     * Gets or creates a dimension-specific Cache.
     * The Cache uses the `long` value of BlockPos as the key and a weak reference to `IUniPosOwner` as the value.
     * This ensures that when IUniPosOwner is no longer strongly referenced, it is automatically removed from the Cache, releasing the lock.
     */
    private Cache<Long, IUniPosOwner> getDimensionCache(Level level) {
        // computeIfAbsent 是原子操作，保证线程安全地获取或创建 Cache
        return dimensionLocks.computeIfAbsent(level.dimension(), k ->
                CacheBuilder.newBuilder()
                        .weakValues() // 键为 long，值（IUniPosOwner）使用弱引用
                        .build()
        );
    }

    /**
     * 获取位置的当前所有者。
     *
     * @param level 维度
     * @param pos   坐标
     * @return 如果存在所有者，则返回所有者对象；否则返回 null。
     */
    @Nullable
    public IUniPosOwner getOwner(Level level, BlockPos pos) {
        return getDimensionCache(level).getIfPresent(pos.asLong());
    }

    /**
     * 尝试为指定位置设置所有者。
     * 如果锁已被同一所有者获得或持有。
     * 此操作是原子操作，避免竞态条件。
     * Attempts to set an owner for the specified location.
     * If the lock is already acquired or held by the same owner.
     * This operation is atomic and avoids competing conditions.
     *
     * @param level 维度
     * @param pos   坐标
     * @param owner 要声明所有权的对象
     * @return true if the lock was acquired or already held by the same owner, false otherwise.
     */
    public boolean tryLock(Level level, BlockPos pos, IUniPosOwner owner) {
        IUniPosOwner existing = getDimensionCache(level).asMap().putIfAbsent(pos.asLong(), owner);
        return existing == null || existing == owner;
    }

    /**
     * 解锁一个位置，但前提是所提供的所有者是当前所有者。
     * 此操作是原子操作。
     * Unlock a location, provided that the supplied owner is the current owner.
     * This operation is atomic.
     *
     * @param level 维度
     * @param pos   要解锁的位置
     * @param owner 尝试解锁的对象
     * @return true if the lock was successfully released by this owner.
     */
    public boolean unLock(Level level, BlockPos pos, IUniPosOwner owner) {
        Cache<Long, IUniPosOwner> cache = dimensionLocks.get(level.dimension());
        if (cache == null) {
            return false; // 该维度没有锁
        }

        // 使用 asMap() 提供的原子操作
        return cache.asMap().remove(pos.asLong(), owner);
    }
}