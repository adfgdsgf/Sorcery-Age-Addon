package com.jujutsuaddon.addon.util.calc;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.damage.data.AbilityDamageData;
import com.jujutsuaddon.addon.util.context.TamedCostContext;
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

/**
 * 分类基准管理器 - 重构版 v2
 *
 * 核心逻辑：
 * 1. 按 AbilityCategory 分类
 * 2. 在每个术式(CursedTechnique)内，自动找出同类型中消耗最低的技能作为基准
 * 3. 计算倍率时，用当前技能消耗 / 该术式该类型的基准消耗
 */
public class CategoryBenchmark {

    // =========================================================
    // 缓存结构
    // =========================================================

    /**
     * 核心缓存：(类型, 术式) -> 基准消耗值
     *
     * 例如：
     *   (INSTANT, SHRINE) -> 4.0f  (Cleave的消耗)
     *   (INSTANT, LIMITLESS) -> 3.0f  (Blue的消耗)
     */
    private static Map<CategoryTechniqueKey, Float> autoBenchmarkCosts = null;

    /**
     * 记录每个组合的基准技能名（用于调试）
     */
    private static Map<CategoryTechniqueKey, String> autoBenchmarkNames = null;

    /**
     * 全局后备基准（当术式基准找不到时使用）
     * Key: AbilityCategory
     */
    private static EnumMap<AbilityCategory, Float> globalFallbackBenchmarks = null;

    /**
     * 无消耗召唤物的手动覆盖 (类名 -> 倍率)
     */
    private static Map<String, Float> zeroCostOverrides = null;

    /** 是否已初始化 */
    private static boolean initialized = false;

    // =========================================================
    // Key 类定义
    // =========================================================

    /**
     * 组合键：类型 + 术式
     */
    private record CategoryTechniqueKey(AbilityCategory category, CursedTechnique technique) {
        @Override
        public String toString() {
            return category.name() + "+" + (technique != null ? technique.name() : "NONE");
        }
    }

    // =========================================================
    // 初始化
    // =========================================================

    /**
     * 初始化基准数据 - 自动扫描所有技能
     */
    public static void initialize() {
        if (initialized) return;

        autoBenchmarkCosts = new HashMap<>();
        autoBenchmarkNames = new HashMap<>();
        globalFallbackBenchmarks = new EnumMap<>(AbilityCategory.class);
        zeroCostOverrides = new HashMap<>();

        // 1. 扫描所有技能，按 (类型, 术式) 分组
        Map<CategoryTechniqueKey, List<AbilityCostEntry>> groupedAbilities = scanAndGroupAbilities();

        // 2. 对每个组，找出消耗最低的作为基准
        findLowestCostBenchmarks(groupedAbilities);

        // 3. 加载全局后备基准（从配置文件）
        loadGlobalFallbackBenchmarks();

        // 4. 加载无消耗召唤物覆盖
        loadZeroCostOverrides();

        initialized = true;

    }

    /**
     * 扫描所有技能并按 (类型, 术式) 分组
     */
    private static Map<CategoryTechniqueKey, List<AbilityCostEntry>> scanAndGroupAbilities() {
        Map<CategoryTechniqueKey, List<AbilityCostEntry>> grouped = new HashMap<>();

        for (Ability ability : JJKAbilities.getAbilities()) {
            try {
                // 获取所属术式
                CursedTechnique technique = getTechniqueForAbility(ability);

                // ★★★ 新增：检查技能是否有伤害数据 ★★★
                AbilityDamageData.CachedData damageData = AbilityDamageData.get(ability);
                boolean hasDamage = damageData.baseDamage() != null && damageData.baseDamage() > 0;

                // ★ 对于召唤物，强制使用 TAMED 状态扫描 ★
                if (ability instanceof Summon<?>) {
                    TamedCostContext.setForceTamed(true);
                    try {
                        AbilityCategory category = CategoryResolver.resolve(ability, null);
                        // 排除类不参与
                        if (!category.shouldBalance()) {
                            continue;
                        }

                        float cost = calculateCostForCategory(ability, category, null);
                        if (cost <= 0) {
                            continue;
                        }

                        // ★★★ 新增：没有伤害的技能不能作为基准（召唤物除外）★★★
                        if (!hasDamage && !category.isSummon()) {
                            continue;
                        }

                        CategoryTechniqueKey key = new CategoryTechniqueKey(category, technique);
                        grouped.computeIfAbsent(key, k -> new ArrayList<>())
                                .add(new AbilityCostEntry(ability, cost));
                    } finally {
                        TamedCostContext.setForceTamed(false);
                    }
                } else {
                    // 非召唤物，正常处理
                    AbilityCategory category = CategoryResolver.resolve(ability, null);
                    if (!category.shouldBalance()) {
                        continue;
                    }

                    float cost = calculateCostForCategory(ability, category, null);
                    if (cost <= 0) {
                        continue;
                    }

                    // ★★★ 新增：没有伤害的技能不能作为伤害基准 ★★★
                    if (!hasDamage) {
                        continue;
                    }

                    CategoryTechniqueKey key = new CategoryTechniqueKey(category, technique);
                    grouped.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new AbilityCostEntry(ability, cost));
                }
            } catch (Exception e) {
                // 某些技能可能有问题，跳过
            }
        }
        return grouped;
    }

    /**
     * 对每个组找出消耗最低的技能作为基准
     */
    private static void findLowestCostBenchmarks(Map<CategoryTechniqueKey, List<AbilityCostEntry>> grouped) {
        for (Map.Entry<CategoryTechniqueKey, List<AbilityCostEntry>> entry : grouped.entrySet()) {
            CategoryTechniqueKey key = entry.getKey();
            List<AbilityCostEntry> abilities = entry.getValue();

            if (abilities.isEmpty()) continue;

            // 找出消耗最低的
            AbilityCostEntry lowest = abilities.stream()
                    .min(Comparator.comparingDouble(e -> e.cost))
                    .orElse(null);

            if (lowest != null && lowest.cost > 0) {
                autoBenchmarkCosts.put(key, lowest.cost);
                autoBenchmarkNames.put(key, lowest.ability.getClass().getSimpleName());
            }
        }
    }

    /**
     * 加载全局后备基准（从配置文件）
     */
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

    /**
     * 加载无消耗召唤物的手动覆盖
     */
    private static void loadZeroCostOverrides() {
        try {
            List<? extends String> overrides = AddonConfig.COMMON.zeroCostSummonOverrides.get();
            for (String entry : overrides) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        String className = parts[0].trim();
                        float multiplier = Float.parseFloat(parts[1].trim());
                        zeroCostOverrides.put(className, multiplier);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * 获取技能所属的术式
     */
    @Nullable
    private static CursedTechnique getTechniqueForAbility(Ability ability) {
        // 方法1：通过 JJKAbilities 获取
        try {
            // 遍历所有术式，检查技能是否属于该术式
            for (CursedTechnique technique : CursedTechnique.values()) {
                // ★ 修复：getAbilities() 返回的是数组，需要转换或直接遍历 ★
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

        // 方法2：通过包名推断
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
            // 可以继续添加更多...
        } catch (Exception ignored) {}

        return null;  // 无法确定术式
    }

    /**
     * 通过类名查找技能
     */
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

    /**
     * 辅助类：技能和消耗的组合
     */
    private record AbilityCostEntry(Ability ability, float cost) {}

    // =========================================================
    // 消耗计算（按分类）
    // =========================================================

    /**
     * 根据分类计算技能的消耗值
     */
    public static float calculateCostForCategory(Ability ability, AbilityCategory category,
                                                 @Nullable LivingEntity owner) {
        if (ability == null) return -1f;
        // ★ 关键修复：始终使用理论消耗 ★
        float rawCost = CostCalculator.getTheoreticalCost(ability);
        if (rawCost <= 0) return -1f;
        switch (category) {
            case INSTANT:
                // 瞬发技能：cost / CD秒（体现频率）
                // ★ 也使用理论冷却 ★
                float cdSeconds = CostCalculator.getTheoreticalCooldownSeconds(ability);
                if (cdSeconds > 0) {
                    return rawCost / cdSeconds;
                }
                return rawCost;
            case SUMMON_UNTAMED:
            case SUMMON_INSTANT:
                // 一次性召唤：直接使用原始消耗
                return rawCost;
            case TOGGLED:
            case SUMMON_TAMED:
                // 每tick消耗：cost × 20 = 每秒消耗
                return rawCost * 20f;
            case CHANNELED:
                // 引导技能：cost × duration = 总消耗
                int duration = CostCalculator.getDuration(ability);
                if (duration <= 0) duration = 100;  // 默认5秒
                return rawCost * duration;
            case ATTACK:
                // 攻击触发：cost × 估算攻速
                float estimatedAttackSpeed = 2.5f;
                return rawCost * estimatedAttackSpeed;
            default:
                return -1f;
        }
    }

    // =========================================================
    // 倍率计算（核心方法）
    // =========================================================

    /**
     * 获取技能的伤害倍率
     *
     * 核心逻辑：
     * 1. 确定技能的 (类型, 术式)
     * 2. 找到该组合的基准消耗
     * 3. 当前消耗 / 基准消耗 → √ → 倍率
     */
    public static float getMultiplier(Ability ability, @Nullable LivingEntity owner) {
        if (!initialized) initialize();
        if (ability == null) return 1.0f;
        // 1. 获取分类
        AbilityCategory category = CategoryResolver.resolve(ability, owner);
        // 2. 排除类不参与计算
        if (!category.shouldBalance()) {
            return 1.0f;
        }
        // 3. 检查无消耗召唤物
        if (category.isSummon()) {
            float zeroCostMult = getZeroCostMultiplier(ability);
            if (zeroCostMult > 0) {
                return zeroCostMult;
            }
        }
        // 4. 获取当前技能的消耗（使用理论消耗）
        float currentCost = calculateCostForCategory(ability, category, owner);
        if (currentCost <= 0) {
            return 1.0f;
        }
        // 5. 获取该技能所属的术式
        CursedTechnique technique = null;
        if (owner != null) {
            ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
            if (cap != null) {
                technique = cap.getTechnique();
            }
        }
        if (technique == null) {
            technique = getTechniqueForAbility(ability);
        }
        // 6. 获取基准消耗
        float benchmarkCost = getBenchmarkCost(category, technique);
        if (benchmarkCost <= 0) {
            return 1.0f;
        }
        // 7. 计算比值
        float ratio = currentCost / benchmarkCost;
        // 8. ★ 应用可配置的指数 ★
        double exponent = AddonConfig.COMMON.balancerScalingExponent.get();
        float multiplier;

        if (Math.abs(exponent) < 0.001) {
            // 指数为0，禁用缩放
            multiplier = 1.0f;
        } else if (Math.abs(exponent - 1.0) < 0.001) {
            // 指数为1，线性
            multiplier = ratio;
        } else {
            // 其他指数
            multiplier = (float) Math.pow(ratio, exponent);
        }
        // 9. 确保最低为1.0（低消耗技能不削弱）
        return Math.max(1.0f, multiplier);
    }

    /**
     * 获取 (类型, 术式) 组合的基准消耗
     *
     * 优先级：
     * 1. 自动扫描得到的 (类型+术式) 基准
     * 2. 配置文件中的全局类型基准
     * 3. 默认值
     */
    public static float getBenchmarkCost(AbilityCategory category, @Nullable CursedTechnique technique) {
        if (!initialized) initialize();

        // 1. 尝试获取 (类型+术式) 的自动基准
        if (technique != null) {
            CategoryTechniqueKey key = new CategoryTechniqueKey(category, technique);
            Float autoCost = autoBenchmarkCosts.get(key);
            if (autoCost != null && autoCost > 0) {
                return autoCost;
            }
        }

        // 2. 尝试获取全局后备基准
        Float fallback = globalFallbackBenchmarks.get(category);
        if (fallback != null && fallback > 0) {
            return fallback;
        }

        // 3. 返回默认值
        return getDefaultBenchmark(category);
    }

    /**
     * 获取默认基准值
     */
    private static float getDefaultBenchmark(AbilityCategory category) {
        switch (category) {
            case INSTANT:
                return 4.0f;   // 大约是 Blue 的每秒消耗
            case TOGGLED:
            case SUMMON_TAMED:
                return 4.0f;   // 玉犬的每秒消耗 (0.2 × 20)
            case CHANNELED:
                return 100.0f; // 估算的引导技能总消耗
            case ATTACK:
                return 5.0f;   // 估算的攻击类每秒消耗
            case SUMMON_UNTAMED:
            case SUMMON_INSTANT:
                return 50.0f;  // 改造人的单次消耗
            default:
                return 1.0f;
        }
    }

    /**
     * 获取基准技能名称（用于调试）
     */
    @Nullable
    public static String getBenchmarkName(AbilityCategory category, @Nullable CursedTechnique technique) {
        if (!initialized) initialize();

        if (technique != null) {
            CategoryTechniqueKey key = new CategoryTechniqueKey(category, technique);
            return autoBenchmarkNames.get(key);
        }
        return null;
    }

    /**
     * 获取无消耗召唤物的倍率
     */
    private static float getZeroCostMultiplier(Ability ability) {
        float rawCost = CostCalculator.getRawCost(ability, null);

        if (rawCost > 0) {
            return -1f;  // 有消耗，不是无消耗召唤物
        }

        String className = ability.getClass().getSimpleName();
        String fullName = ability.getClass().getName();

        if (zeroCostOverrides.containsKey(className)) {
            return zeroCostOverrides.get(className);
        }
        if (zeroCostOverrides.containsKey(fullName)) {
            return zeroCostOverrides.get(fullName);
        }

        return AddonConfig.COMMON.zeroCostSummonMultiplier.get().floatValue();
    }

    // =========================================================
    // 缓存管理
    // =========================================================

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

    // =========================================================
    // 调试
    // =========================================================


    /**
     * 获取调试信息
     */
    public static String getDebugInfo(Ability ability, @Nullable LivingEntity owner) {
        if (!initialized) initialize();

        StringBuilder sb = new StringBuilder();

        if (ability == null) {
            sb.append("Ability: null\n");
            return sb.toString();
        }

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

        sb.append("Ability: ").append(ability.getClass().getSimpleName()).append("\n");
        sb.append("Category: ").append(category.name()).append("\n");
        sb.append("Technique: ").append(technique != null ? technique.name() : "NONE").append("\n");
        sb.append("CostFormula: ").append(category.getCostFormula()).append("\n");
        sb.append("CurrentCost: ").append(String.format("%.2f", currentCost)).append("\n");
        sb.append("BenchmarkCost: ").append(String.format("%.2f", benchmarkCost)).append("\n");
        sb.append("BenchmarkAbility: ").append(benchmarkName != null ? benchmarkName : "(default)").append("\n");
        sb.append("Multiplier: ").append(String.format("%.2fx", multiplier)).append("\n");

        return sb.toString();
    }
}
