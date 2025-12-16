package com.jujutsuaddon.addon.client;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.gui.screen.cursemanagement.CurseManagementScreen;
import com.jujutsuaddon.addon.client.gui.screen.HUDEditScreen;
import com.jujutsuaddon.addon.client.gui.screen.SkillBarConfigScreen;
import com.jujutsuaddon.addon.client.gui.screen.shadowstorage.ShadowStorageScreen;  // ★ 新增
import com.jujutsuaddon.addon.client.skillbar.*;
import com.jujutsuaddon.addon.client.util.AbilityTriggerHelper;
import com.jujutsuaddon.addon.client.util.FeatureToggleManager;
import com.jujutsuaddon.addon.network.*;
import com.jujutsuaddon.addon.network.c2s.StopChannelingC2SPacket;
import com.jujutsuaddon.addon.network.c2s.StoreShadowItemC2SPacket;
import com.jujutsuaddon.addon.network.c2s.TriggerTenShadowsAbilityC2SPacket;
import com.jujutsuaddon.addon.util.helper.TechniqueAccessHelper;
import com.jujutsuaddon.addon.util.helper.TenShadowsHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static boolean skillKeysEnabled = true;
    private static final Map<Integer, Ability> channelingSlots = new HashMap<>();
    private static final Set<Integer> keysDown = new HashSet<>();

    // ★★★ 影子库存能力的 ResourceLocation ★★★
    private static final ResourceLocation SHADOW_STORAGE_KEY =
            new ResourceLocation("jujutsu_kaisen", "shadow_storage");

    public static boolean areSkillKeysEnabled() { return skillKeysEnabled; }
    public static void setSkillKeysEnabled(boolean enabled) { skillKeysEnabled = enabled; }
    public static boolean isSlotHeld(int slot) { return channelingSlots.containsKey(slot); }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        // ★★★ 功能开关检查 ★★★
        if (!FeatureToggleManager.isSkillBarEnabled()) return;

        if (mc.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (AddonKeyBindings.OPEN_SKILL_CONFIG.consumeClick()) {
                mc.setScreen(new SkillBarConfigScreen());
                return;
            }
            if (AddonKeyBindings.OPEN_HUD_EDIT.consumeClick()) {
                mc.setScreen(new HUDEditScreen());
                return;
            }
            if (AddonKeyBindings.TOGGLE_SKILL_KEYS.consumeClick()) {
                skillKeysEnabled = !skillKeysEnabled;
                player.displayClientMessage(Component.translatable(
                        skillKeysEnabled ? "message.jujutsu_addon.keys_enabled"
                                : "message.jujutsu_addon.keys_disabled"), true);
                return;
            }

            if (!AddonClientConfig.CLIENT.enableSkillBar.get()) return;

            if (AddonKeyBindings.NEXT_PRESET.consumeClick()) {
                SkillBarManager.nextPreset();
                return;
            }
            if (AddonKeyBindings.PREV_PRESET.consumeClick()) {
                SkillBarManager.prevPreset();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // ★★★ 功能开关检查 ★★★
        if (!FeatureToggleManager.isSkillBarEnabled()) {
            stopAllChanneling(Minecraft.getInstance().player);
            keysDown.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            stopAllChanneling(player);
            keysDown.clear();
            return;
        }
        if (!AddonClientConfig.CLIENT.enableSkillBar.get() || !skillKeysEnabled) {
            stopAllChanneling(player);
            keysDown.clear();
            return;
        }

        for (int i = 0; i < AddonKeyBindings.SKILL_SLOT_KEYS.size(); i++) {
            KeyMapping keyMapping = AddonKeyBindings.SKILL_SLOT_KEYS.get(i);
            // ★★★ 使用 isDown() 而不是 InputConstants.isKeyDown() ★★★
            boolean isDown = keyMapping.isDown();
            boolean wasDown = keysDown.contains(i);
            if (isDown && !wasDown) {
                keysDown.add(i);
                onSlotKeyPressed(player, i);
            } else if (!isDown && wasDown) {
                keysDown.remove(i);
                onSlotKeyReleased(player, i);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        resetState();
        SkillBarManager.onPlayerLogin(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SkillBarManager.onPlayerLogout();
        resetState();
    }

    public static void resetState() {
        channelingSlots.clear();
        keysDown.clear();
        skillKeysEnabled = true;
    }

    private static void onSlotKeyPressed(LocalPlayer player, int slot) {
        // ★★★ 咒灵管理槽位 ★★★
        if (SkillBarManager.isSlotCurseManagement(slot)) {
            if (!SkillBarManager.hasCurseManipulation()) {
                if (SkillBarManager.ownsCurseManipulation()) {
                    player.displayClientMessage(
                            Component.translatable("message.jujutsu_addon.curse_manipulation_not_active"), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.jujutsu_addon.curse_management_unavailable"), true);
                }
                return;
            }
            Minecraft.getInstance().setScreen(new CurseManagementScreen());
            return;
        }

        // ★★★ 术式切换槽位 ★★★
        if (SkillBarManager.isSlotTechnique(slot)) {
            CursedTechnique technique = SkillBarManager.getSlotTechnique(slot);
            if (technique == null || !SkillBarManager.playerHasTechnique(slot)) return;
            AbilityTriggerHelper.toggleExtraTechnique(technique);
            return;
        }

        // ★★★ 普通技能槽位 ★★★
        Ability ability = SkillBarManager.getSlot(slot);
        if (ability == null || !SkillBarManager.playerHasAbility(slot)) return;

        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return;

        // ★★★ 拦截 ShadowStorage 能力 ★★★
        if (SHADOW_STORAGE_KEY.equals(key)) {
            handleShadowStorage(player);
            return;
        }

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return;

        boolean ignoreCooldown = player.isCreative();
        int cooldown = SkillBarManager.getCooldown(slot);

        // ★★★ 十影技能 ★★★
        if (TenShadowsHelper.isEnabled() && TenShadowsHelper.isTenShadowsAbility(ability)) {
            handleTenShadowsAbility(player, slot, ability, key, data, cooldown, ignoreCooldown);
            return;
        }

        // ===== 非十影技能的原有逻辑 =====
        Ability.ActivationType type = ability.getActivationType(player);
        boolean isActive = AbilityTriggerHelper.isAbilityActive(ability);

        if (type == Ability.ActivationType.TOGGLED || ability instanceof Summon<?>) {
            if (isActive) {
                AbilityTriggerHelper.deactivateAbility(ability);
            } else {
                if (cooldown > 0 && !ignoreCooldown) return;
                if (ability.getStatus(player) != Ability.Status.SUCCESS && !ignoreCooldown) return;
                AbilityTriggerHelper.activateAbility(ability, ignoreCooldown);
            }
            return;
        }

        if (type == Ability.ActivationType.CHANNELED) {
            if (cooldown > 0 && !ignoreCooldown) return;
            if (ability.getStatus(player) != Ability.Status.SUCCESS && !ignoreCooldown) return;
            channelingSlots.put(slot, ability);
            PacketHandler.sendToServer(new TriggerAbilityC2SPacket(key));
            return;
        }

        if (cooldown > 0 && !ignoreCooldown) return;
        if (ability.getStatus(player) != Ability.Status.SUCCESS && !ignoreCooldown) return;
        PacketHandler.sendToServer(new TriggerAbilityC2SPacket(key));
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ★★★ 新增：影子库存处理 ★★★
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    private static void handleShadowStorage(LocalPlayer player) {
        // ★★★ 功能开关检查 ★★★
        if (!FeatureToggleManager.isShadowStorageEnabled()) {
            return;
        }

        if (!TechniqueAccessHelper.canUseTenShadows(player)) {
            if (TechniqueAccessHelper.ownsTenShadows(player)) {
                player.displayClientMessage(
                        Component.translatable("message.jujutsu_addon.ten_shadows_not_active"), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.jujutsu_addon.no_ten_shadows"), true);
            }
            return;
        }

        // ★★★ 如果主手有物品且没按 Shift → 直接存入（快捷操作）★★★
        if (!player.isShiftKeyDown() && !player.getMainHandItem().isEmpty()) {
            AddonNetwork.sendToServer(new StoreShadowItemC2SPacket());
            playShadowStorageEffect(player);
            return;
        }

        // ★★★ 直接打开客户端界面（不需要服务端！）★★★
        Minecraft.getInstance().setScreen(new ShadowStorageScreen());
    }

    /**
     * 播放影子存储的视觉效果
     */
    private static void playShadowStorageEffect(LocalPlayer player) {
        // 客户端粒子效果
        for (int i = 0; i < 8; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            player.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    player.getX() + offsetX,
                    player.getY() + 0.5,
                    player.getZ() + offsetZ,
                    0, 0.05, 0
            );
        }
    }

    private static void handleTenShadowsAbility(LocalPlayer player, int slot, Ability ability,
                                                ResourceLocation key, ISorcererData data,
                                                int cooldown, boolean ignoreCooldown) {
        boolean isActive = AbilityTriggerHelper.isAbilityActive(ability);
        Ability.ActivationType type = ability.getActivationType(player);

        if (isActive) {
            AbilityTriggerHelper.deactivateAbility(ability);
            return;
        }

        if (cooldown > 0 && !ignoreCooldown) return;

        if (TenShadowsHelper.hasSummonConflict(player, ability)) {
            player.displayClientMessage(
                    Component.translatable("message.jujutsu_addon.summon_conflict"), true);
            return;
        }

        TenShadowsMode required = TenShadowsHelper.getRequiredMode(ability);

        if (required != null) {
            ITenShadowsData tenData = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (tenData != null && tenData.getMode() != required) {
                tenData.setMode(required);
            }
        }

        if (type == Ability.ActivationType.CHANNELED) {
            channelingSlots.put(slot, ability);
        }

        AddonNetwork.sendToServer(new TriggerTenShadowsAbilityC2SPacket(key, required));
    }

    private static void onSlotKeyReleased(LocalPlayer player, int slot) {
        Ability ability = channelingSlots.remove(slot);
        if (ability == null) return;

        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return;

        stopChannelingClient(player);
        AddonNetwork.sendToServer(new StopChannelingC2SPacket(key));
    }

    private static void stopChannelingClient(LocalPlayer player) {
        if (player == null) return;
        player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            Ability channeled = cap.getChanneled();
            if (channeled != null) cap.channel(channeled);
        });
    }

    private static void stopAllChanneling(LocalPlayer player) {
        if (channelingSlots.isEmpty()) return;
        stopChannelingClient(player);
        if (player != null) {
            for (Ability ability : channelingSlots.values()) {
                ResourceLocation key = JJKAbilities.getKey(ability);
                if (key != null) {
                    AddonNetwork.sendToServer(new StopChannelingC2SPacket(key));
                }
            }
        }
        channelingSlots.clear();
    }
}
