package com.jujutsuaddon.addon;

import java.util.UUID;

public class AddonConstants {
    // ==================== UUIDs ====================
    // 玩家/生物 属性修饰符
    public static final UUID CONVERTED_ARMOR_UUID = UUID.fromString("99887766-5544-3322-1100-aabbccddeeff");
    public static final UUID CONVERTED_TOUGHNESS_UUID = UUID.fromString("11223344-5566-7788-9900-ffeeddccbbaa");
    public static final UUID ADDON_HEALTH_UUID = UUID.fromString("a8b9c0d1-e2f3-4a5b-6c7d-8e9f0a1b2c3d");
    public static final UUID PLAYFUL_CLOUD_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-56789abcdef0");

    // 召唤物 属性修饰符
    public static final UUID SUMMON_HP_MODIFIER_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000001");
    public static final UUID SUMMON_ATK_MODIFIER_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000002");
    public static final UUID SUMMON_FOLLOW_RANGE_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000003");

    // JJK 原版 UUID (用于兼容/覆盖)
    public static final UUID JJK_ATTACK_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");
    public static final UUID TARGET_JJK_HEALTH_UUID = UUID.fromString("72ff5080-3a82-4a03-8493-3be970039cfe");

    // ==================== NBT Keys ====================
    public static final String NBT_JJK_SOURCE_CLASS = "jjk_addon_source_class";
    public static final String NBT_LAST_GRADE = "jujutsu_addon_last_grade";
    public static final String NBT_TRAITS_INITIALIZED = "jujutsu_addon_traits_initialized";
    public static final String NBT_FACTION_INITIALIZED = "jujutsu_addon_faction_initialized";

    // 召唤物状态 NBT (用于替代 Mixin 中的字段)
    public static final String NBT_SUMMON_LAST_HP_BONUS = "jjk_addon_last_hp_bonus";
    public static final String NBT_SUMMON_LAST_ATK_BONUS = "jjk_addon_last_atk_bonus";
    public static final String NBT_SUMMON_LAST_MAX_HP = "jjk_addon_last_max_hp";
}
