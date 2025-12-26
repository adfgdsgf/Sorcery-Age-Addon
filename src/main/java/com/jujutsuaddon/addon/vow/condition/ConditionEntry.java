package com.jujutsuaddon.addon.vow.condition;

import com.jujutsuaddon.addon.api.vow.ICondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 条件条目
 * Condition Entry
 */
public class ConditionEntry {

    /** 条件实现 */
    private final ICondition condition;

    /** 条件的配置参数 (去掉 final 以便修改) */
    private ConditionParams params;

    public ConditionEntry(ICondition condition, ConditionParams params) {
        this.condition = condition;
        this.params = params;
    }

    public ICondition getCondition() {
        return condition;
    }

    public ConditionParams getParams() {
        return params;
    }

    /**
     * ★ 新增：允许更新参数
     */
    public void setParams(ConditionParams params) {
        this.params = params;
    }

    /**
     * 序列化为NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("conditionId", condition.getId().toString());
        tag.put("params", params.serializeNBT());
        return tag;
    }

    /**
     * 从NBT反序列化
     */
    @Nullable
    public static ConditionEntry deserializeNBT(CompoundTag tag) {
        ResourceLocation id = new ResourceLocation(tag.getString("conditionId"));
        ICondition condition = ConditionRegistry.get(id);
        if (condition == null) {
            return null;
        }

        ConditionParams params = ConditionParams.fromNBT(tag.getCompound("params"));
        return new ConditionEntry(condition, params);
    }
}
