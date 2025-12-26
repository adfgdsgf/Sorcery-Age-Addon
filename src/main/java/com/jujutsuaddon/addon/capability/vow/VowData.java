package com.jujutsuaddon.addon.capability.vow;

import com.jujutsuaddon.addon.config.VowConfig;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.SyncVowListS2CPacket;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.benefit.BenefitCategory;
import com.jujutsuaddon.addon.vow.calculation.VowCalculator;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.manager.DeactivateReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.*;

/**
 * 玩家誓约数据实现类
 * Player Vow Data Implementation
 */
public class VowData implements IVowData {

    private final Map<UUID, CustomBindingVow> vows = new LinkedHashMap<>();
    private final Set<UUID> activeVowIds = new HashSet<>();
    private final Map<String, UUID> occupiedConditionOwners = new HashMap<>();
    private long penaltyEndTime = 0L;
    private boolean dirty = false;
    private LivingEntity owner;

    @Override
    public void init(LivingEntity owner) {
        this.owner = owner;
    }

    // ==================== 誓约管理 ====================

    @Override
    public void addVow(CustomBindingVow vow) {
        vows.put(vow.getVowId(), vow);
        markDirty();
    }

    @Override
    public void removeVow(UUID vowId) {
        CustomBindingVow vow = vows.get(vowId);
        if (vow != null) {
            // ★★★ 核心修改：这里不再做任何权限检查 ★★★
            // 所有的权限检查（是否创造模式、是否永久誓约）都已移至 VowManager.deleteVow
            // 这里只负责执行删除指令
            vows.remove(vowId);
            activeVowIds.remove(vowId);
            releaseOccupancy(vowId);
            markDirty();
        }
    }

    @Override
    public @Nullable CustomBindingVow getVow(UUID vowId) {
        return vows.get(vowId);
    }

    @Override
    public List<CustomBindingVow> getAllVows() {
        return List.copyOf(vows.values());
    }

    @Override
    public List<CustomBindingVow> getActiveVows() {
        return vows.values().stream()
                .filter(v -> v.getState() == VowState.ACTIVE)
                .toList();
    }

    @Override
    public List<CustomBindingVow> getVowsByState(VowState state) {
        return vows.values().stream()
                .filter(v -> v.getState() == state)
                .toList();
    }

    @Override
    public @Nullable ConditionParams getActiveConditionParams(ResourceLocation conditionId) {
        for (CustomBindingVow vow : getActiveVows()) {
            for (ConditionEntry entry : vow.getConditions()) {
                if (entry.getCondition().getId().equals(conditionId)) {
                    return entry.getParams();
                }
            }
        }
        return null;
    }

    // ==================== 消耗一次性收益 ====================

    @Override
    public void consumeOneTimeBenefit(ResourceLocation benefitId) {
        if (VowConfig.isPermanentVowBenefitsEnabled()) {
            return;
        }
        CustomBindingVow targetVow = null;
        for (CustomBindingVow vow : getActiveVows()) {
            if (vow.isPermanent() && vow.hasBenefit(benefitId)) {
                targetVow = vow;
                break;
            }
        }
        if (targetVow != null) {
            deactivateVow(targetVow.getVowId(), this.owner, DeactivateReason.EXHAUSTED);
            if (this.owner instanceof ServerPlayer player) {
                AddonNetwork.sendToPlayer(
                        new SyncVowListS2CPacket(
                                getAllVows(),
                                getOccupiedConditionOwners(),
                                penaltyEndTime
                        ),
                        player
                );
            }
        }
    }

    // ==================== 条件独占逻辑 ====================

    private void recordOccupancy(CustomBindingVow vow) {
        UUID vowId = vow.getVowId();
        for (ConditionEntry entry : vow.getConditions()) {
            String key = entry.getCondition().getOccupancyKey(entry.getParams());
            if (key != null) {
                occupiedConditionOwners.put(key, vowId);
            }
        }
    }

    private void releaseOccupancy(UUID vowId) {
        occupiedConditionOwners.entrySet().removeIf(e -> vowId.equals(e.getValue()));
    }

    @Override
    public boolean canActivateVow(CustomBindingVow vow) {
        UUID vowId = vow.getVowId();
        for (ConditionEntry entry : vow.getConditions()) {
            String key = entry.getCondition().getOccupancyKey(entry.getParams());
            if (key != null) {
                UUID ownerId = occupiedConditionOwners.get(key);
                if (ownerId != null && !ownerId.equals(vowId)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Map<String, UUID> getOccupiedConditionOwners() {
        return Collections.unmodifiableMap(occupiedConditionOwners);
    }

    @Override
    public void clearOccupiedConditions() {
        occupiedConditionOwners.clear();
        markDirty();
    }

    // ==================== 全局惩罚管理 ====================

    @Override
    public long getPenaltyEndTime() {
        return penaltyEndTime;
    }

    @Override
    public void setPenaltyEndTime(long timestamp) {
        this.penaltyEndTime = timestamp;
        markDirty();
        if (this.owner instanceof ServerPlayer player) {
            AddonNetwork.sendToPlayer(
                    new SyncVowListS2CPacket(
                            getAllVows(),
                            getOccupiedConditionOwners(),
                            penaltyEndTime
                    ),
                    player
            );
        }
    }
    @Override
    public boolean isUnderPenalty() {
        return System.currentTimeMillis() < this.penaltyEndTime;
    }

    // ==================== 激活/停用 ====================

    @Override
    public boolean activateVow(UUID vowId, LivingEntity owner) {
        CustomBindingVow vow = vows.get(vowId);
        if (vow == null) return false;

        VowState state = vow.getState();
        if (state == VowState.ACTIVE) return false;
        if (state == VowState.VIOLATED) return false;

        if (!canActivateVow(vow)) return false;

        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return false;

        if (vow.activate(owner, data)) {
            activeVowIds.add(vowId);
            recordOccupancy(vow);
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public void deactivateVow(UUID vowId, LivingEntity owner, DeactivateReason reason) {
        CustomBindingVow vow = vows.get(vowId);
        if (vow == null) return;

        if (vow.getState() != VowState.ACTIVE && reason != DeactivateReason.ADMIN) return;

        // ★★★ 核心修改：这里也不再做权限检查 ★★★
        // 权限检查移至 VowManager.dissolveVow
        // 这里只负责执行停用逻辑

        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return;

        vow.deactivate(owner, data, reason);

        if (reason == DeactivateReason.EXHAUSTED) {
            vow.setState(VowState.EXHAUSTED);
        } else if (reason == DeactivateReason.VIOLATION) {
            vow.setState(VowState.VIOLATED);
        }

        activeVowIds.remove(vowId);
        releaseOccupancy(vowId);
        markDirty();
    }

    @Override
    public void deactivateAllVows(LivingEntity owner, DeactivateReason reason) {
        for (UUID vowId : new ArrayList<>(activeVowIds)) {
            deactivateVow(vowId, owner, reason);
        }
    }

    // ==================== 收益查询 ====================

    @Override
    public float getTotalOutputBonus() {
        float total = 0f;
        for (CustomBindingVow vow : getActiveVows()) {
            for (var entry : vow.getBenefits()) {
                if (entry.getBenefit().getCategory() == BenefitCategory.OUTPUT) {
                    total += entry.getBenefit().getCurrentBonus(null, entry.getParams());
                }
            }
        }
        return Math.min(total, VowCalculator.MAX_OUTPUT_BONUS);
    }

    @Override
    public float getTotalEnergyBonus() {
        float total = 0f;
        for (CustomBindingVow vow : getActiveVows()) {
            for (var entry : vow.getBenefits()) {
                if (entry.getBenefit().getCategory() == BenefitCategory.ENERGY) {
                    total += entry.getBenefit().getCurrentBonus(null, entry.getParams());
                }
            }
        }
        return Math.min(total, VowCalculator.MAX_ENERGY_BONUS);
    }

    @Override
    public float getTotalCooldownReduction() {
        float total = 0f;
        for (CustomBindingVow vow : getActiveVows()) {
            for (var entry : vow.getBenefits()) {
                if (entry.getBenefit().getCategory() == BenefitCategory.COOLDOWN) {
                    total += entry.getBenefit().getCurrentBonus(null, entry.getParams());
                }
            }
        }
        return Math.min(total, VowCalculator.MAX_COOLDOWN_REDUCTION);
    }

    // ==================== NBT序列化 ====================

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag vowList = new ListTag();
        for (CustomBindingVow vow : vows.values()) {
            vowList.add(vow.serializeNBT());
        }
        tag.put("vows", vowList);
        tag.putLong("penaltyEndTime", penaltyEndTime);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        vows.clear();
        activeVowIds.clear();
        occupiedConditionOwners.clear();

        ListTag vowList = nbt.getList("vows", Tag.TAG_COMPOUND);
        for (int i = 0; i < vowList.size(); i++) {
            try {
                CustomBindingVow vow = CustomBindingVow.deserializeNBT(vowList.getCompound(i));
                vows.put(vow.getVowId(), vow);

                if (vow.getState() == VowState.ACTIVE) {
                    activeVowIds.add(vow.getVowId());
                    recordOccupancy(vow);
                }
            } catch (Exception e) {
                System.err.println("[VowData] Failed to load vow: " + e.getMessage());
            }
        }

        if (nbt.contains("penaltyEndTime")) {
            this.penaltyEndTime = nbt.getLong("penaltyEndTime");
        }
    }

    @Override
    public void markDirty() { this.dirty = true; }
    @Override
    public boolean isDirty() { return dirty; }
    @Override
    public void clearDirty() { this.dirty = false; }
}
