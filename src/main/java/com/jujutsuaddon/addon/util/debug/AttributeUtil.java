package com.jujutsuaddon.addon.util.debug;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AttributeUtil {

    /**
     * 获取经过筛选、排序后的属性列表组件
     */
    public static MutableComponent getDynamicAttributeComponent(LivingEntity entity) {
        MutableComponent miscLine = Component.literal(" §8> §7");
        miscLine.append(Component.translatable("debug.jujutsu_addon.summon.attr_label")); // "属性:"
        miscLine.append(" §r");

        boolean first = true;

        // 1. 获取
        Collection<AttributeInstance> allAttributes = entity.getAttributes().getSyncableAttributes();

        // 2. 排序
        List<AttributeInstance> sortedAttributes = allAttributes.stream()
                .sorted(Comparator.comparing(inst -> Component.translatable(inst.getAttribute().getDescriptionId()).getString()))
                .toList();

        // 3. 遍历与拼接
        for (AttributeInstance instance : sortedAttributes) {
            Attribute attr = instance.getAttribute();
            double value = instance.getValue();

            // 过滤规则：
            // 1. 不显示最大生命 (因为通常有单独的 HP 条)
            if (attr == Attributes.MAX_HEALTH) continue;

            // 2. 不显示数值为 0 的属性
            if (Math.abs(value) < 0.001) continue;

            if (!first) miscLine.append(" | ");

            MutableComponent attrName = Component.translatable(attr.getDescriptionId());
            miscLine.append(attrName).append(String.format(": §b%.2f§r", value));
            first = false;
        }

        return miscLine;
    }
}
