package com.jujutsuaddon.addon.util.calc;

/**
 * 技能分类枚举 - 重构版
 *
 * 核心原则：同类型技能之间才进行比较，不同类型不做归一化
 */
public enum AbilityCategory {

    // =========== 普通技能 ===========

    /** 瞬发技能：一次性消耗，有冷却 (苍、赫、蜘蛛) */
    INSTANT("instant", false),

    /** 切换技能：每tick消耗 (无下限) */
    TOGGLED("toggled", false),

    /** 引导技能：引导期间每tick消耗，有固定持续时间 (极之番) */
    CHANNELED("channeled", false),

    /** 攻击增强技能：每次攻击触发消耗 (蓝拳、赫拳) - IAttack */
    ATTACK("attack", false),

    // =========== 召唤物 ===========

    /** 调伏式神：每tick消耗维持 (玉犬、调伏魔虚罗) */
    SUMMON_TAMED("summon_tamed", true),

    /** 未调伏式神：一次性召唤消耗 (未调伏魔虚罗、仪式召唤) */
    SUMMON_UNTAMED("summon_untamed", true),

    /** 瞬发召唤物：一次性消耗，非式神 (改造人) */
    SUMMON_INSTANT("summon_instant", true),

    // =========== 排除 ===========

    /** 排除：不参与平衡计算 (体术、反转术式、功能性技能) */
    EXCLUDED("excluded", false);

    private final String configKey;
    private final boolean isSummonType;

    AbilityCategory(String configKey, boolean isSummonType) {
        this.configKey = configKey;
        this.isSummonType = isSummonType;
    }

    /**
     * 配置文件中的键名
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 是否是召唤物类别
     */
    public boolean isSummon() {
        return isSummonType;
    }

    /**
     * 是否应该参与平衡计算
     */
    public boolean shouldBalance() {
        return this != EXCLUDED;
    }

    /**
     * 是否是每tick消耗类型（用于计算公式选择）
     */
    public boolean isTickBased() {
        return this == TOGGLED || this == CHANNELED || this == SUMMON_TAMED;
    }

    /**
     * 是否是一次性消耗类型
     */
    public boolean isOneTimeCost() {
        return this == INSTANT || this == SUMMON_UNTAMED || this == SUMMON_INSTANT;
    }

    /**
     * 是否是攻击触发类型
     */
    public boolean isAttackTriggered() {
        return this == ATTACK;
    }

    /**
     * 获取该分类的消耗计算描述（用于调试）
     */
    public String getCostFormula() {
        switch (this) {
            case INSTANT:
            case SUMMON_UNTAMED:
            case SUMMON_INSTANT:
                return "cost (单次)";
            case TOGGLED:
            case SUMMON_TAMED:
                return "cost × 20 (每秒)";
            case CHANNELED:
                return "cost × duration (总消耗)";
            case ATTACK:
                return "cost × 攻速 (每秒估算)";
            default:
                return "N/A";
        }
    }
}
