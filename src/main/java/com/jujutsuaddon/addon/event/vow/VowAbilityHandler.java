package com.jujutsuaddon.addon.event.vow;

import com.jujutsuaddon.addon.capability.vow.IVowData;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.ViolationRecord;
import com.jujutsuaddon.addon.vow.manager.VowManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.AbilityTriggerEvent;
import radon.jujutsu_kaisen.ability.base.Ability;

@Mod.EventBusSubscriber(modid = "jujutsu_addon")
public class VowAbilityHandler {

    // 监听事件（用于兼容部分非Mixin触发的情况）
    @SubscribeEvent
    public static void onAbilityUse(AbilityTriggerEvent.Pre event) {
        if (event.getEntity() instanceof Player player) {
            // 如果 checkAndHandle 返回 true，说明是永久束缚，必须拦截
            if (checkAndHandle(player, event.getAbility()) && event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 核心判断逻辑
     * @return true = 拦截技能 (物理墙壁); false = 放行技能 (允许释放)
     */
    public static boolean checkAndHandle(LivingEntity owner, Ability ability) {
        // ★★★ 新增：创造模式豁免 ★★★
        // 逻辑：创造模式下，即便违反束缚也不进行判定，直接放行。
        // 这样就不会触发 handleViolation，也不会产生新的惩罚。
        if (owner instanceof Player player && player.isCreative()) {
            return false;
        }

        IVowData data = VowManager.getVowData(owner);
        if (data == null) return false;

        // 检查是否有违规
        ViolationRecord violation = VowManager.getBanViolation(owner, ability);

        if (violation != null) {
            CustomBindingVow vow = data.getVow(violation.getVowId());
            if (vow == null) return false;

            // =================================================
            // 情况 1: 永久束缚 (Permanent) -> 比如 AbilityBanCondition
            // =================================================
            if (vow.isPermanent()) {
                if (owner instanceof Player player) {
                    player.displayClientMessage(
                            Component.translatable("vow.message.permanent_restriction", vow.getName())
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                // 返回 true -> Mixin 会拦截 -> 技能发不出来
                return true;
            }

            // =================================================
            // 情况 2: 普通束缚 (Normal) -> 比如 时间限制
            // =================================================
            else {
                if (owner instanceof Player player) {
                    // 提示违约
                    player.displayClientMessage(
                            Component.translatable("vow.message.violation", vow.getName())
                                    .withStyle(ChatFormatting.DARK_RED),
                            true
                    );

                    // 服务端执行惩罚 (扣属性、破誓)
                    if (!owner.level().isClientSide()) {
                        VowManager.handleViolation(owner, vow);
                    }
                }

                // ★★★ 关键点 ★★★
                // 返回 false -> Mixin 不拦截 -> 技能成功释放 -> 达成“能用但违约”的效果
                return false;
            }
        }

        return false;
    }
}
