package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.conflict;

import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 无下限冲突解决器
 * 处理多个无下限用户之间的相互作用
 */
public final class InfinityConflictResolver {

    private InfinityConflictResolver() {}

    // ==================== 缓存 ====================
    private static final Map<UUID, Long> strengthCheckCache = new HashMap<>();
    private static final Map<UUID, InfinityStrength> strengthResultCache = new HashMap<>();
    private static final long CACHE_DURATION = 10; // 10 ticks

    // ★★★ 新增：记录缓存时的 pressureLevel，用于检测变化 ★★★
    private static final Map<UUID, Integer> cachedPressureLevels = new HashMap<>();

    // ==================== 数据类 ====================

    /**
     * 无下限强度数据
     */
    public static class InfinityStrength {
        public final boolean hasInfinity;
        public final int pressureLevel;
        public final float cursedEnergyOutput;
        public final double totalStrength;

        public InfinityStrength(boolean hasInfinity, int pressureLevel, float cursedEnergyOutput) {
            this.hasInfinity = hasInfinity;
            this.pressureLevel = pressureLevel;
            this.cursedEnergyOutput = cursedEnergyOutput;
            // 综合强度 = 等级 × 咒力输出
            this.totalStrength = hasInfinity ? pressureLevel * cursedEnergyOutput : 0;
        }

        public static final InfinityStrength NONE = new InfinityStrength(false, 0, 0);
    }

    /**
     * 冲突解决结果
     */
    public static class ConflictResult {
        /** 是否能影响目标 */
        public final boolean canAffect;
        /** 有效压力倍率 (0.0 ~ 1.0+) */
        public final double effectiveMultiplier;
        /** 目标的无下限强度 */
        public final InfinityStrength targetStrength;

        public ConflictResult(boolean canAffect, double effectiveMultiplier, InfinityStrength targetStrength) {
            this.canAffect = canAffect;
            this.effectiveMultiplier = effectiveMultiplier;
            this.targetStrength = targetStrength;
        }

        public static final ConflictResult FULL_EFFECT = new ConflictResult(true, 1.0, InfinityStrength.NONE);
        public static final ConflictResult NO_EFFECT = new ConflictResult(false, 0.0, InfinityStrength.NONE);
    }

    // ==================== 缓存管理 ====================

    /**
     * ★★★ 当 pressureLevel 改变时调用，清除该实体的缓存 ★★★
     * 这确保消耗和冲突计算能立即使用新的等级值
     */
    public static void invalidateCache(UUID entityId) {
        if (entityId == null) return;
        strengthCheckCache.remove(entityId);
        strengthResultCache.remove(entityId);
        cachedPressureLevels.remove(entityId);
    }

    // ==================== 核心方法 ====================

    /**
     * 获取实体的无下限强度
     */
    public static InfinityStrength getInfinityStrength(LivingEntity entity) {
        if (entity == null) return InfinityStrength.NONE;

        UUID uuid = entity.getUUID();
        long currentTime = entity.level().getGameTime();

        // ★★★ 先快速检查 pressureLevel 是否变化 ★★★
        int currentPressure = getCurrentPressureLevelFast(entity);
        Integer cachedPressure = cachedPressureLevels.get(uuid);

        // 如果 pressureLevel 变了，立即使缓存失效
        if (cachedPressure != null && cachedPressure != currentPressure) {
            invalidateCache(uuid);
        }

        // 检查缓存
        Long lastCheck = strengthCheckCache.get(uuid);
        if (lastCheck != null && currentTime - lastCheck < CACHE_DURATION) {
            InfinityStrength cached = strengthResultCache.get(uuid);
            if (cached != null) {
                return cached;
            }
        }

        // 计算强度
        InfinityStrength strength = calculateStrength(entity);

        // 缓存结果
        strengthCheckCache.put(uuid, currentTime);
        strengthResultCache.put(uuid, strength);
        cachedPressureLevels.put(uuid, strength.pressureLevel);

        return strength;
    }

    /**
     * ★★★ 快速获取当前 pressureLevel（不经过完整计算）★★★
     */
    private static int getCurrentPressureLevelFast(LivingEntity entity) {
        try {
            ISorcererData data = entity.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
            if (data instanceof IInfinityPressureAccessor accessor) {
                return accessor.jujutsuAddon$getInfinityPressure();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static InfinityStrength calculateStrength(LivingEntity entity) {
        try {
            // 检查是否开启无下限
            if (!JJKAbilities.hasToggled(entity, JJKAbilities.INFINITY.get())) {
                return InfinityStrength.NONE;
            }

            ISorcererData data = entity.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
            if (data == null) {
                return InfinityStrength.NONE;
            }

            // 获取压力等级
            int pressureLevel = 0;
            if (data instanceof IInfinityPressureAccessor accessor) {
                pressureLevel = accessor.jujutsuAddon$getInfinityPressure();
            }

            // 如果压力等级为0，无下限没有压制效果
            if (pressureLevel <= 0) {
                return new InfinityStrength(true, 0, data.getAbilityPower());
            }

            return new InfinityStrength(true, pressureLevel, data.getAbilityPower());

        } catch (Exception e) {
            return InfinityStrength.NONE;
        }
    }

    /**
     * ★★★ 解决两个无下限之间的冲突 ★★★
     *
     * @param attacker 发起压制的一方
     * @param defender 被压制的一方
     * @return 冲突解决结果
     */
    public static ConflictResult resolveConflict(LivingEntity attacker, LivingEntity defender) {
        if (attacker == null || defender == null) {
            return ConflictResult.FULL_EFFECT;
        }

        InfinityStrength attackerStrength = getInfinityStrength(attacker);
        InfinityStrength defenderStrength = getInfinityStrength(defender);

        // 防守方没有无下限 → 完全生效
        if (!defenderStrength.hasInfinity || defenderStrength.pressureLevel <= 0) {
            return ConflictResult.FULL_EFFECT;
        }

        // 进攻方没有无下限或压力为0 → 这种情况不应该发生，但以防万一
        if (!attackerStrength.hasInfinity || attackerStrength.pressureLevel <= 0) {
            return ConflictResult.NO_EFFECT;
        }

        // ★★★ 双方都有无下限，比较强度 ★★★
        double attackerTotal = attackerStrength.totalStrength;
        double defenderTotal = defenderStrength.totalStrength;

        if (attackerTotal <= defenderTotal) {
            // 进攻方弱于或等于防守方 → 无效果
            return new ConflictResult(false, 0.0, defenderStrength);
        }

        // 进攻方更强 → 用超出部分计算有效倍率
        double excessRatio = (attackerTotal - defenderTotal) / attackerTotal;
        excessRatio = Math.max(0.0, Math.min(1.0, excessRatio));

        return new ConflictResult(true, excessRatio, defenderStrength);
    }

    /**
     * 解决投射物的无下限冲突
     */
    public static ConflictResult resolveProjectileConflict(LivingEntity defender, Projectile projectile) {
        if (projectile == null) {
            return ConflictResult.FULL_EFFECT;
        }

        Entity owner = projectile.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return ConflictResult.FULL_EFFECT;
        }

        InfinityStrength ownerStrength = getInfinityStrength(livingOwner);

        if (!ownerStrength.hasInfinity || ownerStrength.pressureLevel <= 0) {
            return ConflictResult.FULL_EFFECT;
        }

        InfinityStrength defenderStrength = getInfinityStrength(defender);

        if (!defenderStrength.hasInfinity) {
            return ConflictResult.NO_EFFECT;
        }

        double defenderTotal = defenderStrength.totalStrength;
        double attackerTotal = ownerStrength.totalStrength;

        if (defenderTotal <= attackerTotal) {
            return new ConflictResult(false, 0.0, ownerStrength);
        }

        double excessRatio = (defenderTotal - attackerTotal) / defenderTotal;
        excessRatio = Math.max(0.0, Math.min(1.0, excessRatio));

        return new ConflictResult(true, excessRatio, ownerStrength);
    }

    /**
     * 计算对目标的有效压力值
     */
    public static double getEffectivePressure(double basePressure, ConflictResult conflictResult) {
        if (!conflictResult.canAffect) {
            return 0;
        }
        return basePressure * conflictResult.effectiveMultiplier;
    }

    /**
     * 计算对目标的有效推力
     */
    public static double getEffectiveForce(double baseForce, ConflictResult conflictResult) {
        if (!conflictResult.canAffect) {
            return 0;
        }
        return baseForce * conflictResult.effectiveMultiplier;
    }

    // ==================== 缓存清理 ====================

    public static void cleanupCache(long currentTime) {
        strengthCheckCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > CACHE_DURATION * 5);
        strengthResultCache.keySet().retainAll(strengthCheckCache.keySet());
        cachedPressureLevels.keySet().retainAll(strengthCheckCache.keySet());
    }
}
