package com.jujutsuaddon.addon.client;

import com.jujutsuaddon.addon.network.InfinityFieldData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端无下限场数据缓存
 */
@OnlyIn(Dist.CLIENT)
public class InfinityFieldClientCache {

    private static volatile List<InfinityFieldData> currentFields = Collections.emptyList();
    private static volatile long lastUpdateTime = 0;

    // 数据过期时间（毫秒）
    private static final long EXPIRE_TIME_MS = 500;

    /**
     * 更新场数据（从网络包调用）
     */
    public static void updateFields(List<InfinityFieldData> fields, long serverTick) {
        currentFields = new ArrayList<>(fields);
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 获取当前所有有效的无下限场
     */
    public static List<InfinityFieldData> getFields() {
        if (System.currentTimeMillis() - lastUpdateTime > EXPIRE_TIME_MS) {
            return Collections.emptyList();
        }
        return currentFields;
    }

    /**
     * 检查是否在任何无下限场范围内
     */
    public static boolean isInAnyField() {
        return !getFields().isEmpty();
    }

    /**
     * 修改移动向量（备用方法）
     */
    public static Vec3 modifyMovement(Vec3 playerPos, Vec3 originalMovement) {
        List<InfinityFieldData> fields = getFields();
        if (fields.isEmpty()) {
            return originalMovement;
        }

        Vec3 modifiedMovement = originalMovement;

        for (InfinityFieldData field : fields) {
            modifiedMovement = modifyMovementForField(field, playerPos, modifiedMovement);
        }

        return modifiedMovement;
    }

    private static Vec3 modifyMovementForField(InfinityFieldData field, Vec3 playerPos, Vec3 movement) {
        double zenoMultiplier = field.calculateZenoMultiplier(playerPos);

        if (zenoMultiplier < 0.01) {
            return movement;
        }

        Vec3 horizontalPush = field.getHorizontalPushDirection(playerPos);
        Vec3 horizontalMovement = new Vec3(movement.x, 0, movement.z);

        double radialComponent = horizontalMovement.dot(horizontalPush);

        if (radialComponent >= 0) {
            return movement;  // 远离，不干预
        }

        // 接近施术者
        double newRadialComponent;
        if (zenoMultiplier >= 1.0) {
            newRadialComponent = 0;
        } else {
            double keepRatio = 1.0 - zenoMultiplier;
            newRadialComponent = radialComponent * keepRatio;
        }

        Vec3 radialVec = horizontalPush.scale(radialComponent);
        Vec3 tangentialVec = horizontalMovement.subtract(radialVec);
        Vec3 newRadialVec = horizontalPush.scale(newRadialComponent);
        Vec3 newHorizontalMovement = newRadialVec.add(tangentialVec);

        return new Vec3(newHorizontalMovement.x, movement.y, newHorizontalMovement.z);
    }

    /**
     * 清除缓存
     */
    public static void clear() {
        currentFields = Collections.emptyList();
        lastUpdateTime = 0;
    }
}
