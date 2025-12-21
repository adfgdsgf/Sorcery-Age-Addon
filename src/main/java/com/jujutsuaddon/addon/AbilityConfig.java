package com.jujutsuaddon.addon;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class AbilityConfig {

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {

        // ==================== 总开关 ====================
        public final ForgeConfigSpec.BooleanValue enableInfinityPressure;

        // ==================== 范围与区域 ====================
        public final ForgeConfigSpec.DoubleValue level1Range;
        public final ForgeConfigSpec.DoubleValue level10Range;
        public final ForgeConfigSpec.DoubleValue rangeCurveExponent;
        public final ForgeConfigSpec.DoubleValue slowdownZoneParts;
        public final ForgeConfigSpec.DoubleValue balancePointParts;

        public final ForgeConfigSpec.DoubleValue balanceRadiusMinimum;
        public final ForgeConfigSpec.DoubleValue balanceRadiusMaxRatio;
        public final ForgeConfigSpec.DoubleValue zenoCurveExponent;
        public final ForgeConfigSpec.DoubleValue zenoRatio;

        // ==================== 生物压制 - 推力 ====================
       // public final ForgeConfigSpec.DoubleValue basePushForce;
        public final ForgeConfigSpec.DoubleValue maxPushForce;
        //public final ForgeConfigSpec.DoubleValue breachRepelForce;
        public final ForgeConfigSpec.DoubleValue pinForce;

        // ==================== 生物压制 - 阻力 ====================
        //public final ForgeConfigSpec.DoubleValue lateralResistance;
        //public final ForgeConfigSpec.DoubleValue escapeResistance;

        // ==================== 生物压制 - 压力值 ====================
        public final ForgeConfigSpec.DoubleValue basePressure;
        public final ForgeConfigSpec.DoubleValue distanceDecay;
        public final ForgeConfigSpec.DoubleValue approachMultiplier;
        public final ForgeConfigSpec.DoubleValue breachPressureMult;

        // ==================== 生物压制 - 等级因子 ====================
        public final ForgeConfigSpec.DoubleValue minLevelMult;
        public final ForgeConfigSpec.DoubleValue maxLevelMult;

        // ==================== 伤害 ====================
        public final ForgeConfigSpec.DoubleValue minPressureForDamage;
        public final ForgeConfigSpec.DoubleValue maxDamagePerHit;
        public final ForgeConfigSpec.IntValue minDamageInterval;
        public final ForgeConfigSpec.IntValue maxDamageInterval;
        public final ForgeConfigSpec.DoubleValue maxPressureForInterval;
        public final ForgeConfigSpec.DoubleValue maxPressureMultiplier;
        public final ForgeConfigSpec.DoubleValue curveSteepness;
        public final ForgeConfigSpec.DoubleValue collisionMinDistance;
        public final ForgeConfigSpec.DoubleValue blockPressureRate;

        // ==================== 投射物 ====================
        public final ForgeConfigSpec.BooleanValue affectProjectiles;
        public final ForgeConfigSpec.IntValue projectileMinLevel;
        public final ForgeConfigSpec.DoubleValue projectileEntrySpeed;
        //public final ForgeConfigSpec.DoubleValue projectileStopSpeed;
        public final ForgeConfigSpec.DoubleValue reflectSpeedMultiplier;

        // ==================== 方块 ====================
        public final ForgeConfigSpec.BooleanValue actuallyBreakBlocks;
        public final ForgeConfigSpec.BooleanValue dropBlockItems;
        public final ForgeConfigSpec.DoubleValue minPressureForBlockBreak;
        public final ForgeConfigSpec.DoubleValue breakThresholdMult;

        // ==================== 方块硬度影响 ====================
        public final ForgeConfigSpec.DoubleValue softHardnessThreshold;
        public final ForgeConfigSpec.DoubleValue hardHardnessThreshold;
        public final ForgeConfigSpec.DoubleValue softBlockPressureMult;
        public final ForgeConfigSpec.DoubleValue normalBlockPressureMult;
        public final ForgeConfigSpec.DoubleValue hardBlockPressureMult;
        public final ForgeConfigSpec.DoubleValue bedrockPressureMult;

        // ==================== 掉落物 ====================
        public final ForgeConfigSpec.BooleanValue pushDroppedItems;
        public final ForgeConfigSpec.DoubleValue itemPushForceMultiplier;

        // ==================== 效果 ====================
        public final ForgeConfigSpec.BooleanValue enableParticles;
        public final ForgeConfigSpec.BooleanValue enableSounds;

        // ==================== 交互 ====================
        public final ForgeConfigSpec.BooleanValue respectDomainSureHit;
        public final ForgeConfigSpec.BooleanValue respectDomainAmplification;

        // ==================== 咒力消耗 ====================
        public final ForgeConfigSpec.BooleanValue enablePressureCost;
        public final ForgeConfigSpec.DoubleValue baseCursedEnergyCost;
        public final ForgeConfigSpec.DoubleValue costPerLevel;
        public final ForgeConfigSpec.DoubleValue maxCostMultiplier;

        public Common(ForgeConfigSpec.Builder builder) {

            builder.push("01_Infinity_Pressure");

            builder.comment(
                    " ",
                    "================================================================================",
                    " INFINITY PRESSURE SYSTEM / 无下限压力系统",
                    "================================================================================",
                    " ",
                    " Zone Layout (from outside to inside / 从外到内):",
                    " ",
                    " |<-- Slowdown Zone -->|<-- Stop Zone -->|<-- Push Zone -->| Player",
                    " |<------ 减速区 ----->|<--- 停止区 ---->|<--- 推力区 ---->| 玩家",
                    " ",
                    " - Slowdown: Projectiles slow down, entities pushed gently",
                    " - Stop: Projectiles hover, entities pushed harder",
                    " - Push: Breach = strong repel + damage",
                    " ",
                    " - 减速区：投射物减速，生物被轻推",
                    " - 停止区：投射物悬浮，生物被强推",
                    " - 推力区：突破后强推+伤害",
                    " "
            );

            // ==================== 总开关 ====================
            enableInfinityPressure = builder
                    .comment(" Enable the Infinity Pressure system.",
                            " 启用无下限压力系统。")
                    .translation("config.jujutsu_addon.pressure.enable")
                    .define("EnableSystem", true);

// ==================== 范围与区域 / Range & Zones ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " RANGE & ZONES / 范围与区域",
                    " ═══════════════════════════════════════════════════════════════",
                    " ",
                    " ★★★ Zeno's Paradox Physics Model / 芝诺悖论物理模型 ★★★",
                    " ",
                    " Core Formula: zenoMultiplier = balanceRadius / distance",
                    " 核心公式：芝诺倍率 = 平衡点半径 / 距离",
                    " ",
                    " Zone Layout (Spherical, from inside to outside):",
                    " 区域布局（球形，从内到外）：",
                    " ",
                    "   Caster ──── Balance Point ──────────────── Edge",
                    "   施术者 ──── 平衡点(墙) ─────────────────── 边界",
                    "     d=0         d=balanceR                   d=maxRange",
                    "   倍率=∞         倍率=1                       倍率→0",
                    " ",
                    " [How It Works / 原理说明]",
                    " ",
                    " The 'Balance Point' is an invisible wall.",
                    " 平衡点就是一堵无形的墙。",
                    " ",
                    " - At balance point: push = approach speed → standstill",
                    " - 在平衡点：推力 = 向内速度 → 静止",
                    " ",
                    " - Inside balance point (closer to caster):",
                    "   multiplier > 1 → push > speed → pushed out",
                    " - 平衡点内侧（更靠近施术者）：",
                    "   倍率 > 1 → 推力 > 速度 → 被推开",
                    " ",
                    " - Outside balance point (farther from caster):",
                    "   multiplier < 1 → push < speed → slowed but can move",
                    " - 平衡点外侧（更远离施术者）：",
                    "   倍率 < 1 → 推力 < 速度 → 减速但能移动",
                    " ",
                    " [Level & Output Effect / 等级与出力的作用]",
                    " ",
                    " Level/Output does NOT change push strength!",
                    " 等级/出力不改变推力大小！",
                    " ",
                    " Instead, it changes WHERE the balance point (wall) is.",
                    " 而是改变平衡点（墙）的位置。",
                    " ",
                    " Low level:  Wall is close to caster",
                    " High level: Wall is far from caster",
                    " 低等级：墙靠近施术者",
                    " 高等级：墙远离施术者",
                    " ",
                    " [Range Formula / 范围计算公式]",
                    " Range = Level1 + (Level10 - Level1) × (Level ÷ 10) ^ Exponent",
                    " 总范围 = 等级1范围 + (等级10范围 - 等级1范围) × (等级÷10)^指数",
                    " ",
                    " [Default Range Table / 默认配置范围表]",
                    " (Level1=2, Level10=10, Exponent=2.0, BalanceParts=3, SlowParts=7)",
                    " ",
                    " ┌────────┬────────┬──────────────┬──────────────┐",
                    " │ Level  │ Total  │ Balance Pt   │ Slowdown     │",
                    " │ 等级   │ 总范围 │ 平衡点(墙)   │ 减速区       │",
                    " ├────────┼────────┼──────────────┼──────────────┤",
                    " │ Lv.1   │ 2.1    │ 0.6          │ 0.6~2.1      │",
                    " │ Lv.3   │ 2.7    │ 0.8          │ 0.8~2.7      │",
                    " │ Lv.5   │ 4.0    │ 1.2          │ 1.2~4.0      │",
                    " │ Lv.7   │ 5.9    │ 1.8          │ 1.8~5.9      │",
                    " │ Lv.10  │ 10.0   │ 3.0          │ 3.0~10.0     │",
                    " └────────┴────────┴──────────────┴──────────────┘",
                    " ",
                    " Example at Lv.10 (balanceR=3, maxRange=10):",
                    " 等级10示例（平衡点=3格，最大=10格）：",
                    " ",
                    " Distance 1.5: multiplier = 3/1.5 = 2.0 → pushed back",
                    " Distance 3.0: multiplier = 3/3.0 = 1.0 → standstill (the wall)",
                    " Distance 6.0: multiplier ≈ 0.3 → slowed but can walk",
                    " Distance 10:  multiplier → 0 → free movement",
                    " ",
                    " 距离1.5格：倍率 = 3/1.5 = 2.0 → 被推开",
                    " 距离3.0格：倍率 = 3/3.0 = 1.0 → 静止（墙）",
                    " 距离6.0格：倍率 ≈ 0.3 → 减速但能走",
                    " 距离10格：倍率 → 0 → 自由移动",
                    " "
            ).push("RangeAndZones");
// --- Total Range / 总范围 ---
            builder.comment(" ",
                    " Total range settings. Affects where the edge (no effect) is.",
                    " 总范围设置。影响边界（不受力）的位置。"
            ).push("TotalRange");
            level1Range = builder
                    .comment(" Range at level 1 (blocks). / 等级1时的总范围（格）。")
                    .translation("config.jujutsu_addon.pressure.range.level1")
                    .defineInRange("Level1", 2.0, 0.5, 50.0);
            level10Range = builder
                    .comment(" Range at level 10 (blocks). / 等级10时的总范围（格）。")
                    .translation("config.jujutsu_addon.pressure.range.level10")
                    .defineInRange("Level10", 10.0, 2.0, 100.0);
            rangeCurveExponent = builder
                    .comment(" ",
                            " Curve exponent for range scaling. / 范围增长曲线指数。",
                            " ",
                            " 1.0 = Linear (same increase per level) / 线性",
                            " 2.0 = Quadratic (high levels stronger) [Default] / 平方 [默认]",
                            " 3.0 = Cubic (low weak, high explosive) / 立方")
                    .translation("config.jujutsu_addon.pressure.range.curve")
                    .defineInRange("CurveExponent", 1.5, 0.5, 4.0);
            builder.pop(); // TotalRange
// --- Zone Ratio / 区域占比 ---
            builder.comment(" ",
                    " Zone ratio (parts). Total range is divided by these ratios.",
                    " 区域占比（份数），总范围按此比例划分。",
                    " ",
                    " Balance Point = where multiplier = 1 (the invisible wall)",
                    " 平衡点 = 倍率为1的位置（无形的墙）",
                    " ",
                    " Example: Balance=3, Slowdown=7 (Total=10)",
                    " 示例：平衡点=3, 减速=7 (总计=10)",
                    " → Balance point at 30% of total range",
                    " → 平衡点在总范围的30%位置"
            ).push("ZoneRatio");
            balancePointParts = builder
                    .comment(" Balance point parts (the wall position). / 平衡点份数（墙的位置）。",
                            " ",
                            " Inside this radius: multiplier > 1 → pushed out",
                            " At this radius: multiplier = 1 → standstill",
                            " 此半径内：倍率 > 1 → 被推开",
                            " 此半径处：倍率 = 1 → 静止")
                    .translation("config.jujutsu_addon.pressure.zone.balance")
                    .defineInRange("BalancePoint", 4.0, 0.1, 100.0);
            slowdownZoneParts = builder
                    .comment(" Slowdown zone parts (from balance point to edge). / 减速区份数（从平衡点到边界）。",
                            " ",
                            " multiplier < 1 → slower than approach speed",
                            " Standing still = no effect, moving toward caster = slowed",
                            " 倍率 < 1 → 推力小于向内速度",
                            " 站着不动=不受力，向施术者移动=减速")
                    .translation("config.jujutsu_addon.pressure.zone.slowdown")
                    .defineInRange("Slowdown", 6.0, 0.5, 200.0);

            balanceRadiusMinimum = builder
                    .comment(" Minimum balance radius (blocks). / 平衡点最小半径（格）。")
                    .translation("config.jujutsu_addon.pressure.zone.min_radius")
                    .defineInRange("MinimumRadius", 0.5, 0.1, 3.0);
            balanceRadiusMaxRatio = builder
                    .comment(" Maximum balance radius as ratio of maxRange. / 平衡点占最大范围的最大比例。",
                            " Example: 0.95 means balance point can't exceed 95% of max range.",
                            " 示例：0.95 表示平衡点不能超过最大范围的95%。")
                    .translation("config.jujutsu_addon.pressure.zone.max_ratio")
                    .defineInRange("MaxRangeRatio", 0.95, 0.5, 0.99);

            zenoCurveExponent = builder
                    .comment(" Zeno curve exponent. Higher = more sudden stop near balance point. / 芝诺曲线指数。",
                            " 2.0 = gradual (current), 4.0 = moderate, 8.0 = very sudden",
                            " 2.0 = 渐进（当前），4.0 = 适中，8.0 = 非常突然")
                    .translation("config.jujutsu_addon.pressure.zone.zeno_exponent")
                    .defineInRange("ZenoCurveExponent", 4.0, 1.0, 16.0);

            zenoRatio = builder
                    .comment(" Zeno ratio: fraction of remaining distance that can be traveled per tick.",
                            " 芝诺系数：每tick能移动剩余距离的比例。",
                            " ",
                            " 0.5 = Each step covers half remaining distance (default) / 每步走剩余一半（默认）",
                            " 0.3 = More aggressive slowdown / 更激进减速",
                            " 0.7 = Gentler slowdown / 更温和减速")
                    .translation("config.jujutsu_addon.pressure.zone.zeno_ratio")
                    .defineInRange("ZenoRatio", 0.5, 0.1, 0.9);

            builder.pop(); // ZoneRatio
            builder.pop(); // RangeAndZones
// ==================== 生物压制 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " ENTITY PRESSURE / 生物压制",
                    " ═══════════════════════════════════════════════════════════════",
                    " ",
                    " These settings affect damage and pinning, NOT push strength.",
                    " 这些设置影响伤害和钉压，不影响推力大小。",
                    " ",
                    " Push strength is determined by Zeno formula only.",
                    " 推力大小只由芝诺公式决定。"
            ).push("EntityPressure");

          /*  basePushForce = builder
                    .comment(" Base push force per tick. / 每tick基础推力。")
                    .translation("config.jujutsu_addon.pressure.force.base")
                    .defineInRange("Base", 0.015, 0.001, 0.1);*/

            maxPushForce = builder
                    .comment(" Maximum push force cap. / 推力上限。")
                    .translation("config.jujutsu_addon.pressure.force.max")
                    .defineInRange("Max", 0.25, 0.05, 1.0);

          /*  breachRepelForce = builder
                    .comment(" Repel force when entity breaches push zone.",
                            " 实体突破推力区时的反推力。")
                    .translation("config.jujutsu_addon.pressure.force.breach")
                    .defineInRange("BreachRepel", 0.2, 0.05, 0.5);*/

            pinForce = builder
                    .comment(" Extra force when pinned against wall.",
                            " 被压在墙上时的额外力。")
                    .translation("config.jujutsu_addon.pressure.force.pin")
                    .defineInRange("Pin", 0.03, 0.01, 0.2);

            // --- 阻力 ---
            builder.comment(" Resistance settings. / 阻力设置。").push("Resistance");

            /*lateralResistance = builder
                    .comment(" Resistance to sideways movement. / 侧向移动阻力。")
                    .translation("config.jujutsu_addon.pressure.resist.lateral")
                    .defineInRange("Lateral", 0.7, 0.0, 1.0);*/

           /* escapeResistance = builder
                    .comment(" Resistance when trying to escape. / 逃跑时的阻力。")
                    .translation("config.jujutsu_addon.pressure.resist.escape")
                    .defineInRange("Escape", 0.85, 0.0, 1.0);*/

            builder.pop(); // Resistance

            // --- 压力值 ---
            builder.comment(" Pressure value calculation. / 压力值计算。").push("PressureValue");

            basePressure = builder
                    .comment(" Base pressure value. / 基础压力值。")
                    .translation("config.jujutsu_addon.pressure.value.base")
                    .defineInRange("Base", 1.0, 0.1, 10.0);

            distanceDecay = builder
                    .comment(" How quickly pressure decays with distance.",
                            " 压力随距离衰减的速度。")
                    .translation("config.jujutsu_addon.pressure.value.decay")
                    .defineInRange("DistanceDecay", 2.0, 0.5, 10.0);

            approachMultiplier = builder
                    .comment(" Pressure bonus when owner moves toward target.",
                            " 玩家向目标移动时的压力加成。")
                    .translation("config.jujutsu_addon.pressure.value.approach")
                    .defineInRange("ApproachBonus", 2.0, 1.0, 5.0);

            breachPressureMult = builder
                    .comment(" Pressure multiplier when breaching push zone.",
                            " 突破推力区时的压力倍率。")
                    .translation("config.jujutsu_addon.pressure.value.breach_mult")
                    .defineInRange("BreachMult", 3.0, 1.0, 10.0);

            builder.pop(); // PressureValue

            // --- 等级因子 ---
            builder.comment(" Level factor. Scales with pressure level.",
                    " 等级因子。随压力等级缩放。").push("LevelFactor");

            minLevelMult = builder
                    .comment(" Multiplier at level 1. / 等级1的倍率。")
                    .translation("config.jujutsu_addon.pressure.level.min")
                    .defineInRange("Level1Mult", 0.1, 0.01, 1.0);

            maxLevelMult = builder
                    .comment(" Multiplier at level 10. / 等级10的倍率。")
                    .translation("config.jujutsu_addon.pressure.level.max")
                    .defineInRange("Level10Mult", 1.0, 0.5, 5.0);

            builder.pop(); // LevelFactor
            builder.pop(); // EntityPressure

// ==================== 伤害 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " DAMAGE / 伤害",
                    " ═══════════════════════════════════════════════════════════════",
                    " ",
                    " ★ Damage now uses unified damage system! ★",
                    " ★ 伤害现在使用统一伤害系统！ ★",
                    " ",
                    " Damage = PressureValue × RoleMultiplier × SpeedModifier × GlobalMultiplier",
                    " 伤害 = 压力值 × 角色倍率 × 攻速修正 × 全局倍率",
                    " ",
                    " Role multipliers are configured in main AddonConfig.",
                    " 角色倍率在主配置 AddonConfig 中设置。"
            ).push("Damage");
            minPressureForDamage = builder
                    .comment(" Minimum pressure to deal damage (requires wall collision).",
                            " 造成伤害所需的最低压力（需要撞墙）。")
                    .translation("config.jujutsu_addon.pressure.damage.min_pressure")
                    .defineInRange("MinPressure", 2.0, 0.5, 1000.0);
            maxDamagePerHit = builder
                    .comment(" Maximum damage per hit (after all multipliers).",
                            " 单次伤害上限（所有倍率之后）。")
                    .translation("config.jujutsu_addon.pressure.damage.max")
                    .defineInRange("MaxPerHit", 20.0, 1.0, 100000.0);
            minDamageInterval = builder
                    .comment(" Minimum ticks between damage (at highest pressure).",
                            " 伤害间隔下限（最高压力时）。")
                    .translation("config.jujutsu_addon.pressure.damage.min_interval")
                    .defineInRange("MinInterval", 2, 1, 20000);
            maxDamageInterval = builder
                    .comment(" Maximum ticks between damage (at lowest pressure).",
                            " 伤害间隔上限（最低压力时）。")
                    .translation("config.jujutsu_addon.pressure.damage.max_interval")
                    .defineInRange("MaxInterval", 15, 5, 6000);

            maxPressureForInterval = builder
                    .comment(" Maximum pressure value for interval calculation.",
                            " Higher pressure = shorter interval. Values above this = minimum interval.",
                            " 用于间隔计算的最大压力值。超过此值=最短间隔。",
                            " ",
                            " Should match your game's max possible pressure.",
                            " 应匹配游戏中可能的最大压力值。")
                    .translation("config.jujutsu_addon.pressure.damage.max_pressure_interval")
                    .defineInRange("MaxPressureForInterval", 100.0, 10.0, 100000.0);
            builder.pop(); // Damage

            // ==================== 压力曲线 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " PRESSURE CURVE / 压力曲线",
                    " ═══════════════════════════════════════════════════════════════",
                    " ",
                    " When target is pushed into a wall, pressure increases",
                    " exponentially as they get closer to the caster.",
                    " 当目标被压到墙上时，距离施术者越近，压力指数上升。",
                    " ",
                    " Curve Formula / 曲线公式:",
                    " multiplier = 1 + (MaxMult - 1) × (1 - distance/balanceRadius)^Steepness",
                    " ",
                    " Example at balance radius = 3 blocks:",
                    " 示例（平衡点半径 = 3格）：",
                    " ",
                    " ┌──────────┬────────────┬─────────────────┐",
                    " │ Distance │ Multiplier │ Effect          │",
                    " │ 距离     │ 倍率       │ 效果            │",
                    " ├──────────┼────────────┼─────────────────┤",
                    " │ 3.0      │ 1.0×       │ At wall (墙边)  │",
                    " │ 2.0      │ ~2.5×      │ Crushing (压迫) │",
                    " │ 1.0      │ ~8×        │ Critical (危险) │",
                    " │ 0.5      │ ~13×       │ Lethal (致命)   │",
                    " └──────────┴────────────┴─────────────────┘",
                    " "
            ).push("PressureCurve");
            maxPressureMultiplier = builder
                    .comment(" Maximum pressure multiplier when very close to caster.",
                            " 非常接近施术者时的最大压力倍率。")
                    .translation("config.jujutsu_addon.pressure.curve.max_mult")
                    .defineInRange("MaxPressureMultiplier", 15.0, 2.0, 500.0);
            curveSteepness = builder
                    .comment(" Curve steepness. Higher = more sudden increase near caster.",
                            " 曲线陡峭度。越高=接近施术者时增长越剧烈。",
                            " ",
                            " 1.0 = Linear (gentle) / 线性（平缓）",
                            " 2.5 = Default (moderate) / 默认（适中）",
                            " 4.0 = Steep (aggressive) / 陡峭（激进）")
                    .translation("config.jujutsu_addon.pressure.curve.steepness")
                    .defineInRange("CurveSteepness", 2.5, 1.0, 60.0);
            collisionMinDistance = builder
                    .comment(" Minimum distance for calculations (prevents division by zero).",
                            " 计算用最小距离（防止除零）。")
                    .translation("config.jujutsu_addon.pressure.curve.min_distance")
                    .defineInRange("MinDistance", 0.3, 0.1, 10.0);
            blockPressureRate = builder
                    .comment(" Block pressure accumulation rate per tick.",
                            " Lower = slower block breaking.",
                            " 每tick的方块压力累积速率。越低=破坏越慢。",
                            " ",
                            " 0.06 = Fast (original) / 快速（原始）",
                            " 0.03 = Normal (default) / 正常（默认）",
                            " 0.015 = Slow / 慢速")
                    .translation("config.jujutsu_addon.pressure.curve.block_rate")
                    .defineInRange("BlockPressureRate", 0.03, 0.005, 1.0);
            builder.pop(); // PressureCurve

            // ==================== 投射物 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " PROJECTILES / 投射物",
                    " ═══════════════════════════════════════════════════════════════"
            ).push("Projectiles");

            affectProjectiles = builder
                    .comment(" Whether pressure affects projectiles.",
                            " 是否影响投射物。")
                    .translation("config.jujutsu_addon.pressure.projectile.enable")
                    .define("Enable", true);

            projectileMinLevel = builder
                    .comment(" Minimum pressure level to affect projectiles.",
                            " 影响投射物所需的最低压力等级。")
                    .translation("config.jujutsu_addon.pressure.projectile.min_level")
                    .defineInRange("MinLevel", 1, 0, 10);

            projectileEntrySpeed = builder
                    .comment(" Speed ratio when projectile enters range (0.0-1.0).",
                            " 1.0 = full speed, 0.4 = 40% speed on entry.",
                            " 投射物进入范围时的速度比例。1.0=全速，0.4=进入时40%速度。")
                    .translation("config.jujutsu_addon.pressure.projectile.entry_speed")
                    .defineInRange("EntrySpeed", 0.4, 0.1, 1.0);

            /*projectileStopSpeed = builder
                    .comment(" Speed ratio at stop zone boundary (nearly stopped).",
                            " 停止区边界的速度比例（接近停止）。")
                    .translation("config.jujutsu_addon.pressure.projectile.stop_speed")
                    .defineInRange("StopSpeed", 0.02, 0.001, 0.1);*/

            // ==================== 投射物反弹 ====================
            reflectSpeedMultiplier = builder
                    .comment(" Speed multiplier for reflected projectiles.",
                            " 反弹投射物的速度倍率。")
                    .translation("config.jujutsu_addon.pressure.reflect_speed")
                    .defineInRange("ReflectSpeedMultiplier", 1.5, 0.5, 5.0);

            builder.pop(); // Projectiles

            // ==================== 方块 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " BLOCKS / 方块破坏",
                    " ═══════════════════════════════════════════════════════════════"
            ).push("Blocks");

            actuallyBreakBlocks = builder
                    .comment(" Whether to actually destroy blocks.",
                            " false = only show crack animation.",
                            " 是否真正破坏方块。false=只显示裂痕动画。")
                    .translation("config.jujutsu_addon.pressure.block.actually_break")
                    .define("ActuallyBreak", true);

            dropBlockItems = builder
                    .comment(" Whether broken blocks drop items.",
                            " 破坏的方块是否掉落物品。")
                    .translation("config.jujutsu_addon.pressure.block.drop_items")
                    .define("DropItems", true);

            minPressureForBlockBreak = builder
                    .comment(" Minimum pressure to break blocks.",
                            " 破坏方块所需的最低压力。")
                    .translation("config.jujutsu_addon.pressure.block.min_pressure")
                    .defineInRange("MinPressure", 4.0, 1.0, 15.0);

            breakThresholdMult = builder
                    .comment(" Break threshold = Hardness × this value.",
                            " 破坏阈值 = 硬度 × 此值。")
                    .translation("config.jujutsu_addon.pressure.block.threshold_mult")
                    .defineInRange("ThresholdMult", 10.0, 1.0, 50.0);

            builder.pop(); // Blocks

            // ==================== 方块硬度影响 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " BLOCK HARDNESS EFFECT / 方块硬度影响",
                    " ═══════════════════════════════════════════════════════════════",
                    " ",
                    " When entity is crushed against a block, hardness affects pressure.",
                    " 当实体被压在方块上时，硬度影响压力值。",
                    " ",
                    " [Reference Hardness / 参考硬度值]",
                    " ┌──────────────┬──────────┬─────────────────────┐",
                    " │ Block        │ Hardness │ 方块                │",
                    " ├──────────────┼──────────┼─────────────────────┤",
                    " │ Grass/Dirt   │ 0.5-0.6  │ 草方块/泥土         │",
                    " │ Wood         │ 2.0      │ 木头                │",
                    " │ Stone        │ 1.5      │ 石头                │",
                    " │ Iron Block   │ 5.0      │ 铁块                │",
                    " │ Obsidian     │ 50.0     │ 黑曜石              │",
                    " │ Bedrock      │ -1       │ 基岩（不可破坏）    │",
                    " └──────────────┴──────────┴─────────────────────┘",
                    " ",
                    " Soft block → Low pressure → Low damage, easy to break",
                    " Hard block → High pressure → High damage, hard to break",
                    " 软方块 → 低压力 → 低伤害，容易破坏",
                    " 硬方块 → 高压力 → 高伤害，难以破坏"
            ).push("HardnessEffect");
            softHardnessThreshold = builder
                    .comment(" Hardness below this = soft block. / 低于此值=软方块。")
                    .translation("config.jujutsu_addon.pressure.hardness.soft_threshold")
                    .defineInRange("SoftThreshold", 1.0, 0.1, 5.0);
            hardHardnessThreshold = builder
                    .comment(" Hardness above this = hard block. / 高于此值=硬方块。")
                    .translation("config.jujutsu_addon.pressure.hardness.hard_threshold")
                    .defineInRange("HardThreshold", 10.0, 2.0, 100.0);
            softBlockPressureMult = builder
                    .comment(" Pressure multiplier for soft blocks (grass, dirt).",
                            " 软方块的压力倍率（草、泥土）。")
                    .translation("config.jujutsu_addon.pressure.hardness.soft_mult")
                    .defineInRange("SoftMult", 0.3, 0.1, 1.0);
            normalBlockPressureMult = builder
                    .comment(" Pressure multiplier for normal blocks (stone, wood).",
                            " 普通方块的压力倍率（石头、木头）。")
                    .translation("config.jujutsu_addon.pressure.hardness.normal_mult")
                    .defineInRange("NormalMult", 1.0, 0.5, 2.0);
            hardBlockPressureMult = builder
                    .comment(" Pressure multiplier for hard blocks (obsidian).",
                            " 硬方块的压力倍率（黑曜石）。")
                    .translation("config.jujutsu_addon.pressure.hardness.hard_mult")
                    .defineInRange("HardMult", 2.0, 1.0, 5.0);
            bedrockPressureMult = builder
                    .comment(" Pressure multiplier for unbreakable blocks (bedrock).",
                            " 不可破坏方块的压力倍率（基岩）。")
                    .translation("config.jujutsu_addon.pressure.hardness.bedrock_mult")
                    .defineInRange("BedrockMult", 3.0, 1.5, 10.0);
            builder.pop(); // HardnessEffect

            // ==================== 掉落物 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " DROPPED ITEMS / 掉落物",
                    " ═══════════════════════════════════════════════════════════════"
            ).push("DroppedItems");

            pushDroppedItems = builder
                    .comment(" Whether to push dropped items away.",
                            " 是否推开掉落物。")
                    .translation("config.jujutsu_addon.pressure.items.push")
                    .define("PushItems", true);

            itemPushForceMultiplier = builder
                    .comment(" Force multiplier for pushing items (they're lighter).",
                            " 推开掉落物的力量倍率（它们更轻）。")
                    .translation("config.jujutsu_addon.pressure.items.force_mult")
                    .defineInRange("ForceMult", 2.0, 0.1, 10.0);

            builder.pop(); // DroppedItems

            // ==================== 效果 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " EFFECTS / 效果",
                    " ═══════════════════════════════════════════════════════════════"
            ).push("Effects");

            enableParticles = builder
                    .comment(" Enable particle effects. / 启用粒子效果。")
                    .translation("config.jujutsu_addon.pressure.effects.particles")
                    .define("Particles", true);

            enableSounds = builder
                    .comment(" Enable sound effects. / 启用音效。")
                    .translation("config.jujutsu_addon.pressure.effects.sounds")
                    .define("Sounds", true);

            builder.pop(); // Effects

            // ==================== 交互 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " INTERACTIONS / 交互",
                    " ═══════════════════════════════════════════════════════════════"
            ).push("Interactions");

            respectDomainSureHit = builder
                    .comment(" Domain sure-hit bypasses pressure.",
                            " 领域必中效果穿透压力。")
                    .translation("config.jujutsu_addon.pressure.interact.sure_hit")
                    .define("RespectDomainSureHit", true);

            respectDomainAmplification = builder
                    .comment(" Domain Amplification bypasses pressure.",
                            " 领域增幅穿透压力。")
                    .translation("config.jujutsu_addon.pressure.interact.amplification")
                    .define("RespectDomainAmplification", true);

            builder.pop(); // Interactions

            // ==================== 咒力消耗 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " CURSED ENERGY COST / 咒力消耗",
                    " ═══════════════════════════════════════════════════════════════",
                    " Formula: baseCost × (1 + level × costPerLevel)",
                    " 公式：基础消耗 × (1 + 等级 × 每级增加)"
            ).push("CursedEnergyCost");

            enablePressureCost = builder
                    .comment(" Enable pressure level affecting cost.",
                            " 启用压力等级影响消耗。")
                    .translation("config.jujutsu_addon.pressure.cost.enable")
                    .define("Enable", true);

            baseCursedEnergyCost = builder
                    .comment(" Base cost at level 0 (vanilla=0.8).",
                            " 等级0时的基础消耗（原版=0.8）。")
                    .translation("config.jujutsu_addon.pressure.cost.base")
                    .defineInRange("Base", 0.8, 0.0, 5.0);

            costPerLevel = builder
                    .comment(" Cost multiplier increase per level.",
                            " Example: 0.2 → Level 5 = 0.8 × 2.0 = 1.6",
                            " 每级增加的消耗倍率。示例：0.2→等级5=0.8×2.0=1.6")
                    .translation("config.jujutsu_addon.pressure.cost.per_level")
                    .defineInRange("PerLevel", 0.2, 0.0, 1.0);

            maxCostMultiplier = builder
                    .comment(" Maximum cost multiplier cap. / 最大消耗倍率上限。")
                    .translation("config.jujutsu_addon.pressure.cost.max_mult")
                    .defineInRange("MaxMult", 4.0, 1.0, 10.0);

           builder.pop(); // CursedEnergyCost

            builder.pop(); // 01_Infinity_Pressure
        }
    }
}
