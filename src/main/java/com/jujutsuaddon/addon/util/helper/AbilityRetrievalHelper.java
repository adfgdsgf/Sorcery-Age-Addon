package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.util.helper.tenshadows.TenShadowsHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 技能获取工具类
 * 统一处理：原生技能、十影(含死亡)、偷窃/复制的多层嵌套技能获取
 */
public class AbilityRetrievalHelper {

    /**
     * 获取玩家拥有的所有技能（扁平化列表）
     * 包含：原生、十影（含死亡）、偷窃、复制（含嵌套）
     * 已自动去重
     */
    public static List<Ability> getAllPlayerAbilities(Player player) {
        List<Ability> result = new ArrayList<>();
        if (player == null) return result;

        Set<ResourceLocation> collectedKeys = new HashSet<>();
        Set<ResourceLocation> extraAbilityKeys = new HashSet<>();

        // 1. 预扫描：收集所有额外术式(偷窃/复制)的Key，用于从原生列表中剔除重复项
        // (防止 JJK 原版 getAbilities 把偷来的技能也算作原生技能返回)
        for (CursedTechnique technique : TechniqueHelper.getAllExtraTechniques(player)) {
            if (technique != null) {
                for (Ability ability : technique.getAbilities()) {
                    ResourceLocation key = JJKAbilities.getKey(ability);
                    if (key != null) extraAbilityKeys.add(key);
                }
            }
        }

        boolean useTenShadowsHelper = TenShadowsHelper.isEnabled();

        // 2. 获取原生技能 (过滤掉属于额外术式的)
        for (Ability ability : JJKAbilities.getAbilities(player)) {
            if (ability == null) continue;
            ResourceLocation key = JJKAbilities.getKey(ability);

            // 过滤条件：无Key、属于额外术式、或者已经收集过
            if (key == null || extraAbilityKeys.contains(key) || collectedKeys.contains(key)) continue;

            boolean isValid;
            if (useTenShadowsHelper && TenShadowsHelper.isTenShadowsAbility(ability)) {
                isValid = true; // 十影技能特殊处理，即使未调伏也显示
            } else {
                isValid = ability.isValid(player);
            }

            if (isValid) {
                result.add(ability);
                collectedKeys.add(key);
            }
        }

        // 3. 获取原生十影技能 (包含死亡式神)
        if (useTenShadowsHelper) {
            List<Ability> tenShadowsAbilities = TenShadowsHelper.getAllTenShadowsAbilitiesIncludingDead(player);
            for (Ability ability : tenShadowsAbilities) {
                if (ability == null) continue;
                ResourceLocation key = JJKAbilities.getKey(ability);

                if (key == null || extraAbilityKeys.contains(key) || collectedKeys.contains(key)) continue;

                result.add(ability);
                collectedKeys.add(key);
            }
        }

        // 4. 获取额外术式技能 (偷窃/复制) - 包含深度递归逻辑
        for (CursedTechnique technique : TechniqueHelper.getAllExtraTechniques(player)) {
            if (technique == null) continue;

            // 使用 TenShadowsHelper 获取，以支持偷来的十影术
            List<Ability> abilities = TenShadowsHelper.getAbilitiesForTechnique(player, technique);

            for (Ability ability : abilities) {
                if (ability == null) continue;

                // 检查有效性 (十影除外)
                if (!TenShadowsHelper.shouldSkipValidCheck(player, ability)) {
                    if (!ability.isValid(player)) continue;
                }

                ResourceLocation key = JJKAbilities.getKey(ability);
                if (key != null && !collectedKeys.contains(key)) {
                    result.add(ability);
                    collectedKeys.add(key);
                }
            }
        }

        return result;
    }
}
