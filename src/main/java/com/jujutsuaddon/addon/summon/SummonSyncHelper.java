package com.jujutsuaddon.addon.summon;

import com.google.common.collect.Multimap;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class SummonSyncHelper {

    /**
     * 移除装备上的基础属性修饰符（避免双重计算）
     * 这些属性已经通过 runScalingLogic 单独计算了
     */
    public static void removeBaseStats(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains("AttributeModifiers", 9)) {
            tag.put("AttributeModifiers", new ListTag());
            return;
        }

        ListTag modifiers = tag.getList("AttributeModifiers", 10);

        for (int i = modifiers.size() - 1; i >= 0; i--) {
            CompoundTag modifier = modifiers.getCompound(i);
            String attrName = modifier.getString("AttributeName");

            // 精确匹配基础属性
            if ("minecraft:generic.max_health".equals(attrName) ||
                    "minecraft:generic.armor".equals(attrName) ||
                    "minecraft:generic.armor_toughness".equals(attrName) ||
                    "minecraft:generic.attack_damage".equals(attrName) ||
                    "minecraft:generic.attack_speed".equals(attrName) ||
                    "minecraft:generic.movement_speed".equals(attrName)) {
                modifiers.remove(i);
                continue;
            }

            // 暴击属性模糊匹配
            if (attrName.contains("crit_chance") || attrName.contains("crit_damage")) {
                modifiers.remove(i);
            }
        }
    }

    /**
     * 检查装备上有哪些属性召唤物不支持（仅 debug 用）
     */
    public static void checkMissingAttributes(Player owner, TamableAnimal summon, EquipmentSlot slot, ItemStack stack) {
        if (!DebugManager.isDebugging(owner)) return;

        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);

        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            Attribute attr = entry.getKey();
            if (summon.getAttribute(attr) == null) {
                var key = ForgeRegistries.ATTRIBUTES.getKey(attr);
                if (key == null) continue;

                String attrId = key.toString();
                // 只警告非基础属性（基础属性是被故意移除的）
                if (!shouldRemoveAttribute(attrId)) {
                    DamageDebugUtil.logMissingAttribute(owner, attrId);
                }
            }
        }
    }

    /**
     * 判断属性是否是被故意移除的基础属性
     * 与 removeBaseStats 保持一致
     */
    private static boolean shouldRemoveAttribute(String attrName) {
        // 精确匹配基础属性
        if ("minecraft:generic.max_health".equals(attrName) ||
                "minecraft:generic.armor".equals(attrName) ||
                "minecraft:generic.armor_toughness".equals(attrName) ||
                "minecraft:generic.attack_damage".equals(attrName) ||
                "minecraft:generic.attack_speed".equals(attrName) ||
                "minecraft:generic.movement_speed".equals(attrName)) {
            return true;
        }

        // 暴击属性模糊匹配
        if (attrName.contains("crit_chance") || attrName.contains("crit_damage")) {
            return true;
        }

        return false;
    }
}
