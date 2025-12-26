package com.jujutsuaddon.addon.mixin.summon;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import com.jujutsuaddon.addon.summon.SummonSyncHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Mob.class)
public class MixinSummonSync {

    private static final String SYNC_MARKER = "jjk_sync_pending";
    private static final String HP_FIX_MARKER = "jjk_hp_fix_pending";
    private static final String LAST_MAX_HP_KEY = "jjk_last_max_hp";

    @Inject(method = "tick", at = @At("HEAD"))
    private void syncEquipmentHead(CallbackInfo ci) {
        Mob summon = (Mob) (Object) this;

        if (summon.level().isClientSide) return;
        if (!summon.isAlive()) return;
        if (summon.tickCount % 10 != 0) return;

        if (summon instanceof TamableAnimal tamable && tamable.getOwner() instanceof Player owner) {
            String className = summon.getClass().getName();
            if (!className.startsWith("radon.jujutsu_kaisen")) return;

            boolean isTamed = tamable.isTame();
            boolean allowUntamed = AddonConfig.COMMON.enableUntamedEquipSync.get();
            if (!isTamed && !allowUntamed) return;

            // 记录同步前的最大血量
            float beforeMaxHp = summon.getMaxHealth();
            summon.getPersistentData().putFloat(LAST_MAX_HP_KEY, beforeMaxHp);

            boolean changed = false;

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack ownerItem = owner.getItemBySlot(slot);
                ItemStack currentSummonItem = summon.getItemBySlot(slot);
                ItemStack expectedSummonItem = ownerItem.copy();

                // 调用 Helper 移除基础属性
                SummonSyncHelper.removeBaseStats(expectedSummonItem);

                if (!ItemStack.matches(expectedSummonItem, currentSummonItem)) {
                    DamageDebugUtil.logItemSync(owner, tamable, slot, expectedSummonItem);
                    summon.setItemSlot(slot, expectedSummonItem);
                    SummonSyncHelper.checkMissingAttributes(owner, tamable, slot, expectedSummonItem);
                    summon.setDropChance(slot, 0.0f);
                    changed = true;
                }
            }

            if (changed) {
                summon.getPersistentData().putBoolean(SYNC_MARKER, true);
                summon.getPersistentData().putBoolean(HP_FIX_MARKER, true);
            }
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void fixHealthTail(CallbackInfo ci) {
        Mob summon = (Mob) (Object) this;
        if (summon.level().isClientSide) return;

        // [核心修复] 处理血量问题
        if (summon.getPersistentData().contains(HP_FIX_MARKER)) {
            summon.getPersistentData().remove(HP_FIX_MARKER);

            float lastMaxHp = summon.getPersistentData().getFloat(LAST_MAX_HP_KEY);
            float currentMaxHp = summon.getMaxHealth();
            float currentHp = summon.getHealth();

            Player debugOwner = null;
            if (summon instanceof TamableAnimal tamable && tamable.getOwner() instanceof Player p) {
                debugOwner = p;
            }

            // [关键] 如果最大血量异常低，强制移除负面修饰符
            if (currentMaxHp < 5.0f) {
                jujutsuaddon$removeNegativeHealthModifiers(summon, debugOwner);
                currentMaxHp = summon.getMaxHealth(); // 重新获取
            }

            // 按比例恢复血量
            float targetHp;
            if (lastMaxHp > 0 && currentHp > 0) {
                float ratio = Math.min(1.0f, currentHp / lastMaxHp);
                targetHp = currentMaxHp * ratio;
            } else {
                targetHp = currentMaxHp; // 满血
            }

            // 确保血量在有效范围内
            if (currentHp > currentMaxHp || currentHp < 1 || Math.abs(currentHp - targetHp) > 1.0f) {
                summon.setHealth(Math.max(1, targetHp));

                if (debugOwner != null && DebugManager.isDebugging(debugOwner)) {
                    debugOwner.sendSystemMessage(Component.literal(
                            "§8[HP修复] " + summon.getName().getString() +
                                    " §7血量: §c" + String.format("%.1f", currentHp) +
                                    " §7-> §a" + String.format("%.1f", summon.getHealth()) +
                                    " §7(最大: " + String.format("%.1f", currentMaxHp) + ")"
                    ));
                }
            }
        }

        if (summon.getPersistentData().contains(SYNC_MARKER)) {
            summon.getPersistentData().remove(SYNC_MARKER);
            if (summon instanceof TamableAnimal tamable && tamable.getOwner() instanceof Player owner) {
                DamageDebugUtil.logSyncUpdate(owner, tamable);
            }
        }
    }

    /**
     * [核心] 强制移除实体属性中的负面血量修饰符
     */
    @Unique
    private void jujutsuaddon$removeNegativeHealthModifiers(Mob summon, Player debugOwner) {
        AttributeInstance hpAttr = summon.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr == null) return;

        List<AttributeModifier> toRemove = new ArrayList<>();

        for (AttributeModifier mod : hpAttr.getModifiers()) {
            boolean isNegative = false;

            // ADDITION 类型：负数就是减血量
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                if (mod.getAmount() < 0) {
                    isNegative = true;
                }
            }
            // MULTIPLY_BASE 或 MULTIPLY_TOTAL：小于0的值会减少血量
            else {
                if (mod.getAmount() < 0) {
                    isNegative = true;
                }
            }

            if (isNegative) {
                toRemove.add(mod);
                if (debugOwner != null && DebugManager.isDebugging(debugOwner)) {
                    debugOwner.sendSystemMessage(Component.literal(
                            "§c[移除负面修饰符] " + mod.getName() +
                                    " | 值: " + String.format("%.2f", mod.getAmount()) +
                                    " | 类型: " + mod.getOperation().name()
                    ));
                }
            }
        }

        for (AttributeModifier mod : toRemove) {
            hpAttr.removeModifier(mod);
        }

        // 同样处理护甲和护甲韧性
        jujutsuaddon$removeNegativeModifiersFromAttribute(summon, Attributes.ARMOR, debugOwner);
        jujutsuaddon$removeNegativeModifiersFromAttribute(summon, Attributes.ARMOR_TOUGHNESS, debugOwner);
    }

    @Unique
    private void jujutsuaddon$removeNegativeModifiersFromAttribute(Mob summon,
                                                                   net.minecraft.world.entity.ai.attributes.Attribute attribute, Player debugOwner) {
        AttributeInstance attr = summon.getAttribute(attribute);
        if (attr == null) return;

        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier mod : attr.getModifiers()) {
            if (mod.getAmount() < 0) {
                toRemove.add(mod);
            }
        }
        for (AttributeModifier mod : toRemove) {
            attr.removeModifier(mod);
        }
    }
}
