package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsMode;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 十影术式辅助类
 */
public class TenShadowsHelper {

    /**
     * 技能不可用的原因
     */
    public enum UnavailableReason {
        NONE,                    // 可用
        FUSION_COMPONENT_DEAD,   // 融合组件死亡
        SHIKIGAMI_DEAD,          // 式神已死亡
        SHIKIGAMI_SUMMONED,      // 式神已在场
        NOT_TAMED,               // 未调伏
        CONDITIONS_NOT_MET       // 其他条件未满足
    }

    // ==================== 配置方法 ====================

    public static boolean isEnabled() {
        return AddonConfig.COMMON.enableTenShadowsModeBypass.get();
    }

    public static boolean allowSimultaneous() {
        return isEnabled() && AddonConfig.COMMON.allowSimultaneousSummonAndAbility.get();
    }

    public static boolean hasTenShadows(LivingEntity owner) {
        ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return false;

        if (cap.getTechnique() == CursedTechnique.TEN_SHADOWS) return true;
        if (cap.getCurrentCopied() == CursedTechnique.TEN_SHADOWS) return true;

        Set<CursedTechnique> copied = cap.getCopied();
        return copied != null && copied.contains(CursedTechnique.TEN_SHADOWS);
    }

    public static boolean isTenShadowsAbility(Ability ability) {
        if (ability == null) return false;
        for (Ability a : CursedTechnique.TEN_SHADOWS.getAbilities()) {
            if (a == ability) return true;
        }
        return ability == CursedTechnique.TEN_SHADOWS.getDomain();
    }

    // ==================== 核心方法 ====================

    /**
     * 获取所有可用的十影技能
     */
    public static List<Ability> getAllAvailableTenShadowsAbilities(LivingEntity owner) {
        Set<Ability> result = new LinkedHashSet<>();

        if (!isEnabled()) return new ArrayList<>();
        if (!hasTenShadows(owner)) return new ArrayList<>();

        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        if (tenData == null) return new ArrayList<>();

        TenShadowsMode originalMode = tenData.getMode();

        tenData.setMode(TenShadowsMode.SUMMON);
        collectValidAbilities(owner, result);

        tenData.setMode(TenShadowsMode.ABILITY);
        collectValidAbilities(owner, result);

        tenData.setMode(originalMode);

        Ability domain = CursedTechnique.TEN_SHADOWS.getDomain();
        if (domain != null && (!domain.isUnlockable() || domain.isUnlocked(owner))) {
            result.add(domain);
        }

        return new ArrayList<>(result);
    }

    private static void collectValidAbilities(LivingEntity owner, Set<Ability> result) {
        for (Ability ability : CursedTechnique.TEN_SHADOWS.getAbilities()) {
            if (ability == null) continue;
            if (ability.isUnlockable() && !ability.isUnlocked(owner)) continue;

            try {
                if (ability.isValid(owner)) {
                    result.add(ability);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 智能检测技能所需的模式
     */
    @Nullable
    public static TenShadowsMode getRequiredMode(LivingEntity owner, Ability ability) {
        if (ability == null) return null;

        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        if (tenData == null) return null;

        TenShadowsMode originalMode = tenData.getMode();

        boolean validInAbility = false;
        boolean validInSummon = false;

        try {
            tenData.setMode(TenShadowsMode.ABILITY);
            validInAbility = ability.isValid(owner);

            tenData.setMode(TenShadowsMode.SUMMON);
            validInSummon = ability.isValid(owner);
        } catch (Exception ignored) {
        } finally {
            tenData.setMode(originalMode);
        }

        if (validInAbility && !validInSummon) return TenShadowsMode.ABILITY;
        if (validInSummon && !validInAbility) return TenShadowsMode.SUMMON;

        return null;
    }

    /**
     * 智能自动切换模式
     */
    public static boolean autoSwitchMode(LivingEntity owner, Ability ability) {
        if (!isEnabled()) return false;

        TenShadowsMode required = getRequiredMode(owner, ability);
        if (required == null) return false;

        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        if (tenData == null) return false;

        if (tenData.getMode() != required) {
            tenData.setMode(required);
            return true;
        }
        return false;
    }

    /**
     * 检测是否有召唤冲突
     */
    public static boolean hasSummonConflict(LivingEntity owner, Ability ability) {
        if (allowSimultaneous()) return false;
        if (ability == null) return false;

        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        if (tenData == null) return false;

        TenShadowsMode originalMode = tenData.getMode();

        boolean validInAny = false;

        try {
            tenData.setMode(TenShadowsMode.ABILITY);
            if (ability.isValid(owner)) validInAny = true;

            if (!validInAny) {
                tenData.setMode(TenShadowsMode.SUMMON);
                if (ability.isValid(owner)) validInAny = true;
            }
        } catch (Exception ignored) {
        } finally {
            tenData.setMode(originalMode);
        }

        return !validInAny;
    }

    /**
     * ★★★ 获取技能不可用的具体原因 ★★★
     */
    public static UnavailableReason getUnavailableReason(LivingEntity owner, Ability ability) {
        if (ability == null) return UnavailableReason.CONDITIONS_NOT_MET;
        if (!(ability instanceof Summon<?> summon)) return UnavailableReason.CONDITIONS_NOT_MET;
        if (!summon.isTenShadows()) return UnavailableReason.CONDITIONS_NOT_MET;

        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        if (tenData == null) return UnavailableReason.CONDITIONS_NOT_MET;

        try {
            Registry<EntityType<?>> registry = owner.level().registryAccess()
                    .registryOrThrow(Registries.ENTITY_TYPE);

            // ★ 融合式神：检查组件是否死亡 ★
            if (summon.isTotality()) {
                List<EntityType<?>> fusions = summon.getFusions();
                for (EntityType<?> fusionType : fusions) {
                    if (tenData.isDead(registry, fusionType)) {
                        return UnavailableReason.FUSION_COMPONENT_DEAD;
                    }
                }
            }

            // ★ 式神是否已死亡 ★
            if (summon.canDie() && summon.isDead(owner)) {
                return UnavailableReason.SHIKIGAMI_DEAD;
            }

            // ★ 式神是否已在场 ★
            ISorcererData sorcererData = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
            if (sorcererData != null && sorcererData.hasSummonOfClass(summon.getClazz())) {
                return UnavailableReason.SHIKIGAMI_SUMMONED;
            }

            // ★ 是否未调伏 ★
            if (!summon.isTamed(owner)) {
                return UnavailableReason.NOT_TAMED;
            }

        } catch (Exception ignored) {
        }

        return UnavailableReason.CONDITIONS_NOT_MET;
    }

    // ==================== 包含死亡式神的方法 ====================

    /**
     * ★★★ 获取所有十影技能（包括已死亡的式神）★★★
     * 用于技能栏配置界面
     */
    public static List<Ability> getAllTenShadowsAbilitiesIncludingDead(LivingEntity owner) {
        Set<Ability> result = new LinkedHashSet<>();
        if (!hasTenShadows(owner)) return new ArrayList<>();
        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        if (tenData == null) return new ArrayList<>();
        TenShadowsMode originalMode = tenData.getMode();
        try {
            Registry<EntityType<?>> registry = owner.level().registryAccess()
                    .registryOrThrow(Registries.ENTITY_TYPE);
            for (Ability ability : CursedTechnique.TEN_SHADOWS.getAbilities()) {
                if (ability == null) continue;
                if (ability.isUnlockable() && !ability.isUnlocked(owner)) continue;
                if (ability instanceof Summon<?> summon && summon.isTenShadows()) {
                    boolean shouldShow = false;
                    if (summon.isTotality()) {
                        // ★★★ 融合式神：检查所有组件是否都拥有过 ★★★
                        List<EntityType<?>> fusions = summon.getFusions();
                        if (!fusions.isEmpty()) {
                            boolean allOwned = true;
                            for (EntityType<?> fusionType : fusions) {
                                if (!isComponentOwned(owner, tenData, registry, fusionType)) {
                                    allOwned = false;
                                    break;
                                }
                            }
                            shouldShow = allOwned;
                        }
                    } else {
                        // ★★★ 普通式神 ★★★
                        // 1. 先用 isValid 检测当前是否可用
                        tenData.setMode(TenShadowsMode.SUMMON);
                        boolean validSummon = false;
                        try { validSummon = ability.isValid(owner); } catch (Exception ignored) {}
                        tenData.setMode(TenShadowsMode.ABILITY);
                        boolean validAbility = false;
                        try { validAbility = ability.isValid(owner); } catch (Exception ignored) {}
                        if (validSummon || validAbility) {
                            // 当前可用（包括未调伏的式神在 SUMMON 模式下也是 valid 的）
                            shouldShow = true;
                        } else {
                            // 2. isValid 返回 false，检查原因
                            if (summon.isTamed(owner)) {
                                // 已调伏但当前不可用（可能式神在场上、死亡、或在冷却）
                                shouldShow = true;
                            } else {
                                // 未调伏，检查是否已死亡
                                for (EntityType<?> type : summon.getTypes()) {
                                    if (tenData.isDead(registry, type)) {
                                        shouldShow = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (shouldShow) {
                        result.add(ability);
                    }
                } else {
                    // 非召唤类技能
                    tenData.setMode(TenShadowsMode.SUMMON);
                    boolean validSummon = false;
                    try { validSummon = ability.isValid(owner); } catch (Exception ignored) {}
                    tenData.setMode(TenShadowsMode.ABILITY);
                    boolean validAbility = false;
                    try { validAbility = ability.isValid(owner); } catch (Exception ignored) {}
                    if (validSummon || validAbility) {
                        result.add(ability);
                    }
                }
            }
        } finally {
            tenData.setMode(originalMode);
        }
        // 领域
        Ability domain = CursedTechnique.TEN_SHADOWS.getDomain();
        if (domain != null && (!domain.isUnlockable() || domain.isUnlocked(owner))) {
            result.add(domain);
        }
        return new ArrayList<>(result);
    }

    /**
     * ★★★ 检查融合组件是否已拥有 ★★★
     */
    private static boolean isComponentOwned(LivingEntity owner, ITenShadowsData tenData,
                                            Registry<EntityType<?>> registry, EntityType<?> componentType) {
        // 已调伏
        if (tenData.hasTamed(registry, componentType)) {
            return true;
        }

        // 已死亡（曾经拥有过）
        if (tenData.isDead(registry, componentType)) {
            return true;
        }

        // ★★★ 检查是否是默认式神（通过找到对应的 Summon 并调用 isTamed）★★★
        for (Ability ability : CursedTechnique.TEN_SHADOWS.getAbilities()) {
            if (!(ability instanceof Summon<?> summon)) continue;
            if (!summon.isTenShadows()) continue;

            for (EntityType<?> type : summon.getTypes()) {
                if (type == componentType) {
                    // 找到了对应的召唤技能，使用 isTamed 检查
                    // isTamed 内部会处理 canTame()=false 的情况（默认式神）
                    return summon.isTamed(owner);
                }
            }
        }

        return false;
    }

    /**
     * 检查式神是否已死亡
     */
    public static boolean isShikigamiDead(LivingEntity owner, Ability ability) {
        if (!(ability instanceof Summon<?> summon)) return false;
        if (!summon.isTenShadows() || !summon.canDie()) return false;

        // ★★★ 融合式神：检查任一组件是否死亡 ★★★
        if (summon.isTotality()) {
            ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
            if (tenData == null) return false;

            try {
                Registry<EntityType<?>> registry = owner.level().registryAccess()
                        .registryOrThrow(Registries.ENTITY_TYPE);
                List<EntityType<?>> fusions = summon.getFusions();
                for (EntityType<?> fusionType : fusions) {
                    if (tenData.isDead(registry, fusionType)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
            return false;
        }

        // ★★★ 普通式神：使用内置方法 ★★★
        return summon.isDead(owner);
    }

    /**
     * 检查是否是融合式神
     */
    public static boolean isFusionShikigami(Ability ability) {
        if (!(ability instanceof Summon<?> summon)) return false;
        return summon.isTenShadows() && summon.isTotality();
    }

    /**
     * 检查技能当前是否可直接使用
     */
    public static boolean isCurrentlyAvailable(LivingEntity owner, Ability ability) {
        if (ability == null) return false;
        try {
            return ability.isValid(owner);
        } catch (Exception e) {
            return false;
        }
    }
}
