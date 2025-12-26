package com.jujutsuaddon.addon.vow.benefit.types.permanent;

import com.jujutsuaddon.addon.api.vow.IBenefit;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitCategory;
import com.jujutsuaddon.addon.vow.benefit.BenefitParams;
import com.jujutsuaddon.addon.vow.manager.VowManager;
import com.jujutsuaddon.addon.vow.util.VowConditionHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;

public class WorldSlashBypassBenefit implements IBenefit {

    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "world_slash_bypass");

    /**
     * 外部调用入口：当玩家释放世界斩时调用
     */
    public static void onWorldSlashCast(LivingEntity caster) {
        if (caster.level().isClientSide) return;
        // 调用 Manager 消耗收益
        VowManager.consumeBenefit(caster, ID);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("benefit.jujutsu_addon.world_slash_bypass");
    }

    @Override
    public Component getDescription(BenefitParams params) {
        return Component.translatable("benefit.jujutsu_addon.world_slash_bypass.desc");
    }

    @Override
    public float getRequiredWeight(BenefitParams params) {
        // 永久誓约不计算权重平衡，返回 0 即可
        return 0.0f;
    }

    @Override
    public boolean isScalable() {
        return false;
    }

    @Override
    public float getMaxBonus() {
        return 1.0f;
    }

    @Override
    public BenefitCategory getCategory() {
        return BenefitCategory.MECHANIC;
    }

    @Override
    public void apply(LivingEntity owner, ISorcererData data, BenefitParams params) {
        // Mixin 自动处理
    }

    @Override
    public void remove(LivingEntity owner, ISorcererData data, BenefitParams params) {
        // Mixin 自动处理
    }

    @Override
    public float getCurrentBonus(@Nullable LivingEntity owner, BenefitParams params) {
        return 1.0f;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return null;
    }

    @Override
    public BenefitParams createDefaultParams() {
        return new BenefitParams();
    }

    @Override
    public CompoundTag serializeParams(BenefitParams params) {
        return params.serializeNBT();
    }

    @Override
    public BenefitParams deserializeParams(CompoundTag nbt) {
        return BenefitParams.fromNBT(nbt);
    }
    @Override
    public boolean isAvailable(LivingEntity owner) {
        // 调用工具类检查：只有拥有御厨子术式的玩家可用
        return VowConditionHelper.hasTechniqueAccess(owner, CursedTechnique.SHRINE);
    }
    @Override
    public VowType getAllowedVowType() {
        // 如果你希望这个只能在“永久誓约”分类下看到
        return VowType.PERMANENT;

        // 如果你没写这个方法，默认是 DISSOLVABLE (普通/可解除)
    }
}
