package com.jujutsuaddon.addon.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * 束缚誓约系统配置
 * Custom Binding Vow System Configuration
 */
public class VowConfig {

    public static final VowConfig.Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {

        // ==========================================
        // 1. 基础设置 (Basic Settings)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableVowSystem;
        public final ForgeConfigSpec.IntValue maxActiveVows;
        public final ForgeConfigSpec.BooleanValue debugMode;
        // ★★★ 新增配置项 ★★★
        public final ForgeConfigSpec.BooleanValue enablePermanentVowBenefits;

        // ==========================================
        // 2. 收益上限 (Benefit Caps)
        // ==========================================


        // ==========================================
        // 3. 誓约类型倍率 (Vow Type Multipliers)
        // ==========================================
        public final ForgeConfigSpec.DoubleValue permanentVowMultiplier;
        public final ForgeConfigSpec.DoubleValue dissolvableVowMultiplier;

        // ==========================================
        // 4. 权重计算 (Weight Calculation)
        // ==========================================
        public final ForgeConfigSpec.DoubleValue baseWeightScale;
        public final ForgeConfigSpec.DoubleValue weightScalingExponent;
        public final ForgeConfigSpec.DoubleValue benefitScalingExponent;


        // ==========================================
        // 5. 条件权重配置 (Condition Weight Settings)
        // ==========================================


        // ==========================================
        // 6. 收益消耗配置 (Benefit Cost Settings)
        // ==========================================


        // ==========================================
        // 7. 违约惩罚 (Violation Penalties)
        // ==========================================
        public final ForgeConfigSpec.DoubleValue violationDamagePercent;
        public final ForgeConfigSpec.IntValue violationCooldownTicks;
        public final ForgeConfigSpec.IntValue violationSealDurationTicks;

        // ==========================================
        // 8. 网络与同步 (Network & Sync)
        // ==========================================
        public final ForgeConfigSpec.IntValue syncIntervalTicks;
        public final ForgeConfigSpec.IntValue checkIntervalTicks;

        // ==========================================
        // 9. 预设誓约 (Preset Vows)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enablePresetVows;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledPresetVows;

        public Common(ForgeConfigSpec.Builder builder) {

            // ==========================================================================
            // 1. 基础设置 (Basic Settings)
            // ==========================================================================
            builder.push("01_Basic_Settings");

            enableVowSystem = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Custom Binding Vow System]",
                            " Master switch. If FALSE, all vow features are disabled.",
                            "----------------------------------------------------------------",
                            " [启用自定义束缚誓约系统]",
                            " 总开关。若为 FALSE，所有誓约功能将被禁用。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.enable_system")
                    .define("EnableVowSystem", true);

            maxActiveVows = builder
                    .comment(" ",
                            "================================================================",
                            " [Maximum Active Vows]",
                            " Max vows a player can have ACTIVE simultaneously.",
                            "----------------------------------------------------------------",
                            " [最大激活誓约数量]",
                            " 玩家同时可以激活的誓约数量上限。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.max_active_vows")
                    .defineInRange("MaxActiveVows", 3, 1, 10);

            debugMode = builder
                    .comment(" ",
                            "================================================================",
                            " [Debug Mode]",
                            " Prints vow calculations and violation checks to console.",
                            "----------------------------------------------------------------",
                            " [调试模式]",
                            " 在控制台打印誓约计算和违约检查信息。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.debug_mode")
                    .define("DebugMode", false);

            // ★★★ 新增配置定义 ★★★
            enablePermanentVowBenefits = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Permanent Vow Passive Benefits]",
                            " TRUE = Gameplay Mode: Permanent vows are NOT consumed after use (Infinite use).",
                            " FALSE = Lore Mode: Permanent vows are consumed after use (One-time use).",
                            "----------------------------------------------------------------",
                            " [启用永久誓约常驻收益]",
                            " TRUE = 游戏性模式：永久誓约在使用后不会消失（无限次使用）。",
                            " FALSE = 原著模式：永久誓约在使用后会消失（一次性使用）。",
                            "================================================================")
                    .define("EnablePermanentVowBenefits", true);

            builder.pop();


            // ==========================================================================
            // 3. 誓约类型倍率 (Vow Type Multipliers)
            // ==========================================================================
            builder.push("03_Vow_Type_Multipliers");

            permanentVowMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Permanent Vow Benefit Multiplier]",
                            " Benefits from PERMANENT vows are multiplied by this.",
                            " Higher risk (can't cancel) = Higher reward.",
                            " Example: 10% base benefit * 1.5 = 15% actual benefit.",
                            "----------------------------------------------------------------",
                            " [永久誓约收益倍率]",
                            " 永久誓约的收益会乘以此倍率。",
                            " 高风险（无法取消） = 高回报。",
                            " 例如：10% 基础收益 * 1.5 = 15% 实际收益。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.permanent_multiplier")
                    .defineInRange("PermanentVowMultiplier", 1.5, 1.0, 3.0);

            dissolvableVowMultiplier = builder
                    .comment(" ",
                            "================================================================",
                            " [Dissolvable Vow Benefit Multiplier]",
                            " Benefits from DISSOLVABLE vows are multiplied by this.",
                            " Can be cancelled anytime, so lower reward.",
                            "----------------------------------------------------------------",
                            " [可解除誓约收益倍率]",
                            " 可解除誓约的收益会乘以此倍率。",
                            " 可随时取消，所以回报较低。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.dissolvable_multiplier")
                    .defineInRange("DissolvableVowMultiplier", 1.0, 0.5, 2.0);

            builder.pop();

            // ==========================================================================
            // 4. 权重计算 (Weight Calculation)
            // ==========================================================================
            builder.push("04_Weight_Calculation");

            baseWeightScale = builder
                    .comment(" ",
                            "================================================================",
                            " [Base Weight Scale]",
                            " Global multiplier for all condition weights.",
                            " Increase to make conditions more valuable.",
                            "----------------------------------------------------------------",
                            " [基础权重缩放]",
                            " 所有条件权重的全局倍率。",
                            " 增加此值使条件更有价值。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.base_weight_scale")
                    .defineInRange("BaseWeightScale", 1.0, 0.1, 5.0);

            weightScalingExponent = builder
                    .comment(" ",
                            "================================================================",
                            " [Weight Scaling Exponent]",
                            " Controls how condition severity translates to weight.",
                            " 1.0 = Linear (double restriction = double weight)",
                            " 1.2 = Rewards harsher restrictions slightly more",
                            " Formula: FinalWeight = BaseWeight ^ Exponent",
                            "----------------------------------------------------------------",
                            " [权重缩放指数]",
                            " 控制条件严苛程度如何转化为权重。",
                            " 1.0 = 线性（双倍限制 = 双倍权重）",
                            " 1.2 = 对更严格的限制有额外奖励",
                            " 公式：最终权重 = 基础权重 ^ 指数",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.weight_exponent")
                    .defineInRange("WeightScalingExponent", 1.1, 1.0, 2.0);

            benefitScalingExponent = builder
                    .comment(" ",
                            "================================================================",
                            " [Benefit Scaling Exponent]",
                            " Controls diminishing returns on benefits.",
                            " 1.0 = Linear (no diminishing returns)",
                            " 0.7 = Moderate diminishing returns (recommended)",
                            " 0.5 = Strong diminishing returns",
                            " Formula: BenefitValue = InvestedWeight ^ Exponent * Factor",
                            "----------------------------------------------------------------",
                            " [收益缩放指数]",
                            " 控制收益的边际递减。",
                            " 1.0 = 线性（无边际递减）",
                            " 0.7 = 中等边际递减（推荐）",
                            " 0.5 = 强烈边际递减",
                            " 公式：收益值 = 投入权重 ^ 指数 * 系数",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.benefit_exponent")
                    .defineInRange("BenefitScalingExponent", 0.8, 0.3, 1.5);


            builder.pop();


            // ==========================================================================
            // 7. 违约惩罚 (Violation Penalties)
            // ==========================================================================
            builder.push("07_Violation_Penalties");


            violationDamagePercent = builder
                    .comment(" ",
                            "================================================================",
                            " [Violation Damage Percent]",
                            " Percentage of MAX HEALTH dealt when violating a vow.",
                            " 0.2 = 20% of max health.",
                            " For PERMANENT vows, multiply by PermanentViolationDamageMultiplier.",
                            "----------------------------------------------------------------",
                            " [违约伤害百分比]",
                            " 违约时造成的伤害（占最大生命值的百分比）。",
                            " 0.2 = 最大生命值的 20%。",
                            " 永久誓约会乘以下方的额外倍率。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.violation_damage_percent")
                    .defineInRange("ViolationDamagePercent", 0.2, 0.0, 1.0);

            violationCooldownTicks = builder
                    .comment(" ",
                            "================================================================",
                            " [Violation Cooldown]",
                            " Ticks before player can create new vows after violation.",
                            " 6000 ticks = 5 minutes.",
                            "----------------------------------------------------------------",
                            " [违约冷却时间]",
                            " 违约后多少 tick 内无法创建新誓约。",
                            " 6000 tick = 5 分钟。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.violation_cooldown")
                    .defineInRange("ViolationCooldownTicks", 6000, 0, 72000);


            violationSealDurationTicks = builder
                    .comment(" ",
                            "================================================================",
                            " [Violation Seal Duration]",
                            " How long abilities are sealed after violation (ticks).",
                            " 1200 ticks = 60 seconds = 1 minute.",
                            "----------------------------------------------------------------",
                            " [违约封印持续时间]",
                            " 违约后技能封印持续多少 tick。",
                            " 1200 tick = 60 秒 = 1 分钟。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.violation_seal_duration")
                    .defineInRange("ViolationSealDurationTicks", 1200, 0, 12000);
            builder.pop();
            // ==========================================================================
            // 8. 网络与同步 (Network & Sync)
            // ==========================================================================
            builder.push("08_Network_Sync");
            syncIntervalTicks = builder
                    .comment(" ",
                            "================================================================",
                            " [Sync Interval]",
                            " How often vow data syncs to client (ticks).",
                            " Lower = More responsive, more network traffic.",
                            " 20 ticks = 1 second.",
                            "----------------------------------------------------------------",
                            " [同步间隔]",
                            " 誓约数据同步到客户端的频率（tick）。",
                            " 更低 = 更及时但网络流量更大。",
                            " 20 tick = 1 秒。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.sync_interval")
                    .defineInRange("SyncIntervalTicks", 20, 1, 200);
            checkIntervalTicks = builder
                    .comment(" ",
                            "================================================================",
                            " [Condition Check Interval]",
                            " How often time-based conditions are checked (ticks).",
                            " Only affects passive checks, not event-triggered ones.",
                            " 20 ticks = 1 second (recommended).",
                            "----------------------------------------------------------------",
                            " [条件检查间隔]",
                            " 基于时间的条件检查频率（tick）。",
                            " 只影响被动检查，不影响事件触发的检查。",
                            " 20 tick = 1 秒（推荐）。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.check_interval")
                    .defineInRange("CheckIntervalTicks", 20, 1, 100);
            builder.pop();
            // ==========================================================================
            // 9. 预设誓约 (Preset Vows)
            // ==========================================================================
            builder.push("09_Preset_Vows");
            enablePresetVows = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Preset Vows]",
                            " If TRUE, players can select from pre-made vow templates.",
                            " These are balanced, ready-to-use vows for convenience.",
                            "----------------------------------------------------------------",
                            " [启用预设誓约]",
                            " 若为 TRUE，玩家可以从预制誓约模板中选择。",
                            " 这些是平衡好的、即用型誓约，方便快速使用。",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.enable_presets")
                    .define("EnablePresetVows", true);
            disabledPresetVows = builder
                    .comment(" ",
                            "================================================================",
                            " [Disabled Preset Vows]",
                            " List of preset vow IDs to disable.",
                            " Format: modid:preset_name",
                            "----------------------------------------------------------------",
                            " [禁用的预设誓约]",
                            " 要禁用的预设誓约ID列表。",
                            " 格式：modid:preset_name",
                            "================================================================")
                    .translation("config.jujutsu_addon.vow.disabled_presets")
                    .defineList("DisabledPresetVows", Arrays.asList(), entry -> entry instanceof String);
            builder.pop();
        }
    }
    // ==================== 便捷访问方法 ====================
    public static boolean isEnabled() {
        return COMMON.enableVowSystem.get();
    }
    public static int getMaxActiveVows() {
        return COMMON.maxActiveVows.get();
    }
    // ★★★ 新增静态读取方法 ★★★
    public static boolean isPermanentVowBenefitsEnabled() {
        return COMMON.enablePermanentVowBenefits.get();
    }

    public static float getPermanentVowMultiplier() {
        return COMMON.permanentVowMultiplier.get().floatValue();
    }
    public static float getDissolvableVowMultiplier() {
        return COMMON.dissolvableVowMultiplier.get().floatValue();
    }
    public static float getBaseWeightScale() {
        return COMMON.baseWeightScale.get().floatValue();
    }
    public static float getWeightScalingExponent() {
        return COMMON.weightScalingExponent.get().floatValue();
    }
    public static float getBenefitScalingExponent() {
        return COMMON.benefitScalingExponent.get().floatValue();
    }

    public static float getViolationDamagePercent() {
        return COMMON.violationDamagePercent.get().floatValue();
    }

    public static int getViolationSealDurationTicks() {
        return COMMON.violationSealDurationTicks.get();
    }
    public static int getSyncIntervalTicks() {
        return COMMON.syncIntervalTicks.get();
    }
    public static int getCheckIntervalTicks() {
        return COMMON.checkIntervalTicks.get();
    }
    public static boolean isDebugMode() {
        return COMMON.debugMode.get();
    }

}
