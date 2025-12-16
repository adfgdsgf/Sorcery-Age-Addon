package com.jujutsuaddon.addon.client.gui.screen.cursemanagement;

import com.jujutsuaddon.addon.client.util.EntityRenderHelper;
import com.jujutsuaddon.addon.client.util.RenderHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 咒灵管理渲染器
 */
public class CurseManagementRenderer {

    private final Font font;
    private final CurseManagementLayout layout;

    public CurseManagementRenderer(Font font, CurseManagementLayout layout) {
        this.font = font;
        this.layout = layout;
    }

    // ==================== 主渲染 ====================

    public void renderBackground(GuiGraphics graphics) {
        int bgPadding = Math.max(3, layout.getCellPadding());
        int x = layout.getGridStartX();
        int y = layout.getGridStartY();
        int w = layout.getGridWidth();
        int h = layout.getGridHeight();

        graphics.fill(x - bgPadding, y - bgPadding,
                x + w + bgPadding, y + h + bgPadding, 0xCC222222);
        graphics.renderOutline(x - bgPadding, y - bgPadding,
                w + bgPadding * 2, h + bgPadding * 2, 0xFF555555);
    }

    public void renderTitle(GuiGraphics graphics, Component title,
                            int totalCount, int uniqueTypeCount) {
        int centerX = layout.getScreenWidth() / 2;
        graphics.drawCenteredString(font, title, centerX, 12, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("gui.jujutsu_addon.curse_management.count_grouped",
                        totalCount, uniqueTypeCount),
                centerX, 25, 0xAAAAAA);
    }

    public void renderPageIndicator(GuiGraphics graphics, int currentPage, int totalPages) {
        int pageY = layout.getGridStartY() + layout.getGridHeight() + 6;
        graphics.drawCenteredString(font,
                (currentPage + 1) + " / " + totalPages,
                layout.getScreenWidth() / 2, pageY, 0xFFFF00);
    }

    // ==================== 格子渲染 ====================

    public void renderCell(GuiGraphics graphics,
                           CurseManagementDataManager.GroupedCurse group,
                           int cellX, int cellY, boolean isHovered) {
        int cellSize = layout.getCellSize();
        int count = group.getCount();
        boolean showVariant = group.shouldShowVariantTag(); // ★★★ 使用新方法 ★★★

        // 背景颜色
        int bgColor = getCellBgColor(isHovered, showVariant, count);
        graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, bgColor);

        // 边框颜色
        int borderColor = getCellBorderColor(isHovered, showVariant, count);
        graphics.renderOutline(cellX, cellY, cellSize, cellSize, borderColor);

        // 渲染实体
        renderEntityInCell(graphics, group.getEntityType(), cellX, cellY, cellSize, isHovered);

        // 名称
        String entityName = group.getDisplayName().getString();
        int maxNameWidth = cellSize - 4;
        if (font.width(entityName) > maxNameWidth) {
            entityName = font.plainSubstrByWidth(entityName, maxNameWidth - 6) + "..";
        }
        graphics.drawCenteredString(font, entityName, cellX + cellSize / 2, cellY + cellSize - 10, 0xCCCCCC);

        // 数量徽章
        if (count > 1) {
            renderCountBadge(graphics, cellX, cellY, cellSize, count);
        }

        // ★★★ 变体标记（只有 JJK 生物才显示）★★★
        if (showVariant) {
            graphics.drawString(font, "★", cellX + 2, cellY + 2, 0x55FFFF, true);
        }
    }

    public void renderEmptyCell(GuiGraphics graphics, int cellX, int cellY) {
        int cellSize = layout.getCellSize();
        graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, 0x40333333);
        graphics.renderOutline(cellX, cellY, cellSize, cellSize, 0xFF444444);
    }

    private int getCellBgColor(boolean isHovered, boolean hasVariant, int count) {
        if (isHovered) return 0xDD445566;
        if (hasVariant) return 0xDD3A4455;
        if (count >= 5) return 0xDD443366;
        if (count >= 3) return 0xDD3A3A5A;
        return 0xDD333344;
    }

    private int getCellBorderColor(boolean isHovered, boolean hasVariant, int count) {
        if (isHovered) return 0xFF66AAFF;
        if (hasVariant) return 0xFF55AAAA;
        if (count >= 5) return 0xFFAA66FF;
        if (count >= 3) return 0xFF8888CC;
        return 0xFF666699;
    }

    // ==================== 实体渲染 ====================

    private void renderEntityInCell(GuiGraphics graphics, EntityType<?> entityType,
                                    int x, int y, int size, boolean isHovered) {
        Entity entity = EntityRenderHelper.getOrCreateEntity(entityType);

        if (entity == null) {
            // 回退：显示缩写
            String name = EntityType.getKey(entityType).getPath();
            String abbrev = RenderHelper.getAbbreviation(name.replace("cursed_", "").replace("curse_", ""), 3);
            graphics.fill(x + 4, y + 4, x + size - 4, y + size - 14, 0xFF222233);
            graphics.drawCenteredString(font, abbrev, x + size / 2, y + size / 2 - 8, 0xAA66FF);
            return;
        }

        int entityScale = EntityRenderHelper.calculateEntityScale(entity, size);
        int centerX = x + size / 2;
        int centerY = y + size / 2 + (int)(entity.getBbHeight() * entityScale / 3);

        float angleX = isHovered ? 0.3f : 0.0f;
        EntityRenderHelper.renderEntityInGui(graphics.pose(), centerX, centerY, entityScale, angleX, -0.2f, entity);
    }

    // ==================== 数量徽章 ====================

    private void renderCountBadge(GuiGraphics graphics, int cellX, int cellY, int cellSize, int count) {
        String countText = count >= 99 ? "99+" : String.valueOf(count);
        int textWidth = font.width(countText);

        int badgePadding = 2;
        int badgeWidth = textWidth + badgePadding * 2;
        int badgeHeight = 10;
        int badgeX = cellX + cellSize - badgeWidth - 2;
        int badgeY = cellY + 2;

        int badgeBgColor;
        int badgeTextColor;
        if (count >= 10) {
            badgeBgColor = 0xEEFF4444;
            badgeTextColor = 0xFFFFFF;
        } else if (count >= 5) {
            badgeBgColor = 0xEEFFAA00;
            badgeTextColor = 0x000000;
        } else {
            badgeBgColor = 0xEE44AA44;
            badgeTextColor = 0xFFFFFF;
        }

        graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, badgeBgColor);
        graphics.renderOutline(badgeX, badgeY, badgeWidth, badgeHeight, 0xFF000000);
        graphics.drawString(font, countText, badgeX + badgePadding, badgeY + 1, badgeTextColor, false);
    }

    // ==================== Tooltip ====================

    public void renderTooltip(GuiGraphics graphics,
                              CurseManagementDataManager.GroupedCurse group,
                              CurseManagementDataManager dataManager,
                              int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        // 名称
        tooltip.add(group.getDisplayName().copy().withStyle(s -> s.withColor(0xAA66FF)));
        // 类型
        tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.type",
                EntityType.getKey(group.getEntityType()).toString()).withStyle(s -> s.withColor(0x888888)));
        // 数量
        int count = group.getCount();
        if (count > 1) {
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.count_same", count)
                    .withStyle(s -> s.withColor(0xFFAA00)));
        }
        // 经验和消耗
        int firstIndex = group.getFirstIndex();
        AbsorbedCurse curse = dataManager.getRawCurseAt(firstIndex);
        if (curse != null) {
            // 经验值
            float experience = dataManager.getCurseExperience(curse);
            if (experience > 0) {
                tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.experience",
                        String.format("%.0f", experience)).withStyle(s -> s.withColor(0x55FF55)));
            }
            // 单个召唤消耗
            float cost = JJKAbilities.getCurseCost(curse);
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.summon_cost",
                    String.format("%.1f", cost)).withStyle(s -> s.withColor(0x55AAFF)));
        }
        // 变体标记（只有 JJK 生物才显示）
        if (group.shouldShowVariantTag()) {
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.has_data")
                    .withStyle(s -> s.withColor(0x55FFFF)));
        }
        tooltip.add(Component.empty());
        // 操作提示
        tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.click_to_summon_one")
                .withStyle(s -> s.withColor(0x55FF55)));
        if (count > 1) {
            // 计算该组所有咒灵的总消耗
            float totalCost = 0;
            for (int idx : group.getOriginalIndices()) {
                AbsorbedCurse c = dataManager.getRawCurseAt(idx);
                if (c != null) {
                    totalCost += JJKAbilities.getCurseCost(c);
                }
            }
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.shift_click_to_summon_all_cost",
                    count, String.format("%.1f", totalCost)).withStyle(s -> s.withColor(0x55AAFF)));
        }
        graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }
}

