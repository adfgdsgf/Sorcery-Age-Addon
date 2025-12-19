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
        public final ForgeConfigSpec.DoubleValue stopZoneParts;
        public final ForgeConfigSpec.DoubleValue pushZoneParts;

        // ==================== 生物压制 - 推力 ====================
        public final ForgeConfigSpec.DoubleValue basePushForce;
        public final ForgeConfigSpec.DoubleValue maxPushForce;
        public final ForgeConfigSpec.DoubleValue breachRepelForce;
        public final ForgeConfigSpec.DoubleValue pinForce;

        // ==================== 生物压制 - 阻力 ====================
        public final ForgeConfigSpec.DoubleValue lateralResistance;
        public final ForgeConfigSpec.DoubleValue escapeResistance;

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
        public final ForgeConfigSpec.DoubleValue pressureToDamage;
        public final ForgeConfigSpec.DoubleValue maxDamagePerHit;
        public final ForgeConfigSpec.IntValue minDamageInterval;
        public final ForgeConfigSpec.IntValue maxDamageInterval;
        public final ForgeConfigSpec.DoubleValue surgeDamageMult;

        // ==================== 投射物 ====================
        public final ForgeConfigSpec.BooleanValue affectProjectiles;
        public final ForgeConfigSpec.IntValue projectileMinLevel;
        public final ForgeConfigSpec.DoubleValue projectileEntrySpeed;
        public final ForgeConfigSpec.DoubleValue projectileStopSpeed;

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
                    " [Range Formula / 范围计算公式]",
                    " Range = Level1 + (Level10 - Level1) × (Level ÷ 10) ^ CurveExponent",
                    " 总范围 = 等级1范围 + (等级10范围 - 等级1范围) × (等级÷10)^曲线指数",
                    " ",
                    " [Default Config Range Table / 默认配置各等级实际范围]",
                    " (Level1=2, Level10=10, Curve=2.0)",
                    " ",
                    " ┌────────┬───────────────────────┬────────┐",
                    " │ Level  │ Calc: 2+8×(n/10)^2    │ Range  │",
                    " │ 等级   │ 计算: 2+8×(n/10)^2    │ 范围   │",
                    " ├────────┼───────────────────────┼────────┤",
                    " │ Lv.1   │ 2 + 8 × 0.01          │ 2.1    │",
                    " │ Lv.2   │ 2 + 8 × 0.04          │ 2.3    │",
                    " │ Lv.3   │ 2 + 8 × 0.09          │ 2.7    │",
                    " │ Lv.4   │ 2 + 8 × 0.16          │ 3.3    │",
                    " │ Lv.5   │ 2 + 8 × 0.25          │ 4.0    │",
                    " │ Lv.6   │ 2 + 8 × 0.36          │ 4.9    │",
                    " │ Lv.7   │ 2 + 8 × 0.49          │ 5.9    │",
                    " │ Lv.8   │ 2 + 8 × 0.64          │ 7.1    │",
                    " │ Lv.9   │ 2 + 8 × 0.81          │ 8.5    │",
                    " │ Lv.10  │ 2 + 8 × 1.00          │ 10.0   │",
                    " └────────┴───────────────────────┴────────┘",
                    " ",
                    " [Zone Division / 区域划分] (Default: Slow=4, Stop=3, Push=3)",
                    " ",
                    "   Player     Push(30%)    Stop(30%)     Slowdown(40%)   Edge",
                    "   玩家       推力区        停止区         减速区          边界",
                    "     ├────30%────┼────30%────┼────40%────┤",
                    " ",
                    " [Zone Distance by Level / 各等级各区域实际距离]",
                    " ┌────────┬────────┬──────────┬──────────┬──────────┐",
                    " │ Level  │ Total  │ Push     │ Stop     │ Slowdown │",
                    " │ 等级   │ 总范围 │ 推力区   │ 停止区   │ 减速区   │",
                    " ├────────┼────────┼──────────┼──────────┼──────────┤",
                    " │ Lv.1   │ 2.1    │ 0~0.6    │ 0.6~1.3  │ 1.3~2.1  │",
                    " │ Lv.5   │ 4.0    │ 0~1.2    │ 1.2~2.4  │ 2.4~4.0  │",
                    " │ Lv.10  │ 10.0   │ 0~3.0    │ 3.0~6.0  │ 6.0~10.0 │",
                    " └────────┴────────┴──────────┴──────────┴──────────┘"
            ).push("RangeAndZones");
// --- Total Range / 总范围 ---
            builder.comment(" ",
                    " Total range settings. / 总范围设置。"
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
                            " [Curve Comparison / 不同曲线指数对比]",
                            " ┌────────┬─────────┬─────────┬─────────┐",
                            " │ Level  │ 1.0 Lin │ 2.0 Sqr │ 3.0 Cub │",
                            " │ 等级   │ 线性    │ 平方    │ 立方    │",
                            " ├────────┼─────────┼─────────┼─────────┤",
                            " │ Lv.3   │ 4.4     │ 2.7     │ 2.2     │",
                            " │ Lv.5   │ 6.0     │ 4.0     │ 3.0     │",
                            " │ Lv.7   │ 7.6     │ 5.9     │ 4.7     │",
                            " │ Lv.10  │ 10.0    │ 10.0    │ 10.0    │",
                            " └────────┴─────────┴─────────┴─────────┘",
                            " ",
                            " 1.0 = Linear (same increase per level) / 线性（每级相同增加）",
                            " 2.0 = Quadratic (high levels stronger) [Recommended] / 平方（高级更强）[推荐]",
                            " 3.0 = Cubic (low weak, high explosive) / 立方（低级极弱，高级爆发）",
                            " 0.5 = Square root (low stronger) / 开方（低级更强）")
                    .translation("config.jujutsu_addon.pressure.range.curve")
                    .defineInRange("CurveExponent", 2.0, 0.5, 4.0);
            builder.pop(); // TotalRange
// --- Zone Ratio / 区域占比 ---
            builder.comment(" ",
                    " Zone ratio (parts). Defines how total range is divided.",
                    " 区域占比（份数），定义总范围如何划分。",
                    " ",
                    " Calculation: ZoneRadius = TotalRange × (ZoneParts ÷ TotalParts)",
                    " 计算方式：某区域半径 = 总范围 × (该区域份数 ÷ 总份数)",
                    " ",
                    " Default: Slowdown=4, Stop=3, Push=3 (Total=10)",
                    " 默认: 减速=4, 停止=3, 推力=3 (总计=10份)",
                    " → Push=30%, Stop=30%, Slowdown=40%"
            ).push("ZoneRatio");

            slowdownZoneParts = builder
                    .comment(" Slowdown zone parts (outermost). / 减速区份数（最外层）。",
                            " Projectiles slow from entry speed to near-stop here.",
                            " 投射物在此区域从入口速度逐渐减速至接近停止。")
                    .translation("config.jujutsu_addon.pressure.zone.slowdown")
                    .defineInRange("Slowdown", 5.0, 0.5, 20.0);

            stopZoneParts = builder
                    .comment(" Stop zone parts (middle layer). / 停止区份数（中间层）。",
                            " Projectiles hover here. Entities pushed strongly.",
                            " 投射物在此区域悬浮，生物被强推。")
                    .translation("config.jujutsu_addon.pressure.zone.stop")
                    .defineInRange("Stop", 2.0, 0.1, 15.0);

            pushZoneParts = builder
                    .comment(" Push zone parts (innermost). / 推力区份数（最内层）。",
                            " Entity entering = Breach. Strong repel + damage.",
                            " 生物进入此区域=突破，会被强推+伤害。")
                    .translation("config.jujutsu_addon.pressure.zone.push")
                    .defineInRange("Push", 3.0, 0.1, 10.0);
            builder.pop(); // ZoneRatio
            builder.pop(); // RangeAndZones

            // ==================== 生物压制 ====================
            builder.comment(" ",
                    " ═══════════════════════════════════════════════════════════════",
                    " ENTITY PRESSURE / 生物压制",
                    " ═══════════════════════════════════════════════════════════════"
            ).push("EntityPressure");

            // --- 推力 ---
            builder.comment(" Push force settings. / 推力设置。").push("Force");

            basePushForce = builder
                    .comment(" Base push force per tick. / 每tick基础推力。")
                    .translation("config.jujutsu_addon.pressure.force.base")
                    .defineInRange("Base", 0.015, 0.001, 0.1);

            maxPushForce = builder
                    .comment(" Maximum push force cap. / 推力上限。")
                    .translation("config.jujutsu_addon.pressure.force.max")
                    .defineInRange("Max", 0.25, 0.05, 1.0);

            breachRepelForce = builder
                    .comment(" Repel force when entity breaches push zone.",
                            " 实体突破推力区时的反推力。")
                    .translation("config.jujutsu_addon.pressure.force.breach")
                    .defineInRange("BreachRepel", 0.2, 0.05, 0.5);

            pinForce = builder
                    .comment(" Extra force when pinned against wall.",
                            " 被压在墙上时的额外力。")
                    .translation("config.jujutsu_addon.pressure.force.pin")
                    .defineInRange("Pin", 0.03, 0.01, 0.2);

            builder.pop(); // Force

            // --- 阻力 ---
            builder.comment(" Resistance settings. / 阻力设置。").push("Resistance");

            lateralResistance = builder
                    .comment(" Resistance to sideways movement. / 侧向移动阻力。")
                    .translation("config.jujutsu_addon.pressure.resist.lateral")
                    .defineInRange("Lateral", 0.7, 0.0, 1.0);

            escapeResistance = builder
                    .comment(" Resistance when trying to escape. / 逃跑时的阻力。")
                    .translation("config.jujutsu_addon.pressure.resist.escape")
                    .defineInRange("Escape", 0.85, 0.0, 1.0);

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
                    " Damage is dealt when entity is pushed into a wall.",
                    " 当实体被推到墙上时造成伤害。"
            ).push("Damage");

            minPressureForDamage = builder
                    .comment(" Minimum pressure to deal damage (requires wall collision).",
                            " 造成伤害所需的最低压力（需要撞墙）。")
                    .translation("config.jujutsu_addon.pressure.damage.min_pressure")
                    .defineInRange("MinPressure", 2.0, 0.5, 10.0);

            pressureToDamage = builder
                    .comment(" Damage per pressure point. / 每点压力的伤害。")
                    .translation("config.jujutsu_addon.pressure.damage.per_pressure")
                    .defineInRange("DamagePerPressure", 0.15, 0.01, 1.0);

            maxDamagePerHit = builder
                    .comment(" Maximum damage per hit. / 单次伤害上限。")
                    .translation("config.jujutsu_addon.pressure.damage.max")
                    .defineInRange("MaxPerHit", 15.0, 1.0, 100.0);

            minDamageInterval = builder
                    .comment(" Minimum ticks between damage. / 伤害间隔下限（tick）。")
                    .translation("config.jujutsu_addon.pressure.damage.min_interval")
                    .defineInRange("MinInterval", 2, 1, 20);

            maxDamageInterval = builder
                    .comment(" Maximum ticks between damage. / 伤害间隔上限（tick）。")
                    .translation("config.jujutsu_addon.pressure.damage.max_interval")
                    .defineInRange("MaxInterval", 15, 5, 60);

            surgeDamageMult = builder
                    .comment(" Damage multiplier for sudden pressure surge.",
                            " 压力突变时的伤害倍率。")
                    .translation("config.jujutsu_addon.pressure.damage.surge_mult")
                    .defineInRange("SurgeMult", 1.5, 1.0, 5.0);

            builder.pop(); // Damage

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

            projectileStopSpeed = builder
                    .comment(" Speed ratio at stop zone boundary (nearly stopped).",
                            " 停止区边界的速度比例（接近停止）。")
                    .translation("config.jujutsu_addon.pressure.projectile.stop_speed")
                    .defineInRange("StopSpeed", 0.02, 0.001, 0.1);

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
