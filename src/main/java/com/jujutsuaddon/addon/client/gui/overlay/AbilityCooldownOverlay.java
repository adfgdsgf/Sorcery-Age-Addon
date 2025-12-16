package com.jujutsuaddon.addon.client.gui.overlay;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.config.AddonClientConfig.AnchorPoint;
import com.jujutsuaddon.addon.client.util.FeatureToggleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.client.gui.overlay.AbilityOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "jujutsu_addon", value = Dist.CLIENT)
public class AbilityCooldownOverlay {

    private static final int ICON_SIZE = 18;
    private static final int PADDING = 3;
    private static final int TEXT_HEIGHT = 10;

    // 本地冷却追踪（用于同步延迟时的备用显示）
    private static final Map<Ability, LocalCooldown> localCooldowns = new HashMap<>();

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.EXPERIENCE_BAR.type()) return;
        if (!FeatureToggleManager.isCooldownHudEnabled()) return;
        if (!AddonClientConfig.CLIENT.enableCooldownHUD.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.options.hideGui) return;

        // 直接在渲染时获取冷却数据
        List<CooldownEntry> cooldowns = collectCooldownsDirect(player);

        if (cooldowns.isEmpty()) return;

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        float scale = AddonClientConfig.CLIENT.hudScale.get().floatValue();

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().scale(scale, scale, 1.0F);

        int[] pos = calculatePosition(screenWidth, screenHeight, cooldowns.size(), scale);
        renderCooldowns(event.getGuiGraphics(), mc, cooldowns, pos[0], pos[1]);

        event.getGuiGraphics().pose().popPose();
    }

    /**
     * 直接收集冷却数据（每帧调用）
     */
    private static List<CooldownEntry> collectCooldownsDirect(LocalPlayer player) {
        List<CooldownEntry> cooldowns = new ArrayList<>();

        // 获取 capability
        ISorcererData cap = player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return cooldowns;

        // 获取装备技能
        Ability meleeSelected = null;
        Ability j2tsuSelected = null;
        try {
            meleeSelected = AbilityOverlay.getSelected();
            j2tsuSelected = AbilityOverlay.getSelected2();
        } catch (Exception ignored) {}

        long currentTime = System.currentTimeMillis();

        // 遍历所有注册的技能
        for (Ability ability : JJKAbilities.getAbilities()) {
            if (ability == null) continue;

            // 从服务端同步的数据获取冷却
            int serverRemaining = cap.getRemainingCooldown(ability);
            int total = ability.getRealCooldown(player);

            // 检查本地追踪
            LocalCooldown local = localCooldowns.get(ability);

            int displayRemaining = serverRemaining;
            int displayTotal = total;

            if (serverRemaining > 0) {
                // 服务端有冷却，更新本地追踪
                localCooldowns.put(ability, new LocalCooldown(currentTime, serverRemaining, Math.max(total, serverRemaining)));
                displayRemaining = serverRemaining;
                displayTotal = Math.max(total, serverRemaining);
            } else if (local != null) {
                // 服务端没冷却，但本地有追踪（可能是同步延迟）
                long elapsed = currentTime - local.startTime;
                int elapsedTicks = (int) (elapsed / 50); // 50ms = 1 tick
                int localRemaining = local.initialRemaining - elapsedTicks;

                if (localRemaining > 0) {
                    // 本地追踪仍有效，使用本地数据
                    displayRemaining = localRemaining;
                    displayTotal = local.totalCooldown;
                } else {
                    // 本地追踪已过期，移除
                    localCooldowns.remove(ability);
                    continue;
                }
            } else {
                // 没有冷却
                continue;
            }

            if (displayRemaining > 0) {
                boolean isEquipped = (ability == meleeSelected || ability == j2tsuSelected);
                cooldowns.add(new CooldownEntry(ability, displayRemaining, displayTotal, isEquipped));
            }
        }

        // 清理过期的本地追踪
        cleanupLocalCooldowns(currentTime);

        // 排序
        sortCooldowns(cooldowns);

        // 限制数量
        return limitCooldowns(cooldowns);
    }

    private static void cleanupLocalCooldowns(long currentTime) {
        localCooldowns.entrySet().removeIf(entry -> {
            long elapsed = currentTime - entry.getValue().startTime;
            int elapsedTicks = (int) (elapsed / 50);
            return entry.getValue().initialRemaining - elapsedTicks <= 0;
        });
    }

    private static void sortCooldowns(List<CooldownEntry> cooldowns) {
        boolean prioritize = AddonClientConfig.CLIENT.prioritizeEquippedSkills.get();
        cooldowns.sort((a, b) -> {
            if (prioritize) {
                if (a.isEquipped && !b.isEquipped) return -1;
                if (!a.isEquipped && b.isEquipped) return 1;
            }
            return Integer.compare(a.remaining, b.remaining);
        });
    }

    private static List<CooldownEntry> limitCooldowns(List<CooldownEntry> cooldowns) {
        int max = AddonClientConfig.CLIENT.maxDisplayCount.get();
        if (max > 0 && cooldowns.size() > max) {
            List<CooldownEntry> limited = new ArrayList<>();
            int normalCount = 0;

            for (CooldownEntry entry : cooldowns) {
                if (entry.isEquipped) {
                    limited.add(entry);
                } else if (normalCount < max) {
                    limited.add(entry);
                    normalCount++;
                }
            }
            return limited;
        }
        return cooldowns;
    }

    private static int[] calculatePosition(int screenWidth, int screenHeight, int count, float scale) {
        AnchorPoint anchor = AddonClientConfig.CLIENT.hudAnchor.get();
        int offsetXPermille = AddonClientConfig.CLIENT.hudOffsetX.get();
        int offsetYPermille = AddonClientConfig.CLIENT.hudOffsetY.get();
        boolean horizontal = AddonClientConfig.CLIENT.horizontalLayout.get();
        // 偏移百分比转像素
        int offsetX = (int) (screenWidth * offsetXPermille / 1000.0);
        int offsetY = (int) (screenHeight * offsetYPermille / 1000.0);
        int contentWidth, contentHeight;
        if (horizontal) {
            contentWidth = count * (ICON_SIZE + PADDING) - PADDING;
            contentHeight = ICON_SIZE + (AddonClientConfig.CLIENT.showCooldownText.get() ? TEXT_HEIGHT : 0);
        } else {
            contentWidth = ICON_SIZE;
            contentHeight = count * (ICON_SIZE + PADDING + (AddonClientConfig.CLIENT.showCooldownText.get() ? TEXT_HEIGHT : 0));
        }
        int scaledWidth = (int) (screenWidth / scale);
        int scaledHeight = (int) (screenHeight / scale);
        int x, y;
        switch (anchor) {
            case TOP_LEFT -> { x = 0; y = 0; }
            case TOP_CENTER -> { x = (scaledWidth - contentWidth) / 2; y = 0; }
            case TOP_RIGHT -> { x = scaledWidth - contentWidth; y = 0; }
            case CENTER_LEFT -> { x = 0; y = (scaledHeight - contentHeight) / 2; }
            case CENTER -> { x = (scaledWidth - contentWidth) / 2; y = (scaledHeight - contentHeight) / 2; }
            case CENTER_RIGHT -> { x = scaledWidth - contentWidth; y = (scaledHeight - contentHeight) / 2; }
            case BOTTOM_LEFT -> { x = 0; y = scaledHeight - contentHeight; }
            case BOTTOM_CENTER -> { x = (scaledWidth - contentWidth) / 2; y = scaledHeight - contentHeight; }
            case BOTTOM_RIGHT -> { x = scaledWidth - contentWidth; y = scaledHeight - contentHeight; }
            default -> { x = 0; y = 0; }
        }
        // 偏移也要考虑缩放
        int scaledOffsetX = (int) (offsetX / scale);
        int scaledOffsetY = (int) (offsetY / scale);
        return new int[]{x + scaledOffsetX, y + scaledOffsetY};
    }

    private static void renderCooldowns(GuiGraphics graphics, Minecraft mc,
                                        List<CooldownEntry> cooldowns,
                                        int startX, int startY) {
        boolean horizontal = AddonClientConfig.CLIENT.horizontalLayout.get();
        boolean showBg = AddonClientConfig.CLIENT.showIconBackground.get();
        boolean showText = AddonClientConfig.CLIENT.showCooldownText.get();
        boolean showProgress = AddonClientConfig.CLIENT.showProgressOverlay.get();

        int x = startX;
        int y = startY;

        for (CooldownEntry entry : cooldowns) {
            if (showBg) {
                int bgColor = entry.isEquipped ? 0xAA442222 : 0x80000000;
                graphics.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, bgColor);
            }

            if (entry.isEquipped) {
                graphics.renderOutline(x - 2, y - 2, ICON_SIZE + 4, ICON_SIZE + 4, 0xFFFF6600);
            }

            // 图标
            boolean iconRendered = false;
            ResourceLocation abilityKey = JJKAbilities.getKey(entry.ability);

            if (abilityKey != null) {
                ResourceLocation icon = new ResourceLocation(abilityKey.getNamespace(),
                        "textures/ability/" + abilityKey.getPath() + ".png");
                try {
                    RenderSystem.enableBlend();
                    graphics.blit(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                    RenderSystem.disableBlend();
                    iconRendered = true;
                } catch (Exception ignored) {}
            }

            if (!iconRendered) {
                graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF333333);
                try {
                    String name = entry.ability.getName().getString();
                    if (!name.isEmpty()) {
                        String initial = com.jujutsuaddon.addon.client.util.RenderHelper.getAbbreviation(name, 2);
                        graphics.drawCenteredString(mc.font, initial, x + ICON_SIZE / 2, y + 5, 0xFFFFFF);
                    }
                } catch (Exception ignored) {}
            }

            if (showProgress && entry.total > 0) {
                float progress = (float) entry.remaining / entry.total;
                int maskHeight = (int) (ICON_SIZE * progress);
                if (maskHeight > 0) {
                    graphics.fill(x, y, x + ICON_SIZE, y + maskHeight, 0xAA000000);
                }
            }

            if (showText) {
                String timeText = formatTime(entry.remaining);
                int textX = x + ICON_SIZE / 2 - mc.font.width(timeText) / 2;
                int textY = y + ICON_SIZE + 1;
                graphics.drawString(mc.font, timeText, textX, textY, 0xFFFFFF, true);
            }

            if (horizontal) {
                x += ICON_SIZE + PADDING;
            } else {
                y += ICON_SIZE + PADDING + (showText ? TEXT_HEIGHT : 0);
            }
        }
    }

    private static String formatTime(int ticks) {
        float seconds = ticks / 20.0f;
        if (seconds >= 60) {
            int mins = (int) (seconds / 60);
            int secs = (int) (seconds % 60);
            return String.format("%d:%02d", mins, secs);
        } else if (seconds >= 10) {
            return String.format("%.0f", seconds);
        } else {
            return String.format("%.1f", seconds);
        }
    }

    // 本地冷却追踪记录
    private record LocalCooldown(long startTime, int initialRemaining, int totalCooldown) {}

    private record CooldownEntry(Ability ability, int remaining, int total, boolean isEquipped) {}
}
