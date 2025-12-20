package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 方块验证器
 *
 * 用特征判断方块是否是"固体墙"
 * 不硬编码具体方块类型，自动兼容其他mod
 */
public class BlockValidator {

    // ==================== 配置参数 ====================

    /** 碰撞箱最小高度（低于此值不算墙，如地毯、压力板）*/
    private static final double MIN_COLLISION_HEIGHT = 0.2;

    /** 碰撞箱最小体积比例（相对于完整方块）*/
    private static final double MIN_VOLUME_RATIO = 0.1;

    /** 最小硬度（低于此值太脆弱，不算有效墙）*/
    private static final float MIN_HARDNESS = 0.0f;

    // ==================== 主判断方法 ====================

    /**
     * 判断方块是否是固体墙（可以产生碰撞压力）
     */
    public static boolean isSolidWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // 空气
        if (state.isAir()) {
            return false;
        }

        // 检查碰撞箱
        if (!hasValidCollision(level, pos, state)) {
            return false;
        }

        // 检查硬度（基岩等负硬度方块也算墙）
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness >= 0 && hardness < MIN_HARDNESS) {
            return false;
        }

        return true;
    }

    /**
     * 检查碰撞箱是否有效
     */
    private static boolean hasValidCollision(Level level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty());

        // 没有碰撞箱（火把、花、红石线等）
        if (shape.isEmpty()) {
            return false;
        }

        // 检查高度（排除地毯、压力板、雪层等扁平物）
        double minY = shape.min(Direction.Axis.Y);
        double maxY = shape.max(Direction.Axis.Y);
        double height = maxY - minY;

        if (height < MIN_COLLISION_HEIGHT) {
            return false;
        }

        // 检查体积（排除按钮、拉杆等小物件）
        double minX = shape.min(Direction.Axis.X);
        double maxX = shape.max(Direction.Axis.X);
        double minZ = shape.min(Direction.Axis.Z);
        double maxZ = shape.max(Direction.Axis.Z);

        double width = maxX - minX;
        double depth = maxZ - minZ;
        double volume = width * height * depth;

        // 完整方块体积 = 1.0
        if (volume < MIN_VOLUME_RATIO) {
            return false;
        }

        return true;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取方块硬度
     * 返回 -1 表示不是有效固体方块
     */
    public static float getBlockHardness(Level level, BlockPos pos) {
        if (!isSolidWall(level, pos)) {
            return -1;
        }
        return level.getBlockState(pos).getDestroySpeed(level, pos);
    }

    /**
     * 判断是否是完整方块（整个1x1x1的立方体）
     */
    public static boolean isFullBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos);
    }

    /**
     * 判断是否是不可破坏的方块（基岩等）
     */
    public static boolean isUnbreakable(Level level, BlockPos pos) {
        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
        return hardness < 0;
    }
}
