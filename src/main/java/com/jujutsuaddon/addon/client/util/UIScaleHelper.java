package com.jujutsuaddon.addon.client.util;

import net.minecraft.client.Minecraft;

/**
 * 统一UI缩放管理器 - 真正的自适应布局
 */
public class UIScaleHelper {

    // ==================== 获取MC的GUI缩放信息 ====================

    /**
     * 获取当前GUI缩放比例
     */
    public static double getGuiScale() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getWindow().getGuiScale();
    }

    /**
     * 获取实际像素尺寸（物理分辨率）
     */
    public static int[] getActualPixelSize() {
        Minecraft mc = Minecraft.getInstance();
        return new int[]{
                mc.getWindow().getWidth(),
                mc.getWindow().getHeight()
        };
    }

    /**
     * 获取GUI缩放后的尺寸
     */
    public static int[] getScaledSize() {
        Minecraft mc = Minecraft.getInstance();
        return new int[]{
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight()
        };
    }

    // ==================== 影子库存界面 ====================
    /**
     * 影子库存：计算网格布局
     * 返回 [列数, 行数, 格子尺寸, 格子间距]
     */
    public static int[] calculateShadowStorageLayout(int screenWidth, int screenHeight) {
        int[] actualSize = getActualPixelSize();
        int actualPixelHeight = actualSize[1];
        // 目标配置（比咒灵管理稍大，因为物品图标更小）
        int[] targetConfig;
        if (actualPixelHeight >= 800) {
            targetConfig = new int[]{8, 5};  // 40格
        } else if (actualPixelHeight >= 600) {
            targetConfig = new int[]{7, 4};  // 28格
        } else if (actualPixelHeight >= 480) {
            targetConfig = new int[]{6, 4};  // 24格
        } else {
            targetConfig = new int[]{5, 3};  // 15格
        }
        int targetCols = targetConfig[0];
        int targetRows = targetConfig[1];
        int titleSpace = 55;
        int buttonSpace = 70;
        int horizontalMargin = 15;
        int availableWidth = screenWidth - horizontalMargin * 2;
        int availableHeight = screenHeight - titleSpace - buttonSpace;
        int minCellSize = 20;
        int maxCellSize = 40;
        int basePadding = 2;
        int[][] fallbackConfigs = {
                {targetCols, targetRows},
                {targetCols, targetRows - 1},
                {targetCols - 1, targetRows},
                {targetCols - 1, targetRows - 1},
                {6, 4},
                {5, 4},
                {5, 3},
                {4, 3},
        };
        int bestCols = 4;
        int bestRows = 3;
        int bestCellSize = minCellSize;
        for (int[] config : fallbackConfigs) {
            int cols = config[0];
            int rows = config[1];
            if (cols < 4 || rows < 3) continue;
            int totalHPadding = basePadding * (cols - 1);
            int totalVPadding = basePadding * (rows - 1);
            int maxCellWidth = (availableWidth - totalHPadding) / cols;
            int maxCellHeight = (availableHeight - totalVPadding) / rows;
            int cellSize = Math.min(maxCellWidth, maxCellHeight);
            if (cellSize < minCellSize) continue;
            cellSize = Math.min(cellSize, maxCellSize);
            int totalWidth = cols * cellSize + totalHPadding;
            int totalHeight = rows * cellSize + totalVPadding;
            if (totalWidth <= availableWidth && totalHeight <= availableHeight) {
                bestCols = cols;
                bestRows = rows;
                bestCellSize = cellSize;
                break;
            }
        }
        return new int[]{bestCols, bestRows, bestCellSize, basePadding};
    }

    // ==================== 影子库存侧边按钮 ====================
    /**
     * 计算影子库存侧边按钮布局（排序、HUD编辑等）
     * 返回 [按钮尺寸, 按钮间距, 距容器的间隙]
     */
    public static int[] calculateShadowStorageSideButtonLayout() {
        int[] actualSize = getActualPixelSize();
        int actualPixelHeight = actualSize[1];

        int btnSize;
        int btnGap;
        int containerGap;

        if (actualPixelHeight >= 1080) {
            btnSize = 20;
            btnGap = 4;
            containerGap = 4;
        } else if (actualPixelHeight >= 720) {
            btnSize = 18;
            btnGap = 3;
            containerGap = 3;
        } else {
            btnSize = 16;
            btnGap = 2;
            containerGap = 2;
        }

        return new int[]{btnSize, btnGap, containerGap};
    }

    // ==================== 咒灵管理界面 ====================

    /**
     * 基于实际像素高度决定目标网格配置
     * 这样无论 GUI Scale 设置如何，相同分辨率都会得到相同的目标配置
     */
    private static int[] getTargetGridConfig(int actualPixelHeight) {
        if (actualPixelHeight >= 800) {       // 900p, 1080p, 1440p, 4K
            return new int[]{6, 4};           // 24格
        } else if (actualPixelHeight >= 600) { // 720p, 800x600
            return new int[]{5, 4};           // 20格
        } else if (actualPixelHeight >= 480) { // 小窗口
            return new int[]{5, 3};           // 15格
        } else {                               // 非常小的窗口
            return new int[]{4, 3};           // 12格
        }
    }

    /**
     * 咒灵管理：计算最佳网格配置
     * ★★★ 基于实际像素分辨率决定目标配置，然后在 GUI 空间中渲染 ★★★
     * 返回 [列数, 行数, 格子尺寸, 格子间距]
     */
    public static int[] calculateCurseGridLayout(int screenWidth, int screenHeight) {
        // ★★★ 获取实际像素分辨率来决定目标配置 ★★★
        int[] actualSize = getActualPixelSize();
        int actualPixelHeight = actualSize[1];

        int[] targetConfig = getTargetGridConfig(actualPixelHeight);
        int targetCols = targetConfig[0];
        int targetRows = targetConfig[1];

        // 预留空间（在 guiScaled 坐标系中）
        int titleSpace = 42;
        int buttonSpace = 78;
        int horizontalMargin = 12;

        // 可用空间（guiScaled）
        int availableWidth = screenWidth - horizontalMargin * 2;
        int availableHeight = screenHeight - titleSpace - buttonSpace;

        // 格子尺寸限制 - 允许更小的格子以适应高 GUI Scale
        int minCellSize = 24;
        int maxCellSize = 70;
        int basePadding = 3;

        // ★★★ 从目标配置开始，逐步降级 ★★★
        int[][] fallbackConfigs = {
                {targetCols, targetRows},           // 目标配置
                {targetCols, targetRows - 1},       // 减少行
                {targetCols - 1, targetRows},       // 减少列
                {targetCols - 1, targetRows - 1},   // 都减少
                {4, 3},
                {3, 3},
                {3, 2},
        };

        int bestCols = 3;
        int bestRows = 2;
        int bestCellSize = minCellSize;

        for (int[] config : fallbackConfigs) {
            int cols = config[0];
            int rows = config[1];

            if (cols < 3 || rows < 2) continue;

            int totalHPadding = basePadding * (cols - 1);
            int totalVPadding = basePadding * (rows - 1);

            int maxCellWidth = (availableWidth - totalHPadding) / cols;
            int maxCellHeight = (availableHeight - totalVPadding) / rows;
            int cellSize = Math.min(maxCellWidth, maxCellHeight);

            // 如果格子太小，跳过这个配置继续尝试下一个
            if (cellSize < minCellSize) continue;

            cellSize = Math.min(cellSize, maxCellSize);

            int totalWidth = cols * cellSize + totalHPadding;
            int totalHeight = rows * cellSize + totalVPadding;

            if (totalWidth <= availableWidth && totalHeight <= availableHeight) {
                bestCols = cols;
                bestRows = rows;
                bestCellSize = cellSize;
                break;
            }
        }

        return new int[]{bestCols, bestRows, bestCellSize, basePadding};
    }

    // ==================== 容器界面按钮 ====================
    /**
     * 计算容器界面（如影子库存）内小按钮的布局
     * 返回 [x, y, width, height]
     *
     * @param screenWidth   屏幕宽度
     * @param screenHeight  屏幕高度
     * @param containerX    容器左上角X
     * @param containerY    容器左上角Y
     * @param containerWidth 容器宽度
     */
    public static int[] calculateContainerButtonLayout(int screenWidth, int screenHeight,
                                                       int containerX, int containerY,
                                                       int containerWidth) {
        int[] actualSize = getActualPixelSize();
        int actualPixelHeight = actualSize[1];
        // 根据分辨率决定按钮大小
        int btnHeight;
        int btnWidth;
        int btnMargin;
        if (actualPixelHeight >= 1080) {
            // 高分辨率
            btnHeight = 16;
            btnWidth = 36;
            btnMargin = 4;
        } else if (actualPixelHeight >= 720) {
            // 中等分辨率
            btnHeight = 14;
            btnWidth = 32;
            btnMargin = 3;
        } else {
            // 低分辨率
            btnHeight = 12;
            btnWidth = 28;
            btnMargin = 2;
        }
        // 按钮位置：容器右上角外侧
        int btnX = containerX + containerWidth - btnWidth - btnMargin;
        int btnY = containerY - btnHeight - btnMargin;
        // 确保按钮不会超出屏幕
        if (btnY < 2) {
            // 如果上方没空间，放到容器内部右上角
            btnY = containerY + btnMargin;
            btnX = containerX + containerWidth - btnWidth - btnMargin - 4;
        }
        // 确保不超出右边界
        if (btnX + btnWidth > screenWidth - 2) {
            btnX = screenWidth - btnWidth - 2;
        }
        return new int[]{btnX, btnY, btnWidth, btnHeight};
    }
    /**
     * 计算容器界面标题栏内的按钮（放在标题右侧）
     * 返回 [x, y, width, height]
     */
    public static int[] calculateTitleBarButtonLayout(int screenWidth, int screenHeight,
                                                      int containerX, int containerY,
                                                      int containerWidth, int titleY) {
        int[] actualSize = getActualPixelSize();
        int actualPixelHeight = actualSize[1];
        // 根据分辨率决定按钮大小
        int btnHeight;
        int btnWidth;
        if (actualPixelHeight >= 1080) {
            btnHeight = 12;
            btnWidth = 28;
        } else if (actualPixelHeight >= 720) {
            btnHeight = 11;
            btnWidth = 26;
        } else {
            btnHeight = 10;
            btnWidth = 24;
        }
        // 按钮位置：标题栏右侧
        int btnX = containerX + containerWidth - btnWidth - 8;
        int btnY = containerY + titleY;
        return new int[]{btnX, btnY, btnWidth, btnHeight};
    }

    // ==================== 技能配置界面 ====================
    // （保持不变）

    public static int[] calculateSkillConfigLayout(int screenWidth, int screenHeight) {
        int topMargin = 40;
        int bottomMargin = 50;
        int sideMargin = 8;
        int gap = 15;

        int availableHeight = screenHeight - topMargin - bottomMargin;
        int availableWidth = screenWidth - sideMargin * 2;

        int listWidth = clamp((int)(availableWidth * 0.38f), 110, 200);
        int listHeight = availableHeight;
        int listX = sideMargin;
        int listY = topMargin;

        int slotAreaWidth = availableWidth - listWidth - gap;
        int slotSize = clamp(Math.min(slotAreaWidth / 3 - 4, availableHeight / 2 - 10), 22, 38);
        int slotAreaX = listX + listWidth + gap;
        int slotAreaY = listY + 15;

        return new int[]{listWidth, listHeight, listX, listY, slotSize, slotAreaX, slotAreaY};
    }

    public static int[] calculateButtonLayout(int screenWidth, int screenHeight,
                                              int contentBottomY, int minBottomMargin) {
        int btnHeight = clamp(screenHeight / 25, 14, 20);
        int btnSmall = clamp(screenWidth / 14, 40, 65);
        int btnMed = clamp(screenWidth / 10, 60, 100);
        int btnLarge = clamp(screenWidth / 7, 80, 130);

        int buttonY = Math.min(contentBottomY + 8, screenHeight - btnHeight - minBottomMargin);

        return new int[]{buttonY, btnHeight, btnSmall, btnMed, btnLarge};
    }

    // ==================== 誓约列表界面 ====================
    /**
     * 誓约列表：计算条目布局
     * 返回 [条目高度, 条目间距, 快捷按钮宽度, 快捷按钮高度, 按钮右边距, 按钮垂直居中偏移]
     */
    public static int[] calculateVowListEntryLayout() {
        int[] actualSize = getActualPixelSize();
        int actualPixelHeight = actualSize[1];
        int entryHeight, entryGap, btnWidth, btnHeight, btnMarginRight, btnOffsetY;
        if (actualPixelHeight >= 1080) {
            entryHeight = 54;
            entryGap = 5;
            btnWidth = 55;
            btnHeight = 20;
            btnMarginRight = 10;
        } else if (actualPixelHeight >= 720) {
            entryHeight = 50;
            entryGap = 4;
            btnWidth = 50;
            btnHeight = 18;
            btnMarginRight = 8;
        } else {
            entryHeight = 44;
            entryGap = 3;
            btnWidth = 44;
            btnHeight = 16;
            btnMarginRight = 6;
        }
        // 按钮垂直居中
        btnOffsetY = (entryHeight - btnHeight) / 2;
        return new int[]{entryHeight, entryGap, btnWidth, btnHeight, btnMarginRight, btnOffsetY};
    }
    /**
     * 誓约列表：计算列表区域布局
     * 返回 [listX, listY, listWidth, listHeight, margin]
     */
    public static int[] calculateVowListAreaLayout(int screenWidth, int screenHeight) {
        int[] actualSize = getActualPixelSize();
        int actualPixelHeight = actualSize[1];
        int margin, topSpace, bottomSpace;
        if (actualPixelHeight >= 1080) {
            margin = 25;
            topSpace = 45;
            bottomSpace = 55;
        } else if (actualPixelHeight >= 720) {
            margin = 20;
            topSpace = 40;
            bottomSpace = 50;
        } else {
            margin = 15;
            topSpace = 35;
            bottomSpace = 45;
        }
        int listX = margin;
        int listY = topSpace;
        int listWidth = screenWidth - margin * 2;
        int listHeight = screenHeight - topSpace - bottomSpace;
        return new int[]{listX, listY, listWidth, listHeight, margin};
    }

    // ==================== 确认对话框 ====================

    public static int[] getDialogSize(int screenWidth, int screenHeight) {
        int width = clamp((int)(screenWidth * 0.55f), 160, 260);
        int height = clamp((int)(screenHeight * 0.22f), 70, 110);
        return new int[]{width, height};
    }

    // ==================== 工具方法 ====================

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int centerX(int screenWidth, int contentWidth) {
        return (screenWidth - contentWidth) / 2;
    }

    public static int centerY(int screenHeight, int contentHeight) {
        return (screenHeight - contentHeight) / 2;
    }
}
