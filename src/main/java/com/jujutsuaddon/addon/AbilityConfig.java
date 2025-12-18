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

        // ========== 总开关 ==========
        public final ForgeConfigSpec.BooleanValue enableInfinityPressure;

        // ========== 范围 ==========
        public final ForgeConfigSpec.DoubleValue level0Range;
        public final ForgeConfigSpec.DoubleValue level1Range;
        public final ForgeConfigSpec.DoubleValue level10Range;

        // ========== 停止距离 ==========
        public final ForgeConfigSpec.DoubleValue level1HaltDistance;
        public final ForgeConfigSpec.DoubleValue level5HaltDistance;
        public final ForgeConfigSpec.DoubleValue level10HaltDistance;
        public final ForgeConfigSpec.DoubleValue haltTransitionZone;

        // ========== 压力值 ==========
        public final ForgeConfigSpec.DoubleValue basePressure;
        public final ForgeConfigSpec.DoubleValue distanceDecay;
        public final ForgeConfigSpec.DoubleValue approachMultiplier;
        public final ForgeConfigSpec.DoubleValue breachPressureMult;

        // ========== 推力/阻力 ==========
        public final ForgeConfigSpec.DoubleValue basePushForce;
        public final ForgeConfigSpec.DoubleValue minLevelMult;
        public final ForgeConfigSpec.DoubleValue maxLevelMult;
        public final ForgeConfigSpec.DoubleValue maxPushForce;
        public final ForgeConfigSpec.DoubleValue pinForce;
        public final ForgeConfigSpec.DoubleValue haltRepelForce;
        public final ForgeConfigSpec.DoubleValue breachRepelForce;

        // ========== 阻力系统 ==========
        public final ForgeConfigSpec.DoubleValue lateralResistance;
        public final ForgeConfigSpec.DoubleValue escapeResistance;
        public final ForgeConfigSpec.DoubleValue approachResistanceBase;
        public final ForgeConfigSpec.DoubleValue approachResistanceMax;

        // ========== 压力阈值 ==========
        public final ForgeConfigSpec.DoubleValue minPressureForDamage;
        public final ForgeConfigSpec.DoubleValue minPressureForBlockBreak;
        public final ForgeConfigSpec.DoubleValue minPressureForPush;

        // ========== 方块破坏 ==========
        public final ForgeConfigSpec.DoubleValue pressureDecayRate;
        public final ForgeConfigSpec.DoubleValue breakThresholdMult;
        public final ForgeConfigSpec.IntValue breakStages;
        public final ForgeConfigSpec.LongValue pressureTimeoutMs;
        public final ForgeConfigSpec.BooleanValue actuallyBreakBlocks;  // ★ 新增
        public final ForgeConfigSpec.BooleanValue dropBlockItems;       // ★ 新增

        // ========== 投射物 ==========
        public final ForgeConfigSpec.BooleanValue affectProjectiles;
        public final ForgeConfigSpec.IntValue projectileMinPressure;
        public final ForgeConfigSpec.DoubleValue projectileSlowdownRate;
        public final ForgeConfigSpec.DoubleValue projectileMinSpeed;
        public final ForgeConfigSpec.DoubleValue projectileRepelForce;
        public final ForgeConfigSpec.DoubleValue projectileReflectSpeedMult;
        public final ForgeConfigSpec.DoubleValue projectileReflectMinSpeed;
        public final ForgeConfigSpec.DoubleValue projectileReflectMaxSpeed;
        public final ForgeConfigSpec.IntValue projectileReflectImmuneTicks;

        // ========== 伤害 ==========
        public final ForgeConfigSpec.DoubleValue pressureToDamage;
        public final ForgeConfigSpec.DoubleValue pressureChangeDamageMult;
        public final ForgeConfigSpec.IntValue maxDamageInterval;
        public final ForgeConfigSpec.IntValue minDamageInterval;
        public final ForgeConfigSpec.DoubleValue intervalPressureScale;
        public final ForgeConfigSpec.DoubleValue maxDamagePerHit;

        // ========== 伤害预兆 ==========
        public final ForgeConfigSpec.IntValue damageWarningTicks;
        public final ForgeConfigSpec.DoubleValue pressureSurgeThreshold;
        public final ForgeConfigSpec.DoubleValue surgeDamageMult;

        // ========== 效果 ==========
        public final ForgeConfigSpec.BooleanValue enablePressureParticles;
        public final ForgeConfigSpec.BooleanValue enablePressureSounds;

        // ========== 掉落物 ========== ★ 新增
        public final ForgeConfigSpec.BooleanValue pushDroppedItems;
        public final ForgeConfigSpec.DoubleValue itemPushForceMultiplier;
        public final ForgeConfigSpec.IntValue itemPushMinPressure;

        // ========== 领域交互 ========== ★ 添加这两行！ ★
        public final ForgeConfigSpec.BooleanValue respectDomainSureHit;
        public final ForgeConfigSpec.BooleanValue respectDomainAmplification;

        public Common(ForgeConfigSpec.Builder builder) {

            builder.push("01_Infinity_Pressure");

            builder.comment(
                    " ",
                    "================================================================================",
                    " INFINITY PRESSURE SYSTEM / 无下限压力系统",
                    "================================================================================",
                    " "
            );

            // --- 总开关 ---
            enableInfinityPressure = builder
                    .comment(" ", " Enable or disable the entire Infinity Pressure system.",
                            " 启用或禁用整个无下限压力系统。")
                    .translation("config.jujutsu_addon.infinity_pressure.enable")
                    .define("EnableInfinityPressure", true);

            // ==================== 范围 ====================
            builder.comment(" ", "========== Range Settings / 范围设置 ==========").push("Range");

            level0Range = builder
                    .comment(" Range when pressure level is 0.", " 压力等级为0时的范围。")
                    .translation("config.jujutsu_addon.infinity_pressure.range.level0")
                    .defineInRange("Level0Range", 0.5, 0.1, 20.0);

            level1Range = builder
                    .comment(" Range at pressure level 1.", " 压力等级1时的范围。")
                    .translation("config.jujutsu_addon.infinity_pressure.range.level1")
                    .defineInRange("Level1Range", 1.5, 0.5, 30.0);

            level10Range = builder
                    .comment(" Range at pressure level 10.", " 压力等级10时的范围。")
                    .translation("config.jujutsu_addon.infinity_pressure.range.level10")
                    .defineInRange("Level10Range", 9.0, 3.0, 50.0);

            builder.pop();

            // ==================== 停止距离 ====================
            builder.comment(" ", "========== Halt Distance / 停止距离 ==========").push("HaltDistance");

            level1HaltDistance = builder
                    .comment(" Halt distance at level 1 (blocks).", " 等级1的停止距离（格）。")
                    .translation("config.jujutsu_addon.infinity_pressure.halt.level1")
                    .defineInRange("Level1HaltDistance", 0.3, 0.1, 5.0);

            level5HaltDistance = builder
                    .comment(" Halt distance at level 5 (blocks).", " 等级5的停止距离（格）。")
                    .translation("config.jujutsu_addon.infinity_pressure.halt.level5")
                    .defineInRange("Level5HaltDistance", 2.5, 0.5, 10.0);

            level10HaltDistance = builder
                    .comment(" Halt distance at level 10 (blocks).", " 等级10的停止距离（格）。")
                    .translation("config.jujutsu_addon.infinity_pressure.halt.level10")
                    .defineInRange("Level10HaltDistance", 6.0, 2.0, 20.0);

            haltTransitionZone = builder
                    .comment(" Width of the transition zone (blocks).", " 过渡区宽度（格）。")
                    .translation("config.jujutsu_addon.infinity_pressure.halt.transition")
                    .defineInRange("HaltTransitionZone", 1.5, 0.5, 5.0);

            builder.pop();

            // ==================== 压力值 ====================
            builder.comment(" ", "========== Pressure Value / 压力值计算 ==========").push("PressureValue");

            basePressure = builder
                    .comment(" Base pressure value.", " 基础压力值。")
                    .translation("config.jujutsu_addon.infinity_pressure.pressure.base")
                    .defineInRange("BasePressure", 1.0, 0.1, 10.0);

            distanceDecay = builder
                    .comment(" How quickly pressure decays with distance.", " 压力随距离衰减的速度。")
                    .translation("config.jujutsu_addon.infinity_pressure.pressure.decay")
                    .defineInRange("DistanceDecay", 2.0, 0.5, 10.0);

            approachMultiplier = builder
                    .comment(" Pressure bonus when moving towards target.", " 向目标移动时的压力加成。")
                    .translation("config.jujutsu_addon.infinity_pressure.pressure.approach")
                    .defineInRange("ApproachMultiplier", 2.0, 1.0, 5.0);

            breachPressureMult = builder
                    .comment(" Pressure multiplier when breaching halt boundary.", " 突破停止边界时的压力倍率。")
                    .translation("config.jujutsu_addon.infinity_pressure.pressure.breach")
                    .defineInRange("BreachPressureMult", 3.0, 1.0, 10.0);

            builder.pop();

            // ==================== 推力 ====================
            builder.comment(" ", "========== Push Force / 推力设置 ==========").push("Force");

            basePushForce = builder
                    .comment(" Base push force per tick.", " 每tick的基础推力。")
                    .translation("config.jujutsu_addon.infinity_pressure.force.base")
                    .defineInRange("BasePushForce", 0.015, 0.001, 0.1);

            minLevelMult = builder
                    .comment(" Level multiplier at level 1.", " 等级1的等级倍率。")
                    .translation("config.jujutsu_addon.infinity_pressure.force.min_level")
                    .defineInRange("MinLevelMult", 0.1, 0.01, 1.0);

            maxLevelMult = builder
                    .comment(" Level multiplier at level 10.", " 等级10的等级倍率。")
                    .translation("config.jujutsu_addon.infinity_pressure.force.max_level")
                    .defineInRange("MaxLevelMult", 1.0, 0.5, 5.0);

            maxPushForce = builder
                    .comment(" Maximum push force.", " 推力上限。")
                    .translation("config.jujutsu_addon.infinity_pressure.force.max")
                    .defineInRange("MaxPushForce", 0.25, 0.1, 1.0);

            pinForce = builder
                    .comment(" Extra force when pinned against wall.", " 被压到墙上时的额外力。")
                    .translation("config.jujutsu_addon.infinity_pressure.force.pin")
                    .defineInRange("PinForce", 0.03, 0.01, 0.2);

            haltRepelForce = builder
                    .comment(" Repel force at halt boundary.", " 停止边界处的反推力。")
                    .translation("config.jujutsu_addon.infinity_pressure.force.halt_repel")
                    .defineInRange("HaltRepelForce", 0.08, 0.01, 0.3);

            breachRepelForce = builder
                    .comment(" Strong repel force when breaching.", " 突破时的强反推力。")
                    .translation("config.jujutsu_addon.infinity_pressure.force.breach_repel")
                    .defineInRange("BreachRepelForce", 0.2, 0.05, 0.5);

            builder.pop();

            // ==================== 阻力 ====================
            builder.comment(" ", "========== Resistance / 阻力设置 ==========").push("Resistance");

            lateralResistance = builder
                    .comment(" Resistance to sideways movement.", " 侧向移动阻力。")
                    .translation("config.jujutsu_addon.infinity_pressure.resist.lateral")
                    .defineInRange("LateralResistance", 0.7, 0.0, 1.0);

            escapeResistance = builder
                    .comment(" Resistance when escaping.", " 逃跑时的阻力。")
                    .translation("config.jujutsu_addon.infinity_pressure.resist.escape")
                    .defineInRange("EscapeResistance", 0.85, 0.0, 1.0);

            approachResistanceBase = builder
                    .comment(" Base approach resistance.", " 接近时的基础阻力。")
                    .translation("config.jujutsu_addon.infinity_pressure.resist.approach_base")
                    .defineInRange("ApproachResistanceBase", 0.3, 0.0, 1.0);

            approachResistanceMax = builder
                    .comment(" Max resistance at halt boundary.", " 停止边界处的最大阻力。")
                    .translation("config.jujutsu_addon.infinity_pressure.resist.approach_max")
                    .defineInRange("ApproachResistanceMax", 0.95, 0.5, 1.0);

            builder.pop();

            // ==================== 阈值 ====================
            builder.comment(" ", "========== Thresholds / 触发阈值 ==========").push("Thresholds");

            minPressureForPush = builder
                    .comment(" Min pressure to push.", " 触发推力的最低压力。")
                    .translation("config.jujutsu_addon.infinity_pressure.threshold.push")
                    .defineInRange("MinPressureForPush", 0.5, 0.0, 5.0);

            minPressureForDamage = builder
                    .comment(" Min pressure to damage.", " 造成伤害的最低压力。")
                    .translation("config.jujutsu_addon.infinity_pressure.threshold.damage")
                    .defineInRange("MinPressureForDamage", 2.0, 0.5, 10.0);

            minPressureForBlockBreak = builder
                    .comment(" Min pressure to break blocks.", " 破坏方块的最低压力。")
                    .translation("config.jujutsu_addon.infinity_pressure.threshold.block")
                    .defineInRange("MinPressureForBlockBreak", 4.0, 1.0, 15.0);

            builder.pop();

            // ==================== 方块破坏 ====================
            builder.comment(" ", "========== Block Breaking / 方块破坏 ==========").push("BlockBreaking");

            pressureDecayRate = builder
                    .comment(" Pressure decay rate per tick.", " 每tick压力衰减速度。")
                    .translation("config.jujutsu_addon.infinity_pressure.block.decay")
                    .defineInRange("PressureDecayRate", 0.2, 0.01, 1.0);

            breakThresholdMult = builder
                    .comment(" Break threshold = Hardness * this.", " 破坏阈值 = 硬度 × 此值。")
                    .translation("config.jujutsu_addon.infinity_pressure.block.threshold")
                    .defineInRange("BreakThresholdMult", 10.0, 1.0, 50.0);

            breakStages = builder
                    .comment(" Visual break stages.", " 可视破坏阶段数。")
                    .translation("config.jujutsu_addon.infinity_pressure.block.stages")
                    .defineInRange("BreakStages", 10, 1, 10);

            pressureTimeoutMs = builder
                    .comment(" Timeout before pressure resets (ms).", " 压力重置超时时间（毫秒）。")
                    .translation("config.jujutsu_addon.infinity_pressure.block.timeout")
                    .defineInRange("PressureTimeoutMs", 2000L, 500L, 10000L);

            // ★ 新增配置项 ★
            actuallyBreakBlocks = builder
                    .comment(" ",
                            " Whether to actually destroy blocks.",
                            " true = Blocks are destroyed.",
                            " false = Only show crack animation, blocks don't break.",
                            "----------------------------------------------------------------",
                            " 是否真正破坏方块。",
                            " true = 方块会被破坏。",
                            " false = 只显示裂痕动画，方块不会被破坏。")
                    .translation("config.jujutsu_addon.infinity_pressure.block.actually_break")
                    .define("ActuallyBreakBlocks", true);

            dropBlockItems = builder
                    .comment(" ",
                            " Whether destroyed blocks drop items.",
                            " Only applies when ActuallyBreakBlocks is true.",
                            "----------------------------------------------------------------",
                            " 被破坏的方块是否掉落物品。",
                            " 仅在 ActuallyBreakBlocks 为 true 时有效。")
                    .translation("config.jujutsu_addon.infinity_pressure.block.drop_items")
                    .define("DropBlockItems", true);

            builder.pop();

            // ==================== 投射物 ====================
            builder.comment(" ", "========== Projectiles / 投射物 ==========").push("Projectiles");
            affectProjectiles = builder
                    .comment(" ",
                            " Whether pressure affects projectiles (arrows, fireballs, etc.).",
                            " This replaces the original Infinity freeze effect with gradual slowdown.",
                            "----------------------------------------------------------------",
                            " 压力是否影响投射物（箭矢、火球等）。",
                            " 这会用渐进减速替代原版的冻结效果。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.enable")
                    .define("AffectProjectiles", true);
            projectileMinPressure = builder
                    .comment(" ",
                            " Minimum pressure level to affect projectiles.",
                            " 影响投射物所需的最低压力等级。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.min_level")
                    .defineInRange("ProjectileMinPressure", 1, 0, 10);
            projectileSlowdownRate = builder
                    .comment(" ",
                            " How quickly projectiles slow down (0.0-1.0).",
                            " Higher = faster slowdown.",
                            "----------------------------------------------------------------",
                            " 投射物减速的速度（0.0-1.0）。",
                            " 越高 = 减速越快。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.slowdown")
                    .defineInRange("ProjectileSlowdownRate", 0.15, 0.01, 1.0);
            projectileMinSpeed = builder
                    .comment(" ",
                            " Minimum speed before projectile is considered 'stopped'.",
                            " Projectiles below this speed will hover in place.",
                            "----------------------------------------------------------------",
                            " 被视为'停止'的最低速度。",
                            " 低于此速度的投射物会悬浮在原地。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.min_speed")
                    .defineInRange("ProjectileMinSpeed", 0.01, 0.001, 0.1);
            projectileRepelForce = builder
                    .comment(" ",
                            " Force applied to push back projectiles that breach the halt boundary.",
                            " 突破停止边界时推回投射物的力。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.repel")
                    .defineInRange("ProjectileRepelForce", 0.1, 0.01, 0.5);

            projectileReflectSpeedMult = builder
                    .comment(" ",
                            " Multiplier for reflected projectile speed.",
                            " Based on: original approach speed × pressure level × this value.",
                            "----------------------------------------------------------------",
                            " 反弹速度倍率。",
                            " 基于：原接近速度 × 压力等级 × 此值。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.reflect_mult")
                    .defineInRange("ProjectileReflectSpeedMult", 0.8, 0.1, 3.0);
            projectileReflectMinSpeed = builder
                    .comment(" ",
                            " Minimum speed for a projectile to be considered 'reflected'.",
                            " Below this, projectile just hovers.",
                            "----------------------------------------------------------------",
                            " 被视为'反弹'的最低速度。",
                            " 低于此值只会悬浮。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.reflect_min")
                    .defineInRange("ProjectileReflectMinSpeed", 0.3, 0.1, 1.0);
            projectileReflectMaxSpeed = builder
                    .comment(" ",
                            " Maximum reflected speed cap.",
                            " 反弹速度上限。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.reflect_max")
                    .defineInRange("ProjectileReflectMaxSpeed", 2.5, 1.0, 5.0);
            projectileReflectImmuneTicks = builder
                    .comment(" ",
                            " Ticks of immunity after being reflected.",
                            " During this time, projectile won't be slowed again.",
                            "----------------------------------------------------------------",
                            " 反弹后的豁免时间（tick）。",
                            " 在此期间投射物不会再被减速。")
                    .translation("config.jujutsu_addon.infinity_pressure.projectile.reflect_immune")
                    .defineInRange("ProjectileReflectImmuneTicks", 15, 5, 60);

            builder.pop();

            // ==================== 伤害 ====================
            builder.comment(" ", "========== Damage / 伤害设置 ==========").push("Damage");

            pressureToDamage = builder
                    .comment(" Damage per pressure point.", " 每点压力的伤害。")
                    .translation("config.jujutsu_addon.infinity_pressure.damage.base")
                    .defineInRange("PressureToDamage", 0.15, 0.01, 1.0);

            pressureChangeDamageMult = builder
                    .comment(" Extra damage from pressure changes.", " 压力变化的额外伤害。")
                    .translation("config.jujutsu_addon.infinity_pressure.damage.change")
                    .defineInRange("PressureChangeDamageMult", 0.5, 0.0, 2.0);

            maxDamageInterval = builder
                    .comment(" Max ticks between damage.", " 伤害间隔上限。")
                    .translation("config.jujutsu_addon.infinity_pressure.damage.max_interval")
                    .defineInRange("MaxDamageInterval", 15, 5, 40);

            minDamageInterval = builder
                    .comment(" Min ticks between damage.", " 伤害间隔下限。")
                    .translation("config.jujutsu_addon.infinity_pressure.damage.min_interval")
                    .defineInRange("MinDamageInterval", 2, 1, 10);

            intervalPressureScale = builder
                    .comment(" How pressure affects interval.", " 压力对间隔的影响。")
                    .translation("config.jujutsu_addon.infinity_pressure.damage.interval_scale")
                    .defineInRange("IntervalPressureScale", 1.5, 0.5, 5.0);

            maxDamagePerHit = builder
                    .comment(" Maximum damage per tick.", " 每次伤害上限。")
                    .translation("config.jujutsu_addon.infinity_pressure.damage.max")
                    .defineInRange("MaxDamagePerHit", 15.0, 1.0, 100.0);

            builder.pop();

            // ==================== 伤害预兆 ====================
            builder.comment(" ", "========== Damage Warning / 伤害预兆 ==========").push("DamageWarning");

            damageWarningTicks = builder
                    .comment(" Warning ticks before damage.", " 伤害前的预警tick数。")
                    .translation("config.jujutsu_addon.infinity_pressure.warning.ticks")
                    .defineInRange("DamageWarningTicks", 5, 0, 20);

            pressureSurgeThreshold = builder
                    .comment(" Pressure change for surge mode.", " 触发冲击模式的压力变化。")
                    .translation("config.jujutsu_addon.infinity_pressure.warning.surge_threshold")
                    .defineInRange("PressureSurgeThreshold", 5.0, 1.0, 20.0);

            surgeDamageMult = builder
                    .comment(" Surge damage multiplier.", " 冲击伤害倍率。")
                    .translation("config.jujutsu_addon.infinity_pressure.warning.surge_mult")
                    .defineInRange("SurgeDamageMult", 1.5, 1.0, 5.0);

            builder.pop();

            // ==================== 效果 ====================
            builder.comment(" ", "========== Effects / 效果设置 ==========").push("Effects");

            enablePressureParticles = builder
                    .comment(" Enable particles.", " 启用粒子效果。")
                    .translation("config.jujutsu_addon.infinity_pressure.effects.particles")
                    .define("EnablePressureParticles", true);

            enablePressureSounds = builder
                    .comment(" Enable sounds.", " 启用音效。")
                    .translation("config.jujutsu_addon.infinity_pressure.effects.sounds")
                    .define("EnablePressureSounds", true);

            builder.pop();

            // ==================== 掉落物 ==================== ★ 新增
            builder.comment(" ", "========== Dropped Items / 掉落物推开 ==========").push("DroppedItems");

            pushDroppedItems = builder
                    .comment(" ",
                            " Whether to push dropped items away.",
                            " 是否推开掉落物。")
                    .translation("config.jujutsu_addon.infinity_pressure.items.push")
                    .define("PushDroppedItems", true);

            itemPushForceMultiplier = builder
                    .comment(" ",
                            " Force multiplier for pushing items.",
                            " Items are lighter, so they fly further.",
                            "----------------------------------------------------------------",
                            " 推开掉落物的力量倍率。",
                            " 掉落物比生物轻，会飞得更远。")
                    .translation("config.jujutsu_addon.infinity_pressure.items.force_mult")
                    .defineInRange("ItemPushForceMultiplier", 2.0, 0.1, 10.0);

            itemPushMinPressure = builder
                    .comment(" ",
                            " Minimum pressure level to push items.",
                            " 推开掉落物所需的最低压力等级。")
                    .translation("config.jujutsu_addon.infinity_pressure.items.min_pressure")
                    .defineInRange("ItemPushMinPressure", 1, 0, 10);

            builder.pop();

            // ==================== 领域交互 ====================
            builder.comment(" ", "========== Domain Interaction / 领域交互 ==========").push("DomainInteraction");
            respectDomainSureHit = builder
                    .comment(" ",
                            " Whether domain sure-hit effect bypasses pressure.",
                            " true = Domain owners can approach through pressure.",
                            " false = Pressure always works (not lore-accurate).",
                            "----------------------------------------------------------------",
                            " 领域必中是否能穿透压力。",
                            " true = 领域所有者可以穿透压力接近。",
                            " false = 压力始终生效（不符合原作设定）。")
                    .translation("config.jujutsu_addon.infinity_pressure.domain.respect_sure_hit")
                    .define("RespectDomainSureHit", true);
            respectDomainAmplification = builder
                    .comment(" ",
                            " Whether Domain Amplification bypasses pressure.",
                            " 领域增幅是否能穿透压力。")
                    .translation("config.jujutsu_addon.infinity_pressure.domain.respect_amplification")
                    .define("RespectDomainAmplification", true);
            builder.pop();

            builder.pop(); // 结束 01_Infinity_Pressure
        }
    }
}
