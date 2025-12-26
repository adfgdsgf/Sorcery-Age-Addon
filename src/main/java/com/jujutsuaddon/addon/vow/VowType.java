package com.jujutsuaddon.addon.vow;

/**
 * 誓约类型枚举
 * Vow Type
 *
 * 定义誓约的持久性类型，影响：
 * - 是否可以主动解除
 * - 收益的加成倍率（永久誓约加成更高）
 * - 违约惩罚的严重程度
 */
public enum VowType {

    /**
     * 永久誓约
     * 一旦激活无法主动解除，只能通过违约结束
     * 收益加成倍率更高（约1.5x）
     * 违约惩罚更严重
     */
    PERMANENT("vow.type.permanent", 1.5f),

    /**
     * 可解除誓约
     * 可以随时主动解除
     * 收益加成倍率正常（1.0x）
     * 违约惩罚相对较轻
     */
    DISSOLVABLE("vow.type.dissolvable", 1.0f);

    /** 本地化键 */
    private final String translationKey;

    /** 收益加成倍率 */
    private final float benefitMultiplier;

    VowType(String translationKey, float benefitMultiplier) {
        this.translationKey = translationKey;
        this.benefitMultiplier = benefitMultiplier;
    }

    /**
     * 获取本地化键
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * 获取收益加成倍率
     * 永久誓约因为风险更高，所以收益也更高
     */
    public float getBenefitMultiplier() {
        return benefitMultiplier;
    }
}
