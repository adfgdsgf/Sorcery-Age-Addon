package com.jujutsuaddon.addon.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class AddonConfig {
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {
        // ==========================================
        // 1. 核心伤害系数 (Core Multipliers)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableAttackDamageScaling;
        public final ForgeConfigSpec.DoubleValue globalDamageMultiplier;

        public final ForgeConfigSpec.DoubleValue sorcererMeleePreservation;
        public final ForgeConfigSpec.DoubleValue sorcererMeleeMultiplier;
        public final ForgeConfigSpec.DoubleValue sorcererTechniquePreservation;
        public final ForgeConfigSpec.DoubleValue sorcererTechniqueMultiplier;

        public final ForgeConfigSpec.DoubleValue hrMeleePreservation;
        public final ForgeConfigSpec.DoubleValue hrMeleeMultiplier;

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> meleeSkillWhitelist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> skillMultipliers;

        // ==========================================
        // 2. 额外属性与附魔 (Attributes & Enchants)
        // ==========================================
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> extraAttributeScaling;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> bonusMultiplierAttributes;
        public final ForgeConfigSpec.BooleanValue useAdditiveExternalAttributes;
        public final ForgeConfigSpec.BooleanValue enableEnchantmentScaling;
        public final ForgeConfigSpec.DoubleValue enchantmentMultiplier;
        public final ForgeConfigSpec.BooleanValue restrictToWeapons;
        // 在 02_Attributes_Enchants 区块中添加：
        public final ForgeConfigSpec.BooleanValue enableEnchantmentTriggers;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enchantmentTriggerWhitelist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enchantmentTriggerBlacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enchantmentDamageBonuses;
        public final ForgeConfigSpec.BooleanValue triggerEnchantForUnknownDamage;
        public final ForgeConfigSpec.BooleanValue enableSummonEnchantTrigger;
        public final ForgeConfigSpec.BooleanValue enableWeaponEffectProxy;
        public final ForgeConfigSpec.BooleanValue enableItemHurtEnemyTrigger;
        public final ForgeConfigSpec.BooleanValue enableWhitelistMobWeaponProxy;
        public final ForgeConfigSpec.BooleanValue enableShikigamiWeaponProxy;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> hurtEnemyBlacklist;


        // ==========================================
        // 3. 暴击系统 (Critical Hits)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableCritSystem;
        public final ForgeConfigSpec.DoubleValue baseCritChance;
        public final ForgeConfigSpec.DoubleValue baseCritDamage;

        // ==========================================
        // 4. 机制设置 (Mechanics & PVP)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue debugMode;
        public final ForgeConfigSpec.BooleanValue enableDamageInterceptor;
        public final ForgeConfigSpec.BooleanValue enablePvpBalance;
        public final ForgeConfigSpec.DoubleValue pvpDamageMultiplier;

        public final ForgeConfigSpec.BooleanValue ignoreAllIframes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> ignoreIframeSkills;
        public final ForgeConfigSpec.IntValue iframeSetTo;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> iframeBypassWhitelist;
        public final ForgeConfigSpec.BooleanValue enableHealthToArmor;
        public final ForgeConfigSpec.DoubleValue sorcererHealthToArmorRatio;
        public final ForgeConfigSpec.DoubleValue sorcererHealthToToughnessRatio;

        public final ForgeConfigSpec.DoubleValue hrHealthToArmorRatio;
        public final ForgeConfigSpec.DoubleValue hrHealthToToughnessRatio;
        public final ForgeConfigSpec.BooleanValue enableMobHealthToArmor;
        public final ForgeConfigSpec.DoubleValue mobHealthToArmorRatio;
        public final ForgeConfigSpec.DoubleValue mobHealthToToughnessRatio;
        public final ForgeConfigSpec.DoubleValue mobHRHealthToArmorRatio;
        public final ForgeConfigSpec.DoubleValue mobHRHealthToToughnessRatio;
        public final ForgeConfigSpec.BooleanValue clearAdaptationOnDeath;
        public final ForgeConfigSpec.BooleanValue playerAdaptationEnabled;
        public final ForgeConfigSpec.BooleanValue playerAutoCounter;
        public final ForgeConfigSpec.BooleanValue playerTechniqueDisruption;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> singleHitProjectiles;

        // ==========================================
        // 5. 召唤物增强 (Summon Scaling)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableSummonScaling;
        public final ForgeConfigSpec.DoubleValue summonHpRatio;
        public final ForgeConfigSpec.DoubleValue summonAtkRatio;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> summonWhitelist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> summonAttributeInheritance;
        public final ForgeConfigSpec.DoubleValue swarmScalingModifier;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> swarmEntityList;
        public final ForgeConfigSpec.BooleanValue enableTierScaling;
        // public final ForgeConfigSpec.ConfigValue<String> benchmarkEntityId; // 已删除
        // public final ForgeConfigSpec.DoubleValue tierScalingExponent; // 已删除
        public final ForgeConfigSpec.DoubleValue minimumTierMultiplier;
        // public final ForgeConfigSpec.ConfigValue<List<? extends String>> customBenchmarkCosts; // 已删除
        public final ForgeConfigSpec.DoubleValue summonDpsCompensationFactor;
        public final ForgeConfigSpec.DoubleValue autoAttributeInheritanceRatio;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> autoAttributeBlacklist;
        public final ForgeConfigSpec.DoubleValue summonRegroupDistance;
        public final ForgeConfigSpec.BooleanValue enableUntamedStatScaling;
        public final ForgeConfigSpec.BooleanValue enableUntamedEquipSync;
        public final ForgeConfigSpec.DoubleValue manualTargetDistanceMultiplier;

        // ==========================================
        // 6. 天与咒缚专属 (Heavenly Restriction)
        // ==========================================
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> heavenlyRestrictionBonus;
        public final ForgeConfigSpec.DoubleValue defaultHeavenlyRestrictionMultiplier;
        public final ForgeConfigSpec.DoubleValue enableSoulTrueDamage;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> armorPenetrationAttributes;
        public final ForgeConfigSpec.DoubleValue sskDamageCorrection;
        public final ForgeConfigSpec.DoubleValue soulResistanceBypass;
        public final ForgeConfigSpec.DoubleValue playfulCloudAttributeBonus;

        // ==========================================
        // 7. 攻速收益 (Attack Speed Scaling)
        // ==========================================
        public final ForgeConfigSpec.DoubleValue hrAttackSpeedScaling;
        public final ForgeConfigSpec.DoubleValue sorcererAttackSpeedScaling;

        // ==========================================
        // 8. 技能动态平衡 (Skill Balancer)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableSkillBalancer;
        //public final ForgeConfigSpec.DoubleValue balancerCostWeight;
        //public final ForgeConfigSpec.DoubleValue balancerCdWeight;
        public final ForgeConfigSpec.DoubleValue balancerMaxMultiplier;
        public final ForgeConfigSpec.DoubleValue balancerScalingExponent;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> balancerBenchmarks;
        //public final ForgeConfigSpec.ConfigValue<List<? extends String>> chargedSkillsConfig;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> balancerExcludedSkills;

        // 反转术式关键词（用于排除）
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> rctKeywords;
        // 各分类的基准技能
        public final ForgeConfigSpec.ConfigValue<String> instantBenchmark;
        public final ForgeConfigSpec.ConfigValue<String> toggledBenchmark;
        public final ForgeConfigSpec.ConfigValue<String> channeledBenchmark;
        public final ForgeConfigSpec.ConfigValue<String> attackBenchmark;
        public final ForgeConfigSpec.ConfigValue<String> summonTamedBenchmark;
        public final ForgeConfigSpec.ConfigValue<String> summonUntamedBenchmark;
        public final ForgeConfigSpec.ConfigValue<String> summonInstantBenchmark;
        // 无消耗召唤物处理
        public final ForgeConfigSpec.DoubleValue zeroCostSummonMultiplier;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> zeroCostSummonOverrides;

        // ==========================================
        // 9. 角色特化平衡 (Character Specific)
        // ==========================================
        public final ForgeConfigSpec.DoubleValue todoMeleeMultiplier;
        public final ForgeConfigSpec.DoubleValue todoMeleePreservation;
        public final ForgeConfigSpec.DoubleValue todoTechniqueMultiplier;
        public final ForgeConfigSpec.DoubleValue todoTechniquePreservation;
        public final ForgeConfigSpec.DoubleValue noTechniqueMeleeMultiplier;
        public final ForgeConfigSpec.DoubleValue noTechniqueMeleePreservation;
        public final ForgeConfigSpec.DoubleValue mbaMeleeMultiplier;
        public final ForgeConfigSpec.DoubleValue mbaMeleePreservation;
        public final ForgeConfigSpec.DoubleValue mbaTechniqueMultiplier;
        public final ForgeConfigSpec.DoubleValue mbaTechniquePreservation;

        // ==========================================
        // 10. 生物兼容性 (Mob Compatibility) [新增]
        // ==========================================
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> compatMobList;
        public final ForgeConfigSpec.BooleanValue enableMobAI;
        public final ForgeConfigSpec.BooleanValue enableMobCompatibility;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> randomTechniqueBlacklist;
        public final ForgeConfigSpec.BooleanValue enableTechniqueReroll;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> rerollItems;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> traitRerollItems;
        public final ForgeConfigSpec.BooleanValue enableUniqueTraitLimit;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> factionRerollItems;

        // ==========================================
        // 11. 十影术式增强 (Ten Shadows Enhancement)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableTenShadowsModeBypass;
        public final ForgeConfigSpec.BooleanValue allowSimultaneousSummonAndAbility;


        public Common(ForgeConfigSpec.Builder builder) {
            // ==========================================================================
            // 1. 核心伤害系数 (Core Multipliers)
            // ==========================================================================
            // ==========================================================================
// ★★★ 核心公式速查 ★★★
// ==========================================================================
//
// 最终伤害 = 基础部分 + 面板部分
//
// 基础部分 = 技能原始伤害 × 保留比例(Preservation) × 外部加成
// 面板部分 = 攻击力属性 × 职业倍率(Multiplier) × 攻速加成 × 外部加成
//
// -------------------------------------------------------------------------
// 举例：咒术师用赫(Red)，原始伤害50，面板攻击50，攻速1.0
//
// 保留比例 = 2.0, 职业倍率 = 2.5
// 基础部分 = 50 × 2.0 = 100
// 面板部分 = 50 × 2.5 × 1.0 = 125
// 最终 = 100 + 125 = 225
// -------------------------------------------------------------------------
            builder.push("01_Core_Multipliers");
            enableAttackDamageScaling = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Panel Scaling]",
                            " If TRUE: Skill damage = Original Part + Attack Panel Part",
                            " If FALSE: Skill damage = Original Part only (ignores your Attack stat)",
                            "----------------------------------------------------------------",
                            " [启用面板加成]",
                            " 若为真：技能伤害 = 原始部分 + 攻击力面板部分",
                            " 若为假：技能伤害 = 仅原始部分（完全无视你的攻击力属性）",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_attack_scaling")
                    .define("EnableAttackDamageScaling", true);
            globalDamageMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Global Final Multiplier]",
                            " Applied LAST to all damage after all other calculations.",
                            " 1.0 = Normal, 0.5 = Half damage, 2.0 = Double damage",
                            "----------------------------------------------------------------",
                            " [全局最终倍率]",
                            " 在所有计算完成后的最后一步应用。",
                            " 1.0 = 正常, 0.5 = 伤害减半, 2.0 = 伤害翻倍",
                            "================================================================")
                    .translation("config.jujutsu_addon.global_multiplier")
                    .defineInRange("GlobalDamageMultiplier", 1.0, 0.0, 100.0);
// =========== 咒术师体术 ===========
            sorcererMeleePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [Sorcerer Melee - Original Damage Ratio]",
                            " How much of the skill's ORIGINAL damage is kept.",
                            " ",
                            " Formula: Base Part = Original Damage × This Ratio",
                            " ",
                            " Example: Punch deals 10 base damage, Ratio = 2.0",
                            "   -> Base Part = 10 × 2.0 = 20",
                            "----------------------------------------------------------------",
                            " [咒术师体术 - 原始伤害保留]",
                            " 技能原始伤害保留多少。",
                            " ",
                            " 公式：基础部分 = 原始伤害 × 此比例",
                            " ",
                            " 举例：拳击原始 10，比例 2.0",
                            "   -> 基础部分 = 10 × 2.0 = 20",
                            "================================================================")
                    .translation("config.jujutsu_addon.sorcerer_melee_preservation")
                    .defineInRange("SorcererMeleePreservation", 0.5, 0.0, 100.0);
            sorcererMeleeMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Sorcerer Melee - Panel Scaling Efficiency]",
                            " How efficiently your Attack Damage converts to skill damage.",
                            " ",
                            " Formula: Panel Part = Attack × This Multiplier × Speed",
                            " ",
                            " Example: 50 Attack, Multiplier = 0.5, Speed = 1.0",
                            "   -> Panel Part = 50 × 0.5 × 1.0 = 25",
                            "----------------------------------------------------------------",
                            " [咒术师体术 - 面板转化效率]",
                            " 攻击力面板转化为技能伤害的效率。",
                            " ",
                            " 公式：面板部分 = 攻击力 × 此倍率 × 攻速",
                            " ",
                            " 举例：50 攻击，倍率 0.5，攻速 1.0",
                            "   -> 面板部分 = 50 × 0.5 × 1.0 = 25",
                            "================================================================")
                    .translation("config.jujutsu_addon.sorcerer_melee_multiplier")
                    .defineInRange("SorcererMeleeMultiplier", 0.5, 0.0, 100.0);
// =========== 咒术师术式 ===========
            sorcererTechniquePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [Sorcerer Technique - Original Damage Ratio]",
                            " How much of the technique's ORIGINAL damage is kept.",
                            " ",
                            " Formula: Base Part = Original Damage × This Ratio",
                            " ",
                            " Example: Red deals 50 base damage, Ratio = 2.0",
                            "   -> Base Part = 50 × 2.0 = 100",
                            "----------------------------------------------------------------",
                            " [咒术师术式 - 原始伤害保留]",
                            " 术式原始伤害保留多少。",
                            " ",
                            " 公式：基础部分 = 原始伤害 × 此比例",
                            " ",
                            " 举例：赫原始 50，比例 2.0",
                            "   -> 基础部分 = 50 × 2.0 = 100",
                            "================================================================")
                    .translation("config.jujutsu_addon.sorcerer_technique_preservation")
                    .defineInRange("SorcererTechniquePreservation", 1.0, 0.0, 100.0);
            sorcererTechniqueMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Sorcerer Technique - Panel Scaling Efficiency]",
                            " How efficiently your Attack converts to technique damage.",
                            " ",
                            " Formula: Panel Part = Attack × This Multiplier × Speed",
                            " ",
                            " [Complete Formula]",
                            " Final = (Original × Preservation) + (Panel × Multiplier × Speed)",
                            " ",
                            " Example: Red 50 base, 50 ATK, Preservation 2.0, Multiplier 2.5",
                            "   = (50 × 2.0) + (50 × 2.5 × 1.0)",
                            "   = 100 + 125 = 225 damage",
                            "----------------------------------------------------------------",
                            " [咒术师术式 - 面板转化效率]",
                            " 攻击力面板转化为术式伤害的效率。",
                            " ",
                            " 公式：面板部分 = 攻击力 × 此倍率 × 攻速",
                            " ",
                            " [完整公式]",
                            " 最终 = (原始 × 保留比例) + (面板 × 此倍率 × 攻速)",
                            " ",
                            " 举例：赫原始 50，50 攻击，保留 2.0，倍率 2.5",
                            "   = (50 × 2.0) + (50 × 2.5 × 1.0)",
                            "   = 100 + 125 = 225 伤害",
                            "================================================================")
                    .translation("config.jujutsu_addon.sorcerer_technique_multiplier")
                    .defineInRange("SorcererTechniqueMultiplier", 1.15, 0.0, 10000.0);
// =========== 天与咒缚 ===========
            hrMeleePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [Heavenly Restriction - Original Damage Ratio]",
                            " How much of the original melee damage is kept.",
                            " ",
                            " Formula: Base Part = Original Damage × This Ratio",
                            "----------------------------------------------------------------",
                            " [天与咒缚 - 原始伤害保留]",
                            " 体术原始伤害保留多少。",
                            " ",
                            " 公式：基础部分 = 原始伤害 × 此比例",
                            "================================================================")
                    .translation("config.jujutsu_addon.hr_melee_preservation")
                    .defineInRange("HRMeleePreservation", 1.1, 0.0, 100.0);
            hrMeleeMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Heavenly Restriction - Panel Scaling Efficiency]",
                            " How efficiently Attack Damage converts to melee damage.",
                            " ",
                            " Formula: Panel Part = Attack × This Multiplier × Speed",
                            "----------------------------------------------------------------",
                            " [天与咒缚 - 面板转化效率]",
                            " 攻击力面板转化为体术伤害的效率。",
                            " ",
                            " 公式：面板部分 = 攻击力 × 此倍率 × 攻速",
                            "================================================================")
                    .translation("config.jujutsu_addon.hr_melee_multiplier")
                    .defineInRange("HRMeleeMultiplier", 1.35, 0.0, 10000.0);

            meleeSkillWhitelist = builder
                    .comment(" ",
                            "================================================================",
                            " [Melee Skill Whitelist]",
                            " List of damage source IDs or Skill Class Names considered as MELEE attacks.",
                            "----------------------------------------------------------------",
                            " [体术技能白名单]",
                            " 被视为体术攻击的伤害来源ID或技能类名列表。",
                            "================================================================")
                    .translation("config.jujutsu_addon.melee_skill_whitelist")
                    .defineList("MeleeSkillWhitelist",
                            Arrays.asList("blitz", "barrage", "slam"),
                            o -> o instanceof String);

            skillMultipliers = builder
                    .comment(" ",
                            " Specific multipliers. Format: SkillName=Multiplier",
                            " 指定技能的独立倍率。格式：技能名=倍率")
                    .translation("config.jujutsu_addon.skill_multipliers")
                    .defineList("SkillMultipliers", Arrays.asList("ExampleSkill=1.5"), entry -> true);

            builder.pop();

            // ==========================================================================
            // 2. 额外属性与附魔 (Attributes & Enchants)
            // ==========================================================================
            // 心华提示：改为 02 开头
            builder.push("02_Attributes_Enchants");

            extraAttributeScaling = builder
                    .comment(" ",
                            "================================================================",
                            " [Extra Attribute Scaling]",
                            " Convert other attributes into Attack Damage Panel.",
                            " Format: modid:attribute=multiplier",
                            " Example: irons_spellbooks:spell_power=0.5 means 100 Spell Power adds 50 Damage.",
                            "----------------------------------------------------------------",
                            " [额外属性转化]",
                            " 将其他模组的属性转化为攻击力面板。",
                            " 格式：模组ID:属性名=转化率",
                            "================================================================")
                    .translation("config.jujutsu_addon.extra_attributes")
                    .defineList("ExtraAttributeScaling", Arrays.asList(

                    ), entry -> true);

            bonusMultiplierAttributes = builder
                    .comment(" ",
                            "================================================================",
                            " [Bonus Multiplier Attributes List]",
                            " List of Attribute IDs that provide percentage-based damage bonus.",
                            " Format: modid:attribute=factor",
                            " Example: irons_spellbooks:spell_power=0.5",
                            " Logic: (AttributeValue - 1.0) * factor",
                            " If Attribute is 1.5 (+50%) and factor is 0.5, result is +25% bonus.",
                            "----------------------------------------------------------------",
                            " [额外倍率属性列表]",
                            " 在使用非体术技能（如术式、远程）时生效的属性ID列表。",
                            " 格式：模组ID:属性名=系数",
                            " 例子：irons_spellbooks:spell_power=0.5",
                            " 逻辑：(属性值 - 1.0) * 系数",
                            " 解释：如果法强是 1.5 (+50%)，系数是 0.5，则最终只提供 +25% 的伤害加成。",
                            "================================================================")
                    .translation("config.jujutsu_addon.bonus_multiplier_attributes")
                    .defineList("BonusMultiplierAttributes", Arrays.asList(
                            "apotheosis:projectile_damage=1.0",  // 神化远程，100%转化
                            "irons_spellbooks:spell_power=1.0"   // 铁魔法法强，100%转化
                    ), entry -> true);

            useAdditiveExternalAttributes = builder
                    .comment(" ",
                            "================================================================",
                            " [External Attribute Calculation Mode]",
                            " TRUE = Additive (Balanced). Formula: Base * (1 + Vanilla% + ModAttributes%)",
                            " FALSE = Multiplicative (Strong). Formula: Base * (1 + Vanilla%) * (1 + ModAttributes%)",
                            "----------------------------------------------------------------",
                            " [额外属性计算模式]",
                            " TRUE (真) = 加算 (平衡)。所有百分比加成在同一个乘区叠加。",
                            " FALSE (假) = 乘算 (强力)。模组属性作为独立乘区计算。",
                            "================================================================")
                    .translation("config.jujutsu_addon.external_attr_additive")
                    .define("UseAdditiveExternalAttributes", true);

            enableEnchantmentScaling = builder
                    .comment(" Enable vanilla enchantment scaling (Sharpness etc).",
                            " 启用原版附魔伤害加成（如锋利）。")
                    .translation("config.jujutsu_addon.enable_enchant_scaling")
                    .define("EnableEnchantmentScaling", true);

            enchantmentMultiplier = builder
                    .comment(" Multiplier for enchantment damage bonus.",
                            " 附魔伤害加成倍率。")
                    .translation("config.jujutsu_addon.enchant_multiplier")
                    .defineInRange("EnchantmentMultiplier", 1.0, 0.0, 100.0);

            restrictToWeapons = builder
                    .comment(" Only apply enchantments if holding a weapon.",
                            " 仅在手持武器时应用附魔加成。")
                    .translation("config.jujutsu_addon.restrict_to_weapons")
                    .define("RestrictToWeapons", true);

            enableEnchantmentTriggers = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Enchantment Triggers for Skills]",
                            " If true, weapon enchantments will trigger when using skills.",
                            " Example: Fire Aspect will ignite targets hit by your techniques.",
                            "----------------------------------------------------------------",
                            " [技能触发附魔效果]",
                            " 若为真，使用技能攻击时会触发武器上的附魔效果。",
                            " 例如：火焰附加会点燃被术式击中的目标。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_enchant_triggers")
                    .define("EnableEnchantmentTriggers", true);
            enchantmentTriggerWhitelist = builder
                    .comment(" ",
                            "================================================================",
                            " [Enchantment Trigger Whitelist]",
                            " Only these enchantments will be triggered by skills.",
                            " Use '*' to allow ALL enchantments (then use blacklist to exclude).",
                            " Format: 'modid:enchantment_name'",
                            "----------------------------------------------------------------",
                            " [附魔触发白名单]",
                            " 只有列表中的附魔会被技能触发。",
                            " 填入 '*' 表示允许所有附魔（再用黑名单排除）。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enchant_trigger_whitelist")
                    .defineList("EnchantmentTriggerWhitelist", Arrays.asList(
                            "*"  // 默认允许所有
                    ), entry -> true);
            enchantmentTriggerBlacklist = builder
                    .comment(" ",
                            "================================================================",
                            " [Enchantment Trigger Blacklist]",
                            " These enchantments will NEVER be triggered by skills.",
                            " Useful for preventing OP combos or buggy interactions.",
                            "----------------------------------------------------------------",
                            " [附魔触发黑名单]",
                            " 这些附魔永远不会被技能触发。",
                            " 用于防止过于强力的组合或BUG。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enchant_trigger_blacklist")
                    .defineList("EnchantmentTriggerBlacklist", Arrays.asList(
                            // "apotheosis:berserkers_fury"  // 示例：狂战士之怒可能太强
                    ), entry -> true);
            enchantmentDamageBonuses = builder
                    .comment(" ",
                            "================================================================",
                            " [Custom Enchantment Damage Bonuses]",
                            " Define extra damage per level for mod enchantments.",
                            " Format: 'modid:enchantment=damage_per_level'",
                            " Example: 'apotheosis:crescendo=1.5' means +1.5 damage per level.",
                            "----------------------------------------------------------------",
                            " [自定义附魔伤害加成]",
                            " 为模组附魔定义每级额外伤害。",
                            " 格式：'模组ID:附魔名=每级伤害'",
                            "================================================================")
                    .translation("config.jujutsu_addon.enchant_damage_bonuses")
                    .defineList("EnchantmentDamageBonuses", Arrays.asList(
                            // "apotheosis:crescendo=1.5",
                            // "irons_spellbooks:mana_siphon=0.5"
                    ), entry -> true);

            triggerEnchantForUnknownDamage = builder
                    .comment(" ",
                            "================================================================",
                            " [Trigger Enchantments for Unknown Damage Sources]",
                            " If true, enchantments will trigger for ANY non-vanilla attack.",
                            " If false, only JJK abilities will trigger enchantments.",
                            " Set to false if you have issues with other mods.",
                            "----------------------------------------------------------------",
                            " [对未知伤害源触发附魔]",
                            " 若为真，所有非原版攻击都会触发附魔。",
                            " 若为假，只有 JJK 技能会触发附魔。",
                            " 如果与其他模组冲突，请设为 false。",
                            "================================================================")
                    .translation("config.jujutsu_addon.trigger_enchant_unknown")  // ← 添加这行
                    .define("TriggerEnchantForUnknownDamage", false);

            enableSummonEnchantTrigger = builder
                    .comment(" ",
                            "================================================================",
                            " [Summon Enchantment Trigger] (Enchant System)",
                            " Shikigami attacks trigger enchantment DAMAGE (Fire Aspect, Sharpness).",
                            " This is handled by the Enchantment system, separate from Weapon Proxy.",
                            "----------------------------------------------------------------",
                            " [召唤物附魔触发] (附魔系统)",
                            " 式神攻击触发附魔伤害（火焰附加、锋利等）。",
                            " 这由附魔系统处理，与武器代理系统分离。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_summon_enchant_trigger")
                    .define("EnableSummonEnchantTrigger", true);

            enableWeaponEffectProxy = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Weapon Effect Proxy] (Master Switch)",
                            " If true, JJK ability damage will trigger weapon effects",
                            " (item specials via hurtEnemy(), kill attribution, etc.).",
                            " ",
                            " This is the MASTER SWITCH. If disabled, the two options below",
                            " (Whitelist Mob / Shikigami) will also be ignored.",
                            "----------------------------------------------------------------",
                            " [启用武器效果代理] (总开关)",
                            " 若为真，JJK技能伤害会触发武器效果",
                            " （通过 hurtEnemy() 的物品特效、击杀归属等）。",
                            " ",
                            " 这是总开关。如果关闭，下方的两个子选项",
                            " （白名单生物/式神）也会被忽略。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_weapon_effect_proxy")
                    .define("EnableWeaponEffectProxy", true);

            enableItemHurtEnemyTrigger = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Item.hurtEnemy() Trigger]",
                            " If true, calls the weapon's hurtEnemy() method on ability hits.",
                            " Some mods use this method for weapon special effects.",
                            " Disable if you experience issues with specific weapons.",
                            "----------------------------------------------------------------",
                            " [启用 Item.hurtEnemy() 触发]",
                            " 若为真，技能命中时会调用武器的 hurtEnemy() 方法。",
                            " 某些模组通过此方法实现武器特效。",
                            " 如果特定武器出现问题，可关闭此选项。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_item_hurt_enemy_trigger")
                    .define("EnableItemHurtEnemyTrigger", true);

            enableWhitelistMobWeaponProxy = builder
                    .comment(" ",
                            "================================================================",
                            " [Whitelist Mob Weapon Proxy]",
                            " Mobs in Compatibility List trigger weapon effects with abilities.",
                            " Limited: Weapons using player-only Capability won't work.",
                            "----------------------------------------------------------------",
                            " [白名单生物武器代理]",
                            " 兼容列表中的生物用技能时触发武器效果。",
                            " 限制：使用玩家专属Capability的武器无效。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_whitelist_mob_weapon_proxy")
                    .define("EnableWhitelistMobWeaponProxy", true);

            enableShikigamiWeaponProxy = builder
                    .comment(" ",
                            "================================================================",
                            " [Shikigami Weapon Proxy] (Experimental, Limited Use)",
                            " Shikigami attacks trigger owner's weapon effects.",
                            " Most mod weapons (Cataclysm, etc.) will NOT work due to",
                            " internal checks. Only simple weapons may work.",
                            "----------------------------------------------------------------",
                            " [式神武器代理]（实验性，效果有限）",
                            " 式神攻击时触发主人的武器效果。",
                            " 大多数模组武器（灾厄等）因内部检查无法生效。",
                            " 仅简单武器可能有效。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_shikigami_weapon_proxy")
                    .define("EnableShikigamiWeaponProxy", false);
            hurtEnemyBlacklist = builder
                    .comment(" ",
                            "================================================================",
                            " [HurtEnemy Blacklist]",
                            " Items in this list will NOT have their hurtEnemy() called.",
                            " Use this to exclude weapons with complex attack effects.",
                            " ",
                            " Supports TWO formats:",
                            " 1. Registry Name (Recommended): 'cataclysm:tidal_claws'",
                            "    - Press F3+H in game, hover over item to see this",
                            " 2. Class Name: 'com.github.L_Ender.cataclysm.items.Tidal_Claws'",
                            " ",
                            " Partial match supported: 'cataclysm:' blocks ALL Cataclysm items",
                            "----------------------------------------------------------------",
                            " [hurtEnemy 黑名单]",
                            " 此列表中的物品不会调用 hurtEnemy() 方法。",
                            " ",
                            " 支持两种格式：",
                            " 1. 注册名（推荐）：'cataclysm:tidal_claws'",
                            "    - 游戏内按 F3+H，鼠标悬停物品即可看到",
                            " 2. 类名：'com.github.L_Ender.cataclysm.items.Tidal_Claws'",
                            " ",
                            " 支持部分匹配：填 'cataclysm:' 可屏蔽所有灾变物品",
                            "================================================================")
                    .translation("config.jujutsu_addon.hurt_enemy_blacklist")
                    .defineList("HurtEnemyBlacklist", Arrays.asList(
                            // 默认空白，让玩家自己添加
                    ), entry -> entry instanceof String);


            builder.pop();

            // ==========================================================================
            // 3. 暴击系统 (Critical Hits)
            // ==========================================================================
            // 心华提示：改为 03 开头
            builder.push("03_Critical_Hits");

            enableCritSystem = builder
                    .comment(" Enable Critical Hits for Skills.",
                            " If you have Apotheosis or similar mods installed,",
                            " it's recommended to keep this DISABLED to avoid",
                            " double crit calculation causing abnormal damage.",
                            " ",
                            " 启用技能暴击系统。",
                            " 如果安装了神化(Apotheosis)等mod，建议保持关闭，",
                            " 避免双重暴击判定导致伤害异常。")
                    .translation("config.jujutsu_addon.enable_crit")
                    .define("EnableCritSystem", false);

            baseCritChance = builder
                    .comment(" Base Crit Chance (0.05 = 5%).",
                            " 基础暴击率 (0.05 = 5%)。")
                    .translation("config.jujutsu_addon.base_crit_chance")
                    .defineInRange("BaseCritChance", 0.05, 0.0, 1.0);

            baseCritDamage = builder
                    .comment(" Base Crit Damage Multiplier (1.5 = 150%).",
                            " 基础暴击伤害倍率 (1.0 = 100%)。")
                    .translation("config.jujutsu_addon.base_crit_damage")
                    .defineInRange("BaseCritDamage", 1.0, 0.0, 100.0);

            builder.pop();

            // ==========================================================================
            // 4. 机制设置 (Mechanics & PVP)
            // ==========================================================================
            // 心华提示：改为 04 开头
            builder.push("04_Mechanics");

            debugMode = builder
                    .comment(" ",
                            "================================================================",
                            " [Debug Mode]",
                            " 1. Prints detailed damage calculation math.",
                            " 2. Prints the 'Class Name' of the skill used.",
                            "    (Use this name for 'IgnoreIframeSkills' or 'BalancerBenchmarks' configs)",
                            "----------------------------------------------------------------",
                            " [调试模式]",
                            " 1. 打印详细的伤害计算数学过程。",
                            " 2. 打印当前技能的【类名】（Class Name）。",
                            "    (你可以复制这个类名填入【无敌帧重置列表】或【动态倍率基准】配置中)",
                            "================================================================")
                    .translation("config.jujutsu_addon.debug_mode")
                    .define("DebugMode", false);

            enableDamageInterceptor = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable JJK Damage Interceptor]",
                            " Prevents other mods (like Projectile Damage mods) from",
                            " double-scaling JJK ability damage.",
                            " ",
                            " How it works:",
                            " - Records damage at HIGHEST priority (our calculated value)",
                            " - Restores damage at LOWEST priority (undo other mods' changes)",
                            " - Only affects JJK damage sources, not vanilla/other mod attacks",
                            "----------------------------------------------------------------",
                            " [启用JJK伤害拦截器]",
                            " 阻止其他mod（如投射物伤害mod）对JJK技能伤害的二次修改。",
                            " ",
                            " 工作原理：",
                            " - 以最高优先级记录伤害（我们计算后的值）",
                            " - 以最低优先级恢复伤害（撤销其他mod的修改）",
                            " - 只影响JJK伤害源，不影响原版/其他mod的攻击",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_damage_interceptor")
                    .define("EnableDamageInterceptor", true);

            enablePvpBalance = builder
                    .comment(" ",
                            "================================================================",
                            " Enable PvP damage balancing.",
                            " If true, applies the PvP Multiplier below.",
                            "----------------------------------------------------------------",
                            " 是否启用 PVP 平衡限制。",
                            " 若为真，则应用下方的 PVP 伤害倍率。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_pvp_balance")
                    .define("EnablePvpBalance", true);

            pvpDamageMultiplier = builder
                    .comment(" ",
                            " Damage multiplier when the target is a Player.",
                            " Default: 0.5 (50% damage to players).",
                            "----------------------------------------------------------------",
                            " [PVP] 当目标是玩家时的伤害倍率。",
                            " 默认为 0.5 (即只造成 50% 伤害)，防止PVP秒杀。",
                            "================================================================")
                    .translation("config.jujutsu_addon.pvp_multiplier")
                    .defineInRange("PvPDamageMultiplier", 0.5, 0.0, 10.0);

            ignoreAllIframes = builder
                    .comment(" If true, ALL skills will modify target's invulnerability frames.",
                            " 若为真，所有技能都将修改目标的无敌帧（使用下方的 Reset 逻辑）。")
                    .translation("config.jujutsu_addon.ignore_all_iframes")
                    .define("IgnoreAllIframes", false);

            singleHitProjectiles = builder
                    .comment(" ",
                            "================================================================",
                            " [Single Hit Projectiles]",
                            " Projectiles in this list will only damage each entity ONCE,",
                            " even if the projectile exists for multiple ticks.",
                            " ",
                            " [When to ADD a projectile here]",
                            " - Slash-type: Dismantle, WorldSlash (exist 10 ticks, hit every tick)",
                            " - Any projectile that should logically only hit once",
                            " ",
                            " [When NOT to add]",
                            " - Blue (苍): Designed as continuous AOE damage",
                            " - Red/Fireball: Already discard on hit (no need)",
                            " - HollowPurple: Continuous beam damage",
                            " ",
                            " Format: Simple class name or partial match",
                            " Example: 'DismantleProjectile' or just 'Dismantle'",
                            "----------------------------------------------------------------",
                            " [单次命中投射物]",
                            " 此列表中的投射物对每个实体只造成一次伤害，",
                            " 即使投射物存在多个 tick。",
                            " ",
                            " [何时添加]",
                            " - 斩击类：解(Dismantle)、界断(WorldSlash) - 存在10tick，每tick命中",
                            " - 任何逻辑上应该只命中一次的投射物",
                            " ",
                            " [何时不添加]",
                            " - 苍(Blue)：设计为持续AOE伤害",
                            " - 赫/火球：命中即消失（不需要）",
                            " - 茈(HollowPurple)：持续光束伤害",
                            " ",
                            " 格式：类名或部分匹配",
                            " 例如：'DismantleProjectile' 或只写 'Dismantle'",
                            "================================================================")
                    .translation("config.jujutsu_addon.single_hit_projectiles")
                    .defineList("SingleHitProjectiles", Arrays.asList(
                            "DismantleProjectile",
                            "WorldSlashProjectile"
                    ), entry -> entry instanceof String);

            ignoreIframeSkills = builder
                    .comment(" ",
                            "================================================================",
                            " [Iframe Reset Whitelist] (Rhythm Control)",
                            " Skills here have SHORTENED invulnerability duration.",
                            " Target still gets protection, but for fewer ticks.",
                            " ",
                            " Example: Default is 20 ticks (1 second).",
                            " If set to 7, target can be hit again after 7 ticks (0.35s).",
                            " ",
                            " Use for: DoT skills, channeled abilities",
                            "----------------------------------------------------------------",
                            " [无敌帧缩短白名单] (节奏控制)",
                            " 此列表中的技能会缩短目标的无敌帧持续时间。",
                            " 目标仍有保护，但时间更短。",
                            " ",
                            " 举例：原版默认20tick (1秒)。",
                            " 若设为7，则7tick后 (0.35秒) 可再次命中。",
                            " ",
                            " 适用于：持续伤害技能、引导类技能",
                            "================================================================")
                    .translation("config.jujutsu_addon.ignore_iframe")
                    .defineList("IgnoreIframeSkills", Arrays.asList(
                            "Dismantle"
                    ), entry -> true);

            iframeSetTo = builder
                    .comment(" ",
                            " [Reset Value]",
                            " The duration (in Ticks) to set the invulnerability timer to.",
                            " --------------------------------------------------------------",
                            " [重置数值]",
                            " 配合上方列表使用。命中后将无敌时间设定为多少 Tick。")
                    .translation("config.jujutsu_addon.iframe_set_to")
                    .defineInRange("IframeSetToTick", 5, 0, 20);

            iframeBypassWhitelist = builder
                    .comment(" ",
                            "================================================================",
                            " [Iframe Bypass Whitelist] (True Multi-Hit)",
                            " Skills here COMPLETELY IGNORE invulnerability frames.",
                            " Every single hit deals FULL damage, even within 1 tick.",
                            " ",
                            " Use for: Rapid-fire skills like Barrage, Blitz",
                            "----------------------------------------------------------------",
                            " [强制穿透白名单] (真·多段伤害)",
                            " 此列表中的技能完全无视无敌帧。",
                            " 每次命中都造成完整伤害，即使在1tick内。",
                            " ",
                            " 适用于：连击类技能如 Barrage, Blitz",
                            "================================================================")
                    .translation("config.jujutsu_addon.iframe_bypass_whitelist")
                    .defineList("IframeBypassWhitelist", Arrays.asList(
                            "Punch",
                            "Slam",
                            "Barrage",
                            "Blitz",
                            "BlueFists"
                    ), entry -> true);
            enableHealthToArmor = builder
                    .comment(" ",
                            "================================================================",
                            " [Health to Armor Conversion]",
                            " If true, Sorcerers will gain Armor/Toughness instead of Max Health from leveling.",
                            " This also prevents the 'Full Heal' effect on level up.",
                            "----------------------------------------------------------------",
                            " [血量转护甲模式]",
                            " 若为真，咒术师升级时将不再增加最大生命值，也不再回满血。",
                            " 而是根据下方的比例，将原本应增加的血量转化为护甲和韧性。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_health_conversion")
                    .define("EnableHealthToArmor", false);
            // --- 普通咒术师配置 (Sorcerer) ---
            sorcererHealthToArmorRatio = builder
                    .comment(" ",
                            " [Sorcerer] Conversion Ratio: Health -> Armor.",
                            " Example: 0.4 means 10 HP (5 Hearts) becomes 4 Armor.",
                            "----------------------------------------------------------------",
                            " [普通咒术师] 转化比例：血量 -> 护甲。",
                            " 例如：0.4 表示原本增加 10点血，现在改为增加 4点护甲。")
                    .translation("config.jujutsu_addon.sorcerer_health_to_armor")
                    .defineInRange("SorcererHealthToArmorRatio", 0.4, 0.0, 10.0);
            sorcererHealthToToughnessRatio = builder
                    .comment(" ",
                            " [Sorcerer] Conversion Ratio: Health -> Armor Toughness.",
                            " Example: 0.2 means 10 HP becomes 2 Toughness.",
                            "----------------------------------------------------------------",
                            " [普通咒术师] 转化比例：血量 -> 护甲韧性。",
                            " 例如：0.2 表示原本增加 10点血，现在改为增加 2点韧性。")
                    .translation("config.jujutsu_addon.sorcerer_health_to_toughness")
                    .defineInRange("SorcererHealthToToughnessRatio", 0.2, 0.0, 100.0);
            // --- 天与咒缚配置 (Heavenly Restriction) ---
            hrHealthToArmorRatio = builder
                    .comment(" ",
                            " [Heavenly Restriction] Conversion Ratio: Health -> Armor.",
                            "----------------------------------------------------------------",
                            " [天与咒缚] 转化比例：血量 -> 护甲。")
                    .translation("config.jujutsu_addon.hr_health_to_armor")
                    .defineInRange("HRHealthToArmorRatio", 0.6, 0.0, 100.0);
            hrHealthToToughnessRatio = builder
                    .comment(" ",
                            " [Heavenly Restriction] Conversion Ratio: Health -> Armor Toughness.",
                            "----------------------------------------------------------------",
                            " [天与咒缚] 转化比例：血量 -> 护甲韧性。")
                    .translation("config.jujutsu_addon.hr_health_to_toughness")
                    .defineInRange("HRHealthToToughnessRatio", 0.3, 0.0, 100.0);
            enableMobHealthToArmor = builder
                    .comment(" ",
                            "================================================================",
                            " [Mob Health to Armor Conversion]",
                            " Separate control for whitelisted mobs (e.g. Maids).",
                            " If true, mobs will convert their grade-based HP bonus into Armor.",
                            "----------------------------------------------------------------",
                            " [生物血量转护甲开关]",
                            " 针对白名单生物（如女仆）的独立控制开关。",
                            " 若为真，生物升级获得的额外血量将转化为护甲。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_mob_health_to_armor") // 新增
                    .define("EnableMobHealthToArmor", false);

            mobHealthToArmorRatio = builder
                    .comment(" [Mob] Conversion Ratio: Health -> Armor.",
                            " [生物] 转化比例：血量 -> 护甲。")
                    .translation("config.jujutsu_addon.mob_health_to_armor_ratio") // 新增
                    .defineInRange("MobHealthToArmorRatio", 0.5, 0.0, 100.0);

            mobHealthToToughnessRatio = builder
                    .comment(" [Mob] Conversion Ratio: Health -> Armor Toughness.",
                            " [生物] 转化比例：血量 -> 护甲韧性。")
                    .translation("config.jujutsu_addon.mob_health_to_toughness_ratio") // 新增
                    .defineInRange("MobHealthToToughnessRatio", 0.25, 0.0, 100.0);

            mobHRHealthToArmorRatio = builder
                    .comment(" ",
                            " [Mob - Heavenly Restriction] Conversion Ratio: Health -> Armor.",
                            " Separate ratio for mobs with Heavenly Restriction trait.",
                            "----------------------------------------------------------------",
                            " [生物 - 天与咒缚] 转化比例：血量 -> 护甲。",
                            " 针对拥有天与咒缚特质的生物的独立转化率。")
                    .translation("config.jujutsu_addon.mob_hr_health_to_armor_ratio")
                    .defineInRange("MobHRHealthToArmorRatio", 0.8, 0.0, 100.0);

            mobHRHealthToToughnessRatio = builder
                    .comment(" ",
                            " [Mob - Heavenly Restriction] Conversion Ratio: Health -> Armor Toughness.",
                            " Separate ratio for mobs with Heavenly Restriction trait.",
                            "----------------------------------------------------------------",
                            " [生物 - 天与咒缚] 转化比例：血量 -> 护甲韧性。",
                            " 针对拥有天与咒缚特质的生物的独立转化率。")
                    .translation("config.jujutsu_addon.mob_hr_health_to_toughness_ratio")
                    .defineInRange("MobHRHealthToToughnessRatio", 0.4, 0.0, 100.0);

            playerAdaptationEnabled = builder
                    .comment(" Enable Mahoraga-style adaptation mechanics for players (Damage Reduction & Immunity).",
                            "----------------------------------------------------------------",
                            " 开启玩家的魔虚罗式适应机制（包含渐进减伤和完全免疫）。")
                    .translation("config.jujutsu_addon.player_adaptation_enabled")
                    .define("PlayerAdaptationEnabled", false);
            playerAutoCounter = builder
                    .comment(" Enable auto-counter attack when player is adapted to melee attacks.",
                            "----------------------------------------------------------------",
                            " 开启玩家适应近战攻击后的自动反击机制。")
                    .translation("config.jujutsu_addon.player_auto_counter")
                    .define("PlayerAutoCounter", false);
            playerTechniqueDisruption = builder
                    .comment(" Enable disrupting enemy techniques (e.g. Infinity) when player is adapted.",
                            "----------------------------------------------------------------",
                            " 开启玩家适应后，攻击可打断对方术式（如无下限）。")
                    .translation("config.jujutsu_addon.player_technique_disruption")
                    .define("PlayerTechniqueDisruption", false);

            clearAdaptationOnDeath = builder
                    .comment(" ",
                            "================================================================",
                            " [Clear Adaptation on Death]",
                            " Whether to clear Mahoraga's adaptation status when the player dies.",
                            "----------------------------------------------------------------",
                            " [死亡清除适应性]",
                            " 是否在玩家死亡时清除魔虚罗的适应性状态。",
                            "================================================================")
                    .translation("config.jujutsu_addon.clear_adaptation_on_death")
                    .define("ClearAdaptationOnDeath", true);

            builder.pop();

            // ==========================================================================
            // 5. 召唤物增强 (Summon Scaling)
            // ==========================================================================
            // 心华提示：改为 05 开头
            builder.push("05_Summon_Scaling");

            enableSummonScaling = builder
                    .comment(" Enable scaling for summons.",
                            " 启用召唤物属性成长。")
                    .translation("config.jujutsu_addon.enable_summon_scaling")
                    .define("EnableSummonScaling", true);

            summonHpRatio = builder
                    .comment(" Summon Max HP += Owner Max HP * Ratio.",
                            " 召唤物额外血量 = 主人最大血量 * 此系数。")
                    .translation("config.jujutsu_addon.summon_hp_ratio")
                    .defineInRange("SummonHpRatio", 0.5, 0.0, 100.0);

            summonAtkRatio = builder
                    .comment(" Summon Attack += Owner Attack * Ratio.",
                            " 召唤物额外攻击 = 主人攻击力 * 此系数。")
                    .translation("config.jujutsu_addon.summon_atk_ratio")
                    .defineInRange("SummonAtkRatio", 0.5, 0.0, 100.0);

            summonWhitelist = builder
                    .comment(" ",
                            "================================================================",
                            " List of Entity Class Names or IDs to apply scaling.",
                            "----------------------------------------------------------------",
                            " 召唤物增强白名单。",
                            "================================================================")
                    .translation("config.jujutsu_addon.summon_whitelist")
                    .defineList("SummonWhitelist", Arrays.asList(
                            "radon.jujutsu_kaisen.entity.ten_shadows",
                            "radon.jujutsu_kaisen.entity.idle_transfiguration",
                            "radon.jujutsu_kaisen.entity.curse.RikaEntity",
                            "radon.jujutsu_kaisen.entity.curse"
                    ), entry -> true);

            // [已删除] customBenchmarkCosts
            // [已删除] benchmarkEntityId
            // [已删除] tierScalingExponent

            summonAttributeInheritance = builder
                    .comment(" ",
                            "================================================================",
                            " List of attributes to inherit from owner (1:1 copy).",
                            " Format: modid:attribute",
                            "----------------------------------------------------------------",
                            " 召唤物继承属性列表 (1:1 复制主人的属性值)。",
                            "================================================================")
                    .translation("config.jujutsu_addon.summon_inherit")
                    .defineList("SummonAttributeInheritance", Arrays.asList(
                            "minecraft:generic.armor",              // 护甲
                            "minecraft:generic.armor_toughness",    // 韧性
                            "minecraft:generic.knockback_resistance", // 抗击退
                            "minecraft:generic.luck"                // 幸运
                    ), entry -> true);

            summonDpsCompensationFactor = builder
                    .comment(" ",
                            "================================================================",
                            " [DPS Compensation Factor]",
                            " Converts player Attack Speed into raw Damage for summons.",
                            " Formula: Base Damage = Player Dmg * Player Atk Speed * Factor",
                            " Default: 0.5 (Balanced). Set to 1.0 for full DPS inheritance.",
                            " Set to 0.0 to disable DPS scaling and use raw Damage only.",
                            "----------------------------------------------------------------",
                            " [DPS 补偿系数]",
                            " 将玩家的攻击速度转化为召唤物的攻击力面板，以补偿召唤物攻速慢的问题。",
                            " 公式：计算基数 = 玩家面板 * 玩家攻速 * 系数",
                            " 默认：0.5 (平衡)。设为 1.0 则完全继承秒伤。",
                            " 设为 0.0 则关闭此功能，仅继承单次攻击面板。",
                            "================================================================")
                    .translation("config.jujutsu_addon.summon_dps_factor")
                    .defineInRange("SummonDpsCompensationFactor", 0.5, 0.0, 100.0);
            autoAttributeInheritanceRatio = builder
                    .comment(" ",
                            "================================================================",
                            " [Auto Attribute Inheritance Ratio]",
                            " Automatically inherits ALL utility attributes (Armor, Luck, Crit, etc.).",
                            " This allows your summons to benefit from your gear and other mods.",
                            " ",
                            " ! IMPORTANT NOTE ! ",
                            " This setting explicitly IGNORES 'Max Health' and 'Attack Damage'.",
                            " Why? Because those are handled by the specific Ratios above.",
                            " This prevents double-stacking stats and allows for better balance.",
                            " ",
                            " 0.0 = Disabled (Use manual whitelist).",
                            " 1.0 = Inherit 100% of player's value (Recommended).",
                            "----------------------------------------------------------------",
                            " [智能属性继承比例]",
                            " 自动继承玩家的所有功能性属性（如护甲、韧性、幸运、暴击率、模组新增属性等）。",
                            " 这让你的式神能享受到你的装备附魔和其他模组带来的加成。",
                            " ",
                            " ! 重要提示 ! ",
                            " 此设置会自动【跳过/忽略】‘最大生命值’和‘攻击力’。",
                            " 为什么？因为这两个属性由上方的专用倍率（HpRatio/AtkRatio）独立控制。",
                            " 这样设计是为了防止属性重复叠加，并允许你单独调整血量攻击的平衡。",
                            " ",
                            " 0.0 = 禁用 (转而使用手动白名单)。",
                            " 1.0 = 继承玩家数值的 100% (推荐，即 1:1 复制你的护甲/暴击等)。",
                            "================================================================")
                    .translation("config.jujutsu_addon.auto_inherit_ratio")
                    .defineInRange("AutoAttributeInheritanceRatio", 1.0, 0.0, 10000.0);
            autoAttributeBlacklist = builder
                    .comment(" ",
                            "================================================================",
                            " [Auto Inheritance Blacklist]",
                            " Attributes to ignore when 'EnableAutoAttributeInheritance' is true.",
                            " Use this to prevent inheriting attributes that might break AI.",
                            "----------------------------------------------------------------",
                            " [智能继承黑名单]",
                            " 开启智能继承时，强制忽略的属性ID列表。",
                            " 用于防止继承某些可能破坏AI的属性（如移动速度）。",
                            "================================================================")
                    .translation("config.jujutsu_addon.auto_inherit_blacklist")
                    .defineList("AutoAttributeBlacklist", Arrays.asList(
                            "minecraft:generic.movement_speed",
                            "minecraft:generic.flying_speed",
                            "minecraft:horse.jump_strength"
                    ), entry -> entry instanceof String);

            swarmScalingModifier = builder
                    .comment(" ",
                            "================================================================",
                            " [Swarm Penalty Modifier]",
                            " Multiplier applied to 'Swarm Entities' (like Rabbit Escape).",
                            "----------------------------------------------------------------",
                            " [群体召唤物惩罚系数]",
                            " 应用于‘群体实体’（如脱兔）的额外乘区。",
                            "================================================================")
                    .translation("config.jujutsu_addon.swarm_modifier")
                    .defineInRange("SwarmScalingModifier", 0.05, 0.0, 1.0);

            swarmEntityList = builder
                    .comment(" List of entities considered as 'Swarm'.",
                            " 这些实体将被视为‘群体召唤物’并应用惩罚系数。")
                    .translation("config.jujutsu_addon.swarm_list")
                    .defineList("SwarmEntityList", Arrays.asList(
                            "radon.jujutsu_kaisen.entity.ten_shadows.RabbitEscapeEntity"
                    ), entry -> true);

            enableTierScaling = builder
                    .comment(" ",
                            "================================================================",
                            " [Tier Scaling System]",
                            " Automatically calculates scaling weight based on a Benchmark Entity.",
                            "----------------------------------------------------------------",
                            " [阶级权重系统]",
                            " 自动根据【基准实体】的属性计算当前召唤物的强度权重。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_tier_scaling")
                    .define("EnableTierScaling", true);

            minimumTierMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Minimum Tier Multiplier]",
                            " The floor value for the tier multiplier.",
                            " If calculated multiplier is lower than this, it will be raised to this value.",
                            " Set to 1.0 if you want weak summons to still get 100% of the bonus.",
                            "----------------------------------------------------------------",
                            " [最小阶级倍率保底]",
                            " 阶级倍率的下限值。",
                            " 如果计算出的倍率低于此数值，将被强制提升为此数值。",
                            " 如果你想让弱小的召唤物（如小型改造人）也能获得 100% 的加成，请设为 1.0。",
                            "================================================================")
                    .translation("config.jujutsu_addon.min_tier_multiplier")
                    .defineInRange("MinimumTierMultiplier", 1.0, 0.0, 10.0);

            summonRegroupDistance = builder
                    .comment(" ",
                            "================================================================",
                            " [Summon Regroup Distance]",
                            " If the summon is further than this distance (blocks) from the owner,",
                            " it will stop attacking and run back to the owner.",
                            " Default: 32.0 blocks.",
                            "----------------------------------------------------------------",
                            " [召唤物强制回防距离]",
                            " 如果召唤物与主人的距离超过此数值（格），",
                            " 它将强制停止攻击并跑回主人身边。",
                            " 默认: 32.0 格。",
                            "================================================================")
                    .translation("config.jujutsu_addon.summon_regroup_distance")
                    .defineInRange("SummonRegroupDistance", 32.0, 0.0, 256.0);

            enableUntamedStatScaling = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Untamed Stat Scaling]",
                            " If true, untamed summons (ritual entities) will benefit from the EXACT SAME attribute scaling logic as tamed ones.",
                            " This means they will inherit stats based on the owner's power (Tier/Swarm multipliers).",
                            "----------------------------------------------------------------",
                            " [启用未调伏式神属性缩放]",
                            " 如果为true，未调伏的式神（仪式怪）将享受与已调伏式神【完全相同】的属性缩放计算。",
                            " 这意味着它们会根据召唤者的强度（阶级/数量倍率）获得大幅属性加成。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_untamed_stat_scaling")
                    .define("EnableUntamedStatScaling", true);
            enableUntamedEquipSync = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Untamed Equipment Sync]",
                            " If true, untamed summons (ritual entities) will copy the owner's armor and held items.",
                            " WARNING: This acts independently of Stat Scaling. Enabling both creates a nightmare difficulty boss.",
                            "----------------------------------------------------------------",
                            " [启用未调伏式神装备同步]",
                            " 如果为true，未调伏的式神（仪式怪）会直接复制主人的护甲和手持物品。",
                            " 警告：此选项独立于属性缩放。如果两者同时开启，BOSS将拥有神装+高倍率面板，难度极高。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_untamed_equip_sync")
                    .define("EnableUntamedEquipSync", true);

            manualTargetDistanceMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Manual Target Distance Multiplier]",
                            " Multiplies the allowed distance from owner when attacking a MANUALLY targeted enemy (Shift+Right Click).",
                            " Default: 3.0 (Allows summons to chase manual targets 3x further than normal).",
                            " Prevents summons from teleporting back/losing aggro when chasing manual targets.",
                            "----------------------------------------------------------------",
                            " [手动目标追击距离倍率]",
                            " 当攻击【手动指定】(Shift+右键) 的目标时，允许式神离开主人的最大距离倍率。",
                            " 默认: 3.0 (允许追击距离是平时的 3 倍)。",
                            " 作用：防止式神在追击手动指定的目标时，因为距离主人太远而强制拉脱/传送回来。",
                            "================================================================")
                    .translation("config.jujutsu_addon.manual_target_dist_mult")
                    .defineInRange("ManualTargetDistanceMultiplier", 3.0, 1.0, 20.0);
            builder.pop();

            // ==========================================================================
            // 6. 天与咒缚专属 (Heavenly Restriction)
            // ==========================================================================
            // 心华提示：改为 06 开头
            builder.push("06_Heavenly_Restriction_Items");

            heavenlyRestrictionBonus = builder
                    .comment(" ",
                            "================================================================",
                            " [Heavenly Restriction Specific Weapon Bonus]",
                            " Defines the damage multiplier when using specific weapons.",
                            "----------------------------------------------------------------",
                            " [天与咒缚 - 特定武器加成]",
                            " 定义拥有天与咒缚时，使用特定武器的伤害倍率。",
                            "================================================================")
                    .translation("config.jujutsu_addon.hr_bonus")
                    .defineList("HeavenlyRestrictionBonus", Arrays.asList(
                            "radon.jujutsu_kaisen:split_soul_katana=1.0" // 释魂刀默认1.0，全靠真伤
                    ), entry -> true);

            sskDamageCorrection = builder
                    .comment(" ",
                            "================================================================",
                            " [Split Soul Katana Damage Correction]",
                            " A hidden multiplier to counteract the Main Mod's internal 1.65x boost.",
                            " Default: 0.6 (Calculated as 1 / 1.65).",
                            " Set to 1.0 to disable this fix (revert to high damage).",
                            "----------------------------------------------------------------",
                            " [释魂刀伤害修正系数]",
                            " 一个隐形的修正倍率，用于抵消主模组内部自带的 1.65倍 增伤。",
                            " 默认: 0.6 (即 1 除以 1.65)。",
                            " 设为 1.0 则禁用此修正（恢复原本的超高伤害）。",
                            "================================================================")
                    .translation("config.jujutsu_addon.ssk_damage_correction")
                    .defineInRange("SSKDamageCorrection", 0.6, 0.0, 10.0);

            defaultHeavenlyRestrictionMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Heavenly Restriction Default Multiplier]",
                            " Multiplier applied to ANY other item held by a HR user.",
                            "----------------------------------------------------------------",
                            " [天与咒缚 - 默认通用倍率]",
                            " 当天与咒缚使用者手持任何其他未在上方定义的物品时，应用的默认倍率。",
                            "================================================================")
                    .translation("config.jujutsu_addon.hr_default_multiplier")
                    .defineInRange("DefaultHeavenlyRestrictionMultiplier", 1.0, 1.0, 100.0);

            enableSoulTrueDamage = builder
                    .comment(" ",
                            "================================================================",
                            " [Soul Damage - Armor Bypass Ratio]",
                            " Percentage of Armor & Enchantment reduction to ignore.",
                            " 0.0 = Normal Damage (Respects armor).",
                            " 0.5 = Bypass 50% of armor reduction.",
                            " 1.0 = True Damage (Ignores armor completely).",
                            " --------------------------------------------------------------",
                            " [灵魂伤害 - 护甲穿透比例]",
                            " 灵魂伤害无视护甲和保护附魔减伤的百分比。",
                            "================================================================")
                    .translation("config.jujutsu_addon.soul_armor_bypass")
                    .defineInRange("SoulArmorBypass", 1.0, 0.0, 1.0);

            playfulCloudAttributeBonus = builder
                    .comment(" ",
                            "================================================================",
                            " [Playful Cloud Attribute Bonus]",
                            " Percentage increase to Attack Damage when holding Playful Cloud.",
                            " 0.3 = +30% Attack Damage (Rabadon's Deathcap effect).",
                            "----------------------------------------------------------------",
                            " [游云属性加成]",
                            " 手持游云时，攻击力面板的百分比加成。",
                            " 0.3 代表增加 30% 攻击力（类似帽子的效果），体现臂力加成。",
                            "================================================================")
                    .translation("config.jujutsu_addon.playful_cloud_bonus")
                    .defineInRange("PlayfulCloudAttributeBonus", 0.3, 0.0, 10.0);

            soulResistanceBypass = builder
                    .comment(" ",
                            "================================================================",
                            " [Soul Damage - Resistance Bypass]",
                            " Percentage of Resistance (Potion Effect) to ignore.",
                            "----------------------------------------------------------------",
                            " [灵魂伤害 - 抗性穿透比例]",
                            " 灵魂伤害穿透【抗性提升(Resistance)】药水效果的百分比。",
                            "================================================================")
                    .translation("config.jujutsu_addon.soul_resistance_bypass")
                    .defineInRange("SoulResistanceBypass", 1.0, 0.0, 1.0);


            armorPenetrationAttributes = builder
                    .comment(" ",
                            "================================================================",
                            " True Armor Penetration Attributes (Flat Value).",
                            " Format: modid:attribute=multiplier",
                            "----------------------------------------------------------------",
                            " 真实护甲穿透属性 (固定数值)。",
                            " 格式：模组ID:属性名=倍率",
                            "================================================================")
                    .translation("config.jujutsu_addon.armor_penetration")
                    .defineList("ArmorPenetrationAttributes", Arrays.asList(

                    ), entry -> true);

            builder.pop();

            // ==========================================================================
            // 7. 攻速收益 (Attack Speed Scaling)
            // ==========================================================================
            // 心华提示：改为 07 开头
            builder.push("07_Attack_Speed_Scaling");

            hrAttackSpeedScaling = builder
                    .comment(" ",
                            "================================================================",
                            " [HR Attack Speed Scaling]",
                            " Damage bonus multiplier per point of attack speed above 4.0 for Heavenly Restriction.",
                            " Default: 0.1 (10% damage increase per 1.0 speed).",
                            "----------------------------------------------------------------",
                            " [天与咒缚 - 攻速收益系数]",
                            " 攻速每超过 4.0 一点，伤害增加的倍率。",
                            " 默认 0.1 代表每点攻速增加 10% 最终伤害。",
                            "================================================================")
                    .translation("config.jujutsu_addon.hr_speed_scaling")
                    .defineInRange("HRAttackSpeedScaling", 0.1, 0.0, 10.0);

            sorcererAttackSpeedScaling = builder
                    .comment(" ",
                            "================================================================",
                            " [Sorcerer Attack Speed Scaling]",
                            " Damage bonus multiplier per point of attack speed above 4.0 for Sorcerers.",
                            " Default: 0.02 (2% damage increase per 1.0 speed).",
                            "----------------------------------------------------------------",
                            " [普通咒术师 - 攻速收益系数]",
                            " 攻速每超过 4.0 一点，伤害增加的倍率。",
                            " 默认 0.02 代表每点攻速仅增加 2% 最终伤害，防止数值崩坏。",
                            "================================================================")
                    .translation("config.jujutsu_addon.sorcerer_speed_scaling")
                    .defineInRange("SorcererAttackSpeedScaling", 0.02, 0.0, 10.0);

            builder.pop();

            // ==========================================================================
// 8. 技能动态平衡 (Skill Balancer)
// ==========================================================================
            builder.push("08_Skill_Balancer");
            enableSkillBalancer = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Skill Balancer]",
                            " Auto-adjusts skill damage based on cost. Uses new category system.",
                            " Skills are grouped by (Category + Technique), lowest cost = benchmark.",
                            " Multiplier = √(CurrentCost / BenchmarkCost), minimum 1.0",
                            "----------------------------------------------------------------",
                            " [启用技能平衡器]",
                            " 根据消耗自动调整技能伤害。使用新的分类系统。",
                            " 技能按 (分类 + 术式) 分组，消耗最低的作为基准。",
                            " 倍率 = √(当前消耗 / 基准消耗)，最低 1.0",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_skill_balancer")
                    .define("EnableSkillBalancer", true);
            balancerMaxMultiplier = builder
                    .comment(" Maximum multiplier cap. | 最大倍率上限。")
                    .translation("config.jujutsu_addon.balancer_max_multiplier")
                    .defineInRange("BalancerMaxMultiplier", 100.0, 1.0, 10000.0);

            balancerScalingExponent = builder
                    .comment(" ",
                            "================================================================",
                            " [Balancer Scaling Exponent]",
                            " Controls how cost ratio translates to damage multiplier.",
                            " ",
                            " Formula: multiplier = (currentCost / benchmarkCost) ^ exponent",
                            " ",
                            " [Values]",
                            " 0.0 = Disabled (all skills get 1.0x, no scaling)",
                            " 0.5 = Square root (gentler curve, less extreme differences)",
                            " 1.0 = Linear (direct ratio, recommended)",
                            " 2.0 = Quadratic (aggressive scaling, big gap between weak/strong)",
                            " ",
                            " [Example] Assume benchmark=4.0, current skill cost=16.0 (ratio=4.0):",
                            " - Exponent 0.5: 4^0.5 = 2.0x multiplier",
                            " - Exponent 1.0: 4^1.0 = 4.0x multiplier",
                            " - Exponent 1.5: 4^1.5 = 8.0x multiplier",
                            "----------------------------------------------------------------",
                            " [平衡器缩放指数]",
                            " 控制消耗比值如何转换为伤害倍率。",
                            " ",
                            " 公式：倍率 = (当前消耗 / 基准消耗) ^ 指数",
                            " ",
                            " [数值说明]",
                            " 0.0 = 禁用（所有技能都是 1.0x，不缩放）",
                            " 0.5 = 开方（曲线平缓，差异较小）",
                            " 1.0 = 线性（直接比值，推荐）",
                            " 2.0 = 平方（激进缩放，强弱差距大）",
                            " ",
                            " [示例] 假设基准=4.0，当前技能消耗=16.0（比值=4.0）：",
                            " - 指数 0.5: 4的0.5次方 = 2.0倍",
                            " - 指数 1.0: 4的1.0次方 = 4.0倍",
                            " - 指数 1.5: 4的1.5次方 = 8.0倍",
                            "================================================================")
                    .translation("config.jujutsu_addon.balancer_scaling_exponent")
                    .defineInRange("BalancerScalingExponent", 0.5, 0.0, 3.0);
// ===== 排除规则 =====
            balancerExcludedSkills = builder
                    .comment(" Skills excluded from balancing (class simple names).",
                            " 排除的技能（类名简称）。")
                    .translation("config.jujutsu_addon.balancer_excluded_skills")
                    .defineList("BalancerExcludedSkills", Arrays.asList(
                            "Blitz", "Barrage", "Punch", "Slam"
                    ), entry -> entry instanceof String);
            rctKeywords = builder
                    .comment(" Keywords to identify RCT skills (excluded). Case-insensitive.",
                            " 反转术式关键词（会被排除）。不区分大小写。")
                    .translation("config.jujutsu_addon.rct_keywords")
                    .defineList("RCTKeywords", Arrays.asList(
                            "rct", "heal", "reverse", "recovery"
                    ), entry -> entry instanceof String);
// ===== 基准覆盖（可选，留空则自动检测）=====
            instantBenchmark = builder
                    .comment(" [Optional] Override for INSTANT category. Leave empty for auto-detect.",
                            " [可选] 瞬发类基准覆盖。留空则自动检测。")
                    .translation("config.jujutsu_addon.instant_benchmark")
                    .define("InstantBenchmark", "");

            toggledBenchmark = builder
                    .comment(" [Optional] Override for TOGGLED category.",
                            " [可选] 切换类基准覆盖。")
                    .translation("config.jujutsu_addon.toggled_benchmark")
                    .define("ToggledBenchmark", "");

            channeledBenchmark = builder
                    .comment(" [Optional] Override for CHANNELED category.",
                            " [可选] 引导类基准覆盖。")
                    .translation("config.jujutsu_addon.channeled_benchmark")
                    .define("ChanneledBenchmark", "");

            attackBenchmark = builder
                    .comment(" [Optional] Override for ATTACK category (IAttack skills).",
                            " [可选] 攻击类基准覆盖（蓝拳等）。")
                    .translation("config.jujutsu_addon.attack_benchmark")
                    .define("AttackBenchmark", "");

            summonTamedBenchmark = builder
                    .comment(" [Optional] Override for SUMMON_TAMED category.",
                            " [可选] 调伏式神基准覆盖。")
                    .translation("config.jujutsu_addon.summon_tamed_benchmark")
                    .define("SummonTamedBenchmark", "");

            summonUntamedBenchmark = builder
                    .comment(" [Optional] Override for SUMMON_UNTAMED category.",
                            " [可选] 未调伏式神基准覆盖。")
                    .translation("config.jujutsu_addon.summon_untamed_benchmark")
                    .define("SummonUntamedBenchmark", "");

            summonInstantBenchmark = builder
                    .comment(" [Optional] Override for SUMMON_INSTANT category.",
                            " [可选] 瞬发召唤基准覆盖。")
                    .translation("config.jujutsu_addon.summon_instant_benchmark")
                    .define("SummonInstantBenchmark", "");

// ===== 无消耗召唤物 =====
            zeroCostSummonMultiplier = builder
                    .comment(" Default multiplier for zero-cost summons (e.g., Cursed Spirit Manipulation).",
                            " 无消耗召唤物（如咒灵操术）的默认倍率。")
                    .translation("config.jujutsu_addon.zero_cost_summon_mult")
                    .defineInRange("ZeroCostSummonMultiplier", 1.0, 0.0, 100.0);

            zeroCostSummonOverrides = builder
                    .comment(" Override for specific zero-cost summons. Format: ClassName=Multiplier",
                            " 特定无消耗召唤物覆盖。格式: 类名=倍率")
                    .translation("config.jujutsu_addon.zero_cost_overrides")
                    .defineList("ZeroCostSummonOverrides", Arrays.asList(), entry -> entry instanceof String);

// ===== 旧版兼容（可删除）=====
            balancerBenchmarks = builder
                    .comment(" [Legacy] Per-technique benchmarks. New system auto-detects, this is optional override.",
                            " [旧版兼容] 按术式的基准。新系统自动检测，此为可选覆盖。")
                    .translation("config.jujutsu_addon.balancer_benchmarks")
                    .defineList("BalancerBenchmarks", Arrays.asList(), entry -> entry instanceof String);
            builder.pop();


            // ==========================================================================
            // 9. 角色特化平衡 (Character Specific)
            // ==========================================================================
            // 心华提示：改为 09 开头
            builder.push("09_Character_Specific");
            // --- 东堂葵 (Todo Aoi) ---
            todoMeleeMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Todo Aoi Melee Multiplier]",
                            " Specific multiplier for Todo Aoi's melee attacks (Boogie Woogie user).",
                            " Since he relies on melee but uses Sorcerer scaling, he might need a buff.",
                            "----------------------------------------------------------------",
                            " [东堂葵体术倍率]",
                            " 东堂葵（不义游戏使用者）体术攻击的专属倍率。",
                            " 因为他只能用体术但吃的是咒术师那被削弱的系数，可能需要补偿。",
                            "================================================================")
                    .translation("config.jujutsu_addon.todo_melee_multiplier")
                    .defineInRange("TodoMeleeMultiplier", 1.5, 0.0, 100.0);

            todoMeleePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [Todo Aoi Melee Preservation]",
                            " Ratio of original damage preserved for Todo's melee attacks.",
                            " 1.0 = 100% original damage kept.",
                            "----------------------------------------------------------------",
                            " [东堂葵体术保留比例]",
                            " 东堂葵体术攻击的原伤害保留比例。",
                            " 1.0 代表保留 100% 原伤害。",
                            "================================================================")
                    .translation("config.jujutsu_addon.todo_melee_preservation")
                    .defineInRange("TodoMeleePreservation", 1.0, 0.0, 100.0);
            todoTechniqueMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Todo Aoi Technique Multiplier]",
                            " Just in case he gets a damaging technique or uses Boogie Woogie for damage.",
                            "----------------------------------------------------------------",
                            " [东堂葵术式倍率]",
                            " 以防万一他获得了有伤害的术式，或者不义游戏造成伤害时的倍率。",
                            "================================================================")
                    .translation("config.jujutsu_addon.todo_technique_multiplier")
                    .defineInRange("TodoTechniqueMultiplier", 1.0, 0.0, 100.0);

            todoTechniquePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [Todo Aoi Technique Preservation]",
                            " Ratio of original damage preserved for Todo's technique.",
                            "----------------------------------------------------------------",
                            " [东堂葵术式保留比例]",
                            " 东堂葵术式攻击的原伤害保留比例。",
                            "================================================================")
                    .translation("config.jujutsu_addon.todo_technique_preservation")
                    .defineInRange("TodoTechniquePreservation", 1.0, 0.0, 100.0);
            // --- 无术式者 (No Technique) ---
            noTechniqueMeleeMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [No Technique Melee Multiplier]",
                            " Multiplier for Sorcerers with NO Cursed Technique (e.g. Yuji, Kusakabe).",
                            " Since they rely purely on martial arts, they shouldn't be nerfed like technique users.",
                            "----------------------------------------------------------------",
                            " [无术式体术倍率]",
                            " 针对没有生得术式的咒术师（如虎杖、日下部）的体术倍率。",
                            " 因为他们纯靠体术，不应该像术式持有者那样被削弱。",
                            "================================================================")
                    .translation("config.jujutsu_addon.no_technique_melee_multiplier")
                    .defineInRange("NoTechniqueMeleeMultiplier", 2.0, 0.0, 100.0);

            noTechniqueMeleePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [No Technique Melee Preservation]",
                            " Ratio of original damage preserved for No-Technique users.",
                            "----------------------------------------------------------------",
                            " [无术式体术保留比例]",
                            " 无术式咒术师体术攻击的原伤害保留比例。",
                            "================================================================")
                    .translation("config.jujutsu_addon.no_technique_melee_preservation")
                    .defineInRange("NoTechniqueMeleePreservation", 1.0, 0.0, 100.0);
            // --- 幻兽琥珀 (Mythical Beast Amber) ---
            mbaMeleeMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Mythical Beast Amber Melee Multiplier]",
                            " Multiplier when Kashimo activates MBA (Transformation).",
                            " Since this is a suicide move, it deserves high damage.",
                            "----------------------------------------------------------------",
                            " [幻兽琥珀体术倍率]",
                            " 鹿紫云一开启幻兽琥珀（变身）后的体术倍率。",
                            " 既然是拼命的大招，理应拥有极高的伤害。",
                            "================================================================")
                    .translation("config.jujutsu_addon.mba_melee_multiplier")
                    .defineInRange("MBAMeleeMultiplier", 4.5, 0.0, 100.0);
            mbaMeleePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [Mythical Beast Amber Melee Preservation]",
                            " Ratio of original damage preserved for MBA melee attacks.",
                            "----------------------------------------------------------------",
                            " [幻兽琥珀体术保留比例]",
                            " 幻兽琥珀状态下体术攻击的原伤害保留比例。",
                            "================================================================")
                    .translation("config.jujutsu_addon.mba_melee_preservation")
                    .defineInRange("MBAMeleePreservation", 2.0, 0.0, 100.0);
            mbaTechniqueMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Mythical Beast Amber Technique Multiplier]",
                            " Multiplier for MBA specific skills (Electric Beam, EMF Blast).",
                            "----------------------------------------------------------------",
                            " [幻兽琥珀术式倍率]",
                            " 幻兽琥珀专属技能（如电击光束、电磁爆破）的伤害倍率。",
                            "================================================================")
                    .translation("config.jujutsu_addon.mba_technique_multiplier")
                    .defineInRange("MBATechniqueMultiplier", 5.0, 0.0, 100.0);
            mbaTechniquePreservation = builder
                    .comment(" ",
                            "================================================================",
                            " [Mythical Beast Amber Technique Preservation]",
                            " Ratio of original damage preserved for MBA skills.",
                            "----------------------------------------------------------------",
                            " [幻兽琥珀术式保留比例]",
                            " 幻兽琥珀技能的原伤害保留比例。",
                            "================================================================")
                    .translation("config.jujutsu_addon.mba_technique_preservation")
                    .defineInRange("MBATechniquePreservation", 2.0, 0.0, 100.0);
            builder.pop();

            // ==========================================================================
            // 10. 生物兼容性 (Mob Compatibility) [新增]
            // ==========================================================================
            // 心华提示：10 保持不变，因为它现在排在 09 后面是正确的
            builder.push("10_Mob_Compatibility");

            enableMobCompatibility = builder
                    .comment(" ",
                            "================================================================",
                            " [Master Switch: Mob Compatibility]",
                            " If FALSE, no mobs will receive Sorcerer capabilities, ignoring the list below.",
                            " If TRUE, mobs in the list will become Sorcerers.",
                            "----------------------------------------------------------------",
                            " [生物兼容性总开关]",
                            " 如果为 FALSE (关闭)，所有生物都不会获得咒术师能力，忽略下方的列表。",
                            " 如果为 TRUE (开启)，且生物在下方列表中，它们才会成为咒术师。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_mob_compatibility")
                    .define("EnableMobCompatibility", true);

            compatMobList = builder
                    .comment(" ",
                            "================================================================",
                            " [Mob Compatibility List]",
                            " Define which mobs should become Sorcerers and what powers they get.",
                            " Format: EntityID | Technique | Grade | Faction (optional)",
                            " ",
                            " [Parameters]",
                            " 1. EntityID: The registry name of the mob (e.g., touhou_little_maid:maid).",
                            " 2. Technique: The Cursed Technique (e.g., SHRINE, LIMITLESS, TEN_SHADOWS).",
                            "    * Use 'RANDOM' for a random technique.",
                            "    * Use 'NONE' for no technique (just Cursed Energy).",
                            " 3. Grade: The Sorcerer Grade (GRADE_4 to SPECIAL_GRADE).",
                            " 4. Faction (Optional): The JujutsuType.",
                            "    * SORCERER - Sorcerer faction (attacks curses)",
                            "    * CURSE - Curse faction (attacks sorcerers)",
                            "    * RANDOM - Random faction (default if not specified)",
                            " ",
                            " [Examples]",
                            " 'touhou_little_maid:maid|SHRINE|GRADE_4' -> Random faction (default)",
                            " 'touhou_little_maid:maid|SHRINE|GRADE_4|SORCERER' -> Sorcerer faction",
                            " 'minecraft:zombie|DISASTER_FLAMES|GRADE_1|CURSE' -> Curse faction",
                            "----------------------------------------------------------------",
                            " [生物兼容列表]",
                            " 定义哪些生物应该成为咒术师，以及它们获得什么能力。",
                            " 格式：实体ID | 术式 | 等级 | 阵营(可选)",
                            " ",
                            " [参数说明]",
                            " 1. 实体ID: 生物的注册名 (如 touhou_little_maid:maid)。",
                            " 2. 术式: 咒术名称 (如 SHRINE, LIMITLESS, TEN_SHADOWS)。",
                            "    * 填 'RANDOM' 则随机分配。",
                            "    * 填 'NONE' 则无术式（只有咒力）。",
                            " 3. 等级: 咒术师等级 (GRADE_4 到 SPECIAL_GRADE)。",
                            " 4. 阵营 (可选): JujutsuType。",
                            "    * SORCERER - 咒术师阵营 (攻击咒灵)",
                            "    * CURSE - 咒灵阵营 (攻击咒术师)",
                            "    * RANDOM - 随机阵营 (不填则默认随机)",
                            " ",
                            " [示例]",
                            " 'touhou_little_maid:maid|SHRINE|GRADE_4' -> 阵营随机 (默认)",
                            " 'touhou_little_maid:maid|SHRINE|GRADE_4|SORCERER' -> 咒术师阵营",
                            " 'minecraft:zombie|DISASTER_FLAMES|GRADE_1|CURSE' -> 咒灵阵营",
                            "================================================================")
                    .translation("config.jujutsu_addon.compat_mob_list")
                    .defineList("CompatMobList", Arrays.asList(
                            "touhou_little_maid:maid|RANDOM|GRADE_4|RANDOM"
                    ), entry -> true);

            enableMobAI = builder
                    .comment(" Enable custom AI for these mobs (Use skills, walk on water).",
                            " 为这些生物启用自定义 AI（自动放技能、水上行走）。")
                    .translation("config.jujutsu_addon.enable_mob_ai")
                    .define("EnableMobAI", true);

            randomTechniqueBlacklist = builder
                    .comment(" ",
                            "================================================================",
                            " [Random Technique Blacklist]",
                            " Techniques in this list will NOT be selected when using 'RANDOM'.",
                            " Default: TEN_SHADOWS (To prevent buggy summons/crashes on some mobs).",
                            "----------------------------------------------------------------",
                            " [随机术式黑名单]",
                            " 当配置为 'RANDOM' 或使用重随功能时，不会随机到此列表中的术式。",
                            " 默认: TEN_SHADOWS (十种影法术)，防止召唤物AI导致崩溃或BUG。",
                            "================================================================")
                    .translation("config.jujutsu_addon.random_technique_blacklist")
                    .defineList("RandomTechniqueBlacklist", Arrays.asList(
                            "TEN_SHADOWS",
                            "BODY_SWAP"
                    ), entry -> true);
            enableTechniqueReroll = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Technique Reroll]",
                            " Allow players to change a mob's technique by Shift+Right Clicking with specific items.",
                            "----------------------------------------------------------------",
                            " [启用术式重随]",
                            " 允许玩家手持特定物品 Shift+右键 来随机更换生物的术式。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_technique_reroll")
                    .define("EnableTechniqueReroll", true);
            rerollItems = builder
                    .comment(" ",
                            "================================================================",
                            " [Reroll Items]",
                            " List of Item IDs used to reroll techniques.",
                            " Default: Dirt and Cobblestone.",
                            "----------------------------------------------------------------",
                            " [重随消耗物品]",
                            " 用于重随术式的物品ID列表。",
                            " 默认：泥土 (minecraft:dirt) 和 圆石 (minecraft:cobblestone)。",
                            "================================================================")
                    .translation("config.jujutsu_addon.reroll_items")
                    .defineList("RerollItems", Arrays.asList(
                            "minecraft:dirt",
                            "minecraft:cobblestone"
                    ), entry -> true);

            traitRerollItems = builder
                    .comment(" ",
                            "================================================================",
                            " [Trait Reroll Items]",
                            " List of Item IDs used to reroll TRAITS (Shift + Right Click).",
                            " This is SEPARATE from the Technique reroll item.",
                            " Default: Amethyst Shard.",
                            "----------------------------------------------------------------",
                            " [特质重随物品列表]",
                            " 用于重随【特质】(Trait) 的物品ID列表。",
                            " 这与术式重随物品是分开的，互不冲突。",
                            " 默认：紫水晶碎片 (minecraft:amethyst_shard)。",
                            "================================================================")
                    .translation("config.jujutsu_addon.trait_reroll_items")
                    .defineList("TraitRerollItems", Arrays.asList(
                            "minecraft:amethyst_shard"
                    ), entry -> true);

            factionRerollItems = builder
                    .comment(" ",
                            "================================================================",
                            " [Faction Reroll Items]",
                            " List of Item IDs used to reroll FACTION/JujutsuType (Shift + Right Click).",
                            " Factions: SORCERER (咒术师), CURSE (咒灵)",
                            " Default: Lapis Lazuli.",
                            "----------------------------------------------------------------",
                            " [阵营重随物品列表]",
                            " 用于重随【阵营】(JujutsuType) 的物品ID列表。",
                            " 阵营：SORCERER (咒术师), CURSE (咒灵)",
                            " 默认：青金石 (minecraft:lapis_lazuli)。",
                            "================================================================")
                    .translation("config.jujutsu_addon.faction_reroll_items")
                    .defineList("FactionRerollItems", Arrays.asList(
                            "minecraft:lapis_lazuli"
                    ), entry -> true);
            enableUniqueTraitLimit = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Unique Trait Limit]",
                            " If true, Unique Traits (like Six Eyes) will be limited to ONE per world save.",
                            " This applies to ALL whitelisted mobs globally.",
                            "----------------------------------------------------------------",
                            " [启用唯一特质限制]",
                            " 若为真，唯一特质（如六眼）在整个存档的所有白名单生物中只能存在一个。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_unique_trait_limit")
                    .define("EnableUniqueTraitLimit", true);

            builder.pop();

            // ==========================================================================
// 11. 十影术式增强 (Ten Shadows Enhancement)
// ==========================================================================
            builder.push("11_Ten_Shadows_Enhancement");
            enableTenShadowsModeBypass = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Ten Shadows Mode Bypass] (Core Feature)",
                            " ",
                            " VANILLA BEHAVIOR (FALSE):",
                            " - In SUMMON mode: Only summon skills are visible (Divine Dogs, Nue...)",
                            " - In ABILITY mode: Only ability skills are visible (Nue Lightning...)",
                            " - Must manually use 'Switch Mode' to change between them.",
                            " ",
                            " WITH THIS ENABLED (TRUE):",
                            " - ALL Ten Shadows skills are always visible regardless of mode.",
                            " - Mode switches AUTOMATICALLY when you use a skill.",
                            " - Example: In SUMMON mode, click Nue Lightning -> auto switch to ABILITY -> cast.",
                            " ",
                            " This is a QUALITY OF LIFE feature. No balance impact.",
                            "----------------------------------------------------------------",
                            " [启用十影模式绕过] (核心功能)",
                            " ",
                            " 原版行为 (FALSE/关闭):",
                            " - 召唤模式下：只显示召唤类技能（玉犬、鵺...）",
                            " - 技能模式下：只显示技能类技能（鵺闪电...）",
                            " - 必须手动使用【切换模式】来切换。",
                            " ",
                            " 开启后 (TRUE):",
                            " - 所有十影技能始终可见，无视当前模式。",
                            " - 使用技能时自动切换到对应模式。",
                            " - 例如：在召唤模式下点击鸯闪电 -> 自动切换到技能模式 -> 释放。",
                            " ",
                            " 这是一个【便利性】功能，不影响平衡。",
                            "================================================================")
                    .translation("config.jujutsu_addon.enable_ten_shadows_mode_bypass")
                    .define("EnableTenShadowsModeBypass", true);
            allowSimultaneousSummonAndAbility = builder
                    .comment(" ",
                            "================================================================",
                            " [Allow Simultaneous Summon and Ability] (Extra Feature)",
                            " ",
                            " VANILLA BEHAVIOR (FALSE):",
                            " - When Nue is summoned, Nue Lightning is DISABLED.",
                            " - Reason: Nue is not inside your shadow, so you can't use its power.",
                            " - Same for Max Elephant + Piercing Water, etc.",
                            " ",
                            " WITH THIS ENABLED (TRUE):",
                            " - You CAN use Nue Lightning even while Nue is summoned.",
                            " - Basically removes the 'shikigami must be in shadow' restriction.",
                            " ",
                            " NOTE: Requires 'EnableTenShadowsModeBypass' to be TRUE.",
                            " WARNING: This changes game balance! Nue + Nue Lightning .",
                            "----------------------------------------------------------------",
                            " [允许召唤与技能同时使用] (额外功能)",
                            " ",
                            " 原版行为 (FALSE/关闭):",
                            " - 当鵺被召唤时，鵺闪电被禁用。",
                            " - 原因：鵺不在你的影子里，你无法使用它的力量。",
                            " - 同理：满象 + 穿刺水流，等等。",
                            " ",
                            " 开启后 (TRUE):",
                            " - 即使鸯已被召唤，你仍然可以使用鸯闪电。",
                            " - 基本上移除了【式神必须在影子里】的限制。",
                            " ",
                            " 注意：需要同时开启上方的【十影模式绕过】才生效。",
                            " 警告：这会影响游戏平衡！鵺 + 鵺闪电。",
                            "================================================================")
                    .translation("config.jujutsu_addon.allow_simultaneous_summon_and_ability")
                    .define("AllowSimultaneousSummonAndAbility", false);
            builder.pop();
        }
    }
}
