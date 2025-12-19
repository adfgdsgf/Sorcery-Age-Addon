package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.conflict;

import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
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

    // ==================== 核心方法 ====================

    /**
     * 获取实体的无下限强度
     */
    public static InfinityStrength getInfinityStrength(LivingEntity entity) {
        if (entity == null) return InfinityStrength.NONE;

        UUID uuid = entity.getUUID();
        long currentTime = entity.level().getGameTime();

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

        return strength;
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
        // 有效倍率 = (攻击强度 - 防御强度) / 攻击强度
        double excessRatio = (attackerTotal - defenderTotal) / attackerTotal;

        // 限制在合理范围内
        excessRatio = Math.max(0.0, Math.min(1.0, excessRatio));

        return new ConflictResult(true, excessRatio, defenderStrength);
    }

    /**
     * 解决投射物的无下限冲突
     *
     * @param defender 无下限持有者（要挡投射物的人）
     * @param projectile 投射物
     * @return 冲突解决结果
     */
    public static ConflictResult resolveProjectileConflict(LivingEntity defender, Projectile projectile) {
        if (projectile == null) {
            return ConflictResult.FULL_EFFECT;
        }

        Entity owner = projectile.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return ConflictResult.FULL_EFFECT;
        }

        // 获取投射物发射者的无下限强度
        InfinityStrength ownerStrength = getInfinityStrength(livingOwner);

        // 发射者没有无下限 → 可以正常拦截
        if (!ownerStrength.hasInfinity || ownerStrength.pressureLevel <= 0) {
            return ConflictResult.FULL_EFFECT;
        }

        // 获取防守者的无下限强度
        InfinityStrength defenderStrength = getInfinityStrength(defender);

        // 防守者没有无下限 → 不应该发生，但以防万一
        if (!defenderStrength.hasInfinity) {
            return ConflictResult.NO_EFFECT;
        }

        // ★★★ 比较双方强度 ★★★
        double defenderTotal = defenderStrength.totalStrength;
        double attackerTotal = ownerStrength.totalStrength;

        if (defenderTotal <= attackerTotal) {
            // 防守方弱于或等于进攻方 → 无法拦截
            return new ConflictResult(false, 0.0, ownerStrength);
        }

        // 防守方更强 → 可以拦截，但效果可能减弱
        double excessRatio = (defenderTotal - attackerTotal) / defenderTotal;
        excessRatio = Math.max(0.0, Math.min(1.0, excessRatio));

        return new ConflictResult(true, excessRatio, ownerStrength);
    }

    /**
     * 计算对目标的有效压力值
     *
     * @param basePressure 基础压力值
     * @param conflictResult 冲突解决结果
     * @return 有效压力值
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
    }
}
