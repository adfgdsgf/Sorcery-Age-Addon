// 文件路径: src/main/java/com/jujutsuaddon/addon/balance/ability/AbilityCategory.java
package com.jujutsuaddon.addon.balance.ability;

/**
 * 技能分类枚举
 */
public enum AbilityCategory {

    INSTANT("instant", false),
    TOGGLED("toggled", false),
    CHANNELED("channeled", false),
    ATTACK("attack", false),

    SUMMON_TAMED("summon_tamed", true),
    SUMMON_UNTAMED("summon_untamed", true),
    SUMMON_INSTANT("summon_instant", true),

    EXCLUDED("excluded", false);

    private final String configKey;
    private final boolean isSummonType;

    AbilityCategory(String configKey, boolean isSummonType) {
        this.configKey = configKey;
        this.isSummonType = isSummonType;
    }

    public String getConfigKey() {
        return configKey;
    }

    public boolean isSummon() {
        return isSummonType;
    }

    public boolean shouldBalance() {
        return this != EXCLUDED;
    }

    public boolean isTickBased() {
        return this == TOGGLED || this == CHANNELED || this == SUMMON_TAMED;
    }

    public boolean isOneTimeCost() {
        return this == INSTANT || this == SUMMON_UNTAMED || this == SUMMON_INSTANT;
    }

    public boolean isAttackTriggered() {
        return this == ATTACK;
    }

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
