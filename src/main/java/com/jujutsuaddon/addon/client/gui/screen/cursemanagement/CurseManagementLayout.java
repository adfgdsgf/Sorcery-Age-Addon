package com.jujutsuaddon.addon.client.gui.screen.cursemanagement;

import com.jujutsuaddon.addon.client.util.UIScaleHelper;

/**
 * 咒灵管理界面布局
 */
public class CurseManagementLayout {

    private final int screenWidth;
    private final int screenHeight;

    private int columns;
    private int rows;
    private int itemsPerPage;
    private int cellSize;
    private int cellPadding;
    private int gridStartX;
    private int gridStartY;
    private int gridWidth;
    private int gridHeight;

    public CurseManagementLayout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        calculate();
    }

    private void calculate() {
        int[] gridConfig = UIScaleHelper.calculateCurseGridLayout(screenWidth, screenHeight);
        columns = gridConfig[0];
        rows = gridConfig[1];
        cellSize = gridConfig[2];
        cellPadding = gridConfig[3];
        itemsPerPage = columns * rows;

        gridWidth = columns * cellSize + (columns - 1) * cellPadding;
        gridHeight = rows * cellSize + (rows - 1) * cellPadding;

        gridStartX = (screenWidth - gridWidth) / 2;
        gridStartY = 45;
    }

    // ==================== Getters ====================

    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public int getItemsPerPage() { return itemsPerPage; }
    public int getCellSize() { return cellSize; }
    public int getCellPadding() { return cellPadding; }
    public int getGridStartX() { return gridStartX; }
    public int getGridStartY() { return gridStartY; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }
    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }

    /**
     * 获取指定索引的格子位置
     */
    public int[] getCellPosition(int localIndex) {
        int col = localIndex % columns;
        int row = localIndex / columns;
        int cellX = gridStartX + col * (cellSize + cellPadding);
        int cellY = gridStartY + row * (cellSize + cellPadding);
        return new int[]{cellX, cellY};
    }

    /**
     * 检查鼠标是否在指定格子内
     */
    public boolean isInCell(double mouseX, double mouseY, int cellX, int cellY) {
        return mouseX >= cellX && mouseX < cellX + cellSize &&
                mouseY >= cellY && mouseY < cellY + cellSize;
    }

    /**
     * 根据鼠标位置获取本地索引（当前页内的索引）
     */
    public int getLocalIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < itemsPerPage; i++) {
            int[] pos = getCellPosition(i);
            if (isInCell(mouseX, mouseY, pos[0], pos[1])) {
                return i;
            }
        }
        return -1;
    }
}
