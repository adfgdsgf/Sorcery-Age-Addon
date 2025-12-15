package com.jujutsuaddon.addon.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 咒灵基准数据
 *
 * 存储每个 EntityType "第一次获得" 时的 data hash 作为永久基准。
 * 用于判断后续获得的同类型咒灵是否为"变体"。
 *
 * 设计原则：
 * - 基准一旦记录就永不改变（除非手动重置）
 * - 即使该咒灵被召唤、死亡、升级，基准依然保留
 */
public class AddonCurseBaselineData {

    // ==================== Capability 注册 ====================

    public static final Capability<AddonCurseBaselineData> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    // ==================== 数据存储 ====================

    /**
     * EntityType ID (String) -> 第一次获得时的 data hash
     * 使用字符串作为 key 方便 NBT 序列化
     */
    private final Map<String, Integer> baselineHashes = new HashMap<>();

    // ==================== 核心操作 ====================

    /**
     * 检查某个 EntityType 是否已有基准记录
     */
    public boolean hasBaseline(EntityType<?> type) {
        return baselineHashes.containsKey(getKey(type));
    }

    /**
     * 获取某个 EntityType 的基准 hash
     * @return 基准 hash，如果没有记录返回 null
     */
    @Nullable
    public Integer getBaseline(EntityType<?> type) {
        return baselineHashes.get(getKey(type));
    }

    /**
     * 记录基准（仅首次有效）
     *
     * @param type 咒灵类型
     * @param dataHash 该咒灵的 data hash
     * @return true = 成功记录（首次），false = 已存在（跳过）
     */
    public boolean recordBaselineIfAbsent(EntityType<?> type, int dataHash) {
        String key = getKey(type);
        if (baselineHashes.containsKey(key)) {
            return false;
        }
        baselineHashes.put(key, dataHash);
        return true;
    }

    /**
     * 判断某个咒灵是否为变体
     *
     * @param type 咒灵类型
     * @param dataHash 该咒灵的 data hash
     * @return true = 是变体（与基准不同），false = 是基准或无记录
     */
    public boolean isVariant(EntityType<?> type, int dataHash) {
        Integer baseline = getBaseline(type);
        return baseline != null && baseline != dataHash;
    }

    /**
     * 获取已记录的 EntityType 数量
     */
    public int getRecordedCount() {
        return baselineHashes.size();
    }

    /**
     * 获取所有已记录的类型 key
     */
    public Set<String> getRecordedTypes() {
        return baselineHashes.keySet();
    }

    // ==================== 同步相关 ====================

    /**
     * 获取所有数据（用于网络同步）
     */
    public Map<String, Integer> getAllBaselines() {
        return new HashMap<>(baselineHashes);
    }

    /**
     * 设置所有数据（用于网络同步）
     */
    public void setAllBaselines(Map<String, Integer> data) {
        baselineHashes.clear();
        baselineHashes.putAll(data);
    }

    // ==================== 序列化 ====================

    private static final String TAG_BASELINES = "CurseBaselines";

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag baselines = new CompoundTag();
        for (Map.Entry<String, Integer> entry : baselineHashes.entrySet()) {
            baselines.putInt(entry.getKey(), entry.getValue());
        }
        tag.put(TAG_BASELINES, baselines);
        return tag;
    }

    public void load(CompoundTag tag) {
        baselineHashes.clear();
        if (tag.contains(TAG_BASELINES, CompoundTag.TAG_COMPOUND)) {
            CompoundTag baselines = tag.getCompound(TAG_BASELINES);
            for (String key : baselines.getAllKeys()) {
                baselineHashes.put(key, baselines.getInt(key));
            }
        }
    }

    // ==================== 工具方法 ====================

    private static String getKey(EntityType<?> type) {
        return EntityType.getKey(type).toString();
    }

    // ==================== Capability Provider ====================

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final AddonCurseBaselineData data = new AddonCurseBaselineData();
        private final LazyOptional<AddonCurseBaselineData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
