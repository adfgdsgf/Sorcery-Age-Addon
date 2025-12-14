package com.jujutsuaddon.addon.client.gui.overlay;

import com.jujutsuaddon.addon.client.AddonKeyBindings;
import com.jujutsuaddon.addon.client.ClientEvents;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.gui.screen.SkillBarConfigScreen;
import com.jujutsuaddon.addon.client.gui.screen.SkillBarEditScreen;
import com.jujutsuaddon.addon.client.skillbar.SkillBarData;
import com.jujutsuaddon.addon.client.skillbar.SkillBarManager;
import com.jujutsuaddon.addon.client.skillbar.render.AbilityStatus;
import com.jujutsuaddon.addon.client.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import static com.jujutsuaddon.addon.client.skillbar.SkillBarConstants.*;

@Mod.EventBusSubscriber(modid = "jujutsu_addon", value = Dist.CLIENT)
public class SkillBarOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        if (!AddonClientConfig.CLIENT.enableSkillBar.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        if (mc.screen instanceof SkillBarConfigScreen || mc.screen instanceof SkillBarEditScreen) return;

        boolean keysEnabled = ClientEvents.areSkillKeysEnabled();
        if (!keysEnabled && AddonClientConfig.CLIENT.hideSkillBarWhenDisabled.get()) return;

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        float scale = AddonClientConfig.CLIENT.skillBarScale.get().floatValue();

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().scale(scale, scale, 1.0f);
        renderSkillBar(event.getGuiGraphics(), mc, (int)(screenWidth / scale), (int)(screenHeight / scale), keysEnabled);
        event.getGuiGraphics().pose().popPose();
    }

    private static void renderSkillBar(GuiGraphics graphics, Minecraft mc, int screenWidth, int screenHeight, boolean keysEnabled) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean horizontal = AddonClientConfig.CLIENT.skillBarHorizontalLayout.get();
        int slotCount = SkillBarData.SLOT_COUNT;
        int totalWidth = horizontal ? slotCount * HUD_SLOT_SIZE + (slotCount - 1) * HUD_SLOT_PADDING : HUD_SLOT_SIZE;
        int totalHeight = horizontal ? HUD_SLOT_SIZE + HUD_HEADER_HEIGHT : slotCount * HUD_SLOT_SIZE + (slotCount - 1) * HUD_SLOT_PADDING + HUD_HEADER_HEIGHT;

        int[] pos = calculatePosition(screenWidth, screenHeight, totalWidth, totalHeight);
        int startX = pos[0], startY = pos[1];

        graphics.drawString(mc.font, keysEnabled ? "§a●" : "§c○", startX, startY, 0xFFFFFF, false);

        int activeCount = SkillBarManager.getData().getActivePresetCount();
        if (activeCount > 1 || !SkillBarManager.getData().isCurrentPresetEmpty()) {
            String presetText = "[ " + (SkillBarManager.getCurrentPresetIndex() + 1) + " ]";
            if (horizontal) {
                graphics.drawCenteredString(mc.font, presetText, startX + totalWidth / 2, startY, 0xFFFF00);
            } else {
                graphics.drawString(mc.font, presetText, startX + (totalWidth - mc.font.width(presetText)) / 2, startY, 0xFFFF00, true);
            }
        }

        int slotStartY = startY + HUD_HEADER_HEIGHT;
        for (int i = 0; i < slotCount; i++) {
            int x = horizontal ? startX + i * (HUD_SLOT_SIZE + HUD_SLOT_PADDING) : startX;
            int y = horizontal ? slotStartY : slotStartY + i * (HUD_SLOT_SIZE + HUD_SLOT_PADDING);
            renderSlot(graphics, mc, player, i, x, y, keysEnabled);
        }
    }

    private static int[] calculatePosition(int screenWidth, int screenHeight, int width, int height) {
        AddonClientConfig.AnchorPoint anchor = AddonClientConfig.CLIENT.skillBarAnchor.get();

        // ★★★ 获取偏移百分比（0-1000 表示 0%-100%）★★★
        int offsetXPermille = AddonClientConfig.CLIENT.skillBarOffsetX.get();
        int offsetYPermille = AddonClientConfig.CLIENT.skillBarOffsetY.get();

        // 将千分比转换为实际像素
        int offsetX = (int) (screenWidth * offsetXPermille / 1000.0);
        int offsetY = (int) (screenHeight * offsetYPermille / 1000.0);
        int x, y;
        switch (anchor) {
            case TOP_LEFT -> { x = 10; y = 10; }
            case TOP_CENTER -> { x = (screenWidth - width) / 2; y = 10; }
            case TOP_RIGHT -> { x = screenWidth - width - 10; y = 10; }
            case CENTER_LEFT -> { x = 10; y = (screenHeight - height) / 2; }
            case CENTER -> { x = (screenWidth - width) / 2; y = (screenHeight - height) / 2; }
            case CENTER_RIGHT -> { x = screenWidth - width - 10; y = (screenHeight - height) / 2; }
            case BOTTOM_LEFT -> { x = 10; y = screenHeight - height - 50; }
            case BOTTOM_CENTER -> { x = (screenWidth - width) / 2; y = screenHeight - height - 50; }
            case BOTTOM_RIGHT -> { x = screenWidth - width - 10; y = screenHeight - height - 50; }
            default -> { x = 0; y = 0; }
        }
        return new int[]{x + offsetX, y + offsetY};
    }

    private static void renderSlot(GuiGraphics graphics, Minecraft mc, LocalPlayer player,
                                   int slot, int x, int y, boolean keysEnabled) {
        // 咒灵管理槽位
        if (SkillBarManager.isSlotCurseManagement(slot)) {
            renderCurseManagementSlot(graphics, mc, slot, x, y, keysEnabled);
            return;
        }
        // 术式切换槽位
        if (SkillBarManager.isSlotTechnique(slot)) {
            renderTechniqueSlot(graphics, mc, slot, x, y, keysEnabled);
            return;
        }
        // 普通技能槽位
        Ability ability = SkillBarManager.getSlot(slot);
        if (ability == null) {
            renderEmptySlot(graphics, x, y);
            return;
        }

        boolean playerOwns = SkillBarManager.playerHasAbility(slot);
        AbilityStatus status = playerOwns ? AbilityStatus.build(player, ability) : new AbilityStatus();
        int cooldown = SkillBarManager.getCooldown(slot);
        int totalCooldown = SkillBarManager.getTotalCooldown(slot);
        boolean onCooldown = cooldown > 0;
        boolean isActive = SkillBarManager.isSlotActive(slot);
        boolean showCooldown = AddonClientConfig.CLIENT.showSkillBarCooldown.get();
        boolean usable = playerOwns && !onCooldown && status.canUse;

        int bgColor = RenderHelper.getSlotBgColor(
                status.isDead, status.techniqueNotActive,
                isActive, status.hasSummon, status.summonConflict,
                playerOwns, status.canUse, onCooldown);
        graphics.fill(x, y, x + HUD_SLOT_SIZE, y + HUD_SLOT_SIZE, bgColor);

        int borderColor = RenderHelper.getSlotBorderColor(
                isActive, status.hasSummon, status.techniqueNotActive,
                status.summonConflict, playerOwns, usable);
        graphics.renderOutline(x, y, HUD_SLOT_SIZE, HUD_SLOT_SIZE, borderColor);

        int iconX = x + HUD_ICON_OFFSET;
        int iconY = y + HUD_ICON_OFFSET;
        if (status.techniqueNotActive) {
            RenderHelper.renderAbilityIconWithTint(graphics, ability, iconX, iconY, HUD_ICON_SIZE, true);
        } else {
            boolean grayed = !playerOwns || status.summonConflict || !status.canUse || onCooldown;
            RenderHelper.renderAbilityIcon(graphics, ability, iconX, iconY, HUD_ICON_SIZE, grayed);
        }

        if (onCooldown && showCooldown) {
            float progress = (float) cooldown / Math.max(1, totalCooldown);
            int maskHeight = (int) (HUD_ICON_SIZE * progress);
            if (maskHeight > 0) {
                graphics.fill(iconX, iconY, iconX + HUD_ICON_SIZE, iconY + maskHeight, 0x80000000);
            }
            String timeText = RenderHelper.formatCooldown(cooldown);
            int textWidth = mc.font.width(timeText);
            int textX = x + (HUD_SLOT_SIZE - textWidth) / 2;
            int textY = y + (HUD_SLOT_SIZE - 8) / 2;
            graphics.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 9, 0xCC000000);
            graphics.drawString(mc.font, timeText, textX, textY, 0xFFFF00, true);
        }

        if (!onCooldown && AddonClientConfig.CLIENT.showSkillBarKeybinds.get()
                && slot < AddonKeyBindings.SKILL_SLOT_KEYS.size()) {
            String keyName = AddonKeyBindings.SKILL_SLOT_KEYS.get(slot).getTranslatedKeyMessage().getString().toUpperCase();
            if (keyName.length() > 2) keyName = keyName.substring(0, 1);
            int keyColor = playerOwns && usable ? 0xFFFFFF : 0x666666;
            graphics.drawString(mc.font, keyName, x + 2, y + HUD_SLOT_SIZE - 9, keyColor, true);
        }
    }

    private static void renderEmptySlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + HUD_SLOT_SIZE, y + HUD_SLOT_SIZE, 0x60222222);
        graphics.renderOutline(x, y, HUD_SLOT_SIZE, HUD_SLOT_SIZE, 0xFF444444);
    }

    private static void renderTechniqueSlot(GuiGraphics graphics, Minecraft mc, int slot, int x, int y, boolean keysEnabled) {
        CursedTechnique technique = SkillBarManager.getSlotTechnique(slot);
        boolean hasTechnique = technique != null;
        boolean playerOwns = hasTechnique && SkillBarManager.playerHasTechnique(slot);
        boolean isActive = hasTechnique && SkillBarManager.isTechniqueActive(slot);

        int bgColor;
        if (!hasTechnique) {
            bgColor = 0x60222222;
        } else if (isActive) {
            bgColor = 0xCC445599;
        } else if (!playerOwns) {
            bgColor = 0x80550000;
        } else {
            bgColor = 0xAA663399;
        }
        graphics.fill(x, y, x + HUD_SLOT_SIZE, y + HUD_SLOT_SIZE, bgColor);

        int borderColor;
        if (!hasTechnique) {
            borderColor = 0xFF444444;
        } else if (isActive) {
            borderColor = RenderHelper.getPulsingActiveBorderColor();
        } else if (!playerOwns) {
            borderColor = 0xFFCC0000;
        } else {
            borderColor = 0xFFAA66CC;
        }
        graphics.renderOutline(x, y, HUD_SLOT_SIZE, HUD_SLOT_SIZE, borderColor);

        if (hasTechnique) {
            RenderHelper.renderTechniqueIcon(graphics, technique, x + HUD_ICON_OFFSET, y + HUD_ICON_OFFSET, HUD_ICON_SIZE, isActive);
        }

        if (AddonClientConfig.CLIENT.showSkillBarKeybinds.get() && slot < AddonKeyBindings.SKILL_SLOT_KEYS.size()) {
            String keyName = AddonKeyBindings.SKILL_SLOT_KEYS.get(slot).getTranslatedKeyMessage().getString().toUpperCase();
            if (keyName.length() > 2) keyName = keyName.substring(0, 1);
            int keyColor = playerOwns ? 0xFFFFFF : 0x666666;
            graphics.drawString(mc.font, keyName, x + 2, y + HUD_SLOT_SIZE - 9, keyColor, true);
        }
    }

    // ★★★ 咒灵管理槽位渲染（只保留一个！）★★★
    private static void renderCurseManagementSlot(GuiGraphics graphics, Minecraft mc,
                                                  int slot, int x, int y, boolean keysEnabled) {
        boolean available = SkillBarManager.hasCurseManipulation();
        boolean owns = SkillBarManager.ownsCurseManipulation();
        // 背景颜色
        int bgColor;
        if (available) {
            bgColor = 0xDD3A2255;  // 可用 - 紫色
        } else if (owns) {
            bgColor = 0x80332244;  // 拥有但未激活 - 暗紫色
        } else {
            bgColor = 0x80222222;  // 完全不可用 - 灰色
        }
        graphics.fill(x, y, x + HUD_SLOT_SIZE, y + HUD_SLOT_SIZE, bgColor);
        // 边框颜色
        int borderColor;
        if (available) {
            borderColor = 0xFFAA55FF;  // 可用 - 亮紫色
        } else if (owns) {
            borderColor = RenderHelper.getPulsingConflictBorderColor();  // 拥有但未激活 - 脉动警告
        } else {
            borderColor = 0xFF555555;  // 完全不可用 - 灰色
        }
        graphics.renderOutline(x, y, HUD_SLOT_SIZE, HUD_SLOT_SIZE, borderColor);
        // 骷髅图标
        int iconColor;
        if (available) {
            iconColor = 0xAA55FF;
        } else if (owns) {
            iconColor = 0x664488;  // 暗紫色，表示未激活
        } else {
            iconColor = 0x555555;
        }
        graphics.drawCenteredString(mc.font, "☠", x + HUD_SLOT_SIZE / 2, y + 6, iconColor);
        // 状态指示
        if (!available && owns) {
            // 拥有但未激活 - 显示闪电符号（表示需要激活）
            graphics.drawCenteredString(mc.font, "⚡", x + HUD_SLOT_SIZE - 6, y + 2, 0xFF6666);
        } else if (!available) {
            // 完全不可用 - 显示叉号
            graphics.drawCenteredString(mc.font, "✗", x + HUD_SLOT_SIZE - 6, y + 2, 0xFF4444);
        }
        // 快捷键
        if (AddonClientConfig.CLIENT.showSkillBarKeybinds.get() && slot < AddonKeyBindings.SKILL_SLOT_KEYS.size()) {
            String keyName = AddonKeyBindings.SKILL_SLOT_KEYS.get(slot).getTranslatedKeyMessage().getString().toUpperCase();
            if (keyName.length() > 2) keyName = keyName.substring(0, 1);
            int keyColor = available ? 0xFFFFFF : 0x666666;
            graphics.drawString(mc.font, keyName, x + 2, y + HUD_SLOT_SIZE - 9, keyColor, true);
        }
    }
}
