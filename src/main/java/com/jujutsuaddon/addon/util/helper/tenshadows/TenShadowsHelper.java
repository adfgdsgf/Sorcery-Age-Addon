package com.jujutsuaddon.addon.util.helper.tenshadows;

import com.jujutsuaddon.addon.config.AddonConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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
        NONE,
        FUSION_COMPONENT_DEAD,
        SHIKIGAMI_DEAD,
        SHIKIGAMI_SUMMONED,
        NOT_TAMED,
        CONDITIONS_NOT_MET
    }

    // ==================== 配置方法 ====================

    public static boolean isEnabled() {
        return AddonConfig.COMMON.enableTenShadowsModeBypass.get();
    }

    public static boolean allowSimultaneous() {
        return isEnabled() && AddonConfig.COMMON.allowSimultaneousSummonAndAbility.get();
    }

    /**
     * ★★★ 修复：检查玩家是否通过任何方式拥有十影 ★★★
     */
    public static boolean hasTenShadows(LivingEntity owner) {
        ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return false;

        // 1. 原生十影
        if (cap.getTechnique() == CursedTechnique.TEN_SHADOWS) return true;

        // 2. 当前激活的复制术式
        if (cap.getCurrentCopied() == CursedTechnique.TEN_SHADOWS) return true;

        // 3. 复制池中的十影
        Set<CursedTechnique> copied = cap.getCopied();
        if (copied != null && copied.contains(CursedTechnique.TEN_SHADOWS)) return true;

        // ★★★ 4. 当前激活的偷取术式 ★★★
        if (cap.getCurrentStolen() == CursedTechnique.TEN_SHADOWS) return true;

        // ★★★ 5. 偷取池中的十影 ★★★
        Set<CursedTechnique> stolen = cap.getStolen();
        if (stolen != null && stolen.contains(CursedTechnique.TEN_SHADOWS)) return true;

        return false;
    }

    public static boolean isTenShadowsAbility(Ability ability) {
        if (ability == null) return false;
        for (Ability a : CursedTechnique.TEN_SHADOWS.getAbilities()) {
            if (a == ability) return true;
        }
        return ability == CursedTechnique.TEN_SHADOWS.getDomain();
    }

    // ==================== 新增：通用接口 ====================

    /**
     * ★★★ 获取术式的技能列表（自动处理十影特殊情况）★★★
     *
     * 调用方无需关心是否是十影，统一调用此方法即可。
     * - 如果是十影术式且玩家拥有十影，返回包含式神状态的完整列表
     * - 否则返回普通的 technique.getAbilities()
     *
     * @param owner 玩家
     * @param technique 术式
     * @return 技能列表
     */
    public static List<Ability> getAbilitiesForTechnique(LivingEntity owner, CursedTechnique technique) {
        if (technique == null) return Collections.emptyList();

        // 非十影术式，直接返回普通列表
        if (technique != CursedTechnique.TEN_SHADOWS) {
            return Arrays.asList(technique.getAbilities());
        }

        // 功能未启用，返回普通列表
        if (!isEnabled()) {
            return Arrays.asList(technique.getAbilities());
        }

        // 玩家不拥有十影（理论上不会发生，但做防御）
        if (!hasTenShadows(owner)) {
            return Arrays.asList(technique.getAbilities());
        }

        // ★★★ 返回包含死亡式神的完整列表 ★★★
        return getAllTenShadowsAbilitiesIncludingDead(owner);
    }

    /**
     * ★★★ 检查技能是否需要跳过 isValid 检查 ★★★
     *
     * 十影技能（尤其是死亡的式神）可能 isValid() 返回 false，
     * 但我们仍然想在列表中显示它们。
     */
    public static boolean shouldSkipValidCheck(LivingEntity owner, Ability ability) {
        if (!isEnabled()) return false;
        if (!isTenShadowsAbility(ability)) return false;
        if (!hasTenShadows(owner)) return false;
        return true;  // 十影技能跳过 isValid 检查，由我们自己的逻辑处理
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

            // 融合式神：检查组件是否死亡
            if (summon.isTotality()) {
                List<EntityType<?>> fusions = summon.getFusions();
                for (EntityType<?> fusionType : fusions) {
                    if (tenData.isDead(registry, fusionType)) {
                        return UnavailableReason.FUSION_COMPONENT_DEAD;
                    }
                }
            }

            // 式神是否已死亡
            if (summon.canDie() && summon.isDead(owner)) {
                return UnavailableReason.SHIKIGAMI_DEAD;
            }

            // 式神是否已在场
            ISorcererData sorcererData = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
            if (sorcererData != null && sorcererData.hasSummonOfClass(summon.getClazz())) {
                return UnavailableReason.SHIKIGAMI_SUMMONED;
            }

            // 是否未调伏
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
                        // 融合式神：检查所有组件是否都拥有过
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
                        // 普通式神
                        tenData.setMode(TenShadowsMode.SUMMON);
                        boolean validSummon = false;
                        try { validSummon = ability.isValid(owner); } catch (Exception ignored) {}

                        tenData.setMode(TenShadowsMode.ABILITY);
                        boolean validAbility = false;
                        try { validAbility = ability.isValid(owner); } catch (Exception ignored) {}

                        if (validSummon || validAbility) {
                            shouldShow = true;
                        } else {
                            if (summon.isTamed(owner)) {
                                shouldShow = true;
                            } else {
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

        // 检查是否是默认式神
        for (Ability ability : CursedTechnique.TEN_SHADOWS.getAbilities()) {
            if (!(ability instanceof Summon<?> summon)) continue;
            if (!summon.isTenShadows()) continue;

            for (EntityType<?> type : summon.getTypes()) {
                if (type == componentType) {
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

        // 融合式神：检查任一组件是否死亡
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

        // 普通式神：使用内置方法
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
