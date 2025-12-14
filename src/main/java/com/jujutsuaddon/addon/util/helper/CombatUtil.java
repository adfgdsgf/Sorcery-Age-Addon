package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
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
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.item.JJKItems;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     */
    public static Ability findAbility(DamageSource source) {
        String msgId = source.getMsgId();
        String lowerMsgId = (msgId != null) ? msgId.toLowerCase() : "";
        Entity directEntity = source.getDirectEntity();
        String directEntityName = (directEntity != null) ? directEntity.getClass().getSimpleName().toLowerCase() : "";
        String cacheKey = lowerMsgId + "|" + directEntityName;

        if (ABILITY_CACHE.containsKey(cacheKey)) {
            return ABILITY_CACHE.get(cacheKey);
        }

        IForgeRegistry<Ability> registry = RegistryManager.ACTIVE.getRegistry(new ResourceLocation("radon.jujutsu_kaisen", "ability"));
        if (registry == null) {
            registry = RegistryManager.ACTIVE.getRegistry(new ResourceLocation("jujutsu_kaisen", "ability"));
        }
        if (registry == null) return null;

        // 1. 检查 NBT 标记
        if (directEntity != null) {
            if (directEntity.getPersistentData().contains("jjk_addon_source_class")) {
                String className = directEntity.getPersistentData().getString("jjk_addon_source_class");
                for (Ability ability : registry) {
                    if (ability.getClass().getName().equals(className)) return ability;
                }
            }
            if (directEntity.getPersistentData().contains("jjk_addon_source")) {
                String sourceAbilityName = directEntity.getPersistentData().getString("jjk_addon_source");
                for (Ability ability : registry) {
                    if (ability.getClass().getSimpleName().equals(sourceAbilityName)) {
                        ABILITY_CACHE.put(cacheKey, ability);
                        return ability;
                    }
                }
            }
        }

        // 2. 特殊硬编码 (苍)
        if (lowerMsgId.contains("blue") && lowerMsgId.contains("fist")) {
            for (Ability ability : registry) {
                if (ability.getClass().getSimpleName().equals("BlueFists")) {
                    ABILITY_CACHE.put(cacheKey, ability);
                    return ability;
                }
            }
        }

        // 3. 模糊匹配
        for (Ability ability : registry) {
            String abilityClassName = ability.getClass().getSimpleName().toLowerCase();
            if (!directEntityName.isEmpty() && directEntityName.contains(abilityClassName)) {
                ABILITY_CACHE.put(cacheKey, ability);
                return ability;
            }
            if (!lowerMsgId.isEmpty() && lowerMsgId.contains(abilityClassName)) {
                ABILITY_CACHE.put(cacheKey, ability);
                return ability;
            }
        }
        return null;
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
