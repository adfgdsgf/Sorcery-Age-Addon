package com.jujutsuaddon.addon.client.util;

/**
 * 数字格式化工具类
 */
public class NumberFormatHelper {

    /**
     * 格式化大数字（标题栏统计用）
     */
    public static String formatLargeCount(long count) {
        if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }

    /**
     * 格式化槽位数量（物品格子显示用）
     */
    public static String formatSlotCount(int count) {
        if (count >= 1_000_000) {
            return String.format("%.0fM", count / 1_000_000.0);
        } else if (count >= 100_000) {
            return String.format("%.0fK", count / 1_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }

    /**
     * 根据数量获取颜色
     */
    public static int getCountColor(int count) {
        if (count >= 1_000_000) return 0xFFFF55;  // 金色
        if (count >= 100_000) return 0xFF55FF;   // 粉色
        if (count >= 10_000) return 0x55FFFF;    // 青色
        if (count >= 1_000) return 0x55FF55;     // 绿色
        if (count >= 100) return 0xFFFF55;       // 黄色
        return 0xFFFFFF;                          // 白色
    }
}
