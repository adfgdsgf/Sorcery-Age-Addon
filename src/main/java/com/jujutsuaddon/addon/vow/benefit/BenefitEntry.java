package com.jujutsuaddon.addon.vow.benefit;

import com.jujutsuaddon.addon.api.vow.IBenefit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 收益条目
 * Benefit Entry
 */
public class BenefitEntry {

    /** 收益实现 */
    private final IBenefit benefit;

    /** 收益的配置参数 (去掉 final 以便修改) */
    private BenefitParams params;

    public BenefitEntry(IBenefit benefit, BenefitParams params) {
        this.benefit = benefit;
        this.params = params;
    }

    public IBenefit getBenefit() {
        return benefit;
    }

    public BenefitParams getParams() {
        return params;
    }

    /**
     * ★ 新增：允许更新参数
     */
    public void setParams(BenefitParams params) {
        this.params = params;
    }

    /**
     * 序列化为NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("benefitId", benefit.getId().toString());
        tag.put("params", params.serializeNBT());
        return tag;
    }

    /**
     * 从NBT反序列化
     */
    @Nullable
    public static BenefitEntry deserializeNBT(CompoundTag tag) {
        ResourceLocation id = new ResourceLocation(tag.getString("benefitId"));
        IBenefit benefit = BenefitRegistry.get(id);
        if (benefit == null) {
            return null;
        }

        BenefitParams params = BenefitParams.fromNBT(tag.getCompound("params"));
        return new BenefitEntry(benefit, params);
    }
}
