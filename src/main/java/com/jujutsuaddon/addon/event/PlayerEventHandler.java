package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PlayerMovementTracker;
import com.jujutsuaddon.addon.damage.ServerDamagePredictor;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket;
import com.jujutsuaddon.addon.damage.calculator.AbilityDamageCalculator;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.item.cursed_tool.PlayfulCloudItem;

import java.util.UUID;

/**
 * 玩家事件处理器
 * 专门负责处理玩家自身的状态变化：重生、Tick 更新、装备更换等。
 */
@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class PlayerEventHandler {

    private static final UUID PLAYFUL_CLOUD_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-56789abcdef0");

    /**
     * 监听玩家重生事件 (PlayerEvent.Clone)
     * 目的：处理魔虚罗适应性数据的继承或清除。
     * 优先级：LOWEST (最后执行，确保数据已复制完毕)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return; // 只处理死亡重生，不处理跨维度传送

        boolean shouldClear = AddonConfig.COMMON.clearAdaptationOnDeath.get();
        Player newPlayer = event.getEntity();
        Player oldPlayer = event.getOriginal();

        newPlayer.getCapability(TenShadowsDataHandler.INSTANCE).ifPresent(newCap -> {
            if (shouldClear) {
                // 策略 A: 死亡清除适应性 (硬核模式)
                boolean changed = false;
                if (!newCap.getAdapted().isEmpty()) { newCap.getAdapted().clear(); changed = true; }
                if (!newCap.getAdapting().isEmpty()) { newCap.getAdapting().clear(); changed = true; }

                if (changed && DebugManager.isDebugging(newPlayer)) {
                    newPlayer.sendSystemMessage(Component.literal("§b[JJK Addon] §fMahoraga adaptation cleared due to death."));
                }
            } else {
                // 策略 B: 死亡保留适应性 (默认模式)
                oldPlayer.getCapability(TenShadowsDataHandler.INSTANCE).ifPresent(oldCap -> {
                    try {
                        // 手动复制旧玩家的数据到新玩家
                        if (!oldCap.getAdapted().isEmpty()) {
                            newCap.getAdapted().clear();
                            newCap.getAdapted().putAll(oldCap.getAdapted());
                        }
                        if (!oldCap.getAdapting().isEmpty()) {
                            newCap.getAdapting().clear();
                            newCap.getAdapting().putAll(oldCap.getAdapting());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    /**
     * 监听玩家每刻更新 (PlayerTickEvent)
     * 目的：清理每 Tick 的临时缓存数据 (如伤害统计、暴击缓存)。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            // 1. 刷新并显示本 Tick 的总伤害 (调试用)
            DamageDebugUtil.flushTickDamage(event.player);

            // 2. 清理技能伤害计算缓存
            AbilityDamageCalculator.clearCache(event.player.getUUID());

            // 3. 清理暴击缓存
            ModEventHandler.vanillaCritCache.remove(event.player.getUUID());

            // 4. 清理释魂刀缓存
            ModEventHandler.SSK_PRE_ARMOR_DAMAGE.remove();

            if (event.player.tickCount % 50 == 0) {
                com.jujutsuaddon.addon.util.helper.WeaponEffectProxy.cleanupExpiredCache(event.player.level().getGameTime());
            }
        }

        // ★★★ 6. 同步伤害预测数据到客户端 (每 20 tick = 1秒) ★★★
        if (event.player.tickCount % 20 == 0 && event.player instanceof ServerPlayer serverPlayer) {
            try {
                SyncDamagePredictionsS2CPacket packet = ServerDamagePredictor.calculateAll(serverPlayer);
                AddonNetwork.sendToPlayer(packet, serverPlayer);
            } catch (Exception ignored) {
                // 防止计算出错导致崩服
            }
        }
        // ★★★ 在 END 阶段记录玩家移动 ★★★
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            PlayerMovementTracker.update(event.player);
        }
    }

    /**
     * 监听装备更换事件 (LivingEquipmentChangeEvent)
     * 目的：处理游云 (Playful Cloud) 对天与咒缚 (Heavenly Restriction) 的特殊加成。
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        AttributeInstance attackAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttribute == null) return;

        ItemStack mainHandItem = player.getMainHandItem();
        boolean isHoldingPlayfulCloud = !mainHandItem.isEmpty() && (mainHandItem.getItem() instanceof PlayfulCloudItem);

        boolean isHR = false;
        ISorcererData cap = player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap != null && cap.hasTrait(Trait.HEAVENLY_RESTRICTION)) {
            isHR = true;
        }

        boolean hasModifier = attackAttribute.getModifier(PLAYFUL_CLOUD_MODIFIER_UUID) != null;

        // 如果是天与咒缚且手持游云 -> 添加额外攻击力修饰符
        if (isHoldingPlayfulCloud && isHR) {
            if (!hasModifier) {
                double bonus = AddonConfig.COMMON.playfulCloudAttributeBonus.get();
                AttributeModifier modifier = new AttributeModifier(PLAYFUL_CLOUD_MODIFIER_UUID, "Playful Cloud Bonus", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
                attackAttribute.addTransientModifier(modifier);
            }
        } else {
            // 否则 -> 移除修饰符
            if (hasModifier) {
                attackAttribute.removeModifier(PLAYFUL_CLOUD_MODIFIER_UUID);
            }
        }
    }
}
