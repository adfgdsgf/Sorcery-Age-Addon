package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.config.AddonConfig;
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

    public static boolean isSoulDamage(DamageSource source) {
        ResourceLocation typeLoc = source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(null);
        if (typeLoc != null) {
            String id = typeLoc.toString();
            return id.equals(JJK_SOUL_DAMAGE_ID) || id.equals(JJK_SSK_DAMAGE_ID);
        }
        String msgId = source.getMsgId();
        return "soul".equals(msgId) || "split_soul_katana".equals(msgId);
    }

    public static boolean shouldApplyTrueDamage(DamageSource source, LivingEntity attacker) {
        if (AddonConfig.COMMON.enableSoulTrueDamage.get() <= 0.001) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return false;

        if (isSoulDamage(source)) return true;

        if (attacker instanceof Player player) {
            // ★★★ 新增：必须是直接攻击 ★★★
            // 防止玩家主手拿刀，副手扔雪球/射箭时也触发真伤
            if (source.getDirectEntity() != attacker) {
                return false;
            }

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

    public static boolean dealSoulDamage(LivingEntity target, DamageSource source, float amount) {
        return target.hurt(source, amount);
    }

    public static float calculateTrueDamageBonus(LivingEntity target, DamageSource source, float currentInputDamage) {
        return 0f;
    }
}
