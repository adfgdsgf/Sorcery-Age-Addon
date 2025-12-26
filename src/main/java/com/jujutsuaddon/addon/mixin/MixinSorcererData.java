package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.event.SorcererProtectionHandler;
import com.jujutsuaddon.addon.api.ability.IInfinityPressureAccessor;
import com.jujutsuaddon.addon.vow.manager.VowManager;
import com.jujutsuaddon.addon.util.ClientSafeCalls; // 引用刚才创建的安全类
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.entity.base.SummonEntity;
import radon.jujutsu_kaisen.util.EntityUtil;

import java.util.Map;
import java.util.UUID;

@Mixin(SorcererData.class)
public abstract class MixinSorcererData implements IInfinityPressureAccessor {

    @Shadow(remap = false)
    private LivingEntity owner;

    @Shadow(remap = false)
    private Map<Ability, Integer> cooldowns;

    // ==================== 无下限压制等级 (保持不变) ====================
    @Unique
    private int jujutsuAddon$infinityPressure = 0;

    @Override
    public int jujutsuAddon$getInfinityPressure() { return this.jujutsuAddon$infinityPressure; }

    @Override
    public void jujutsuAddon$setInfinityPressure(int level) { this.jujutsuAddon$infinityPressure = Math.max(0, Math.min(10, level)); }

    @Override
    public void jujutsuAddon$increaseInfinityPressure() { jujutsuAddon$setInfinityPressure(this.jujutsuAddon$infinityPressure + 1); }

    @Override
    public void jujutsuAddon$decreaseInfinityPressure() { jujutsuAddon$setInfinityPressure(this.jujutsuAddon$infinityPressure - 1); }

    @Inject(method = "serializeNBT", at = @At("RETURN"), remap = false)
    private void jujutsuAddon$serializeInfinityPressure(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag nbt = cir.getReturnValue();
        nbt.putInt("jujutsuAddon_infinityPressure", this.jujutsuAddon$infinityPressure);
    }

    @Inject(method = "deserializeNBT", at = @At("RETURN"), remap = false)
    private void jujutsuAddon$deserializeInfinityPressure(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("jujutsuAddon_infinityPressure")) {
            this.jujutsuAddon$infinityPressure = nbt.getInt("jujutsuAddon_infinityPressure");
        }
    }

    // ==================== 防止魔虚罗轮盘消失 (保持不变) ====================
    @Redirect(
            method = "toggle",
            at = @At(
                    value = "INVOKE",
                    target = "Lradon/jujutsu_kaisen/ability/base/Ability$IToggled;onDisabled(Lnet/minecraft/world/entity/LivingEntity;)V"
            ),
            remap = false
    )
    public void preventWheelClear(Ability.IToggled instance, LivingEntity owner) {
        if (instance == JJKAbilities.WHEEL.get()) {
            return;
        }
        instance.onDisabled(owner);
    }

    // ==================== 属性拦截逻辑 (保持不变) ====================
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lradon/jujutsu_kaisen/util/EntityUtil;applyModifier(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/Attribute;Ljava/util/UUID;Ljava/lang/String;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Z"
            ),
            remap = false
    )
    public boolean redirectApplyModifier(LivingEntity entity, Attribute attribute, UUID uuid, String name, double targetValue, AttributeModifier.Operation operation) {
        if (entity.level().isClientSide && attribute == Attributes.MAX_HEALTH) {
            return false;
        }
        if (entity instanceof SummonEntity) {
            if (attribute == Attributes.MAX_HEALTH) {
                entity.getPersistentData().putDouble("jjk_addon_main_mod_hp_bonus", targetValue);
                return false;
            }
            if (attribute == Attributes.ATTACK_DAMAGE) {
                entity.getPersistentData().putDouble("jjk_addon_main_mod_atk_bonus", targetValue);
                return false;
            }
        }
        if (entity instanceof Player player && attribute == Attributes.MAX_HEALTH) {
            boolean conversionEnabled = AddonConfig.COMMON.enableHealthToArmor.get();
            if (conversionEnabled) {
                if (!entity.level().isClientSide) {
                    double jjkExtraHealth = targetValue;
                    if (jjkExtraHealth > 0) {
                        SorcererData self = (SorcererData) (Object) this;
                        boolean isHR = self.hasTrait(Trait.HEAVENLY_RESTRICTION);
                        double armorRatio = isHR ? AddonConfig.COMMON.hrHealthToArmorRatio.get() : AddonConfig.COMMON.sorcererHealthToArmorRatio.get();
                        double toughnessRatio = isHR ? AddonConfig.COMMON.hrHealthToToughnessRatio.get() : AddonConfig.COMMON.sorcererHealthToToughnessRatio.get();
                        updateModifier(entity, Attributes.ARMOR, SorcererProtectionHandler.CONVERTED_ARMOR_UUID, "Sorcerer Armor Conversion", jjkExtraHealth * armorRatio);
                        updateModifier(entity, Attributes.ARMOR_TOUGHNESS, SorcererProtectionHandler.CONVERTED_TOUGHNESS_UUID, "Sorcerer Toughness Conversion", jjkExtraHealth * toughnessRatio);
                    } else {
                        cleanupArmor(entity);
                    }
                    AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                    if (healthAttr != null && healthAttr.getModifier(uuid) != null) {
                        healthAttr.removeModifier(uuid);
                        if (player.getHealth() > player.getMaxHealth()) {
                            player.setHealth(player.getMaxHealth());
                        }
                    }
                }
                return false;
            } else {
                if (!entity.level().isClientSide) {
                    cleanupArmor(player);
                }
                return EntityUtil.applyModifier(entity, attribute, uuid, name, targetValue, operation);
            }
        }
        return EntityUtil.applyModifier(entity, attribute, uuid, name, targetValue, operation);
    }

    @Unique
    private void updateModifier(LivingEntity entity, Attribute attr, UUID uuid, String name, double value) {
        AttributeInstance instance = entity.getAttribute(attr);
        if (instance != null) {
            AttributeModifier existing = instance.getModifier(uuid);
            if (existing == null || Math.abs(existing.getAmount() - value) > 0.1) {
                instance.removeModifier(uuid);
                if (value > 0.01) {
                    instance.addTransientModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    @Unique
    private void cleanupArmor(LivingEntity entity) {
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(SorcererProtectionHandler.CONVERTED_ARMOR_UUID);
        AttributeInstance toughness = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) toughness.removeModifier(SorcererProtectionHandler.CONVERTED_TOUGHNESS_UUID);
    }

    // ==================== 誓约输出加成 ====================
    @Inject(method = "getOutput", at = @At("RETURN"), cancellable = true, remap = false)
    private void applyVowOutputBonus(CallbackInfoReturnable<Float> cir) {
        float originalOutput = cir.getReturnValue();
        if (this.owner == null) return;
        float bonus = 0f;

        // ★★★ 修复：直接调用安全类，Mixin 文件中不再包含任何客户端代码 ★★★
        if (this.owner.level().isClientSide) {
            bonus = ClientSafeCalls.getOutputBonus(this.owner);
        } else {
            try {
                bonus = VowManager.getActiveOutputBonus(this.owner);
            } catch (Exception ignored) {}
        }
        if (bonus > 0) {
            cir.setReturnValue(originalOutput + bonus);
        }
    }

    // ==================== ★★★ 新增：誓约咒力上限加成 ★★★ ====================
    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true, remap = false)
    private void applyVowMaxEnergyBonus(CallbackInfoReturnable<Float> cir) {
        float originalMax = cir.getReturnValue();
        if (this.owner == null) return;

        float bonusPercent = 0f;

        // ★★★ 修复：直接调用安全类 ★★★
        if (this.owner.level().isClientSide) {
            bonusPercent = ClientSafeCalls.getEnergyBonus(this.owner);
        } else {
            try {
                bonusPercent = VowManager.getActiveEnergyBonus(this.owner);
            } catch (Exception ignored) {}
        }

        if (bonusPercent > 0) {
            cir.setReturnValue(originalMax * (1.0f + bonusPercent));
        }
    }

    // ==================== ★★★ 新增：誓约冷却缩减 ★★★ ====================
    /**
     * 拦截 addCooldown 方法。
     * 原方法逻辑：this.cooldowns.put(ability, ability.getRealCooldown(this.owner));
     * 我们在 put 之前拦截，修改存入的值。
     */
    @Inject(method = "addCooldown", at = @At("HEAD"), cancellable = true, remap = false)
    private void applyVowCooldownReduction(Ability ability, CallbackInfo ci) {
        if (this.owner == null || this.owner.level().isClientSide) return;

        float reductionPercent = VowManager.getActiveCooldownReduction(this.owner);

        if (reductionPercent > 0) {
            // 获取原始冷却时间
            int originalCooldown = ability.getRealCooldown(this.owner);

            // 计算缩减后的冷却
            int newCooldown = (int) (originalCooldown * (1.0f - reductionPercent));
            newCooldown = Math.max(1, newCooldown); // 至少保留1tick

            // 手动存入 map
            this.cooldowns.put(ability, newCooldown);

            // 取消原方法执行，防止覆盖
            ci.cancel();
        }
    }
}
