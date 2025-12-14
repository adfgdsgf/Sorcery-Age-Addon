package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.item.cursed_tool.SplitSoulKatanaItem;

public class SoulDamageUtil {

    private static final String JJK_SOUL_DAMAGE_ID = "jujutsu_kaisen:soul";
    private static final String JJK_SSK_DAMAGE_ID = "jujutsu_kaisen:split_soul_katana";

    // 判断是否是灵魂伤害 (保留，Event 中用到了)
    public static boolean isSoulDamage(DamageSource source) {
        ResourceLocation typeLoc = source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(null);
        if (typeLoc != null) {
            String id = typeLoc.toString();
            return id.equals(JJK_SOUL_DAMAGE_ID) || id.equals(JJK_SSK_DAMAGE_ID);
        }
        String msgId = source.getMsgId();
        return "soul".equals(msgId) || "split_soul_katana".equals(msgId);
    }

    // 判断是否应该应用真实伤害/穿透 (保留，Event 中用到了)
    public static boolean shouldApplyTrueDamage(DamageSource source, LivingEntity attacker) {
        if (AddonConfig.COMMON.enableSoulTrueDamage.get() <= 0.001) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return false;

        if (isSoulDamage(source)) return true;

        if (attacker instanceof Player player) {
            ISorcererData cap = player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
            if (cap != null && cap.hasTrait(Trait.HEAVENLY_RESTRICTION)) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty() && stack.getItem() instanceof SplitSoulKatanaItem) {
                    return true;
                }
            }
        }
        return false;
    }

    // 【修改】这个方法现在只是一个普通的 hurt 包装器
    // 如果你有其他地方调用了这个方法，保留它以防报错。
    // 如果没有地方调用，可以直接删掉。
    public static boolean dealSoulDamage(LivingEntity target, DamageSource source, float amount) {
        // 不再需要 DamageContext.set(amount);
        return target.hurt(source, amount);
    }

    // 这个看起来也没用了，可以删掉
    public static float calculateTrueDamageBonus(LivingEntity target, DamageSource source, float currentInputDamage) {
        return 0f;
    }
}
