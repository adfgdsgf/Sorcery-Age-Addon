// src/main/java/com/jujutsuaddon/addon/mixin/MixinSorcererData.java
package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.event.SorcererProtectionHandler;
import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
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

import java.util.UUID;

@Mixin(SorcererData.class)
public class MixinSorcererData implements IInfinityPressureAccessor {

    // ==================== 无下限压制等级 ====================

    @Unique
    private int jujutsuAddon$infinityPressure = 0;

    @Override
    public int jujutsuAddon$getInfinityPressure() {
        return this.jujutsuAddon$infinityPressure;
    }

    @Override
    public void jujutsuAddon$setInfinityPressure(int level) {
        this.jujutsuAddon$infinityPressure = Math.max(0, Math.min(10, level));
    }

    @Override
    public void jujutsuAddon$increaseInfinityPressure() {
        jujutsuAddon$setInfinityPressure(this.jujutsuAddon$infinityPressure + 1);
    }

    @Override
    public void jujutsuAddon$decreaseInfinityPressure() {
        jujutsuAddon$setInfinityPressure(this.jujutsuAddon$infinityPressure - 1);
    }

    // ==================== NBT 序列化 ====================

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

    // ==================== 原有代码保持不变 ====================

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

        if (entity instanceof Player player && AddonConfig.COMMON.enableHealthToArmor.get() && attribute == Attributes.MAX_HEALTH) {
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

                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
            return false;
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
}
