// src/main/java/com/jujutsuaddon/addon/mixin/access/IInfinityPressureAccessor.java
package com.jujutsuaddon.addon.api;

/**
 * 无下限压制等级访问器
 * 通过 (IInfinityPressureAccessor) sorcererData 来访问
 */
public interface IInfinityPressureAccessor {

    /**
     * 获取无下限压制等级 (0-10)
     * 0 = 关闭压制, 10 = 最大压制
     */
    int jujutsuAddon$getInfinityPressure();

    /**
     * 设置无下限压制等级
     */
    void jujutsuAddon$setInfinityPressure(int level);

    /**
     * 增加压制等级
     */
    void jujutsuAddon$increaseInfinityPressure();

    /**
     * 减少压制等级
     */
    void jujutsuAddon$decreaseInfinityPressure();
}
