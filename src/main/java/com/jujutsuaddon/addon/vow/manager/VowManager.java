package com.jujutsuaddon.addon.vow.manager;

import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.api.vow.IPenalty;
import com.jujutsuaddon.addon.capability.vow.IVowData;
import com.jujutsuaddon.addon.capability.vow.VowDataProvider;
import com.jujutsuaddon.addon.config.VowConfig;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.SyncVowListS2CPacket;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.ViolationRecord;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.calculation.ValidationResult;
import com.jujutsuaddon.addon.vow.calculation.VowCalculator;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;
import com.jujutsuaddon.addon.vow.penalty.PenaltyEntry;
import com.jujutsuaddon.addon.vow.penalty.PenaltyParams;
import com.jujutsuaddon.addon.vow.penalty.PenaltyRegistry;
import com.jujutsuaddon.addon.vow.penalty.ViolationContext;
import com.jujutsuaddon.addon.vow.validation.CheckContext;
import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.*;

/**
 * 誓约管理器
 * Vow Manager
 */
public class VowManager {

    // ==================== ★★★ 权限总控 (Master Switch) ★★★ ====================

    /**
     * 检查实体是否有特权绕过限制
     * 目前逻辑：创造模式玩家 = 有特权
     */
    public static boolean hasPrivilege(LivingEntity entity) {
        return entity instanceof Player p && p.isCreative();
    }

    // ==================== 状态控制 ====================
    private static final ThreadLocal<Boolean> SKIPPING_BENEFITS = ThreadLocal.withInitial(() -> false);

    public static void setSkippingBenefits(boolean skipping) {
        SKIPPING_BENEFITS.set(skipping);
    }

    public static boolean isSkippingBenefits() {
        return SKIPPING_BENEFITS.get();
    }

    // ==================== 获取誓约数据 ====================

    @Nullable
    public static IVowData getVowData(LivingEntity entity) {
        return entity.getCapability(VowDataProvider.VOW_DATA).orElse(null);
    }

    public static Collection<CustomBindingVow> getPlayerVows(LivingEntity entity) {
        IVowData data = getVowData(entity);
        if (data == null) return Collections.emptyList();
        return data.getAllVows();
    }

    @Nullable
    public static CustomBindingVow getVow(LivingEntity entity, UUID vowId) {
        IVowData data = getVowData(entity);
        if (data == null) return null;
        return data.getVow(vowId);
    }

    public static Map<String, UUID> getOccupiedConditionOwners(LivingEntity owner) {
        IVowData data = getVowData(owner);
        if (data == null) return Collections.emptyMap();
        return data.getOccupiedConditionOwners();
    }

    // ==================== 违约检测 ====================

    @Nullable
    public static ViolationRecord checkViolation(LivingEntity owner, CheckContext context) {
        IVowData data = getVowData(owner);
        if (data == null) return null;

        for (CustomBindingVow vow : data.getActiveVows()) {
            ViolationRecord violation = vow.checkViolation(owner, context);
            if (violation != null) {
                handleViolation(owner, vow, violation, context);
                return violation;
            }
        }
        return null;
    }

    @Nullable
    public static ViolationRecord checkOnAbilityUse(LivingEntity owner,
                                                    radon.jujutsu_kaisen.ability.base.Ability ability) {
        CheckContext context = CheckContext.builder(CheckTrigger.ABILITY_ATTEMPT, owner.level().getGameTime())
                .ability(ability)
                .build();
        return checkViolation(owner, context);
    }

    public static void checkOnTick(LivingEntity owner) {
        CheckContext context = CheckContext.builder(CheckTrigger.SECOND, owner.level().getGameTime())
                .build();
        checkViolation(owner, context);
    }

    // ==================== 违约处理 ====================

    public static void handleViolation(LivingEntity owner, CustomBindingVow vow) {
        IVowData data = getVowData(owner);
        if (data == null) return;

        applyGlobalPenalty(owner, data, vow);
        data.deactivateVow(vow.getVowId(), owner, DeactivateReason.VIOLATION);

        if (owner instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable(
                    "vow.message.violated",
                    vow.getName()
            ).withStyle(ChatFormatting.RED));
            syncToClient(player);
        }
    }

    public static void handleViolation(LivingEntity owner, CustomBindingVow vow,
                                       ViolationRecord violation, CheckContext checkContext) {
        IVowData data = getVowData(owner);
        if (data == null) return;

        ISorcererData sorcererData = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return;

        ICondition violatedCondition = findViolatedCondition(vow, violation);

        ViolationContext context = new ViolationContext(
                vow,
                violation,
                violatedCondition,
                owner.level().getGameTime()
        );

        if (checkContext.getAbility() != null) {
            context.withAbility(checkContext.getAbility());
        }

        executePenalties(owner, vow, sorcererData, context);
        applyGlobalPenalty(owner, data, vow);
        data.deactivateVow(vow.getVowId(), owner, DeactivateReason.VIOLATION);

        if (owner instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable(
                    "vow.message.violated",
                    vow.getName()
            ).withStyle(ChatFormatting.RED));
            syncToClient(player);
        }
    }

    @Nullable
    private static ICondition findViolatedCondition(CustomBindingVow vow, ViolationRecord violation) {
        for (ConditionEntry entry : vow.getConditions()) {
            if (entry.getCondition().getId().equals(violation.getConditionId())) {
                return entry.getCondition();
            }
        }
        return null;
    }

    private static void executePenalties(LivingEntity owner, CustomBindingVow vow,
                                         ISorcererData sorcererData, ViolationContext context) {
        ICondition violatedCondition = context.getViolatedCondition();
        if (violatedCondition != null) {
            ResourceLocation defaultPenaltyId = violatedCondition.getDefaultPenaltyId();
            if (defaultPenaltyId != null) {
                IPenalty defaultPenalty = PenaltyRegistry.get(defaultPenaltyId);
                if (defaultPenalty != null) {
                    PenaltyParams defaultParams = violatedCondition.getDefaultPenaltyParams();
                    defaultPenalty.execute(owner, sorcererData, defaultParams, context);

                    if (owner instanceof ServerPlayer player) {
                        player.sendSystemMessage(Component.translatable(
                                "vow.penalty.executed",
                                defaultPenalty.getDisplayName()
                        ).withStyle(ChatFormatting.DARK_RED));
                    }
                }
            }
        }

        for (PenaltyEntry entry : vow.getPenalties()) {
            entry.getPenalty().execute(owner, sorcererData, entry.getParams(), context);

            if (owner instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable(
                        "vow.penalty.executed",
                        entry.getPenalty().getDisplayName()
                ).withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    // ==================== 收益查询 ====================

    public static float getActiveOutputBonus(LivingEntity owner) {
        if (isSkippingBenefits()) return 0f;
        IVowData data = getVowData(owner);
        if (data == null) return 0f;
        return data.getTotalOutputBonus();
    }

    public static float calculateTotalOutputBonus(LivingEntity owner) {
        return getActiveOutputBonus(owner);
    }

    public static float getActiveEnergyBonus(LivingEntity owner) {
        if (isSkippingBenefits()) return 0f;
        IVowData data = getVowData(owner);
        if (data == null) return 0f;
        return data.getTotalEnergyBonus();
    }

    public static float getActiveCooldownReduction(LivingEntity owner) {
        if (isSkippingBenefits()) return 0f;
        IVowData data = getVowData(owner);
        if (data == null) return 0f;
        return data.getTotalCooldownReduction();
    }

    public static void consumeBenefit(LivingEntity owner, ResourceLocation benefitId) {
        // 无限模式下不消耗 (配置优先)
        if (VowConfig.isPermanentVowBenefitsEnabled()) {
            return;
        }

        IVowData data = getVowData(owner);
        if (data == null) return;

        CustomBindingVow targetVow = null;
        for (CustomBindingVow vow : data.getActiveVows()) {
            if (vow.hasBenefit(benefitId)) {
                targetVow = vow;
                break;
            }
        }
        if (targetVow != null) {
            data.deactivateVow(targetVow.getVowId(), owner, DeactivateReason.EXHAUSTED);
            if (owner instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable(
                        "vow.message.exhausted",
                        targetVow.getName()
                ).withStyle(ChatFormatting.GRAY));
                syncToClient(player);
            }
        }
    }

    // ==================== 誓约创建与管理 ====================

    public static CreateVowResult createVow(LivingEntity owner, CustomBindingVow vow) {
        IVowData data = getVowData(owner);
        if (data == null) {
            return CreateVowResult.error("vow.error.no_capability");
        }
        if (vow.getConditions().size() != 1 || vow.getBenefits().size() != 1) {
            return CreateVowResult.error("vow.error.invalid_structure");
        }
        for (ConditionEntry entry : vow.getConditions()) {
            if (!entry.getCondition().isAvailable(owner)) {
                return CreateVowResult.error("vow.error.requirement_not_met");
            }
        }
        for (BenefitEntry entry : vow.getBenefits()) {
            if (!entry.getBenefit().isAvailable(owner)) {
                return CreateVowResult.error("vow.error.requirement_not_met");
            }
        }
        ValidationResult validation = VowCalculator.validateVowBalance(vow);
        if (!validation.isValid()) {
            return CreateVowResult.error(validation.getErrorMessage().getString());
        }
        int maxVows = VowConfig.getMaxActiveVows();
        if (data.getActiveVows().size() >= maxVows) {
            return CreateVowResult.error("vow.error.max_vows_reached", maxVows);
        }
        data.addVow(vow);
        if (owner instanceof ServerPlayer player) {
            syncToClient(player);
        }
        return CreateVowResult.success(vow);
    }

    public static boolean activateVow(LivingEntity owner, UUID vowId) {
        IVowData data = getVowData(owner);
        if (data == null) return false;

        // ★ 使用总控开关
        boolean hasPrivilege = hasPrivilege(owner);

        // 1. 检查全局惩罚冷却 (有特权则绕过)
        if (!hasPrivilege && data.isUnderPenalty()) {
            if (owner instanceof ServerPlayer player) {
                long timeLeft = (data.getPenaltyEndTime() - System.currentTimeMillis()) / 1000;
                player.sendSystemMessage(Component.translatable("vow.error.under_penalty", timeLeft)
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }

        CustomBindingVow vow = data.getVow(vowId);
        if (vow == null) return false;

        // 2. 检查条件是否已被占用 (有特权则绕过)
        // 注意：这里我们允许特权玩家叠加誓约，方便测试
        if (!hasPrivilege && !data.canActivateVow(vow)) {
            if (owner instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("vow.error.combination_used")
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }

        boolean success = data.activateVow(vowId, owner);
        if (success && owner instanceof ServerPlayer player) {
            syncToClient(player);
        }
        return success;
    }

    public static DissolveResult dissolveVow(LivingEntity owner, UUID vowId) {
        IVowData data = getVowData(owner);
        if (data == null) return DissolveResult.error("vow.error.no_capability");

        CustomBindingVow vow = data.getVow(vowId);
        if (vow == null) return DissolveResult.error("vow.error.not_found");

        // ★ 使用总控开关
        boolean hasPrivilege = hasPrivilege(owner);

        // 永久誓约无法解除 (特权除外)
        if (vow.isPermanent() && !hasPrivilege) {
            return DissolveResult.error("vow.error.permanent_cannot_dissolve");
        }

        if (vow.getState() == VowState.DISSOLVED) {
            return DissolveResult.error("vow.error.not_active");
        }

        // 如果是 ACTIVE 状态手动解除，才触发惩罚 (特权除外)
        if (vow.getState() == VowState.ACTIVE && !hasPrivilege) {
            applyGlobalPenalty(owner, data, vow);
        }

        data.deactivateVow(vowId, owner, DeactivateReason.DISSOLVED);
        if (owner instanceof ServerPlayer player) {
            syncToClient(player);
        }
        return DissolveResult.success();
    }

    /**
     * 删除誓约
     */
    public static boolean deleteVow(LivingEntity owner, UUID vowId) {
        IVowData data = getVowData(owner);
        if (data == null) return false;

        CustomBindingVow vow = data.getVow(vowId);
        if (vow == null) return false;

        // ★ 使用总控开关
        boolean hasPrivilege = hasPrivilege(owner);

        // 只能删除非激活状态的誓约
        if (vow.getState() == VowState.ACTIVE) {
            if (hasPrivilege) {
                // 特权玩家：强制停用并删除
                data.deactivateVow(vowId, owner, DeactivateReason.DISSOLVED);
            } else {
                // 普通玩家：禁止删除激活誓约
                return false;
            }
        }

        // 执行删除 (VowData.removeVow 已经不再做权限检查了)
        data.removeVow(vowId);

        if (owner instanceof ServerPlayer player) {
            syncToClient(player);
        }
        return true;
    }

    public static void forceRemoveVow(LivingEntity owner, UUID vowId) {
        IVowData data = getVowData(owner);
        if (data == null) return;

        CustomBindingVow vow = data.getVow(vowId);
        if (vow == null) return;

        if (vow.getState() == VowState.ACTIVE) {
            data.deactivateVow(vowId, owner, DeactivateReason.DISSOLVED);
        }
        data.removeVow(vowId);
        if (owner instanceof ServerPlayer player) {
            syncToClient(player);
        }
    }

    // ==================== 辅助方法 ====================

    private static void applyGlobalPenalty(LivingEntity owner, IVowData data, CustomBindingVow vow) {
        long duration = VowCalculator.calculatePenaltyDuration(vow);
        long endTime = System.currentTimeMillis() + duration;

        if (data.getPenaltyEndTime() < endTime) {
            data.setPenaltyEndTime(endTime);
        }

        if (owner instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable(
                    "vow.message.penalty_applied",
                    duration / 1000
            ).withStyle(ChatFormatting.RED));
        }
    }

    public static boolean isAbilityBanned(LivingEntity player, radon.jujutsu_kaisen.ability.base.Ability ability) {
        return getBanViolation(player, ability) != null;
    }

    @Nullable
    public static ViolationRecord getBanViolation(LivingEntity player, radon.jujutsu_kaisen.ability.base.Ability ability) {
        if (player == null || ability == null) return null;
        long gameTime = player.level().getGameTime();
        CheckContext context = CheckContext.builder(CheckTrigger.ABILITY_ATTEMPT, gameTime)
                .ability(ability)
                .build();
        IVowData data = getVowData(player);
        if (data == null) return null;

        for (CustomBindingVow vow : data.getActiveVows()) {
            for (ConditionEntry entry : vow.getConditions()) {
                if (entry.getCondition().isViolated(player, entry.getParams(), context)) {
                    return new ViolationRecord(
                            vow.getVowId(),
                            entry.getCondition().getId(),
                            CheckTrigger.ABILITY_ATTEMPT,
                            gameTime
                    );
                }
            }
        }
        return null;
    }

    // ==================== 网络同步 ====================

    public static void syncToClient(ServerPlayer player) {
        IVowData data = getVowData(player);
        if (data == null) return;
        AddonNetwork.sendToPlayer(
                new SyncVowListS2CPacket(
                        data.getAllVows(),
                        data.getOccupiedConditionOwners(),
                        data.getPenaltyEndTime()
                ),
                player
        );
    }

    public static void handleSyncFromServer(LivingEntity owner, CompoundTag nbt) {
        IVowData data = getVowData(owner);
        if (data == null) return;
        data.deserializeNBT(nbt);
    }

    // ==================== Mixin 调用 ====================

    public static boolean checkAbilityAttempt(LivingEntity owner, radon.jujutsu_kaisen.ability.base.Ability ability) {
        if (owner == null || ability == null) return true;
        CheckContext context = CheckContext.builder(CheckTrigger.ABILITY_ATTEMPT, owner.level().getGameTime())
                .ability(ability)
                .build();
        IVowData data = getVowData(owner);
        if (data == null) return true;

        for (CustomBindingVow vow : data.getActiveVows()) {
            for (ConditionEntry entry : vow.getConditions()) {
                if (entry.getCondition().isViolated(owner, entry.getParams(), context)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void onAbilityExecuted(LivingEntity owner, radon.jujutsu_kaisen.ability.base.Ability ability) {
        if (owner == null || ability == null) return;
        CheckContext context = CheckContext.builder(CheckTrigger.ABILITY_EXECUTED, owner.level().getGameTime())
                .ability(ability)
                .build();
        checkViolation(owner, context);
    }
}
