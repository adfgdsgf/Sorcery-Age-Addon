package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.AddonConfig;
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

    public static final UUID CONVERTED_ARMOR_UUID = UUID.fromString("99887766-5544-3322-1100-aabbccddeeff");
    public static final UUID CONVERTED_TOUGHNESS_UUID = UUID.fromString("11223344-5566-7788-9900-ffeeddccbbaa");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        handleSorcererProtection(event);
    }

    private static void handleSorcererProtection(LivingHurtEvent event) {
        // 注意：这里不能只检查 AddonConfig.COMMON.enableHealthToArmor.get()
        // 因为那个开关可能只控制玩家。
        // 如果您希望即使玩家开关关了，生物依然能生效，就去掉这个检查，或者单独加一个生物开关。
        // 这里假设只要生物身上有那个 UUID 的修饰符，就应该生效。

        LivingEntity target = event.getEntity();

        // [修改] 不再强制寻找 ownerPlayer，而是确定谁是属性的持有者
        LivingEntity attributeHolder = target;
        boolean isSummon = false;

        // 如果是玩家，持有者是玩家自己
        if (target instanceof Player) {
            // 玩家逻辑受 Config 控制
            if (!AddonConfig.COMMON.enableHealthToArmor.get()) return;
        }
        // 如果是驯服生物 (如式神)，原逻辑是看主人
        else if (target instanceof TamableAnimal summon && summon.getOwner() instanceof Player p) {
            // 检查：是式神继承主人的护甲，还是生物自己有护甲？
            // 如果生物自己有我们添加的修饰符，优先用生物自己的
            if (hasConvertedStats(summon)) {
                attributeHolder = summon;
            } else {
                // 否则回退到原逻辑：看主人
                if (!AddonConfig.COMMON.enableHealthToArmor.get()) return;
                attributeHolder = p;
                isSummon = true;
            }
        }
        // 如果是普通兼容生物 (如无主的女仆)，持有者是自己
        else {
            if (!hasConvertedStats(target)) return;
            attributeHolder = target;
        }

        // 检查持有者是否有咒术能力 (兼容性检查)
        if (!attributeHolder.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return;

        DamageSource source = event.getSource();
        if (SoulDamageUtil.isSoulDamage(source)) return;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        // 排除世界斩
        Entity directEntity = source.getDirectEntity();
        String directEntityName = directEntity != null ? directEntity.getClass().getSimpleName() : "";
        if (directEntityName.contains("WorldSlash") || (source.getMsgId() != null && source.getMsgId().contains("world_slash"))) {
            return;
        }

        boolean shouldCalculate = false;
        boolean bypassesArmor = source.is(DamageTypeTags.BYPASSES_ARMOR);

        // 如果是召唤物/兼容生物，或者攻击穿透护甲，则启用计算
        if (isSummon || attributeHolder != target || bypassesArmor) {
            shouldCalculate = true;
        }
        // 如果生物自己就有转化护甲，那肯定要计算，因为原版护甲机制可能无法处理这种特殊的“转化护甲”带来的高额减伤
        if (hasConvertedStats(attributeHolder)) {
            shouldCalculate = true;
        }

        if (!shouldCalculate) return;

        // [修改] 从 attributeHolder 获取属性
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

        // 计算减伤
        double effectiveDefense = convertedArmor + (convertedToughness * 1.5);
        float originalDamage = event.getAmount();
        float reductionFactor = (float) (1.0 / (1.0 + (effectiveDefense / 20.0)));
        float newDamage = originalDamage * reductionFactor;

        // 保底伤害
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
