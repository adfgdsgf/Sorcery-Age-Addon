package com.jujutsuaddon.addon.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * GUI 控件渲染工具类
 * 可复用的滑块、复选框等控件渲染
 */
public class GuiControlHelper {

    // ==================== 滑块渲染 ====================

    /**
     * 渲染水平滑块
     *
     * @param graphics   渲染上下文
     * @param x          左上角X
     * @param y          左上角Y
     * @param width      滑块宽度
     * @param height     滑块高度
     * @param ratio      当前值比例 (0-1)
     * @param isHovered  是否悬停
     * @param isDragging 是否拖动中
     */
    public static void renderSlider(GuiGraphics graphics, int x, int y, int width, int height,
                                    float ratio, boolean isHovered, boolean isDragging) {
        ratio = Mth.clamp(ratio, 0, 1);

        // 轨道
        int trackColor = isHovered || isDragging ? 0xFF555555 : 0xFF444444;
        graphics.fill(x, y + 4, x + width, y + height - 2, trackColor);

        // 填充
        int fillWidth = (int) (width * ratio);
        int fillColor = isDragging ? 0xFF6ECF6E : (isHovered ? 0xFF5CBF5C : 0xFF4CAF50);
        graphics.fill(x, y + 4, x + fillWidth, y + height - 2, fillColor);

        // 手柄
        int handleX = Mth.clamp(x + fillWidth - 3, x, x + width - 6);
        int handleColor = isDragging ? 0xFFFFFFFF : 0xFFDDDDDD;
        graphics.fill(handleX, y + 1, handleX + 6, y + height - 1, handleColor);
    }

    /**
     * 渲染滑块带数值显示
     */
    public static void renderSliderWithValue(GuiGraphics graphics, Font font,
                                             int x, int y, int width, int height,
                                             float ratio, String valueStr,
                                             boolean isHovered, boolean isDragging,
                                             int textColor) {
        renderSlider(graphics, x, y, width, height, ratio, isHovered, isDragging);
        graphics.drawString(font, valueStr, x + width + 6, y + 2, textColor);
    }

    /**
     * 计算滑块值
     *
     * @param mouseX   鼠标X
     * @param sliderX  滑块左边缘X
     * @param width    滑块宽度
     * @param min      最小值
     * @param max      最大值
     * @param asInt    是否返回整数
     * @return 计算后的值
     */
    public static Number calculateSliderValue(double mouseX, int sliderX, int width,
                                              float min, float max, boolean asInt) {
        float ratio = (float) (mouseX - sliderX) / width;
        ratio = Mth.clamp(ratio, 0, 1);
        float value = min + ratio * (max - min);

        if (asInt) {
            return Math.round(value);
        } else {
            return Math.round(value * 10f) / 10f;
        }
    }

    /**
     * 检查鼠标是否在滑块区域内
     */
    public static boolean isInSlider(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // ==================== 复选框渲染 ====================

    /**
     * 渲染复选框
     *
     * @param graphics  渲染上下文
     * @param font      字体
     * @param x         左上角X
     * @param y         左上角Y
     * @param size      尺寸
     * @param checked   是否选中
     * @param isHovered 是否悬停
     */
    public static void renderCheckbox(GuiGraphics graphics, Font font,
                                      int x, int y, int size,
                                      boolean checked, boolean isHovered) {
        // 边框
        int borderColor = isHovered ? 0xFFAAAAAA : 0xFF888888;
        graphics.renderOutline(x, y, size, size, borderColor);
        // 背景
        if (checked) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF4CAF50);
        } else {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF333333);
        }
    }

    /**
     * 渲染带标签的复选框
     */
    public static void renderCheckboxWithLabel(GuiGraphics graphics, Font font,
                                               int x, int y, int size, String label,
                                               boolean checked, boolean isHovered,
                                               int labelColor) {
        renderCheckbox(graphics, font, x, y, size, checked, isHovered);

        // ★ 标签垂直居中
        int labelY = y + (size - font.lineHeight) / 2;
        graphics.drawString(font, label, x + size + 4, labelY, labelColor);
    }

    /**
     * 检查鼠标是否在复选框区域内
     */
    public static boolean isInCheckbox(double mouseX, double mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    // ==================== 进度条渲染 ====================

    /**
     * 渲染水平进度条
     */
    public static void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height,
                                         float progress, int bgColor, int fillColor, int borderColor) {
        progress = Mth.clamp(progress, 0, 1);

        // 背景
        graphics.fill(x, y, x + width, y + height, bgColor);

        // 填充
        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, fillColor);
        }

        // 边框
        graphics.renderOutline(x, y, width, height, borderColor);
    }

    /**
     * 渲染带文字的进度条
     */
    public static void renderProgressBarWithText(GuiGraphics graphics, Font font,
                                                 int x, int y, int width, int height,
                                                 float progress, String text,
                                                 int bgColor, int fillColor, int borderColor,
                                                 int textColor) {
        renderProgressBar(graphics, x, y, width, height, progress, bgColor, fillColor, borderColor);

        // 文字居中
        int textX = x + (width - font.width(text)) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(font, text, textX, textY, textColor);
    }

    // ==================== 按钮样式 ====================

    /**
     * 渲染简单图标按钮背景
     */
    public static void renderIconButtonBackground(GuiGraphics graphics, int x, int y, int size,
                                                  boolean isHovered, boolean isPressed) {
        int bgColor = isPressed ? 0xFF3A3A5A : (isHovered ? 0xFF4A4A6A : 0xFF2A2A4A);
        int borderColor = isPressed ? 0xFFAA88FF : (isHovered ? 0xFF8866CC : 0xFF6644AA);

        graphics.fill(x, y, x + size, y + size, bgColor);
        graphics.renderOutline(x, y, size, size, borderColor);
    }
    // ==================== 通用区域检测 ====================
    /**
     * 检查鼠标是否在矩形区域内
     */
    public static boolean isInArea(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    /**
     * 检查鼠标是否在矩形区域内（int版本）
     */
    public static boolean isInArea(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
