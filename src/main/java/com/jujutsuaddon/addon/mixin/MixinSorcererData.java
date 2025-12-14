package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.event.SorcererProtectionHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.entity.base.SummonEntity;
import radon.jujutsu_kaisen.util.EntityUtil;

import java.util.UUID;

@Mixin(SorcererData.class)
public class MixinSorcererData {

    // 移除了 onTick 方法！我们不需要每 tick 扫描了，太耗性能且容易闪烁。
    // 我们完全依赖 redirectApplyModifier 来拦截。

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

        // 1. 客户端逻辑：彻底封杀
        // 只要是客户端，且涉及血量，直接返回 false。
        // 这样 JJK 在客户端就永远无法修改血量，也就不会出现 60 血的显示 BUG。
        // 也不需要我们事后再去 removeModifier 了。
        if (entity.level().isClientSide && attribute == Attributes.MAX_HEALTH) {
            return false;
        }

        // 2. 式神逻辑
        if (entity instanceof SummonEntity) {
            if (attribute == Attributes.MAX_HEALTH) {
                entity.getPersistentData().putDouble("jjk_addon_main_mod_hp_bonus", targetValue);
                return false; // 拦截！不让 JJK 加血，我们自己会在 SummonScalingHelper 里统一管理
            }
            if (attribute == Attributes.ATTACK_DAMAGE) {
                entity.getPersistentData().putDouble("jjk_addon_main_mod_atk_bonus", targetValue);
                return false; // 拦截！
            }
        }

        // 3. 玩家血量转护甲逻辑 (服务端)
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

                // 这里不需要再 removeModifier 了，因为我们直接 return false，JJK 根本就没加上去！
                // 只需要处理回血逻辑
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
            return false; // 拦截！
        }

        // 放行其他属性
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
