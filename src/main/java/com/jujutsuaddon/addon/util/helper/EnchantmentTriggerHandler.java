package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 附魔效果触发处理器
 *
 * 【核心原理】
 * 大多数模组附魔通过监听 Forge 事件 (LivingHurtEvent 等) 自动触发，
 * 本类只需要处理那些"硬编码在原版攻击流程中"的附魔。
 */
public class EnchantmentTriggerHandler {

    /**
     * 自定义三参数消费者接口
     */
    @FunctionalInterface
    public interface EnchantmentTrigger {
        void trigger(Player player, LivingEntity target, int level);
    }

    // 只注册那些需要特殊处理的附魔（原版硬编码的）
    private static final Map<ResourceLocation, EnchantmentTrigger> SPECIAL_TRIGGERS = new HashMap<>();

    /**
     * 注册特殊附魔触发器
     * 只有那些不通过事件触发的附魔才需要注册
     */
    public static void registerSpecialTrigger(ResourceLocation enchantmentId, EnchantmentTrigger trigger) {
        SPECIAL_TRIGGERS.put(enchantmentId, trigger);
    }

    /**
     * 初始化 - 只注册原版硬编码的附魔
     */
    public static void init() {
        // ========== 原版硬编码附魔 ==========
        // 这些附魔在 Player.attack() 中被直接调用，技能伤害不会经过那里

        // 火焰附加 - 原版在 Player.attack() 中硬编码
        registerSpecialTrigger(new ResourceLocation("minecraft", "fire_aspect"),
                (Player player, LivingEntity target, int level) -> {
                    if (!target.isOnFire()) {
                        target.setSecondsOnFire(level * 4);
                    }
                });

        // 引雷 - 原版在三叉戟代码中硬编码
        registerSpecialTrigger(new ResourceLocation("minecraft", "channeling"),
                (Player player, LivingEntity target, int level) -> {
                    Level world = target.level();
                    if (world.isThundering() && world.canSeeSky(target.blockPosition())) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
                        if (lightning != null) {
                            lightning.moveTo(target.getX(), target.getY(), target.getZ());
                            world.addFreshEntity(lightning);
                        }
                    }
                });

        // 击退 - 原版在 Player.attack() 中硬编码
        registerSpecialTrigger(new ResourceLocation("minecraft", "knockback"),
                (Player player, LivingEntity target, int level) -> {
                    double dx = player.getX() - target.getX();
                    double dz = player.getZ() - target.getZ();
                    target.knockback(level * 0.5F, dx, dz);
                });
        // ========== 弓的附魔 ==========
// 火矢 - 弓的燃烧效果
        registerSpecialTrigger(new ResourceLocation("minecraft", "flame"),
                (Player player, LivingEntity target, int level) -> {
                    if (!target.isOnFire()) {
                        target.setSecondsOnFire(5);  // 原版火矢燃烧5秒
                    }
                });
// 冲击 - 弓的击退效果
        registerSpecialTrigger(new ResourceLocation("minecraft", "punch"),
                (Player player, LivingEntity target, int level) -> {
                    double dx = player.getX() - target.getX();
                    double dz = player.getZ() - target.getZ();
                    target.knockback(level * 0.5F, dx, dz);  // 和击退类似
                });
    }

    /**
     * 玩家直接攻击时触发附魔
     */
    public static void triggerEnchantments(Player player, LivingEntity target) {
        if (player == null || target == null) return;
        if (!AddonConfig.COMMON.enableEnchantmentTriggers.get()) return;
        if (target.level().isClientSide) return;

        ItemStack weapon = player.getMainHandItem();
        triggerEnchantmentsInternal(player, target, weapon, player);
    }

    /**
     * 召唤物攻击时触发附魔
     * 使用主人的武器附魔，但击退方向基于召唤物位置
     */
    public static void triggerEnchantmentsForSummon(Player owner, TamableAnimal summon, LivingEntity target) {
        if (owner == null || summon == null || target == null) return;
        if (!AddonConfig.COMMON.enableEnchantmentTriggers.get()) return;
        if (!AddonConfig.COMMON.enableSummonEnchantTrigger.get()) return;
        if (target.level().isClientSide) return;

        // 优先使用召唤物自己的武器，否则使用主人的武器
        ItemStack weapon = summon.getMainHandItem();
        if (weapon.isEmpty()) {
            weapon = owner.getMainHandItem();
        }

        triggerEnchantmentsInternal(owner, target, weapon, summon);
    }

    /**
     * 内部实现：触发附魔效果
     *
     * @param owner           附魔来源的主人（用于设置攻击关系）
     * @param target          攻击目标
     * @param weapon          使用的武器
     * @param knockbackSource 击退方向的来源实体（玩家或召唤物）
     */
    private static void triggerEnchantmentsInternal(Player owner, LivingEntity target,
                                                    ItemStack weapon, LivingEntity knockbackSource) {
        List<? extends String> blacklist = AddonConfig.COMMON.enchantmentTriggerBlacklist.get();

        // ========== 1. 触发特殊处理的附魔（原版硬编码的）==========
        if (!weapon.isEmpty()) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(weapon);

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchant = entry.getKey();
                int level = entry.getValue();

                ResourceLocation enchantId = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
                if (enchantId == null) continue;

                // 检查黑名单
                if (isBlacklisted(enchantId.toString(), blacklist)) continue;

                // 只触发特殊注册的附魔
                EnchantmentTrigger trigger = SPECIAL_TRIGGERS.get(enchantId);
                if (trigger != null) {
                    try {
                        String id = enchantId.toString();
                        // 特殊处理击退类附魔：使用实际攻击者的位置
                        if ("minecraft:knockback".equals(id) || "minecraft:punch".equals(id)) {
                            double dx = knockbackSource.getX() - target.getX();
                            double dz = knockbackSource.getZ() - target.getZ();
                            target.knockback(level * 0.5F, dx, dz);
                        } else {
                            trigger.trigger(owner, target, level);
                        }
                    } catch (Exception e) {
                        // 单个附魔出错不影响整体
                    }
                }
            }
        }

        // ========== 2. 调用原版 Helper 方法 ==========
        // 这会触发荆棘、诅咒绑定等附魔的效果
        try {
            EnchantmentHelper.doPostDamageEffects(knockbackSource, target);
            EnchantmentHelper.doPostHurtEffects(target, knockbackSource);
        } catch (Exception ignored) {}

        // ========== 3. 标记攻击关系 ==========
        owner.setLastHurtMob(target);
        target.setLastHurtByMob(knockbackSource);
    }

    private static boolean isBlacklisted(String enchantId, List<? extends String> blacklist) {
        for (String pattern : blacklist) {
            if (enchantId.contains(pattern)) return true;
        }
        return false;
    }
}
