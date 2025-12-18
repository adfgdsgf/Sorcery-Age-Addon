package com.jujutsuaddon.addon.client.util;

import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.ToggleExtraTechniqueC2SPacket;
import com.jujutsuaddon.addon.network.c2s.TriggerTenShadowsAbilityC2SPacket;
import com.jujutsuaddon.addon.network.c2s.UntriggerAbilityC2SPacket;
import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import com.jujutsuaddon.addon.util.helper.tenshadows.TenShadowsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsMode;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.c2s.TriggerAbilityC2SPacket;

import javax.annotation.Nullable;

/**
 * 技能触发工具类
 */
public class AbilityTriggerHelper {

    /**
     * 切换技能状态（开/关）
     */
    public static boolean toggleAbility(@Nullable Ability ability) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (ability == null || player == null) return false;

        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return false;

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return false;

        // 十影技能：使用原子包
        if (TenShadowsHelper.isEnabled() && TenShadowsHelper.isTenShadowsAbility(ability)) {
            return triggerTenShadowsAbility(player, ability, key, data);
        }

        // 召唤物特殊处理
        if (ability instanceof Summon<?> summon) {
            boolean isToggled = data.hasToggled(ability);
            boolean hasSummon = data.hasSummonOfClass(summon.getClazz());

            if (isToggled || hasSummon) {
                AddonNetwork.sendToServer(new UntriggerAbilityC2SPacket(key));
            } else {
                PacketHandler.sendToServer(new TriggerAbilityC2SPacket(key));
            }
            return true;
        }

        // 普通切换类技能
        if (data.hasToggled(ability)) {
            AddonNetwork.sendToServer(new UntriggerAbilityC2SPacket(key));
        } else {
            PacketHandler.sendToServer(new TriggerAbilityC2SPacket(key));
        }
        return true;
    }

    private static boolean triggerTenShadowsAbility(LocalPlayer player, Ability ability,
                                                    ResourceLocation key, ISorcererData data) {
        TenShadowsMode required = TenShadowsHelper.getRequiredMode(player, ability);

        if (ability instanceof Summon<?> summon) {
            boolean isToggled = data.hasToggled(ability);
            boolean hasSummon = data.hasSummonOfClass(summon.getClazz());

            if (isToggled || hasSummon) {
                AddonNetwork.sendToServer(new UntriggerAbilityC2SPacket(key));
            } else {
                updateClientMode(player, required);
                AddonNetwork.sendToServer(new TriggerTenShadowsAbilityC2SPacket(key, required));
            }
            return true;
        }

        if (data.hasToggled(ability)) {
            AddonNetwork.sendToServer(new UntriggerAbilityC2SPacket(key));
        } else {
            updateClientMode(player, required);
            AddonNetwork.sendToServer(new TriggerTenShadowsAbilityC2SPacket(key, required));
        }
        return true;
    }

    private static void updateClientMode(LocalPlayer player, @Nullable TenShadowsMode required) {
        if (required == null) return;
        ITenShadowsData tenData = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
        if (tenData != null && tenData.getMode() != required) {
            tenData.setMode(required);
        }
    }

    /**
     * 激活技能
     */
    public static boolean activateAbility(@Nullable Ability ability, boolean ignoreCooldown) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (ability == null || player == null) return false;

        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return false;

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return false;

        if (!ignoreCooldown) {
            if (data.getRemainingCooldown(ability) > 0) return false;
            if (ability.getStatus(player) != Ability.Status.SUCCESS) return false;
        }

        if (TenShadowsHelper.isEnabled() && TenShadowsHelper.isTenShadowsAbility(ability)) {
            TenShadowsMode required = TenShadowsHelper.getRequiredMode(player, ability);
            updateClientMode(player, required);
            AddonNetwork.sendToServer(new TriggerTenShadowsAbilityC2SPacket(key, required));
            return true;
        }

        PacketHandler.sendToServer(new TriggerAbilityC2SPacket(key));
        return true;
    }

    /**
     * 关闭技能
     */
    public static boolean deactivateAbility(@Nullable Ability ability) {
        if (ability == null) return false;

        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return false;

        AddonNetwork.sendToServer(new UntriggerAbilityC2SPacket(key));
        return true;
    }

    /**
     * 检查技能是否处于激活状态
     */
    public static boolean isAbilityActive(@Nullable Ability ability) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (ability == null || player == null) return false;

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return false;

        if (ability instanceof Summon<?> summon) {
            return data.hasToggled(ability) || data.hasSummonOfClass(summon.getClazz());
        }

        return data.hasToggled(ability);
    }

    /**
     * ★★★ 切换额外术式的激活状态（统一处理复制和偷取）★★★
     */
    public static boolean toggleExtraTechnique(@Nullable CursedTechnique technique) {
        if (technique == null) return false;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return false;

        // 获取术式来源
        TechniqueHelper.TechniqueSource source = TechniqueHelper.getTechniqueSource(player, technique);

        if (source == TechniqueHelper.TechniqueSource.NONE) {
            return false;  // 玩家没有这个术式
        }

        // 检查当前是否激活
        boolean isCurrentlyActive = TechniqueHelper.isTechniqueActive(player, technique);

        // 发送网络包
        AddonNetwork.sendToServer(new ToggleExtraTechniqueC2SPacket(technique, source, isCurrentlyActive));

        // 客户端乐观更新
        if (source == TechniqueHelper.TechniqueSource.STOLEN) {
            data.setCurrentStolen(isCurrentlyActive ? null : technique);
        } else {
            data.setCurrentCopied(isCurrentlyActive ? null : technique);
        }

        return true;
    }

    /**
     * @deprecated 使用 {@link #toggleExtraTechnique(CursedTechnique)} 代替
     */
    @Deprecated
    public static boolean toggleCopiedTechnique(@Nullable CursedTechnique technique) {
        return toggleExtraTechnique(technique);
    }
}
