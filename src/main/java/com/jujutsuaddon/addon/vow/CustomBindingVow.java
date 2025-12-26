package com.jujutsuaddon.addon.vow;

import com.jujutsuaddon.addon.api.vow.IBenefit;
import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.api.vow.IPenalty;
import com.jujutsuaddon.addon.config.VowConfig; // ★ 导入配置
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.benefit.BenefitParams;
import com.jujutsuaddon.addon.vow.calculation.ValidationResult;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.manager.DeactivateReason;
import com.jujutsuaddon.addon.vow.penalty.PenaltyEntry;
import com.jujutsuaddon.addon.vow.penalty.PenaltyParams;
import com.jujutsuaddon.addon.vow.validation.CheckContext;
import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 自定义束缚誓约
 * Custom Binding Vow
 */
public class CustomBindingVow {

    /** 誓约唯一标识符 */
    private final UUID vowId;

    /** 誓约持有者UUID */
    private final UUID ownerId;

    /** 玩家给誓约起的名字 */
    private final String name;

    /** 誓约类型（永久/可解除） */
    private final VowType type;

    /** 条件列表 */
    private final List<ConditionEntry> conditions;

    /** 收益列表 */
    private final List<BenefitEntry> benefits;

    /** 惩罚列表（玩家选择的额外惩罚） */
    private final List<PenaltyEntry> penalties;

    /** 创建时间戳 */
    private final long createdTime;

    /** 当前状态 (内部存储的真实状态) */
    private VowState state;

    /** 激活时间（游戏刻） */
    private long activatedTime;

    /** 停用时间（游戏刻） */
    private long deactivatedTime;

    /** 最后一次违约记录 */
    @Nullable
    private ViolationRecord lastViolation;

    // ==================== 构造器 ====================

    private CustomBindingVow(Builder builder) {
        this.vowId = builder.vowId != null ? builder.vowId : UUID.randomUUID();
        this.ownerId = builder.ownerId;
        this.name = builder.name;
        this.type = builder.type;
        this.conditions = new ArrayList<>(builder.conditions);
        this.benefits = new ArrayList<>(builder.benefits);
        this.penalties = new ArrayList<>(builder.penalties);
        this.createdTime = builder.createdTime;
        this.state = VowState.INACTIVE;
        this.activatedTime = -1;
        this.deactivatedTime = -1;
    }

    /**
     * 创建誓约构建器
     */
    public static Builder builder(UUID ownerId) {
        return new Builder(ownerId);
    }

    // ==================== 权重计算 ====================

    public float calculateTotalWeight() {
        return (float) conditions.stream()
                .mapToDouble(entry -> entry.getCondition().calculateWeight(entry.getParams()))
                .sum();
    }

    public float calculatePenaltyBonusWeight() {
        return (float) penalties.stream()
                .mapToDouble(entry -> entry.getPenalty().getBonusWeight(entry.getParams()))
                .sum();
    }

    public float calculateTotalAvailableWeight() {
        return calculateTotalWeight() + calculatePenaltyBonusWeight();
    }

    public float calculateTotalCost() {
        return (float) benefits.stream()
                .mapToDouble(entry -> entry.getBenefit().getRequiredWeight(entry.getParams()))
                .sum();
    }

    public float calculateRemainingWeight() {
        return calculateTotalAvailableWeight() - calculateTotalCost();
    }

    // ==================== 验证 ====================

    public ValidationResult validate() {
        if (conditions.isEmpty()) {
            return ValidationResult.error("vow.error.no_conditions");
        }
        if (benefits.isEmpty()) {
            return ValidationResult.error("vow.error.no_benefits");
        }
        if (calculateRemainingWeight() < 0) {
            return ValidationResult.error("vow.error.insufficient_weight");
        }
        return ValidationResult.success();
    }

    // ==================== 激活与停用 ====================

    public boolean activate(LivingEntity owner, ISorcererData data) {
        // ★ 允许 INACTIVE 或 DISSOLVED 状态激活
        // 注意：这里检查的是真实状态 state，而不是 getState()
        if (state != VowState.INACTIVE && state != VowState.DISSOLVED) {
            return false;
        }

        for (ConditionEntry entry : conditions) {
            entry.getCondition().onActivate(owner, entry.getParams());
        }

        for (BenefitEntry entry : benefits) {
            entry.getBenefit().apply(owner, data, entry.getParams());
        }

        this.state = VowState.ACTIVE;
        this.activatedTime = owner.level().getGameTime();
        return true;
    }

    public void deactivate(LivingEntity owner, ISorcererData data, DeactivateReason reason) {
        // 检查真实状态
        if (state != VowState.ACTIVE) {
            return;
        }
        // 1. 移除收益
        for (BenefitEntry entry : benefits) {
            entry.getBenefit().remove(owner, data, entry.getParams());
        }
        // 2. 移除条件
        if (!isPermanent()) {
            for (ConditionEntry entry : conditions) {
                entry.getCondition().onDeactivate(owner, entry.getParams());
            }
        }
        this.state = (reason == DeactivateReason.VIOLATION) ? VowState.VIOLATED : VowState.DISSOLVED;
        this.deactivatedTime = owner.level().getGameTime();
    }

    // ==================== 违约检测 ====================

    @Nullable
    public ViolationRecord checkViolation(LivingEntity owner, CheckContext context) {
        // ★★★ 修改：使用 getState() 检查逻辑状态 ★★★
        // 这样即使真实状态是 EXHAUSTED，只要 getState() 返回 ACTIVE，就会进行检查
        if (getState() != VowState.ACTIVE) {
            return null;
        }

        for (ConditionEntry entry : conditions) {
            ICondition condition = entry.getCondition();

            if (!shouldCheckForTrigger(condition, context.getTrigger())) {
                continue;
            }

            if (condition.isViolated(owner, entry.getParams(), context)) {
                ViolationRecord violation = new ViolationRecord(
                        this.vowId,
                        condition.getId(),
                        context.getTrigger(),
                        context.getGameTime()
                );
                this.lastViolation = violation;
                return violation;
            }
        }
        return null;
    }

    private boolean shouldCheckForTrigger(ICondition condition, CheckTrigger trigger) {
        if (condition.requiresTickCheck() && trigger == CheckTrigger.TICK) {
            return true;
        }
        for (CheckTrigger t : condition.getTriggers()) {
            if (t == trigger) {
                return true;
            }
        }
        return false;
    }

    // ==================== NBT 序列化 ====================

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("vowId", vowId);
        tag.putUUID("ownerId", ownerId);
        tag.putString("name", name);
        tag.putString("type", type.name());
        tag.putString("state", state.name()); // 保存真实状态
        tag.putLong("createdTime", createdTime);
        tag.putLong("activatedTime", activatedTime);
        tag.putLong("deactivatedTime", deactivatedTime);

        ListTag conditionList = new ListTag();
        for (ConditionEntry entry : conditions) {
            conditionList.add(entry.serializeNBT());
        }
        tag.put("conditions", conditionList);

        ListTag benefitList = new ListTag();
        for (BenefitEntry entry : benefits) {
            benefitList.add(entry.serializeNBT());
        }
        tag.put("benefits", benefitList);

        ListTag penaltyList = new ListTag();
        for (PenaltyEntry entry : penalties) {
            penaltyList.add(entry.serializeNBT());
        }
        tag.put("penalties", penaltyList);

        if (lastViolation != null) {
            tag.put("lastViolation", lastViolation.serializeNBT());
        }
        return tag;
    }

    public static CustomBindingVow deserializeNBT(CompoundTag tag) {
        UUID ownerId = tag.getUUID("ownerId");
        Builder builder = builder(ownerId)
                .vowId(tag.getUUID("vowId"))
                .name(tag.getString("name"))
                .type(VowType.valueOf(tag.getString("type")))
                .createdTime(tag.getLong("createdTime"));

        ListTag conditionList = tag.getList("conditions", Tag.TAG_COMPOUND);
        for (int i = 0; i < conditionList.size(); i++) {
            ConditionEntry entry = ConditionEntry.deserializeNBT(conditionList.getCompound(i));
            if (entry != null) builder.addCondition(entry);
        }

        ListTag benefitList = tag.getList("benefits", Tag.TAG_COMPOUND);
        for (int i = 0; i < benefitList.size(); i++) {
            BenefitEntry entry = BenefitEntry.deserializeNBT(benefitList.getCompound(i));
            if (entry != null) builder.addBenefit(entry);
        }

        ListTag penaltyList = tag.getList("penalties", Tag.TAG_COMPOUND);
        for (int i = 0; i < penaltyList.size(); i++) {
            PenaltyEntry entry = PenaltyEntry.deserializeNBT(penaltyList.getCompound(i));
            if (entry != null) builder.addPenalty(entry);
        }

        CustomBindingVow vow = builder.build();
        vow.state = VowState.valueOf(tag.getString("state"));
        vow.activatedTime = tag.getLong("activatedTime");
        vow.deactivatedTime = tag.getLong("deactivatedTime");

        if (tag.contains("lastViolation")) {
            vow.lastViolation = ViolationRecord.deserializeNBT(tag.getCompound("lastViolation"));
        }
        return vow;
    }

    // ==================== Getters ====================

    public boolean hasBenefit(ResourceLocation benefitId) {
        for (BenefitEntry entry : benefits) {
            if (entry.getBenefit().getId().equals(benefitId)) {
                return true;
            }
        }
        return false;
    }

    public UUID getVowId() { return vowId; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public VowType getType() { return type; }

    /**
     * ★★★ 核心修改：获取“有效状态” ★★★
     * 这是所有逻辑和GUI应该调用的方法。
     * 如果开启了无限模式，且真实状态是 EXHAUSTED，则伪装成 ACTIVE。
     */
    public VowState getState() {
        // 1. 获取原始存储的状态
        VowState rawState = this.state;

        // 2. 拦截：如果原始状态是“已耗尽”
        if (rawState == VowState.EXHAUSTED) {
            // 3. 检查总控配置：如果开启了无限模式
            if (VowConfig.isPermanentVowBenefitsEnabled()) {
                // 4. 欺骗调用者：告诉它我是“激活”的
                return VowState.ACTIVE;
            }
        }
        // 其他情况返回真实状态
        return rawState;
    }

    /**
     * ★ 新增：获取原始状态 (仅供序列化保存或底层逻辑使用)
     */
    public VowState getRawState() {
        return this.state;
    }

    public List<ConditionEntry> getConditions() { return List.copyOf(conditions); }
    public List<BenefitEntry> getBenefits() { return List.copyOf(benefits); }
    public List<PenaltyEntry> getPenalties() { return List.copyOf(penalties); }
    public long getCreatedTime() { return createdTime; }
    public long getActivatedTime() { return activatedTime; }
    public long getDeactivatedTime() { return deactivatedTime; }

    // isActive 调用 getState()，所以也会自动适配
    public boolean isActive() { return getState() == VowState.ACTIVE; }
    public boolean isPermanent() { return type == VowType.PERMANENT; }

    @Nullable
    public ViolationRecord getLastViolation() { return lastViolation; }

    // ==================== Builder ====================

    public static class Builder {
        private UUID vowId;
        private final UUID ownerId;
        private String name = "Unnamed Vow";
        private VowType type = VowType.DISSOLVABLE;
        private final List<ConditionEntry> conditions = new ArrayList<>();
        private final List<BenefitEntry> benefits = new ArrayList<>();
        private final List<PenaltyEntry> penalties = new ArrayList<>();
        private long createdTime;

        private Builder(UUID ownerId) {
            this.ownerId = ownerId;
            this.createdTime = System.currentTimeMillis();
        }

        public Builder vowId(UUID id) { this.vowId = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(VowType type) { this.type = type; return this; }
        public Builder createdTime(long time) { this.createdTime = time; return this; }

        public Builder addCondition(ICondition condition, ConditionParams params) {
            this.conditions.add(new ConditionEntry(condition, params));
            return this;
        }
        public Builder addCondition(ConditionEntry entry) {
            this.conditions.add(entry);
            return this;
        }
        public Builder addBenefit(IBenefit benefit, BenefitParams params) {
            this.benefits.add(new BenefitEntry(benefit, params));
            return this;
        }
        public Builder addBenefit(BenefitEntry entry) {
            this.benefits.add(entry);
            return this;
        }
        public Builder addPenalty(IPenalty penalty, PenaltyParams params) {
            this.penalties.add(new PenaltyEntry(penalty, params));
            return this;
        }
        public Builder addPenalty(PenaltyEntry entry) {
            this.penalties.add(entry);
            return this;
        }
        public CustomBindingVow build() {
            return new CustomBindingVow(this);
        }
    }

    public void setState(VowState state) {
        this.state = state;
    }
}
