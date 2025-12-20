package com.jujutsuaddon.addon.client;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.client.autoaim.AimAssist;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.gui.screen.cursemanagement.CurseManagementScreen;
import com.jujutsuaddon.addon.client.gui.screen.HUDEditScreen;
import com.jujutsuaddon.addon.client.gui.screen.SkillBarConfigScreen;
import com.jujutsuaddon.addon.client.gui.screen.shadowstorage.ShadowStorageScreen;
import com.jujutsuaddon.addon.client.skillbar.*;
import com.jujutsuaddon.addon.client.util.AbilityTriggerHelper;
import com.jujutsuaddon.addon.client.util.FeatureToggleManager;
import com.jujutsuaddon.addon.network.*;
import com.jujutsuaddon.addon.network.c2s.*;
import com.jujutsuaddon.addon.util.helper.TechniqueAccessHelper;
import com.jujutsuaddon.addon.util.helper.tenshadows.TenShadowsHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static boolean skillKeysEnabled = true;
    private static final Map<Integer, Ability> channelingSlots = new HashMap<>();
    private static final Set<Integer> keysDown = new HashSet<>();

    private static final ResourceLocation SHADOW_STORAGE_KEY =
            new ResourceLocation("jujutsu_kaisen", "shadow_storage");

    // ★★★ 新增：无下限技能的 ResourceLocation ★★★
    private static final ResourceLocation INFINITY_KEY =
            new ResourceLocation("jujutsu_kaisen", "infinity");

    public static boolean areSkillKeysEnabled() { return skillKeysEnabled; }
    public static void setSkillKeysEnabled(boolean enabled) { skillKeysEnabled = enabled; }
    public static boolean isSlotHeld(int slot) { return channelingSlots.containsKey(slot); }

    private static final Map<Integer, Long> channelingStartTime = new HashMap<>();
    private static final long CHANNELING_VERIFY_DELAY = 150;

    // ★★★ 反弹模式枚举 ★★★
    private enum ReflectMode {
        NONE,       // 不反弹
        TO_OWNER,   // 反弹到原发射者
        TO_CURSOR   // 反弹到准星方向
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.screen == null && event.getAction() != GLFW.GLFW_REPEAT) {
            // ===== 自瞄切换 =====
            if (AddonKeyBindings.TOGGLE_AIM_ASSIST != null) {
                if (AddonKeyBindings.TOGGLE_AIM_ASSIST.matches(event.getKey(), event.getScanCode())) {
                    if (AddonClientConfig.CLIENT != null && AddonClientConfig.CLIENT.aimAssistEnabled.get()) {
                        String triggerMode = AddonClientConfig.CLIENT.aimAssistTriggerMode.get();
                        if ("HOLD".equalsIgnoreCase(triggerMode)) {
                            if (event.getAction() == GLFW.GLFW_PRESS) {
                                AimAssist.setKeyHeld(true);
                            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                                AimAssist.setKeyHeld(false);
                            }
                        } else {
                            if (event.getAction() == GLFW.GLFW_PRESS) {
                                AimAssist.toggle();
                            }
                        }
                    }
                    return;
                }
            }
            // ===== 切换技能栏快捷键开关 =====
            if (AddonKeyBindings.TOGGLE_SKILL_KEYS != null) {
                if (AddonKeyBindings.TOGGLE_SKILL_KEYS.matches(event.getKey(), event.getScanCode())) {
                    if (event.getAction() == GLFW.GLFW_PRESS) {
                        skillKeysEnabled = !skillKeysEnabled;
                        String key = skillKeysEnabled
                                ? "message.jujutsu_addon.keys_enabled"
                                : "message.jujutsu_addon.keys_disabled";
                        player.displayClientMessage(Component.translatable(key), true);
                    }
                    return;
                }
            }
            // ===== 打开HUD编辑界面 =====
            if (AddonKeyBindings.OPEN_HUD_EDIT != null) {
                if (AddonKeyBindings.OPEN_HUD_EDIT.matches(event.getKey(), event.getScanCode())) {
                    if (event.getAction() == GLFW.GLFW_PRESS) {
                        mc.setScreen(new HUDEditScreen());
                    }
                    return;
                }
            }
        }
        if (!FeatureToggleManager.isSkillBarEnabled()) return;
        if (mc.screen == null && event.getAction() == GLFW.GLFW_PRESS) {
            // ===== 打开技能栏配置 =====
            if (AddonKeyBindings.OPEN_SKILL_CONFIG != null && AddonKeyBindings.OPEN_SKILL_CONFIG.consumeClick()) {
                mc.setScreen(new SkillBarConfigScreen());
                return;
            }

            if (!AddonClientConfig.CLIENT.enableSkillBar.get()) return;

            // ===== 下一个预设 =====
            if (AddonKeyBindings.NEXT_PRESET != null && AddonKeyBindings.NEXT_PRESET.consumeClick()) {
                SkillBarManager.nextPreset();
                return;
            }

            // ===== 上一个预设 =====
            if (AddonKeyBindings.PREV_PRESET != null && AddonKeyBindings.PREV_PRESET.consumeClick()) {
                SkillBarManager.prevPreset();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!FeatureToggleManager.isSkillBarEnabled()) {
            stopAllChanneling(Minecraft.getInstance().player);
            keysDown.clear();
            channelingStartTime.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            stopAllChanneling(player);
            keysDown.clear();
            channelingStartTime.clear();
            return;
        }
        if (!AddonClientConfig.CLIENT.enableSkillBar.get() || !skillKeysEnabled) {
            stopAllChanneling(player);
            keysDown.clear();
            channelingStartTime.clear();
            return;
        }

        verifyChannelingState(player);

        for (int i = 0; i < AddonKeyBindings.SKILL_SLOT_KEYS.size(); i++) {
            KeyMapping keyMapping = AddonKeyBindings.SKILL_SLOT_KEYS.get(i);
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
        InfinityFieldClientCache.clear();  // ★★★ 加这行 ★★★
    }

    public static void resetState() {
        channelingSlots.clear();
        channelingStartTime.clear();
        keysDown.clear();
        skillKeysEnabled = true;
    }

    private static void onSlotKeyPressed(LocalPlayer player, int slot) {
        // ★★★ 投射物反弹检测（最高优先级）★★★
        Ability slotAbility = SkillBarManager.getSlot(slot);
        if (slotAbility != null && isInfinityAbility(slotAbility)) {
            // 检查是否开启了无下限
            if (JJKAbilities.hasToggled(player, JJKAbilities.INFINITY.get())) {
                ReflectMode reflectMode = getReflectMode();
                if (reflectMode != ReflectMode.NONE) {
                    // 发送反弹包
                    Vec3 lookDir = player.getLookAngle();
                    boolean toCursor = (reflectMode == ReflectMode.TO_CURSOR);
                    AddonNetwork.sendToServer(new ReflectProjectilesC2SPacket(toCursor, lookDir));
                    return;  // 不执行原本的技能触发
                }
            }
        }

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

        // ===== 非十影技能的逻辑 =====
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
            channelingStartTime.put(slot, System.currentTimeMillis());

            AddonNetwork.sendToServer(new TriggerAbilityWithSyncC2SPacket(key));
            return;
        }

        if (cooldown > 0 && !ignoreCooldown) return;
        if (ability.getStatus(player) != Ability.Status.SUCCESS && !ignoreCooldown) return;
        AddonNetwork.sendToServer(new TriggerAbilityWithSyncC2SPacket(key));
    }

    // ★★★ 获取当前反弹模式 ★★★
    private static ReflectMode getReflectMode() {
        // 优先检测准星方向（因为 Ctrl 可能和滚轮冲突，需要更精确）
        if (isKeyDown(AddonKeyBindings.REFLECT_TO_CURSOR_MODIFIER)) {
            return ReflectMode.TO_CURSOR;
        }

        if (isKeyDown(AddonKeyBindings.REFLECT_TO_OWNER_MODIFIER)) {
            return ReflectMode.TO_OWNER;
        }

        return ReflectMode.NONE;
    }

    // ★★★ 检查是否是无下限技能 ★★★
    private static boolean isInfinityAbility(Ability ability) {
        ResourceLocation key = JJKAbilities.getKey(ability);
        return INFINITY_KEY.equals(key);
    }

    private static void verifyChannelingState(LocalPlayer player) {
        if (channelingStartTime.isEmpty()) return;

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return;

        Ability serverChanneled = data.getChanneled();
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<Integer, Long>> it = channelingStartTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            int slot = entry.getKey();
            long startTime = entry.getValue();

            if (now - startTime < CHANNELING_VERIFY_DELAY) continue;

            Ability clientChanneled = channelingSlots.get(slot);
            if (clientChanneled == null) {
                it.remove();
                continue;
            }

            if (serverChanneled != clientChanneled) {
                channelingSlots.remove(slot);
                it.remove();
            } else {
                it.remove();
            }
        }
    }

    private static void handleShadowStorage(LocalPlayer player) {
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

        if (!player.isShiftKeyDown() && !player.getMainHandItem().isEmpty()) {
            AddonNetwork.sendToServer(new StoreShadowItemC2SPacket());
            playShadowStorageEffect(player);
            return;
        }

        Minecraft.getInstance().setScreen(new ShadowStorageScreen());
    }

    private static void playShadowStorageEffect(LocalPlayer player) {
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
            TenShadowsHelper.UnavailableReason reason =
                    TenShadowsHelper.getUnavailableReason(player, ability);

            String messageKey = switch (reason) {
                case FUSION_COMPONENT_DEAD -> "message.jujutsu_addon.fusion_component_dead";
                case SHIKIGAMI_DEAD -> "message.jujutsu_addon.shikigami_dead";
                case NOT_TAMED -> "message.jujutsu_addon.shikigami_not_tamed";
                case NONE, CONDITIONS_NOT_MET, SHIKIGAMI_SUMMONED -> "message.jujutsu_addon.conditions_not_met";
            };

            player.displayClientMessage(Component.translatable(messageKey), true);
            return;
        }

        TenShadowsMode required = TenShadowsHelper.getRequiredMode(player, ability);
        if (required != null) {
            ITenShadowsData tenData = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (tenData != null && tenData.getMode() != required) {
                tenData.setMode(required);
            }
        }

        if (type == Ability.ActivationType.CHANNELED) {
            channelingSlots.put(slot, ability);
            channelingStartTime.put(slot, System.currentTimeMillis());
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
        channelingStartTime.clear();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;
        if (AddonKeyBindings.INFINITY_SCROLL_MODIFIER == null) return;
        if (!isKeyDown(AddonKeyBindings.INFINITY_SCROLL_MODIFIER)) return;
        if (!JJKAbilities.hasToggled(player, JJKAbilities.INFINITY.get())) return;
        double scrollDelta = event.getScrollDelta();
        if (scrollDelta != 0) {
            boolean increase = scrollDelta > 0;
            AddonNetwork.sendToServer(new SyncInfinityPressureC2SPacket(increase));
            event.setCanceled(true);
        }
    }

    private static boolean isKeyDown(KeyMapping keyMapping) {
        if (keyMapping == null || keyMapping.isUnbound()) return false;
        Minecraft mc = Minecraft.getInstance();
        InputConstants.Key key = keyMapping.getKey();
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}