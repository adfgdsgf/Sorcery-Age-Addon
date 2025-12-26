package com.jujutsuaddon.addon.client.gui.screen.shadowstorage;

import com.jujutsuaddon.addon.client.util.NumberFormatHelper;
import com.jujutsuaddon.addon.client.render.SlotRenderHelper;
import com.jujutsuaddon.addon.util.helper.ShadowStorageSortHelper;
import com.jujutsuaddon.addon.util.helper.ShadowStorageSortHelper.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 影子库存渲染器
 */
public class ShadowStorageRenderer {

    private final Font font;
    private final ShadowStorageLayout layout;
    private float shadowPulse = 0;

    public ShadowStorageRenderer(Font font, ShadowStorageLayout layout) {
        this.font = font;
        this.layout = layout;
    }

    public void tick(float partialTick) {
        shadowPulse += partialTick * 0.1f;
    }

    // ==================== 主渲染 ====================

    public void renderBackground(GuiGraphics graphics, int screenWidth, int screenHeight) {
        graphics.fill(0, 0, screenWidth, screenHeight, 0xD0050510);
    }

    /**
     * ★★★ 简化版：只渲染标题，统计信息移到底部 ★★★
     */
    public void renderContainer(GuiGraphics graphics, Component title,
                                ShadowStorageDataManager dataManager, String searchText,
                                int searchBoxX) {
        int x = layout.getLeftPos();
        int y = layout.getTopPos();
        int w = ShadowStorageLayout.CONTAINER_WIDTH;
        int h = ShadowStorageLayout.CONTAINER_HEIGHT;

        // 主背景
        graphics.fill(x, y, x + w, y + h, 0xF0100818);

        // 边框动画
        float pulse = (float) (Math.sin(shadowPulse * 2) * 0.3 + 0.7);
        int borderAlpha = (int) (200 * pulse);
        graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, (borderAlpha << 24) | 0x6622AA);
        graphics.renderOutline(x, y, w, h, 0xFF3311AA);

        // 标题（只显示标题，不显示统计）
        float hue = (float) (Math.sin(shadowPulse * 0.5) * 0.1 + 0.75);
        int titleColor = java.awt.Color.HSBtoRGB(hue, 0.6f, 1.0f) & 0xFFFFFF;
        graphics.drawString(font, title, x + 8, y + 6, titleColor, false);
    }

    /**
     * ★★★ 新增：在界面底部渲染统计信息 ★★★
     */
    public void renderStats(GuiGraphics graphics, ShadowStorageDataManager dataManager,
                            String searchText) {
        int gridX = layout.getShadowGridX();
        int gridWidth = layout.getShadowGridWidth();
        int statsY = layout.getPlayerInvY() - 11; // 和"物品栏"标题同一行
        // 构建统计文本
        int displayCount = dataManager.getDisplayCount();
        int totalCount = dataManager.getTotalCount();
        String totalFormatted = NumberFormatHelper.formatLargeCount(dataManager.getTotalItemCount());
        Component statsComponent;
        if (!searchText.isEmpty() && displayCount != totalCount) {
            statsComponent = Component.translatable(
                    "gui.jujutsu_addon.shadow_storage.stats.filtered",
                    displayCount, totalCount, totalFormatted);
        } else {
            statsComponent = Component.translatable(
                    "gui.jujutsu_addon.shadow_storage.stats.normal",
                    displayCount, totalFormatted);
        }
        String statsText = statsComponent.getString();
        int statsWidth = font.width(statsText);

        // ★★★ 右对齐显示在网格右侧 ★★★
        int statsX = gridX + gridWidth - statsWidth;
        graphics.drawString(font, statsText, statsX, statsY, 0x6688AA, false);
    }

    // ==================== 影子库存网格 ====================

    public void renderShadowInventory(GuiGraphics graphics, int mouseX, int mouseY,
                                      List<ItemStack> displayItems, int scrollRow,
                                      Set<Integer> draggedIndices, boolean isDragRetrieving,
                                      String searchText) {
        int gridX = layout.getShadowGridX();
        int gridY = layout.getShadowGridY();
        int gridW = layout.getShadowGridWidth();
        int gridH = layout.getShadowGridHeight();

        graphics.fill(gridX, gridY, gridX + gridW, gridY + gridH, 0xC0080410);
        graphics.renderOutline(gridX, gridY, gridW, gridH, 0x804422AA);

        if (!searchText.isEmpty() && displayItems.isEmpty()) {
            String noResult = Component.translatable(
                    "gui.jujutsu_addon.shadow_storage.search.no_result").getString();
            int textWidth = font.width(noResult);
            graphics.drawString(font, noResult,
                    gridX + (gridW - textWidth) / 2,
                    gridY + gridH / 2 - 4, 0x888888, false);
            return;
        }

        int startIndex = scrollRow * ShadowStorageLayout.COLS;
        for (int row = 0; row < ShadowStorageLayout.SHADOW_ROWS; row++) {
            for (int col = 0; col < ShadowStorageLayout.COLS; col++) {
                int index = startIndex + row * ShadowStorageLayout.COLS + col;
                int slotX = gridX + col * ShadowStorageLayout.SLOT_SIZE;
                int slotY = gridY + row * ShadowStorageLayout.SLOT_SIZE;

                boolean hovered = SlotRenderHelper.isInSlot(mouseX, mouseY, slotX, slotY);
                boolean dragged = isDragRetrieving && draggedIndices.contains(index);

                if (index < displayItems.size()) {
                    SlotRenderHelper.renderShadowSlot(graphics, font, slotX, slotY,
                            displayItems.get(index), hovered, dragged);
                } else if (index == displayItems.size()) {
                    SlotRenderHelper.renderEmptySlot(graphics, slotX, slotY, hovered);
                }
            }
        }
    }

    // ==================== 玩家背包 ====================

    public void renderPlayerInventory(GuiGraphics graphics, int mouseX, int mouseY,
                                      Set<Integer> draggedSlots, boolean isDragStoring) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Inventory inv = player.getInventory();
        int gridX = layout.getShadowGridX();

        graphics.drawString(font, Component.translatable("container.inventory"),
                gridX, layout.getPlayerInvY() - 11, 0x606080, false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                int slotX = gridX + col * ShadowStorageLayout.SLOT_SIZE;
                int slotY = layout.getPlayerInvY() + row * ShadowStorageLayout.SLOT_SIZE;

                boolean hovered = SlotRenderHelper.isInSlot(mouseX, mouseY, slotX, slotY);
                boolean dragged = isDragStoring && draggedSlots.contains(slotIndex);
                SlotRenderHelper.renderPlayerSlot(graphics, font, slotX, slotY,
                        inv.getItem(slotIndex), hovered, dragged);
            }
        }

        for (int col = 0; col < 9; col++) {
            int slotX = gridX + col * ShadowStorageLayout.SLOT_SIZE;
            int slotY = layout.getPlayerHotbarY();

            boolean hovered = SlotRenderHelper.isInSlot(mouseX, mouseY, slotX, slotY);
            boolean dragged = isDragStoring && draggedSlots.contains(col);
            SlotRenderHelper.renderPlayerSlot(graphics, font, slotX, slotY,
                    inv.getItem(col), hovered, dragged);
        }
    }

    // ==================== 侧边按钮 ====================

    public void renderSideButtons(GuiGraphics graphics, int mouseX, int mouseY,
                                  SortMode sortMode, String searchText) {
        int btnX = layout.getSideButtonX();
        int btnSize = layout.getSideButtonSize();
        int btnY = layout.getTopPos() + 18;

        boolean sortHovered = SlotRenderHelper.isInButton(mouseX, mouseY, btnX, btnY, btnSize);
        renderButton(graphics, btnX, btnY, btnSize,
                ShadowStorageSortHelper.getSortModeIcon(sortMode),
                sortHovered, 0xC0201030, 0xE0302050, 0xFF6633AA, 0xFF8855CC);

        int hudBtnY = btnY + btnSize + layout.getSideButtonGap();
        boolean hudHovered = SlotRenderHelper.isInButton(mouseX, mouseY, btnX, hudBtnY, btnSize);
        renderButton(graphics, btnX, hudBtnY, btnSize, "⚙",
                hudHovered, 0xC0102030, 0xE0203050, 0xFF3366AA, 0xFF5588CC);

        if (!searchText.isEmpty()) {
            int clearBtnY = hudBtnY + btnSize + layout.getSideButtonGap();
            boolean clearHovered = SlotRenderHelper.isInButton(mouseX, mouseY, btnX, clearBtnY, btnSize);
            renderButton(graphics, btnX, clearBtnY, btnSize, "✕",
                    clearHovered, 0xC0302020, 0xE0503030, 0xFFAA3333, 0xFFCC5555);
        }
    }

    private void renderButton(GuiGraphics graphics, int x, int y, int size, String icon,
                              boolean hovered, int bgNormal, int bgHover,
                              int borderNormal, int borderHover) {
        graphics.fill(x, y, x + size, y + size, hovered ? bgHover : bgNormal);
        graphics.renderOutline(x, y, size, size, hovered ? borderHover : borderNormal);

        int iconWidth = font.width(icon);
        graphics.drawString(font, icon,
                x + (size - iconWidth) / 2,
                y + (size - 8) / 2, 0xFFFFFF, false);
    }

    // ==================== 滚动条 ====================

    public void renderScrollbar(GuiGraphics graphics, int scrollRow, int maxScrollRow, int totalRows) {
        if (maxScrollRow <= 0) return;

        int x = layout.getScrollbarX();
        int y = layout.getShadowGridY();
        int height = layout.getShadowGridHeight();

        graphics.fill(x - 2, y, x + 2, y + height, 0x60100820);

        int thumbHeight = Math.max(10, height * ShadowStorageLayout.SHADOW_ROWS / totalRows);
        int thumbY = y + (height - thumbHeight) * scrollRow / maxScrollRow;

        float pulse = (float) (Math.sin(shadowPulse) * 0.2 + 0.8);
        int thumbColor = (int) (200 * pulse) << 24 | 0x6622AA;
        graphics.fill(x - 1, thumbY, x + 1, thumbY + thumbHeight, thumbColor);
    }

    // ==================== 拖拽提示 ====================

    public void renderDragHint(GuiGraphics graphics, int screenWidth, int screenHeight,
                               int containerTop, int containerHeight,
                               int shadowCount, int playerCount) {
        List<String> hints = new ArrayList<>();

        if (shadowCount > 0) {
            hints.add(Component.translatable("gui.jujutsu_addon.shadow_storage.drag_retrieving", shadowCount).getString());
        }
        if (playerCount > 0) {
            hints.add(Component.translatable("gui.jujutsu_addon.shadow_storage.drag_storing", playerCount).getString());
        }

        if (hints.isEmpty()) return;

        String hint = String.join(" | ", hints);
        int textWidth = font.width(hint);
        int x = (screenWidth - textWidth) / 2;

        int preferredY = containerTop + containerHeight + 16; // 留出统计信息空间
        int y;
        if (preferredY + 12 > screenHeight - 5) {
            y = containerTop - 15;
            if (y < 5) {
                y = screenHeight / 2 + 50;
            }
        } else {
            y = preferredY;
        }

        int bgColor = 0xC0000000;
        int textColor;
        if (shadowCount > 0 && playerCount > 0) {
            textColor = 0xFFFFAA55;
        } else if (playerCount > 0) {
            textColor = 0xFF55FF55;
        } else {
            textColor = 0xFF5555FF;
        }
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + 10, bgColor);
        graphics.drawString(font, hint, x, y, textColor, false);
    }
}
