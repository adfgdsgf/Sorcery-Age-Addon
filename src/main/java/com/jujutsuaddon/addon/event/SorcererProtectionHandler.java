// 文件路径: src/main/java/com/jujutsuaddon/addon/event/SorcererProtectionHandler.java
package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.util.helper.SoulDamageUtil;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class SorcererProtectionHandler {

    // ★★★ 在这里定义 UUID，供 Mixin 调用 ★★★
    public static final UUID CONVERTED_ARMOR_UUID = UUID.fromString("99887766-5544-3322-1100-aabbccddeeff");
    public static final UUID CONVERTED_TOUGHNESS_UUID = UUID.fromString("11223344-5566-7788-9900-ffeeddccbbaa");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        handleSorcererProtection(event);
    }

    private static void handleSorcererProtection(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();

        // 1. 确定谁是属性的持有者
        LivingEntity attributeHolder = target;
        boolean isSummon = false;

        // 如果是玩家
        if (target instanceof Player) {
            if (!AddonConfig.COMMON.enableHealthToArmor.get()) return;
        }
        // 如果是驯服生物 (如式神)
        else if (target instanceof TamableAnimal summon && summon.getOwner() instanceof Player p) {
            // 优先检查生物自己是否有护甲修饰符
            if (hasConvertedStats(summon)) {
                attributeHolder = summon;
            } else {
                // 否则回退到原逻辑：看主人
                if (!AddonConfig.COMMON.enableHealthToArmor.get()) return;
                attributeHolder = p;
                isSummon = true;
            }
        }
        // 如果是普通兼容生物
        else {
            if (!hasConvertedStats(target)) return;
            attributeHolder = target;
        }

        // 2. 检查是否有咒术能力
        if (!attributeHolder.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return;

        DamageSource source = event.getSource();
        // 排除灵魂伤害和穿透伤害
        if (SoulDamageUtil.isSoulDamage(source)) return;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        // 排除世界斩 (World Slash)
        Entity directEntity = source.getDirectEntity();
        String directEntityName = directEntity != null ? directEntity.getClass().getSimpleName() : "";
        if (directEntityName.contains("WorldSlash") || (source.getMsgId() != null && source.getMsgId().contains("world_slash"))) {
            return;
        }

        boolean shouldCalculate = false;
        boolean bypassesArmor = source.is(DamageTypeTags.BYPASSES_ARMOR);

        // 3. 决定是否启用自定义计算
        // 如果是召唤物、或者攻击穿透了原版护甲、或者拥有我们要的转化护甲
        if (isSummon || attributeHolder != target || bypassesArmor || hasConvertedStats(attributeHolder)) {
            shouldCalculate = true;
        }

        if (!shouldCalculate) return;

        // 4. 获取转化后的护甲值
        double convertedArmor = 0.0;
        double convertedToughness = 0.0;

        AttributeInstance armorAttr = attributeHolder.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            AttributeModifier mod = armorAttr.getModifier(CONVERTED_ARMOR_UUID);
            if (mod != null) convertedArmor = mod.getAmount();
        }

        AttributeInstance toughnessAttr = attributeHolder.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughnessAttr != null) {
            AttributeModifier mod = toughnessAttr.getModifier(CONVERTED_TOUGHNESS_UUID);
            if (mod != null) convertedToughness = mod.getAmount();
        }

        if (convertedArmor <= 0.01) return;

        // 5. 执行减伤公式 (模拟高护甲效果)
        // effectiveDefense = 护甲 + 1.5倍韧性
        double effectiveDefense = convertedArmor + (convertedToughness * 1.5);
        float originalDamage = event.getAmount();
        // 减伤公式： 1 / (1 + 防御/20)
        float reductionFactor = (float) (1.0 / (1.0 + (effectiveDefense / 20.0)));
        float newDamage = originalDamage * reductionFactor;

        // 6. 设定保底伤害 (防止无敌)
        if (originalDamage > 50 && newDamage < 2) {
            newDamage = 2.0f;
        }

        event.setAmount(newDamage);
    }

    private static boolean hasConvertedStats(LivingEntity entity) {
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        return armor != null && armor.getModifier(CONVERTED_ARMOR_UUID) != null;
    }
}
