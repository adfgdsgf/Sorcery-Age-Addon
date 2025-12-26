package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.util.debug.DebugManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.misc.CursedEnergyFlow;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.item.JJKItems;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatUtil {

    private static final Map<String, Ability> ABILITY_CACHE = new ConcurrentHashMap<>();
    private static final Field HURT_RESISTANT_TIME_FIELD;

    static {
        Field f = null;
        try {
            f = ObfuscationReflectionHelper.findField(LivingEntity.class, "field_70172_ad");
        } catch (Exception e) {
            try {
                f = ObfuscationReflectionHelper.findField(LivingEntity.class, "hurtResistantTime");
            } catch (Exception ignored) {}
        }
        HURT_RESISTANT_TIME_FIELD = f;
    }

    /**
     * 强制设置实体的无敌帧
     */
    public static void setIframe(LivingEntity target, int ticks) {
        target.invulnerableTime = ticks;
        try {
            if (HURT_RESISTANT_TIME_FIELD != null) {
                HURT_RESISTANT_TIME_FIELD.setInt(target, ticks);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 尝试从伤害源中解析出对应的 Ability 对象
     *
     * ★★★ 修复版本：优先使用 JJK 伤害源，过滤纯被动技能 ★★★
     */
    public static Ability findAbility(DamageSource source) {
        // ★★★ 方法1：优先从 JJK 伤害源直接获取（最可靠）★★★
        if (source instanceof JJKDamageSources.JujutsuDamageSource jjkSource) {
            Ability ability = jjkSource.getAbility();
            if (ability != null) {
                // 检查是否被排除
                if (isExcludedAbility(ability)) {
                    return null;
                }
                return ability;
            }
            // JJK 伤害源明确没有技能，不继续猜测
            return null;
        }

        String msgId = source.getMsgId();
        String lowerMsgId = (msgId != null) ? msgId.toLowerCase() : "";
        Entity directEntity = source.getDirectEntity();
        String directEntityName = (directEntity != null) ? directEntity.getClass().getSimpleName().toLowerCase() : "";
        String cacheKey = lowerMsgId + "|" + directEntityName;

        // 检查缓存
        if (ABILITY_CACHE.containsKey(cacheKey)) {
            Ability cached = ABILITY_CACHE.get(cacheKey);
            if (cached == null || !isExcludedAbility(cached)) {
                return cached;
            }
            return null;
        }

        IForgeRegistry<Ability> registry = RegistryManager.ACTIVE.getRegistry(new ResourceLocation("radon.jujutsu_kaisen", "ability"));
        if (registry == null) {
            registry = RegistryManager.ACTIVE.getRegistry(new ResourceLocation("jujutsu_kaisen", "ability"));
        }
        if (registry == null) return null;

        // 2. 检查 NBT 标记
        if (directEntity != null) {
            if (directEntity.getPersistentData().contains("jjk_addon_source_class")) {
                String className = directEntity.getPersistentData().getString("jjk_addon_source_class");
                for (Ability ability : registry) {
                    if (ability.getClass().getName().equals(className) && !isExcludedAbility(ability)) {
                        return ability;
                    }
                }
            }
            if (directEntity.getPersistentData().contains("jjk_addon_source")) {
                String sourceAbilityName = directEntity.getPersistentData().getString("jjk_addon_source");
                for (Ability ability : registry) {
                    if (ability.getClass().getSimpleName().equals(sourceAbilityName) && !isExcludedAbility(ability)) {
                        ABILITY_CACHE.put(cacheKey, ability);
                        return ability;
                    }
                }
            }
        }

        // 3. 特殊硬编码 (苍)
        if (lowerMsgId.contains("blue") && lowerMsgId.contains("fist")) {
            for (Ability ability : registry) {
                if (ability.getClass().getSimpleName().equals("BlueFists")) {
                    ABILITY_CACHE.put(cacheKey, ability);
                    return ability;
                }
            }
        }

        // ★★★ 4. 模糊匹配 - 仅限投射物情况 ★★★
        if (directEntity != null && directEntity != source.getEntity()) {
            for (Ability ability : registry) {
                if (isExcludedAbility(ability)) continue;

                String abilityClassName = ability.getClass().getSimpleName().toLowerCase();

                // 只匹配实体名称
                if (!directEntityName.isEmpty() && directEntityName.contains(abilityClassName)) {
                    ABILITY_CACHE.put(cacheKey, ability);
                    return ability;
                }
            }
        }

        // ★ 移除 msgId 模糊匹配，避免误判 ★
        return null;
    }

    /**
     * 从投射物实体推断技能（仅用于 JJK 投射物）
     * 只匹配实体类名，不做模糊匹配
     */
    public static Ability findAbilityFromProjectile(Entity projectile) {
        if (projectile == null) return null;

        String entityClassName = projectile.getClass().getSimpleName().toLowerCase();

        // 检查 NBT 标记
        if (projectile.getPersistentData().contains("jjk_addon_source_class")) {
            String className = projectile.getPersistentData().getString("jjk_addon_source_class");
            IForgeRegistry<Ability> registry = RegistryManager.ACTIVE.getRegistry(
                    new ResourceLocation("jujutsu_kaisen", "ability"));
            if (registry != null) {
                for (Ability ability : registry) {
                    if (ability.getClass().getName().equals(className)) {
                        return ability;
                    }
                }
            }
        }

        // 从实体类名推断
        IForgeRegistry<Ability> registry = RegistryManager.ACTIVE.getRegistry(
                new ResourceLocation("jujutsu_kaisen", "ability"));
        if (registry == null) return null;

        for (Ability ability : registry) {
            String abilityName = ability.getClass().getSimpleName().toLowerCase();
            if (entityClassName.contains(abilityName) || abilityName.contains(entityClassName)) {
                return ability;
            }
        }

        return null;
    }

    /**
     * 检查技能是否应该被排除（纯被动/状态技能）
     *
     * 排除条件：
     * - 是 IToggled 但不是 IAttack/IDomainAttack（纯被动状态）
     * - 或者在硬编码排除列表中
     */
    private static boolean isExcludedAbility(Ability ability) {
        if (ability == null) return false;

        // ★ 硬编码排除（最高优先级）★
        if (ability instanceof CursedEnergyFlow) {
            return true;
        }

        // ★ 智能判断 ★
        if (ability instanceof Ability.IToggled) {
            // 如果同时是攻击类技能，不排除
            if (ability instanceof Ability.IAttack) {
                return false;  // NueLightning, Cleave 等
            }
            if (ability instanceof Ability.IDomainAttack) {
                return false;
            }

            // 纯 Toggle 且不是术式 = 被动状态
            if (!ability.isTechnique()) {
                return true;  // CursedEnergyFlow
            }
        }

        return false;
    }

    /**
     * 检查并打印主模组装备的额外加成 (游云、钢铁护手)
     */
    public static void checkAndLogMainModBonus(Player player, float currentDamage) {
        if (!DebugManager.isDebugging(player)) return;

        List<Item> stacks = new ArrayList<>();
        stacks.add(player.getMainHandItem().getItem());
        stacks.add(player.getOffhandItem().getItem());

        try {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.findCurios("right_hand").forEach(slotResult -> stacks.add(slotResult.stack().getItem()));
                handler.findCurios("left_hand").forEach(slotResult -> stacks.add(slotResult.stack().getItem()));
            });
        } catch (Exception ignored) {}

        ISorcererData cap = player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        boolean isHR = cap != null && cap.hasTrait(Trait.HEAVENLY_RESTRICTION);

        if (stacks.contains(JJKItems.PLAYFUL_CLOUD.get())) {
            float mult = isHR ? 1.2f : 1.05f;
            String reason = isHR ? "debug.jujutsu_addon.reason.playful_cloud_hr" : "debug.jujutsu_addon.reason.playful_cloud";
            player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.main_mod_bonus",
                    Component.translatable(reason),
                    String.format("%.2f", mult),
                    String.format("%.1f", currentDamage * mult)
            ));
        }

        if (stacks.contains(JJKItems.STEEL_GAUNTLET.get())) {
            float mult = 1.1f;
            player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.main_mod_bonus",
                    Component.translatable("debug.jujutsu_addon.reason.steel_gauntlet"),
                    String.format("%.2f", mult),
                    String.format("%.1f", currentDamage * mult)
            ));
        }
    }
}
