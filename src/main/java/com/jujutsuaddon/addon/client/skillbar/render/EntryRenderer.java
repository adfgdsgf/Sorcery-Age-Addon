package com.jujutsuaddon.addon.client.skillbar.render;

import com.jujutsuaddon.addon.client.util.AbilityDamagePredictor;
import com.jujutsuaddon.addon.client.util.RenderHelper;
import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import javax.annotation.Nullable;

import static com.jujutsuaddon.addon.client.skillbar.SkillBarConstants.*;
import static com.jujutsuaddon.addon.client.util.RenderHelper.HeaderColors.*;

public class EntryRenderer {

    public static final int ITEM_HEIGHT = ENTRY_HEIGHT;

    private final Font font;
    private final int listX;
    private final int listWidth;

    @Nullable public Ability hoveredToggleAbility = null;
    @Nullable public CursedTechnique hoveredDeleteTechnique = null;
    @Nullable public CursedTechnique hoveredSubHeader = null;
    @Nullable public CursedTechnique hoveredActivateTechnique = null;
    @Nullable public TechniqueHelper.TechniqueSource hoveredMainHeader = null;
    @Nullable public CursedTechnique hoveredThirdHeader = null;
    public int toggleBtnX, toggleBtnY;

    public EntryRenderer(Font font, int listX, int listWidth) {
        this.font = font;
        this.listX = listX;
        this.listWidth = listWidth;
    }

    public void resetHoverState() {
        hoveredToggleAbility = null;
        hoveredDeleteTechnique = null;
        hoveredSubHeader = null;
        hoveredActivateTechnique = null;
        hoveredMainHeader = null;
        hoveredThirdHeader = null;
    }

    public void renderEntry(GuiGraphics graphics, AbilityEntry entry, int y,
                            int mouseX, int mouseY, @Nullable LocalPlayer player) {
        switch (entry.type) {
            case NATIVE_TECHNIQUE_HEADER -> renderNativeHeader(graphics, entry, y, mouseX, mouseY);
            case NATIVE_ABILITY, NORMAL_ABILITY -> renderAbilityRow(graphics, entry, y, mouseX, mouseY);
            case MAIN_TECHNIQUE_HEADER -> renderMainHeader(graphics, entry, y, mouseX, mouseY);
            case SUB_TECHNIQUE_HEADER -> renderSubHeader(graphics, entry, y, mouseX, mouseY);
            case SUB_TECHNIQUE_ABILITY -> renderAbilityRow(graphics, entry, y, mouseX, mouseY);
            case THIRD_TECHNIQUE_HEADER -> renderThirdHeader(graphics, entry, y, mouseX, mouseY);
            case THIRD_TECHNIQUE_ABILITY -> renderAbilityRow(graphics, entry, y, mouseX, mouseY);
            case CURSE_MANAGEMENT_ENTRY, CURSE_MANAGEMENT_HEADER ->
                    renderCurseManagementEntry(graphics, entry, y, mouseX, mouseY);
        }
    }

    // ==================== 咒灵管理入口渲染 ====================

    private void renderCurseManagementEntry(GuiGraphics graphics, AbilityEntry entry,
                                            int y, int mouseX, int mouseY) {
        int indent = entry.getIndentPixels();
        int startX = listX + 2 + indent;

        graphics.fill(startX, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, 0xFF3A2255);
        graphics.fill(startX, y, startX + 2, y + ITEM_HEIGHT - 2, 0xFFAA55FF);
        graphics.drawString(font, "☠", startX + 6, y + 4, 0xAA55FF, false);

        Component title = Component.translatable("gui.jujutsu_addon.curse_management.entry", entry.curseCount);
        renderTruncatedText(graphics, title.getString(), startX + 20, y + 4, listWidth - 50 - indent, 0xDDAAFF);
        graphics.drawString(font, "▶", listX + listWidth - 18, y + 4, 0xAAAAAA, false);

        if (isHovered(mouseX, mouseY, startX, y, listWidth - 10 - indent, ITEM_HEIGHT - 2)) {
            graphics.fill(startX, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, 0x30FFFFFF);
        }
    }

    // ==================== 标题渲染 ====================

    private void renderNativeHeader(GuiGraphics graphics, AbilityEntry entry, int y, int mouseX, int mouseY) {
        graphics.fill(listX + 2, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, NATIVE_BG);
        graphics.fill(listX + 2, y, listX + 4, y + ITEM_HEIGHT - 2, NATIVE_BORDER);
        graphics.drawString(font, entry.isCollapsed ? "▶" : "▼", listX + 8, y + 4, 0xFFFFFF, false);

        Component title = entry.technique != null
                ? Component.translatable("gui.jujutsu_addon.native_header.with_name", entry.technique.getName())
                : Component.translatable("gui.jujutsu_addon.native_header.default");
        graphics.drawString(font, title, listX + 20, y + 4, NATIVE_TEXT, false);

        if (isHovered(mouseX, mouseY, listX + 2, y, listWidth - 10, ITEM_HEIGHT - 2)) {
            hoveredMainHeader = TechniqueHelper.TechniqueSource.NATIVE;
            graphics.fill(listX + 2, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, HOVER_OVERLAY);
        }
    }

    private void renderMainHeader(GuiGraphics graphics, AbilityEntry entry, int y, int mouseX, int mouseY) {
        graphics.fill(listX + 2, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, MAIN_BG);
        graphics.fill(listX + 2, y, listX + 4, y + ITEM_HEIGHT - 2, MAIN_BORDER);
        graphics.drawString(font, entry.isCollapsed ? "▶" : "▼", listX + 8, y + 4, 0xFFFFFF, false);

        String key = entry.sourceType == TechniqueHelper.TechniqueSource.STOLEN
                ? "gui.jujutsu_addon.main_header.stolen" : "gui.jujutsu_addon.main_header.copied";
        graphics.drawString(font, Component.translatable(key), listX + 20, y + 4, MAIN_TEXT, false);

        if (isHovered(mouseX, mouseY, listX + 2, y, listWidth - 10, ITEM_HEIGHT - 2)) {
            hoveredMainHeader = entry.sourceType;
            graphics.fill(listX + 2, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, HOVER_OVERLAY);
        }
    }

    private void renderSubHeader(GuiGraphics graphics, AbilityEntry entry, int y, int mouseX, int mouseY) {
        CursedTechnique technique = entry.technique;
        if (technique == null) return;

        int indent = entry.getIndentPixels();
        int startX = listX + 2 + indent;

        int bgColor = entry.isActive ? SUB_BG_ACTIVE : SUB_BG_NORMAL;
        graphics.fill(startX, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, bgColor);

        int borderColor = entry.isActive ? RenderHelper.getPulsingActiveBorderColor() : SUB_BORDER_NORMAL;
        graphics.fill(startX, y, startX + 2, y + ITEM_HEIGHT - 2, borderColor);
        graphics.drawString(font, entry.isCollapsed ? "▶" : "▼", startX + 6, y + 4, 0xCCCCCC, false);

        int nameColor = entry.isActive ? SUB_TEXT_ACTIVE : SUB_TEXT_NORMAL;
        renderTruncatedText(graphics, technique.getName().getString(), startX + 18, y + 4, listWidth - 80 - indent, nameColor);
        renderSourceLabel(graphics, entry.sourceType, y);
        renderTechniqueButtons(graphics, technique, entry.isActive, y, mouseX, mouseY);

        int activateBtnX = listX + listWidth - 28;
        if (isHovered(mouseX, mouseY, startX, y, activateBtnX - startX, ITEM_HEIGHT - 2)) {
            hoveredSubHeader = technique;
            graphics.fill(startX, y, activateBtnX, y + ITEM_HEIGHT - 2, HOVER_OVERLAY_LIGHT);
        }
    }

    private void renderThirdHeader(GuiGraphics graphics, AbilityEntry entry, int y, int mouseX, int mouseY) {
        CursedTechnique technique = entry.technique;
        if (technique == null) return;

        int indent = entry.getIndentPixels();
        int startX = listX + 2 + indent;

        int bgColor = entry.isActive ? THIRD_BG_ACTIVE : THIRD_BG_NORMAL;
        graphics.fill(startX, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, bgColor);

        int borderColor = entry.isActive ? RenderHelper.getPulsingActiveBorderColor() : THIRD_BORDER_NORMAL;
        graphics.fill(startX, y, startX + 2, y + ITEM_HEIGHT - 2, borderColor);
        graphics.drawString(font, entry.isCollapsed ? "▶" : "▼", startX + 6, y + 4, 0xBBBBBB, false);

        int nameColor = entry.isActive ? THIRD_TEXT_ACTIVE : THIRD_TEXT_NORMAL;
        renderTruncatedText(graphics, technique.getName().getString(), startX + 18, y + 4, listWidth - 90 - indent, nameColor);
        graphics.drawString(font, Component.translatable("gui.jujutsu_addon.technique_tag.copied"),
                listX + listWidth - 50, y + 4, TAG_COPIED, false);
        renderTechniqueButtons(graphics, technique, entry.isActive, y, mouseX, mouseY);

        int activateBtnX = listX + listWidth - 28;
        if (isHovered(mouseX, mouseY, startX, y, activateBtnX - startX, ITEM_HEIGHT - 2)) {
            hoveredThirdHeader = technique;
            graphics.fill(startX, y, activateBtnX, y + ITEM_HEIGHT - 2, HOVER_OVERLAY_LIGHT);
        }
    }

    // ==================== 技能行渲染 ====================

    public void renderAbilityRow(GuiGraphics graphics, AbilityEntry entry, int y, int mouseX, int mouseY) {
        Ability ability = entry.ability;
        AbilityStatus status = entry.status;
        if (ability == null || status == null) return;
        int indent = entry.getIndentPixels();
        int startX = listX + 2 + indent;
        int contentX = startX + 2;
        // ★★★ 融合式神和死亡式神特殊处理 ★★★
        boolean isFusion = status.isFusion;
        boolean isDead = status.isDead;
        // 背景和边框颜色
        int bgColor;
        int borderColor;
        if (isDead) {
            // ★★★ 死亡式神：红色背景 ★★★
            bgColor = 0x60441111;
            borderColor = 0xFFAA3333;
        } else if (isFusion) {
            // ★★★ 融合式神：紫色背景 ★★★
            bgColor = status.isActive || status.hasSummon ? 0x603A2A5A : 0x402A2A4A;
            borderColor = RenderHelper.getFusionPulsingBorderColor();
        } else {
            bgColor = RenderHelper.getIconBgColor(status.isDead, status.techniqueNotActive,
                    status.canUse, status.isActive, status.hasSummon, status.summonConflict);
            borderColor = RenderHelper.getBorderColor(
                    status.isDead, status.techniqueNotActive,
                    status.canUse, status.isActive, status.hasSummon,
                    status.summonConflict, true,
                    status.conditionsNotMet);
        }
        graphics.fill(startX, y, listX + listWidth - 8, y + ITEM_HEIGHT - 2, bgColor);
        graphics.fill(startX, y, startX + 1, y + ITEM_HEIGHT - 2, borderColor);
        // ★★★ 融合式神：额外的内边框光效 ★★★
        if (isFusion && !isDead) {
            int innerGlow = 0x30AA55FF;
            graphics.fill(startX + 1, y + 1, listX + listWidth - 9, y + ITEM_HEIGHT - 3, innerGlow);
        }
        // 图标
        if (isDead) {
            // ★★★ 死亡式神：灰色图标 + ☠ 覆盖 ★★★
            RenderHelper.renderAbilityIcon(graphics, ability, contentX, y, ENTRY_ICON_SIZE, true);
            graphics.drawString(font, "☠", contentX + 4, y + 4, 0xFF4444, true);
        } else if (status.techniqueNotActive) {
            RenderHelper.renderAbilityIconWithTint(graphics, ability, contentX, y, ENTRY_ICON_SIZE, true);
        } else if (isFusion) {
            // ★★★ 融合式神：特殊图标渲染 ★★★
            boolean grayed = !status.canUse || status.summonConflict;
            RenderHelper.renderFusionAbilityIcon(graphics, ability, contentX, y, ENTRY_ICON_SIZE, grayed);
        } else {
            boolean grayed = !status.canUse || status.summonConflict;
            RenderHelper.renderAbilityIcon(graphics, ability, contentX, y, ENTRY_ICON_SIZE, grayed);
        }
        // 名称（为伤害显示留出空间）
        int nameColor;
        if (isDead) {
            nameColor = 0xFF6666;  // 红色
        } else if (isFusion) {
            nameColor = status.isActive || status.hasSummon ? 0xDDAAFF : 0xBB99DD;  // 紫色
        } else {
            nameColor = RenderHelper.getTextColor(status.isDead, status.techniqueNotActive,
                    status.canUse, status.isActive, status.hasSummon, status.summonConflict);
        }
        int damageDisplayWidth = status.canPredictDamage ? 40 : 15;
        int nameMaxWidth = listWidth - 85 - indent - damageDisplayWidth;
        // ★★★ 融合式神：名称前加 ◆ ★★★
        String displayName = ability.getName().getString();
        if (isFusion && !isDead) {
            displayName = "◆ " + displayName;
        } else if (isDead) {
            displayName = "☠ " + displayName;
        }
        renderTruncatedText(graphics, displayName, contentX + 20, y + 4, nameMaxWidth, nameColor);
        // ★★★ 伤害显示（死亡式神显示"已死亡"）★★★
        if (isDead) {
            String deadText = Component.translatable("gui.jujutsu_addon.shikigami.dead").getString();
            int textWidth = font.width(deadText);
            int dmgX = listX + listWidth - 58;
            graphics.drawString(font, deadText, dmgX - textWidth, y + 4, 0xFF4444, false);
        } else {
            renderDamageDisplay(graphics, status, y);
        }
        // 状态指示器
        int indicatorX = contentX + 12;
        int indicatorY = y + 10;

        if (status.isTenShadowsSummon && isDead) {
            // 死亡状态已在名称前显示
        } else if (status.isTenShadowsSummon && !status.isTamed) {
            // ★★★ 未调伏要先检查！★★★
            graphics.drawString(font, "?", indicatorX + 2, indicatorY, 0xFFFF44, false);
        } else if (status.conditionsNotMet) {
            graphics.drawString(font, "⚠", indicatorX, indicatorY, 0xFFAA00, false);
        } else if (status.techniqueNotActive) {
            graphics.drawString(font, "⚡", indicatorX, indicatorY, 0xFF6666, false);
        } else if (status.summonConflict) {
            graphics.drawString(font, "!", indicatorX + 2, indicatorY, 0xFFAA00, false);
        }
        // ★★★ 开关按钮（死亡式神显示 "---"）★★★
        if (isDead) {
            renderDeadButton(graphics, y);
        } else if (status.isToggleable || status.isSummon) {
            renderToggleButton(graphics, ability, status, y, mouseX, mouseY);
        }
        // 冷却条（死亡式神不显示）
        if (!isDead && status.cooldown > 0 && status.maxCooldown > 0) {
            float progress = 1.0f - (float) status.cooldown / status.maxCooldown;
            int barWidth = Math.min(50, listWidth - 100 - indent);
            int barX = contentX + 20;
            int barY = y + 14;
            graphics.fill(barX, barY, barX + barWidth, barY + 2, 0xFF333333);
            graphics.fill(barX, barY, barX + (int)(barWidth * progress), barY + 2, 0xFFFFAA00);
        }
    }
    /**
     * ★★★ 渲染死亡式神的禁用按钮 ★★★
     */
    private void renderDeadButton(GuiGraphics graphics, int y) {
        int btnX = listX + listWidth - 28;
        int btnY = y;
        int btnW = 22;
        int btnH = ITEM_HEIGHT - 2;
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF331111);
        graphics.renderOutline(btnX, btnY, btnW, btnH, 0xFF663333);
        graphics.drawString(font, "---", btnX + (btnW - font.width("---")) / 2, y + 4, 0xFF4444, false);
    }

    /**
     * ★★★ 渲染伤害显示 ★★★
     */
    private void renderDamageDisplay(GuiGraphics graphics, AbilityStatus status, int y) {
        int dmgX = listX + listWidth - 58;
        int dmgY = y + 4;
        String dmgText;
        int dmgColor;
        switch (status.damageType) {
            case DIRECT_DAMAGE, POWER_BASED -> {
                if (status.canPredictDamage) {
                    dmgText = status.formatDamage(status.getDisplayDamage());
                    // ★ 修复：根据伤害变化方向选择颜色
                    if (status.isDamageIncreased()) {
                        dmgColor = 0x55FF55;  // 绿色 = 增加
                    } else if (status.isDamageDecreased()) {
                        dmgColor = 0xFF5555;  // 红色 = 减少
                    } else {
                        dmgColor = 0xFFAA55;  // 橙色 = 无变化
                    }
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            case SUMMON -> {
                if (status.canPredictDamage) {
                    dmgText = status.formatDamage(status.addonDamage);  // ★ 修复：使用 addonDamage
                    dmgColor = 0x55AAFF;
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            case DOMAIN -> {
                if (status.canPredictDamage) {
                    dmgText = status.formatDamage(status.addonDamage);  // ★ 修复：使用 addonDamage
                    dmgColor = 0xAA55FF;
                } else {
                    dmgText = "?";
                    dmgColor = 0x888888;
                }
            }
            case UTILITY -> {
                dmgText = "—";
                dmgColor = 0x666666;
            }
            default -> {
                dmgText = "?";
                dmgColor = 0x888888;
            }
        }
        int textWidth = font.width(dmgText);
        graphics.drawString(font, dmgText, dmgX - textWidth, dmgY, dmgColor, false);
        // ★ 修复：根据变化方向显示箭头
        if (status.canPredictDamage && status.hasAddonModification) {
            if (status.isDamageIncreased()) {
                graphics.drawString(font, "↑", dmgX - textWidth - 6, dmgY, 0x55FF55, false);
            } else if (status.isDamageDecreased()) {
                graphics.drawString(font, "↓", dmgX - textWidth - 6, dmgY, 0xFF5555, false);
            }
        }
    }

    private void renderToggleButton(GuiGraphics graphics, Ability ability, AbilityStatus status,
                                    int y, int mouseX, int mouseY) {
        int btnX = listX + listWidth - 28;
        int btnY = y;
        int btnW = 22;
        int btnH = ITEM_HEIGHT - 2;
        boolean hovered = isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
        boolean isOn = status.isOn();

        int bgColor;
        if (!status.canUse && !isOn) {
            bgColor = BTN_DISABLED_BG;
        } else if (isOn) {
            bgColor = hovered ? BTN_ON_BG_HOVER : BTN_ON_BG;
        } else {
            bgColor = hovered ? BTN_OFF_BG_HOVER : BTN_OFF_BG;
        }

        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, bgColor);
        graphics.renderOutline(btnX, btnY, btnW, btnH, BTN_BORDER);

        String text = isOn ? "ON" : "OFF";
        int textColor;
        if (isOn) {
            textColor = BTN_ON_TEXT;
        } else if (status.canUse) {
            textColor = BTN_OFF_TEXT;
        } else {
            textColor = BTN_DISABLED_TEXT;
        }
        graphics.drawString(font, text, btnX + (btnW - font.width(text)) / 2, y + 4, textColor, false);

        if (hovered && (status.canUse || isOn)) {
            hoveredToggleAbility = ability;
            toggleBtnX = btnX;
            toggleBtnY = btnY;
        }
    }

    private void renderSourceLabel(GuiGraphics graphics, TechniqueHelper.TechniqueSource sourceType, int y) {
        String key = sourceType == TechniqueHelper.TechniqueSource.STOLEN
                ? "gui.jujutsu_addon.technique_tag.stolen" : "gui.jujutsu_addon.technique_tag.copied";
        int color = sourceType == TechniqueHelper.TechniqueSource.STOLEN ? TAG_STOLEN : TAG_COPIED;
        graphics.drawString(font, Component.translatable(key), listX + listWidth - 50, y + 4, color, false);
    }

    private void renderTechniqueButtons(GuiGraphics graphics, CursedTechnique technique,
                                        boolean isActive, int y, int mouseX, int mouseY) {
        int activateBtnX = listX + listWidth - 28;
        boolean activateHovered = isHovered(mouseX, mouseY, activateBtnX, y, 12, ITEM_HEIGHT - 2);
        String symbol = isActive ? "★" : "☆";
        int color = isActive ? 0xFFFF00 : 0x888888;
        if (activateHovered) {
            color = isActive ? 0xFFAA00 : 0xCCCCCC;
            hoveredActivateTechnique = technique;
        }
        graphics.drawString(font, symbol, activateBtnX, y + 4, color, false);

        int deleteBtnX = listX + listWidth - 14;
        boolean deleteHovered = isHovered(mouseX, mouseY, deleteBtnX, y, 10, ITEM_HEIGHT - 2);
        if (deleteHovered) hoveredDeleteTechnique = technique;
        graphics.drawString(font, "×", deleteBtnX, y + 4, deleteHovered ? 0xFF4444 : 0x884444, false);
    }

    private void renderTruncatedText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, maxWidth - 6) + "...";
        }
        graphics.drawString(font, text, x, y, color, false);
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // ==================== 按钮颜色常量 ====================

    private static final int BTN_ON_BG = 0xFF114411;
    private static final int BTN_ON_BG_HOVER = 0xFF226622;
    private static final int BTN_OFF_BG = 0xFF2A2A2A;
    private static final int BTN_OFF_BG_HOVER = 0xFF444444;
    private static final int BTN_DISABLED_BG = 0xFF333333;
    private static final int BTN_BORDER = 0xFF666666;
    private static final int BTN_ON_TEXT = 0x55FF55;
    private static final int BTN_OFF_TEXT = 0xFFFFFF;
    private static final int BTN_DISABLED_TEXT = 0x666666;

    private static final int HOVER_OVERLAY = 0x30FFFFFF;
    private static final int HOVER_OVERLAY_LIGHT = 0x20FFFFFF;
}
