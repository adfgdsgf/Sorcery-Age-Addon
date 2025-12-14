package com.jujutsuaddon.addon.compat.mob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 阵营判定辅助类
 * 复用主mod的 JujutsuType 系统
 */
public class MobFactionHelper {

    private static final Random RANDOM = new Random();

    /**
     * 获取实体的阵营类型
     */
    public static JujutsuType getType(LivingEntity entity) {
        if (entity == null) return null;

        Optional<ISorcererData> capOpt = entity.getCapability(SorcererDataHandler.INSTANCE).resolve();
        if (capOpt.isPresent()) {
            return capOpt.get().getType();
        }
        return null;
    }

    /**
     * 设置实体的阵营类型
     */
    public static void setType(LivingEntity entity, JujutsuType type) {
        entity.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            cap.setType(type);
        });
    }

    /**
     * 根据配置字符串获取阵营
     * @param typeName "SORCERER", "CURSE", "RANDOM"
     * @return 对应的 JujutsuType，如果是 RANDOM 则随机返回
     */
    public static JujutsuType parseType(String typeName) {
        if (typeName == null || typeName.isEmpty() || "RANDOM".equalsIgnoreCase(typeName)) {
            return getRandomType();
        }
        try {
            JujutsuType type = JujutsuType.valueOf(typeName.toUpperCase());
            // SHIKIGAMI 暂时 ban，如果配置了就改成随机
            if (type == JujutsuType.SHIKIGAMI) {
                return getRandomType();
            }
            return type;
        } catch (IllegalArgumentException e) {
            return getRandomType();
        }
    }

    /**
     * 随机获取阵营（排除 SHIKIGAMI）
     */
    public static JujutsuType getRandomType() {
        List<JujutsuType> validTypes = new ArrayList<>();
        for (JujutsuType type : JujutsuType.values()) {
            if (type != JujutsuType.SHIKIGAMI) {
                validTypes.add(type);
            }
        }
        return validTypes.get(RANDOM.nextInt(validTypes.size()));
    }

    /**
     * 判断两个实体是否敌对
     *
     * 规则：
     * 1. 不攻击自己
     * 2. 驯化的生物不攻击主人
     * 3. 驯化的生物不攻击同主人的其他宠物
     * 4. 咒术师 vs 咒灵 = 敌对
     * 5. 同阵营 = 友好
     * 6. 不主动攻击玩家
     */
    public static boolean isEnemy(LivingEntity self, LivingEntity target) {
        if (self == null || target == null) return false;
        if (self == target) return false;

        // ★ 驯化检查：不攻击主人 ★
        if (self instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();

            // 不攻击主人
            if (target == owner) return false;

            // 不攻击主人的其他宠物
            if (target instanceof TamableAnimal otherPet && otherPet.isTame()) {
                if (otherPet.getOwner() == owner) return false;
            }
        }

        // 不主动攻击玩家
        if (target instanceof Player) return false;

        JujutsuType selfType = getType(self);
        JujutsuType targetType = getType(target);

        // 如果任何一方没有阵营数据，不判定为敌人
        if (selfType == null || targetType == null) return false;

        // 式神不主动攻击（由主人控制）
        if (selfType == JujutsuType.SHIKIGAMI) return false;

        // 核心对立逻辑
        return switch (selfType) {
            case SORCERER -> targetType == JujutsuType.CURSE;
            case CURSE -> targetType == JujutsuType.SORCERER;
            case SHIKIGAMI -> false;
        };
    }

    /**
     * 判断两个实体是否同阵营
     */
    public static boolean isAlly(LivingEntity self, LivingEntity target) {
        if (self == null || target == null) return false;
        if (self == target) return true;

        JujutsuType selfType = getType(self);
        JujutsuType targetType = getType(target);

        if (selfType == null || targetType == null) return false;

        return selfType == targetType;
    }

    /**
     * 获取阵营的翻译键
     */
    public static String getTypeTranslationKey(JujutsuType type) {
        if (type == null) return "message.jujutsu_addon.faction.none";
        return switch (type) {
            case SORCERER -> "message.jujutsu_addon.faction.sorcerer";
            case CURSE -> "message.jujutsu_addon.faction.curse";
            case SHIKIGAMI -> "message.jujutsu_addon.faction.shikigami";
        };
    }
}
