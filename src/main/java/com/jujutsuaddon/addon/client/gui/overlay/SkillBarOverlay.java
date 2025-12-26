package com.jujutsuaddon.addon.client.gui.overlay;

import com.jujutsuaddon.addon.client.keybind.AddonKeyBindings;
import com.jujutsuaddon.addon.client.ClientEvents;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.gui.screen.SkillBarConfigScreen;
import com.jujutsuaddon.addon.client.gui.screen.SkillBarEditScreen;
import com.jujutsuaddon.addon.client.skillbar.SkillBarData;
import com.jujutsuaddon.addon.client.skillbar.SkillBarManager;
import com.jujutsuaddon.addon.client.skillbar.render.AbilityStatus;
import com.jujutsuaddon.addon.client.util.AbilityDamagePredictor;
import com.jujutsuaddon.addon.client.util.FeatureToggleManager;
import com.jujutsuaddon.addon.client.render.RenderHelper;
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
        if (!FeatureToggleManager.isSkillBarEnabled()) return;

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
        int offsetXPermille = AddonClientConfig.CLIENT.skillBarOffsetX.get();
        int offsetYPermille = AddonClientConfig.CLIENT.skillBarOffsetY.get();
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
        boolean usable = playerOwns && !onCooldown && status.canUse && !status.conditionsNotMet;
        // ★★★ 融合式神特殊处理 ★★★
        boolean isFusion = status.isFusion;
        // 背景颜色
        int bgColor;
        if (isFusion && !status.isDead) {
            // 融合式神特殊背景
            bgColor = isActive ? 0x803A2A5A : 0x802A2A4A;
        } else {
            bgColor = RenderHelper.getSlotBgColor(
                    status.isDead, status.techniqueNotActive,
                    isActive, status.hasSummon, status.summonConflict,
                    playerOwns, status.canUse, onCooldown,
                    status.conditionsNotMet);
        }
        graphics.fill(x, y, x + HUD_SLOT_SIZE, y + HUD_SLOT_SIZE, bgColor);
        // ★★★ 边框颜色 - 融合式神特殊边框 ★★★
        int borderColor;
        if (isActive || status.hasSummon) {
            borderColor = isFusion ? RenderHelper.getFusionPulsingBorderColor()
                    : RenderHelper.getPulsingActiveBorderColor();
        } else if (status.isDead) {
            borderColor = 0xFFAA3333;
        } else if (status.conditionsNotMet) {
            borderColor = 0xFFFF8800;
        } else if (status.techniqueNotActive) {
            borderColor = RenderHelper.getPulsingConflictBorderColor();
        } else if (status.summonConflict) {
            borderColor = 0xFFAA3333;
        } else if (!playerOwns) {
            borderColor = 0xFFCC0000;
        } else if (isFusion) {
            // ★★★ 融合式神：金紫渐变边框 ★★★
            borderColor = usable ? RenderHelper.getFusionPulsingBorderColor() : 0xFFAA66DD;
        } else if (usable) {
            borderColor = 0xFF00CC00;
        } else {
            borderColor = 0xFF888888;
        }
        graphics.renderOutline(x, y, HUD_SLOT_SIZE, HUD_SLOT_SIZE, borderColor);
        // ★★★ 融合式神：额外的内边框光效 ★★★
        if (isFusion && !status.isDead && playerOwns) {
            int innerGlow = 0x40AA55FF;  // 半透明紫色内光
            graphics.renderOutline(x + 1, y + 1, HUD_SLOT_SIZE - 2, HUD_SLOT_SIZE - 2, innerGlow);
        }
        // 图标
        int iconX = x + HUD_ICON_OFFSET;
        int iconY = y + HUD_ICON_OFFSET;
        if (status.techniqueNotActive) {
            RenderHelper.renderAbilityIconWithTint(graphics, ability, iconX, iconY, HUD_ICON_SIZE, true);
        } else if (isFusion && !status.isDead) {
            // ★★★ 融合式神特殊图标渲染 ★★★
            boolean grayed = !playerOwns || status.summonConflict || !status.canUse
                    || onCooldown || status.conditionsNotMet;
            RenderHelper.renderFusionAbilityIcon(graphics, ability, iconX, iconY, HUD_ICON_SIZE, grayed);
        } else {
            boolean grayed = !playerOwns || status.summonConflict || !status.canUse
                    || onCooldown || status.conditionsNotMet;
            RenderHelper.renderAbilityIcon(graphics, ability, iconX, iconY, HUD_ICON_SIZE, grayed);
        }
        // ★★★ 融合标记（右上角 ◆）★★★
        if (isFusion && playerOwns && !onCooldown && !status.isDead) {
            RenderHelper.renderFusionMark(graphics, mc.font, x, y, HUD_SLOT_SIZE);
        }
        // 伤害显示（右上角）- 融合式神时移到左上角避免重叠
        if (playerOwns && AddonClientConfig.CLIENT.showSkillBarDamage.get() && !onCooldown) {
            if (isFusion) {
                renderSlotDamageLeftTop(graphics, mc, status, x, y);
            } else {
                renderSlotDamage(graphics, mc, status, x, y);
            }
        }
        // ★★★ 未调伏式神标记（黄色 ?）★★★
        if (status.isTenShadowsSummon && !status.isTamed && !onCooldown && !status.isDead) {
            graphics.drawString(mc.font, "?", x + 1, y + 1, 0xFFFF00, true);
        }
        // 条件未满足警告（融合式神等）
        else if (status.conditionsNotMet && !onCooldown && !isFusion) {
            graphics.drawString(mc.font, "⚠", x + 1, y + 1, 0xFFAA00, true);
        }
        // ★★★ 死亡状态覆盖显示 ★★★
        if (status.isDead && !onCooldown) {
            // 半透明红色遮罩
            graphics.fill(iconX, iconY, iconX + HUD_ICON_SIZE, iconY + HUD_ICON_SIZE, 0xA0330000);

            // 获取式神名字缩写
            String name = ability.getName().getString();
            String abbrev;
            if (name.length() > 0) {
                char firstChar = name.charAt(0);
                boolean isCJK = Character.UnicodeBlock.of(firstChar) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
                abbrev = name.substring(0, Math.min(2, name.length()));
            } else {
                abbrev = "?";
            }

            // 显示式神名字（上半部分）
            int textWidth = mc.font.width(abbrev);
            int textX = x + (HUD_SLOT_SIZE - textWidth) / 2;
            graphics.drawString(mc.font, abbrev, textX, y + 3, 0xFF6666, true);

            // 显示骷髅图标（下半部分）
            graphics.drawCenteredString(mc.font, "☠", x + HUD_SLOT_SIZE / 2, y + HUD_SLOT_SIZE - 10, 0xFF4444);
        }
        // 冷却显示
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
        // 快捷键显示（左下角）
        if (!onCooldown && AddonClientConfig.CLIENT.showSkillBarKeybinds.get()
                && slot < AddonKeyBindings.SKILL_SLOT_KEYS.size()) {
            String keyName = AddonKeyBindings.SKILL_SLOT_KEYS.get(slot).getTranslatedKeyMessage().getString().toUpperCase();
            if (keyName.length() > 2) keyName = keyName.substring(0, 1);
            int keyColor = playerOwns && usable ? 0xFFFFFF : 0x666666;
            graphics.drawString(mc.font, keyName, x + 2, y + HUD_SLOT_SIZE - 9, keyColor, true);
        }
    }
    /**
     * 渲染槽位伤害显示（左上角版本，用于融合式神）
     */
    private static void renderSlotDamageLeftTop(GuiGraphics graphics, Minecraft mc,
                                                AbilityStatus status, int x, int y) {
        if (status.damageType == AbilityDamagePredictor.DamageType.UTILITY) return;

        String dmgText;
        int dmgColor;

        switch (status.damageType) {
            case DIRECT_DAMAGE, POWER_BASED -> {
                if (status.canPredictDamage) {
                    dmgText = formatCompact(status.addonDamage);
                    dmgColor = status.isDamageIncreased() ? 0x55FF55 :
                            status.isDamageDecreased() ? 0xFF5555 : 0xFFAA55;
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            case SUMMON -> {
                if (status.canPredictDamage && status.addonDamage > 0) {
                    dmgText = formatCompact(status.addonDamage);
                    dmgColor = 0x55AAFF;
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            default -> {
                dmgText = "?";
                dmgColor = 0x666666;
            }
        }
        int dmgX = x + 1;
        int dmgY = y + 1;
        int textWidth = mc.font.width(dmgText);

        graphics.fill(dmgX - 1, dmgY - 1, dmgX + textWidth + 1, dmgY + 8, 0x99000000);
        graphics.drawString(mc.font, dmgText, dmgX, dmgY, dmgColor, false);
    }

    /**
     * 渲染槽位伤害显示（右上角）
     */
    private static void renderSlotDamage(GuiGraphics graphics, Minecraft mc, AbilityStatus status, int x, int y) {
        if (status.damageType == AbilityDamagePredictor.DamageType.UTILITY) {
            return;
        }
        String dmgText;
        int dmgColor;
        String arrow = null;
        switch (status.damageType) {
            case DIRECT_DAMAGE, POWER_BASED -> {
                if (status.canPredictDamage) {
                    dmgText = formatCompact(status.addonDamage);
                    if (status.isDamageIncreased()) {
                        dmgColor = 0x55FF55;
                        arrow = "↑";
                    } else if (status.isDamageDecreased()) {
                        dmgColor = 0xFF5555;
                        arrow = "↓";
                    } else {
                        dmgColor = 0xFFAA55;
                    }
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            case SUMMON -> {
                if (status.canPredictDamage && status.addonDamage > 0) {
                    dmgText = formatCompact(status.addonDamage);
                    dmgColor = 0x55AAFF;
                    if (status.hasAddonModification) {
                        arrow = status.isDamageIncreased() ? "↑" : "↓";
                    }
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            case DOMAIN -> {
                if (status.canPredictDamage && status.addonDamage > 0) {
                    dmgText = formatCompact(status.addonDamage);
                    dmgColor = 0xAA55FF;
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            default -> {
                dmgText = "?";
                dmgColor = 0x666666;
            }
        }
        // ★★★ 组合显示文本 ★★★
        String displayText = (arrow != null ? arrow : "") + dmgText;
        int textWidth = mc.font.width(displayText);

        // ★★★ 确保在槽位内部，右上角 ★★★
        int maxX = x + HUD_SLOT_SIZE - 2;
        int dmgX = maxX - textWidth;
        int dmgY = y + 1;

        // 如果太长，截断到槽位内
        if (dmgX < x + 1) {
            dmgX = x + 1;
        }
        // 半透明背景（限制在槽位内）
        int bgLeft = Math.max(x + 1, dmgX - 1);
        int bgRight = Math.min(x + HUD_SLOT_SIZE - 1, dmgX + textWidth + 1);
        graphics.fill(bgLeft, dmgY - 1, bgRight, dmgY + 8, 0x99000000);
        // 绘制文本
        if (arrow != null) {
            int arrowColor = arrow.equals("↑") ? 0x55FF55 : 0xFF5555;
            graphics.drawString(mc.font, arrow, dmgX, dmgY, arrowColor, false);
            graphics.drawString(mc.font, dmgText, dmgX + mc.font.width(arrow), dmgY, dmgColor, false);
        } else {
            graphics.drawString(mc.font, dmgText, dmgX, dmgY, dmgColor, false);
        }
    }
    /**
     * 紧凑格式化（更短）
     */
    private static String formatCompact(float damage) {
        if (damage < 0) return "?";
        if (damage >= 10000) return String.format("%.0fW", damage / 10000);
        if (damage >= 1000) return String.format("%.0fK", damage / 1000);  // 不要小数点
        if (damage >= 100) return String.format("%.0f", damage);
        return String.format("%.0f", damage);  // 都不要小数点，更短
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

    private static void renderCurseManagementSlot(GuiGraphics graphics, Minecraft mc,
                                                  int slot, int x, int y, boolean keysEnabled) {
        boolean available = SkillBarManager.hasCurseManipulation();
        boolean owns = SkillBarManager.ownsCurseManipulation();

        int bgColor;
        if (available) {
            bgColor = 0xDD3A2255;
        } else if (owns) {
            bgColor = 0x80332244;
        } else {
            bgColor = 0x80222222;
        }
        graphics.fill(x, y, x + HUD_SLOT_SIZE, y + HUD_SLOT_SIZE, bgColor);

        int borderColor;
        if (available) {
            borderColor = 0xFFAA55FF;
        } else if (owns) {
            borderColor = RenderHelper.getPulsingConflictBorderColor();
        } else {
            borderColor = 0xFF555555;
        }
        graphics.renderOutline(x, y, HUD_SLOT_SIZE, HUD_SLOT_SIZE, borderColor);

        int iconColor;
        if (available) {
            iconColor = 0xAA55FF;
        } else if (owns) {
            iconColor = 0x664488;
        } else {
            iconColor = 0x555555;
        }
        graphics.drawCenteredString(mc.font, "☠", x + HUD_SLOT_SIZE / 2, y + 6, iconColor);

        if (!available && owns) {
            graphics.drawCenteredString(mc.font, "⚡", x + HUD_SLOT_SIZE - 6, y + 2, 0xFF6666);
        } else if (!available) {
            graphics.drawCenteredString(mc.font, "✗", x + HUD_SLOT_SIZE - 6, y + 2, 0xFF4444);
        }

        if (AddonClientConfig.CLIENT.showSkillBarKeybinds.get() && slot < AddonKeyBindings.SKILL_SLOT_KEYS.size()) {
            String keyName = AddonKeyBindings.SKILL_SLOT_KEYS.get(slot).getTranslatedKeyMessage().getString().toUpperCase();
            if (keyName.length() > 2) keyName = keyName.substring(0, 1);
            int keyColor = available ? 0xFFFFFF : 0x666666;
            graphics.drawString(mc.font, keyName, x + 2, y + HUD_SLOT_SIZE - 9, keyColor, true);
        }
    }
}
