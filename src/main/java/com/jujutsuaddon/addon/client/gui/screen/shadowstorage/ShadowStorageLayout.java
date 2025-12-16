package com.jujutsuaddon.addon.client.gui.screen.shadowstorage;

import com.jujutsuaddon.addon.client.util.UIScaleHelper;

/**
 * 影子库存界面布局计算
 */
public class ShadowStorageLayout {

    // ==================== 常量 ====================

    public static final int SLOT_SIZE = 18;
    public static final int COLS = 9;
    public static final int SHADOW_ROWS = 6;
    public static final int PADDING = 7;
    public static final int CONTAINER_WIDTH = 176;
    public static final int CONTAINER_HEIGHT = 222;

    // ==================== 实例字段 ====================

    private final int screenWidth;
    private final int screenHeight;
    private final int leftPos;
    private final int topPos;

    // 侧边按钮布局缓存
    private final int sideButtonSize;
    private final int sideButtonGap;
    private final int sideButtonContainerGap;

    public ShadowStorageLayout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.leftPos = (screenWidth - CONTAINER_WIDTH) / 2;
        this.topPos = (screenHeight - CONTAINER_HEIGHT) / 2;

        int[] sideLayout = UIScaleHelper.calculateShadowStorageSideButtonLayout();
        this.sideButtonSize = sideLayout[0];
        this.sideButtonGap = sideLayout[1];
        this.sideButtonContainerGap = sideLayout[2];
    }

    // ==================== 位置获取 ====================

    public int getLeftPos() { return leftPos; }
    public int getTopPos() { return topPos; }

    public int getShadowGridX() { return leftPos + PADDING + 1; }
    public int getShadowGridY() { return topPos + 18; }
    public int getShadowGridWidth() { return COLS * SLOT_SIZE; }
    public int getShadowGridHeight() { return SHADOW_ROWS * SLOT_SIZE; }

    public int getPlayerInvY() { return topPos + 18 + SHADOW_ROWS * SLOT_SIZE + 14; }
    public int getPlayerHotbarY() { return getPlayerInvY() + 3 * SLOT_SIZE + 4; }

    public int getSideButtonX() {
        return leftPos - sideButtonSize - sideButtonContainerGap;
    }
    public int getSideButtonSize() { return sideButtonSize; }
    public int getSideButtonGap() { return sideButtonGap; }

    public int getScrollbarX() { return leftPos + CONTAINER_WIDTH - 5; }

    // ==================== 索引计算 ====================

    /**
     * 获取影子库存槽位索引
     * @return -1 如果不在范围内
     */
    public int getShadowSlotIndex(double mouseX, double mouseY, int scrollRow) {
        int gridX = getShadowGridX();
        int gridY = getShadowGridY();

        if (mouseX < gridX || mouseX >= gridX + getShadowGridWidth() ||
                mouseY < gridY || mouseY >= gridY + getShadowGridHeight()) {
            return -1;
        }

        int col = (int) (mouseX - gridX) / SLOT_SIZE;
        int row = (int) (mouseY - gridY) / SLOT_SIZE;
        return scrollRow * COLS + row * COLS + col;
    }

    /**
     * 获取玩家背包槽位索引
     * @return -1 如果不在范围内
     */
    public int getPlayerSlotIndex(double mouseX, double mouseY) {
        int gridX = getShadowGridX();

        // 主背包 (9-35)
        int invY = getPlayerInvY();
        if (mouseX >= gridX && mouseX < gridX + getShadowGridWidth() &&
                mouseY >= invY && mouseY < invY + 3 * SLOT_SIZE) {
            int col = (int) (mouseX - gridX) / SLOT_SIZE;
            int row = (int) (mouseY - invY) / SLOT_SIZE;
            return 9 + row * 9 + col;
        }

        // 快捷栏 (0-8)
        int hotbarY = getPlayerHotbarY();
        if (mouseX >= gridX && mouseX < gridX + getShadowGridWidth() &&
                mouseY >= hotbarY && mouseY < hotbarY + SLOT_SIZE) {
            return (int) (mouseX - gridX) / SLOT_SIZE;
        }

        return -1;
    }

    /**
     * 检查是否在滚动条区域
     */
    public boolean isInScrollbar(double mouseX, double mouseY) {
        int scrollX = getScrollbarX();
        int gridY = getShadowGridY();
        return mouseX >= scrollX - 3 && mouseX <= scrollX + 3
                && mouseY >= gridY && mouseY < gridY + getShadowGridHeight();
    }
}
