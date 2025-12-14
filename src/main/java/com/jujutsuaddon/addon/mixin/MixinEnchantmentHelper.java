package com.jujutsuaddon.addon.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelper {

    @Inject(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), cancellable = true)
    private static void onGetEnchantmentLevel(Enchantment enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        // 检查是否为有主人的召唤物
        if (entity instanceof TamableAnimal summon && summon.getOwner() instanceof Player owner) {

            // 获取原本等级
            int originalLevel = cir.getReturnValue();

            // 获取主人等级 (支持所有模组附魔，包括饰品栏提供的附魔)
            // 这里不需要过滤防御附魔，因为防御计算通常不走这个方法，而是走装备遍历
            int ownerLevel = EnchantmentHelper.getEnchantmentLevel(enchantment, owner);

            // 取最大值
            if (ownerLevel > originalLevel) {
                cir.setReturnValue(ownerLevel);
            }
        }
    }
}
