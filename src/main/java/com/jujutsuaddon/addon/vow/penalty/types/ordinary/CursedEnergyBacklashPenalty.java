package com.jujutsuaddon.addon.vow.penalty.types.ordinary;

import com.jujutsuaddon.addon.api.vow.IPenalty;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.penalty.PenaltyParams;
import com.jujutsuaddon.addon.vow.penalty.PenaltySeverity;
import com.jujutsuaddon.addon.vow.penalty.PenaltyType;
import com.jujutsuaddon.addon.vow.penalty.ViolationContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;

/**
 * 咒力反噬惩罚
 * Cursed Energy Backlash Penalty
 *
 * 违约时受到基于最大生命值的伤害。
 * 永久誓约的伤害翻倍。
 */
public class CursedEnergyBacklashPenalty implements IPenalty {

    /** 惩罚唯一ID */
    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "cursed_energy_backlash");

    /** 参数键：伤害百分比 */
    public static final String PARAM_DAMAGE_PERCENT = "damagePercent";

    /** 参数键：是否无视护甲 */
    public static final String PARAM_IGNORE_ARMOR = "ignoreArmor";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("penalty.jujutsu_addon.cursed_energy_backlash");
    }

    @Override
    public Component getDescription(PenaltyParams params) {
        int percent = Math.round(params.getFloat(PARAM_DAMAGE_PERCENT, 0.2f) * 100);
        return Component.translatable("penalty.jujutsu_addon.cursed_energy_backlash.desc", percent);
    }

    @Override
    public PenaltyType getType() { return PenaltyType.INSTANT; }

    @Override
    public PenaltySeverity getSeverity() { return PenaltySeverity.MODERATE; }

    @Override
    public void execute(LivingEntity owner, ISorcererData data, PenaltyParams params, ViolationContext context) {
        // 读取伤害百分比
        float damagePercent = params.getFloat(PARAM_DAMAGE_PERCENT, 0.2f);

        // 永久誓约伤害翻倍
        if (context.isPermanentVow()) {
            damagePercent *= 2.0f;
        }

        // 计算实际伤害
        float damage = owner.getMaxHealth() * damagePercent;

        // 造成魔法伤害
        owner.hurt(owner.damageSources().magic(), damage);
    }

    @Override
    public boolean isConfigurable() { return true; }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                .addFloat(PARAM_DAMAGE_PERCENT,
                        Component.translatable("penalty.jujutsu_addon.cursed_energy_backlash.param.damage"),
                        0.05f, 1.0f, 0.2f)
                .addBoolean(PARAM_IGNORE_ARMOR,
                        Component.translatable("penalty.jujutsu_addon.cursed_energy_backlash.param.ignore_armor"),
                        false);
    }

    @Override
    public PenaltyParams createDefaultParams() {
        return new PenaltyParams()
                .setFloat(PARAM_DAMAGE_PERCENT, 0.2f)
                .setBoolean(PARAM_IGNORE_ARMOR, false);
    }

    @Override
    public CompoundTag serializeParams(PenaltyParams params) { return params.serializeNBT(); }

    @Override
    public PenaltyParams deserializeParams(CompoundTag nbt) { return PenaltyParams.fromNBT(nbt); }
}
