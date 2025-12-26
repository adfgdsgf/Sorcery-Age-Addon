// 文件路径: src/main/java/com/jujutsuaddon/addon/balance/ability/CategoryBenchmark.java
package com.jujutsuaddon.addon.balance.ability;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.context.TamedCostContext;
import com.jujutsuaddon.addon.damage.analysis.AbilityDamageData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
import java.util.*;

public class CategoryBenchmark {

    private static Map<CategoryTechniqueKey, Float> autoBenchmarkCosts = null;
    private static Map<CategoryTechniqueKey, String> autoBenchmarkNames = null;
    private static EnumMap<AbilityCategory, Float> globalFallbackBenchmarks = null;
    private static Map<String, Float> zeroCostOverrides = null;
    private static boolean initialized = false;

    private record CategoryTechniqueKey(AbilityCategory category, CursedTechnique technique) {
        @Override
        public String toString() {
            return category.name() + "+" + (technique != null ? technique.name() : "NONE");
        }
    }

    private record AbilityCostEntry(Ability ability, float cost) {}

    // ==================== 初始化 ====================

    public static void initialize() {
        if (initialized) return;

        autoBenchmarkCosts = new HashMap<>();
        autoBenchmarkNames = new HashMap<>();
        globalFallbackBenchmarks = new EnumMap<>(AbilityCategory.class);
        zeroCostOverrides = new HashMap<>();

        Map<CategoryTechniqueKey, List<AbilityCostEntry>> groupedAbilities = scanAndGroupAbilities();
        findLowestCostBenchmarks(groupedAbilities);
        loadGlobalFallbackBenchmarks();
        loadZeroCostOverrides();

        initialized = true;
    }

    private static Map<CategoryTechniqueKey, List<AbilityCostEntry>> scanAndGroupAbilities() {
        Map<CategoryTechniqueKey, List<AbilityCostEntry>> grouped = new HashMap<>();

        for (Ability ability : JJKAbilities.getAbilities()) {
            try {
                CursedTechnique technique = getTechniqueForAbility(ability);
                AbilityDamageData.CachedData damageData = AbilityDamageData.get(ability);
                boolean hasDamage = damageData.baseDamage() != null && damageData.baseDamage() > 0;

                if (ability instanceof Summon<?>) {
                    TamedCostContext.setForceTamed(true);
                    try {
                        AbilityCategory category = CategoryResolver.resolve(ability, null);
                        if (!category.shouldBalance()) continue;
                        float cost = calculateCostForCategory(ability, category, null);
                        if (cost <= 0) continue;
                        if (!hasDamage && !category.isSummon()) continue;

                        CategoryTechniqueKey key = new CategoryTechniqueKey(category, technique);
                        grouped.computeIfAbsent(key, k -> new ArrayList<>())
                                .add(new AbilityCostEntry(ability, cost));
                    } finally {
                        TamedCostContext.setForceTamed(false);
                    }
                } else {
                    AbilityCategory category = CategoryResolver.resolve(ability, null);
                    if (!category.shouldBalance()) continue;
                    float cost = calculateCostForCategory(ability, category, null);
                    if (cost <= 0) continue;
                    if (!hasDamage) continue;

                    CategoryTechniqueKey key = new CategoryTechniqueKey(category, technique);
                    grouped.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new AbilityCostEntry(ability, cost));
                }
            } catch (Exception ignored) {}
        }
        return grouped;
    }

    private static void findLowestCostBenchmarks(Map<CategoryTechniqueKey, List<AbilityCostEntry>> grouped) {
        for (Map.Entry<CategoryTechniqueKey, List<AbilityCostEntry>> entry : grouped.entrySet()) {
            CategoryTechniqueKey key = entry.getKey();
            List<AbilityCostEntry> abilities = entry.getValue();
            if (abilities.isEmpty()) continue;

            AbilityCostEntry lowest = abilities.stream()
                    .min(Comparator.comparingDouble(e -> e.cost))
                    .orElse(null);

            if (lowest != null && lowest.cost > 0) {
                autoBenchmarkCosts.put(key, lowest.cost);
                autoBenchmarkNames.put(key, lowest.ability.getClass().getSimpleName());
            }
        }
    }

    private static void loadGlobalFallbackBenchmarks() {
        loadSingleFallback(AbilityCategory.INSTANT, AddonConfig.COMMON.instantBenchmark.get());
        loadSingleFallback(AbilityCategory.TOGGLED, AddonConfig.COMMON.toggledBenchmark.get());
        loadSingleFallback(AbilityCategory.CHANNELED, AddonConfig.COMMON.channeledBenchmark.get());
        loadSingleFallback(AbilityCategory.ATTACK, AddonConfig.COMMON.attackBenchmark.get());
        loadSingleFallback(AbilityCategory.SUMMON_TAMED, AddonConfig.COMMON.summonTamedBenchmark.get());
        loadSingleFallback(AbilityCategory.SUMMON_UNTAMED, AddonConfig.COMMON.summonUntamedBenchmark.get());
        loadSingleFallback(AbilityCategory.SUMMON_INSTANT, AddonConfig.COMMON.summonInstantBenchmark.get());
    }

    private static void loadSingleFallback(AbilityCategory category, String className) {
        if (className == null || className.isEmpty()) return;
        Ability ability = findAbilityByClassName(className);
        if (ability != null) {
            float cost = calculateCostForCategory(ability, category, null);
            if (cost > 0) {
                globalFallbackBenchmarks.put(category, cost);
            }
        }
    }

    private static void loadZeroCostOverrides() {
        try {
            List<? extends String> overrides = AddonConfig.COMMON.zeroCostSummonOverrides.get();
            for (String entry : overrides) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        zeroCostOverrides.put(parts[0].trim(), Float.parseFloat(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    // ==================== 查找辅助 ====================

    @Nullable
    private static CursedTechnique getTechniqueForAbility(Ability ability) {
        try {
            for (CursedTechnique technique : CursedTechnique.values()) {
                Ability[] techAbilities = technique.getAbilities();
                if (techAbilities != null) {
                    for (Ability techAbility : techAbilities) {
                        if (techAbility != null && techAbility.equals(ability)) {
                            return technique;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            String packageName = ability.getClass().getPackage().getName().toLowerCase();
            if (packageName.contains("shrine")) return CursedTechnique.SHRINE;
            if (packageName.contains("limitless")) return CursedTechnique.LIMITLESS;
            if (packageName.contains("ten_shadows")) return CursedTechnique.TEN_SHADOWS;
            if (packageName.contains("idle_transfiguration")) return CursedTechnique.IDLE_TRANSFIGURATION;
            if (packageName.contains("disaster_flames")) return CursedTechnique.DISASTER_FLAMES;
            if (packageName.contains("disaster_plants")) return CursedTechnique.DISASTER_PLANTS;
            if (packageName.contains("disaster_tides")) return CursedTechnique.DISASTER_TIDES;
            if (packageName.contains("curse_manipulation")) return CursedTechnique.CURSE_MANIPULATION;
            if (packageName.contains("cursed_speech")) return CursedTechnique.CURSED_SPEECH;
            if (packageName.contains("projection_sorcery")) return CursedTechnique.PROJECTION_SORCERY;
            if (packageName.contains("mythical_beast_amber")) return CursedTechnique.MYTHICAL_BEAST_AMBER;
            if (packageName.contains("ratio")) return CursedTechnique.RATIO;
            if (packageName.contains("angel")) return CursedTechnique.ANGEL;
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    private static Ability findAbilityByClassName(String className) {
        for (Ability ability : JJKAbilities.getAbilities()) {
            if (ability.getClass().getName().equals(className) ||
                    ability.getClass().getSimpleName().equals(className)) {
                return ability;
            }
        }
        try {
            if (className.contains(":")) {
                ResourceLocation loc = new ResourceLocation(className);
                return JJKAbilities.getValue(loc);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ==================== 消耗计算 ====================

    public static float calculateCostForCategory(Ability ability, AbilityCategory category,
                                                 @Nullable LivingEntity owner) {
        if (ability == null) return -1f;
        float rawCost = CostCalculator.getTheoreticalCost(ability);
        if (rawCost <= 0) return -1f;

        switch (category) {
            case INSTANT:
                float cdSeconds = CostCalculator.getTheoreticalCooldownSeconds(ability);
                if (cdSeconds > 0) return rawCost / cdSeconds;
                return rawCost;
            case SUMMON_UNTAMED:
            case SUMMON_INSTANT:
                return rawCost;
            case TOGGLED:
            case SUMMON_TAMED:
                return rawCost * 20f;
            case CHANNELED:
                int duration = CostCalculator.getDuration(ability);
                if (duration <= 0) duration = 100;
                return rawCost * duration;
            case ATTACK:
                return rawCost * 2.5f;
            default:
                return -1f;
        }
    }

    // ==================== 倍率计算 ====================

    public static float getMultiplier(Ability ability, @Nullable LivingEntity owner) {
        if (!initialized) initialize();
        if (ability == null) return 1.0f;

        AbilityCategory category = CategoryResolver.resolve(ability, owner);
        if (!category.shouldBalance()) return 1.0f;

        if (category.isSummon()) {
            float zeroCostMult = getZeroCostMultiplier(ability);
            if (zeroCostMult > 0) return zeroCostMult;
        }

        float currentCost = calculateCostForCategory(ability, category, owner);
        if (currentCost <= 0) return 1.0f;

        CursedTechnique technique = null;
        if (owner != null) {
            ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
            if (cap != null) technique = cap.getTechnique();
        }
        if (technique == null) technique = getTechniqueForAbility(ability);

        float benchmarkCost = getBenchmarkCost(category, technique);
        if (benchmarkCost <= 0) return 1.0f;

        float ratio = currentCost / benchmarkCost;
        double exponent = AddonConfig.COMMON.balancerScalingExponent.get();
        float multiplier;

        if (Math.abs(exponent) < 0.001) {
            multiplier = 1.0f;
        } else if (Math.abs(exponent - 1.0) < 0.001) {
            multiplier = ratio;
        } else {
            multiplier = (float) Math.pow(ratio, exponent);
        }

        return Math.max(1.0f, multiplier);
    }

    public static float getBenchmarkCost(AbilityCategory category, @Nullable CursedTechnique technique) {
        if (!initialized) initialize();

        if (technique != null) {
            CategoryTechniqueKey key = new CategoryTechniqueKey(category, technique);
            Float autoCost = autoBenchmarkCosts.get(key);
            if (autoCost != null && autoCost > 0) return autoCost;
        }

        Float fallback = globalFallbackBenchmarks.get(category);
        if (fallback != null && fallback > 0) return fallback;

        return getDefaultBenchmark(category);
    }

    private static float getDefaultBenchmark(AbilityCategory category) {
        return switch (category) {
            case INSTANT -> 4.0f;
            case TOGGLED, SUMMON_TAMED -> 4.0f;
            case CHANNELED -> 100.0f;
            case ATTACK -> 5.0f;
            case SUMMON_UNTAMED, SUMMON_INSTANT -> 50.0f;
            default -> 1.0f;
        };
    }

    @Nullable
    public static String getBenchmarkName(AbilityCategory category, @Nullable CursedTechnique technique) {
        if (!initialized) initialize();
        if (technique != null) {
            return autoBenchmarkNames.get(new CategoryTechniqueKey(category, technique));
        }
        return null;
    }

    private static float getZeroCostMultiplier(Ability ability) {
        float rawCost = CostCalculator.getRawCost(ability, null);
        if (rawCost > 0) return -1f;

        String className = ability.getClass().getSimpleName();
        String fullName = ability.getClass().getName();

        if (zeroCostOverrides.containsKey(className)) return zeroCostOverrides.get(className);
        if (zeroCostOverrides.containsKey(fullName)) return zeroCostOverrides.get(fullName);

        return AddonConfig.COMMON.zeroCostSummonMultiplier.get().floatValue();
    }

    // ==================== 缓存管理 ====================

    public static void reload() {
        initialized = false;
        autoBenchmarkCosts = null;
        autoBenchmarkNames = null;
        globalFallbackBenchmarks = null;
        zeroCostOverrides = null;
        CategoryResolver.reload();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getDebugInfo(Ability ability, @Nullable LivingEntity owner) {
        if (!initialized) initialize();
        if (ability == null) return "Ability: null\n";

        AbilityCategory category = CategoryResolver.resolve(ability, owner);
        CursedTechnique technique = null;
        if (owner != null) {
            ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
            if (cap != null) technique = cap.getTechnique();
        }
        if (technique == null) technique = getTechniqueForAbility(ability);

        float currentCost = calculateCostForCategory(ability, category, owner);
        float benchmarkCost = getBenchmarkCost(category, technique);
        String benchmarkName = getBenchmarkName(category, technique);
        float multiplier = getMultiplier(ability, owner);

        return String.format(
                "Ability: %s\nCategory: %s\nTechnique: %s\nCostFormula: %s\n" +
                        "CurrentCost: %.2f\nBenchmarkCost: %.2f\nBenchmarkAbility: %s\nMultiplier: %.2fx\n",
                ability.getClass().getSimpleName(), category.name(),
                technique != null ? technique.name() : "NONE", category.getCostFormula(),
                currentCost, benchmarkCost, benchmarkName != null ? benchmarkName : "(default)", multiplier
        );
    }
}
