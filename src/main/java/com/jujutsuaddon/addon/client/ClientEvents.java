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
import com.jujutsuaddon.addon.network.c2s.StopChannelingC2SPacket;
import com.jujutsuaddon.addon.network.c2s.StoreShadowItemC2SPacket;
import com.jujutsuaddon.addon.network.c2s.TriggerTenShadowsAbilityC2SPacket;
// ▼▼▼ 新增 import ▼▼▼
import com.jujutsuaddon.addon.network.c2s.TriggerAbilityWithSyncC2SPacket;
// ▲▲▲ 新增 import 结束 ▲▲▲
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
// ▼▼▼ 新增 import ▼▼▼
import java.util.Iterator;
// ▲▲▲ 新增 import 结束 ▲▲▲
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static boolean skillKeysEnabled = true;
    private static final Map<Integer, Ability> channelingSlots = new HashMap<>();
    private static final Set<Integer> keysDown = new HashSet<>();

    private static final ResourceLocation SHADOW_STORAGE_KEY =
            new ResourceLocation("jujutsu_kaisen", "shadow_storage");

    public static boolean areSkillKeysEnabled() { return skillKeysEnabled; }
    public static void setSkillKeysEnabled(boolean enabled) { skillKeysEnabled = enabled; }
    public static boolean isSlotHeld(int slot) { return channelingSlots.containsKey(slot); }

    // ▼▼▼ 新增字段 ▼▼▼
    private static final Map<Integer, Long> channelingStartTime = new HashMap<>();
    private static final long CHANNELING_VERIFY_DELAY = 150; // 150ms
    // ▲▲▲ 新增字段结束 ▲▲▲

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // ... 这个方法不用改，保持原样 ...
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.screen == null && event.getAction() != org.lwjgl.glfw.GLFW.GLFW_REPEAT) {
            if (AddonKeyBindings.TOGGLE_AIM_ASSIST != null) {
                if (AddonKeyBindings.TOGGLE_AIM_ASSIST.matches(event.getKey(), event.getScanCode())) {
                    if (AddonClientConfig.CLIENT != null && AddonClientConfig.CLIENT.aimAssistEnabled.get()) {
                        String triggerMode = AddonClientConfig.CLIENT.aimAssistTriggerMode.get();
                        if ("HOLD".equalsIgnoreCase(triggerMode)) {
                            if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                                AimAssist.setKeyHeld(true);
                            } else if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
                                AimAssist.setKeyHeld(false);
                            }
                        } else {
                            if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                                AimAssist.toggle();
                            }
                        }
                    }
                    return;
                }
            }
        }
        if (!FeatureToggleManager.isSkillBarEnabled()) return;
        if (mc.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (AddonKeyBindings.OPEN_SKILL_CONFIG.consumeClick()) {
                mc.setScreen(new SkillBarConfigScreen());
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

    // ▼▼▼ 修改 onClientTick 方法 ▼▼▼
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!FeatureToggleManager.isSkillBarEnabled()) {
            stopAllChanneling(Minecraft.getInstance().player);
            keysDown.clear();
            channelingStartTime.clear(); // ★ 新增
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            stopAllChanneling(player);
            keysDown.clear();
            channelingStartTime.clear(); // ★ 新增
            return;
        }
        if (!AddonClientConfig.CLIENT.enableSkillBar.get() || !skillKeysEnabled) {
            stopAllChanneling(player);
            keysDown.clear();
            channelingStartTime.clear(); // ★ 新增
            return;
        }

        // ★★★ 新增：验证引导状态 ★★★
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
    // ▲▲▲ 修改 onClientTick 方法结束 ▲▲▲

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

    // ▼▼▼ 修改 resetState 方法 ▼▼▼
    public static void resetState() {
        channelingSlots.clear();
        channelingStartTime.clear(); // ★ 新增
        keysDown.clear();
        skillKeysEnabled = true;
    }
    // ▲▲▲ 修改 resetState 方法结束 ▲▲▲

    // ▼▼▼ 修改 onSlotKeyPressed 方法（只改非十影部分）▼▼▼
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

        // ★★★ 修改：引导技能 ★★★
        if (type == Ability.ActivationType.CHANNELED) {
            if (cooldown > 0 && !ignoreCooldown) return;
            if (ability.getStatus(player) != Ability.Status.SUCCESS && !ignoreCooldown) return;

            channelingSlots.put(slot, ability);
            channelingStartTime.put(slot, System.currentTimeMillis()); // ★ 新增：记录开始时间

            // ★★★ 使用自定义包，确保同步 ★★★
            AddonNetwork.sendToServer(new TriggerAbilityWithSyncC2SPacket(key));
            return;
        }

        // ★★★ 修改：即时技能也用自定义包 ★★★
        if (cooldown > 0 && !ignoreCooldown) return;
        if (ability.getStatus(player) != Ability.Status.SUCCESS && !ignoreCooldown) return;
        AddonNetwork.sendToServer(new TriggerAbilityWithSyncC2SPacket(key));
    }
    // ▲▲▲ 修改 onSlotKeyPressed 方法结束 ▲▲▲

    // ▼▼▼ 新增方法：验证引导状态 ▼▼▼
    /**
     * 验证引导状态是否与服务端一致
     * 防止客户端进入假引导状态（扣蓝无伤害）
     */
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

            // 等待一小段时间让服务端数据同步过来
            if (now - startTime < CHANNELING_VERIFY_DELAY) continue;

            Ability clientChanneled = channelingSlots.get(slot);
            if (clientChanneled == null) {
                it.remove();
                continue;
            }

            // ★★★ 如果服务端没有在引导这个技能，取消客户端状态 ★★★
            if (serverChanneled != clientChanneled) {
                // 服务端拒绝了引导（可能是因为 CD 或其他原因）
                channelingSlots.remove(slot);
                it.remove();
                // 静默取消，不显示消息（避免干扰正常使用）
            } else {
                // 验证通过，移除验证记录
                it.remove();
            }
        }
    }
    // ▲▲▲ 新增方法结束 ▲▲▲

    private static void handleShadowStorage(LocalPlayer player) {
        // ... 保持原样，不用改 ...
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
        // ... 保持原样 ...
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

    // ▼▼▼ 修改 handleTenShadowsAbility 方法（引导技能部分）▼▼▼
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

        // ★★★ 新增：引导技能也记录验证时间 ★★★
        if (type == Ability.ActivationType.CHANNELED) {
            channelingSlots.put(slot, ability);
            channelingStartTime.put(slot, System.currentTimeMillis()); // ★ 新增
        }

        AddonNetwork.sendToServer(new TriggerTenShadowsAbilityC2SPacket(key, required));
    }
    // ▲▲▲ 修改 handleTenShadowsAbility 方法结束 ▲▲▲

    private static void onSlotKeyReleased(LocalPlayer player, int slot) {
        // ... 保持原样 ...
        Ability ability = channelingSlots.remove(slot);
        if (ability == null) return;

        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return;

        stopChannelingClient(player);
        AddonNetwork.sendToServer(new StopChannelingC2SPacket(key));
    }

    private static void stopChannelingClient(LocalPlayer player) {
        // ... 保持原样 ...
        if (player == null) return;
        player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            Ability channeled = cap.getChanneled();
            if (channeled != null) cap.channel(channeled);
        });
    }

    // ▼▼▼ 修改 stopAllChanneling 方法 ▼▼▼
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
        channelingStartTime.clear(); // ★ 新增
    }
    // ▲▲▲ 修改 stopAllChanneling 方法结束 ▲▲▲
}
