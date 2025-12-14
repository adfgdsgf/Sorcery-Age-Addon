package com.jujutsuaddon.addon.client.util;

import com.jujutsuaddon.addon.client.config.AddonClientConfig.AnchorPoint;

/**
 * HUD 定位工具类 - 可复用的位置计算逻辑
 */
public class HudPositionHelper {

    /**
     * 计算 HUD 在屏幕上的位置
     *
     * @param anchor          锚点
     * @param offsetXPermille 水平偏移（千分比，-500~500）
     * @param offsetYPermille 垂直偏移（千分比，-500~500）
     * @param scale           缩放比例
     * @param screenWidth     屏幕宽度（GUI缩放后）
     * @param screenHeight    屏幕高度（GUI缩放后）
     * @param contentWidth    内容宽度（缩放前）
     * @param contentHeight   内容高度（缩放前）
     * @param margin          边缘距离
     * @param hotbarOffset    快捷栏偏移（底部锚点时使用）
     * @return [x, y] 位置
     */
    public static int[] calculatePosition(AnchorPoint anchor,
                                          int offsetXPermille, int offsetYPermille,
                                          float scale,
                                          int screenWidth, int screenHeight,
                                          int contentWidth, int contentHeight,
                                          int margin, int hotbarOffset) {
        // 缩放后的内容尺寸
        int scaledWidth = (int) (contentWidth * scale);
        int scaledHeight = (int) (contentHeight * scale);

        // 偏移百分比转像素
        int offsetX = (int) (screenWidth * offsetXPermille / 1000.0);
        int offsetY = (int) (screenHeight * offsetYPermille / 1000.0);

        int x, y;
        switch (anchor) {
            case TOP_LEFT -> {
                x = margin;
                y = margin;
            }
            case TOP_CENTER -> {
                x = (screenWidth - scaledWidth) / 2;
                y = margin;
            }
            case TOP_RIGHT -> {
                x = screenWidth - scaledWidth - margin;
                y = margin;
            }
            case CENTER_LEFT -> {
                x = margin;
                y = (screenHeight - scaledHeight) / 2;
            }
            case CENTER -> {
                x = (screenWidth - scaledWidth) / 2;
                y = (screenHeight - scaledHeight) / 2;
            }
            case CENTER_RIGHT -> {
                x = screenWidth - scaledWidth - margin;
                y = (screenHeight - scaledHeight) / 2;
            }
            case BOTTOM_LEFT -> {
                x = margin;
                y = screenHeight - scaledHeight - margin - hotbarOffset;
            }
            case BOTTOM_CENTER -> {
                x = (screenWidth - scaledWidth) / 2;
                y = screenHeight - scaledHeight - margin - hotbarOffset;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - scaledWidth - margin;
                y = screenHeight - scaledHeight - margin - hotbarOffset;
            }
            default -> {
                x = margin;
                y = margin;
            }
        }

        return new int[]{x + offsetX, y + offsetY};
    }

    /**
     * 简化版本（默认快捷栏偏移40）
     */
    public static int[] calculatePosition(AnchorPoint anchor,
                                          int offsetXPermille, int offsetYPermille,
                                          float scale,
                                          int screenWidth, int screenHeight,
                                          int contentWidth, int contentHeight,
                                          int margin) {
        return calculatePosition(anchor, offsetXPermille, offsetYPermille, scale,
                screenWidth, screenHeight, contentWidth, contentHeight, margin, 40);
    }

    /**
     * 获取锚点基准位置（不含偏移）
     */
    public static int[] getAnchorBasePosition(AnchorPoint anchor,
                                              float scale,
                                              int screenWidth, int screenHeight,
                                              int contentWidth, int contentHeight,
                                              int margin, int hotbarOffset) {
        return calculatePosition(anchor, 0, 0, scale,
                screenWidth, screenHeight, contentWidth, contentHeight, margin, hotbarOffset);
    }

    /**
     * 将像素偏移转换为千分比
     */
    public static int pixelToPermille(int pixelOffset, int screenSize) {
        return (int) (pixelOffset * 1000.0 / screenSize);
    }

    /**
     * 限制偏移范围
     */
    public static int clampPermille(int permille) {
        return Math.max(-500, Math.min(500, permille));
    }
}
