package com.jujutsuaddon.addon.vow.penalty;

import com.jujutsuaddon.addon.api.vow.IPenalty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 惩罚条目
 * Penalty Entry
 *
 * 包装一个惩罚实例及其参数
 */
public class PenaltyEntry {

    /** 惩罚实例 */
    private final IPenalty penalty;

    /** 惩罚参数 */
    private final PenaltyParams params;

    public PenaltyEntry(IPenalty penalty, PenaltyParams params) {
        this.penalty = penalty;
        this.params = params != null ? params : new PenaltyParams();
    }

    // ==================== Getters ====================

    public IPenalty getPenalty() {
        return penalty;
    }

    public PenaltyParams getParams() {
        return params;
    }

    // ==================== NBT序列化 ====================

    /**
     * 序列化为NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("penaltyId", penalty.getId().toString());
        tag.put("params", params.serializeNBT());
        return tag;
    }

    /**
     * 从NBT反序列化
     */
    @Nullable
    public static PenaltyEntry deserializeNBT(CompoundTag tag) {
        try {
            ResourceLocation penaltyId = new ResourceLocation(tag.getString("penaltyId"));
            IPenalty penalty = PenaltyRegistry.get(penaltyId);

            if (penalty == null) {
                // 惩罚类型不存在（可能是版本更新后移除了）
                return null;
            }

            PenaltyParams params = PenaltyParams.deserializeNBT(tag.getCompound("params"));
            return new PenaltyEntry(penalty, params);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取惩罚显示名称
     */
    public String getDisplayName() {
        return penalty.getDisplayName().getString();
    }

    /**
     * 获取惩罚ID
     */
    public ResourceLocation getId() {
        return penalty.getId();
    }

    @Override
    public String toString() {
        return "PenaltyEntry{" +
                "penalty=" + penalty.getId() +
                ", params=" + params +
                '}';
    }
}
