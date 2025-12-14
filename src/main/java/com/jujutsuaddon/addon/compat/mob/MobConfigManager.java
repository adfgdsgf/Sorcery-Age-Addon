package com.jujutsuaddon.addon.compat.mob;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import radon.jujutsu_kaisen.capability.data.sorcerer.JujutsuType;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class MobConfigManager {
    private static final Map<EntityType<?>, MobConfigData> FAST_CACHE = new IdentityHashMap<>();
    private static final Map<String, MobConfigData> STRING_CACHE = new HashMap<>();

    private static boolean isCacheDirty = true;

    // ★ 扩展：添加 jujutsuTypeName 字段 ★
    public record MobConfigData(
            String techniqueName,
            String gradeName,
            String jujutsuTypeName  // "SORCERER", "CURSE", "RANDOM"
    ) {}

    public static void markDirty() {
        isCacheDirty = true;
    }

    private static void reloadConfig() {
        STRING_CACHE.clear();
        FAST_CACHE.clear();

        List<? extends String> list = AddonConfig.COMMON.compatMobList.get();
        for (String entry : list) {
            try {
                String[] parts = entry.split("\\|");
                if (parts.length >= 3) {
                    String mobId = parts[0].trim();
                    String technique = parts[1].trim();
                    String grade = parts[2].trim();

                    // ★ 解析第4个参数：阵营（可选，默认 RANDOM）★
                    String jujutsuType = "RANDOM";
                    if (parts.length >= 4) {
                        jujutsuType = parts[3].trim().toUpperCase();
                    }

                    STRING_CACHE.put(mobId, new MobConfigData(technique, grade, jujutsuType));
                }
            } catch (Exception e) {
                System.err.println("[JujutsuAddon] Config parse error: " + entry);
            }
        }
        isCacheDirty = false;
    }

    public static MobConfigData getMobConfig(LivingEntity entity) {
        if (!AddonConfig.COMMON.enableMobCompatibility.get()) return null;
        if (isCacheDirty) reloadConfig();

        EntityType<?> type = entity.getType();

        if (FAST_CACHE.containsKey(type)) {
            return FAST_CACHE.get(type);
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        MobConfigData data = (id != null) ? STRING_CACHE.get(id.toString()) : null;

        FAST_CACHE.put(type, data);

        return data;
    }
}
