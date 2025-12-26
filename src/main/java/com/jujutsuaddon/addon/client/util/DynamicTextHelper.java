package com.jujutsuaddon.addon.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态文本工具类
 * 处理文本换行、参数格式化、动态描述等
 */
public class DynamicTextHelper {

    // ==================== 文本换行 ====================

    /**
     * 将文本按最大宽度换行
     *
     * @param font     字体
     * @param text     原文本
     * @param maxWidth 最大宽度
     * @return 换行后的文本列表
     */
    public static List<String> wrapText(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();

        // 先按换行符分割
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            // 检查是否包含空格（英文/混合文本）
            if (paragraph.contains(" ")) {
                wrapByWords(font, paragraph, maxWidth, lines);
            } else {
                // 中文等无空格文本，按字符换行
                wrapByChars(font, paragraph, maxWidth, lines);
            }
        }

        return lines;
    }

    /** 按单词换行（适合英文） */
    private static void wrapByWords(Font font, String text, int maxWidth, List<String> lines) {
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            if (currentLine.length() == 0) {
                if (font.width(word) > maxWidth) {
                    // 单词太长，强制截断
                    lines.add(truncateToWidth(font, word, maxWidth));
                } else {
                    currentLine.append(word);
                }
            } else {
                String test = currentLine + " " + word;
                if (font.width(test) > maxWidth) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
    }

    /** 按字符换行（适合中文） */
    private static void wrapByChars(Font font, String text, int maxWidth, List<String> lines) {
        StringBuilder currentLine = new StringBuilder();

        for (char c : text.toCharArray()) {
            String test = currentLine.toString() + c;
            if (font.width(test) > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
            currentLine.append(c);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
    }

    /** 截断文本到指定宽度 */
    public static String truncateToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
    }

    // ==================== 动态行高计算 ====================

    /**
     * 计算多行文本需要的高度（考虑缩放）
     *
     * @param font        字体
     * @param text        文本
     * @param maxWidth    最大宽度（已缩放后的像素宽度）
     * @param scale       文本缩放比例
     * @param lineGap     行间距
     * @param minHeight   最小高度
     * @param verticalPad 垂直内边距（上下各加）
     * @return 计算后的高度
     */
    public static int calculateScaledTextHeight(Font font, String text, int maxWidth,
                                                float scale, int lineGap,
                                                int minHeight, int verticalPad) {
        // 反算缩放前的最大宽度
        int unscaledMaxWidth = (int) (maxWidth / scale);
        List<String> lines = wrapText(font, text, unscaledMaxWidth);

        int scaledLineHeight = (int) (font.lineHeight * scale);
        int textHeight = lines.size() * scaledLineHeight + Math.max(0, lines.size() - 1) * lineGap;

        return Math.max(minHeight, textHeight + verticalPad * 2);
    }

    /**
     * 计算多行文本需要的高度（简化版，默认参数）
     *
     * @param font      字体
     * @param text      文本
     * @param maxWidth  最大宽度（已缩放后的像素宽度）
     * @param scale     文本缩放比例
     * @param minHeight 最小高度
     * @return 计算后的高度
     */
    public static int calculateScaledTextHeight(Font font, String text, int maxWidth,
                                                float scale, int minHeight) {
        return calculateScaledTextHeight(font, text, maxWidth, scale, 1, minHeight, 3);
    }

    // ==================== 多行文本渲染 ====================

    /**
     * 渲染多行文本
     *
     * @param graphics  渲染上下文
     * @param font      字体
     * @param text      文本
     * @param x         起始X
     * @param y         起始Y
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度（超出部分不渲染）
     * @param color     文本颜色
     * @param lineGap   行间距
     * @return 实际渲染的高度
     */
    public static int renderWrappedText(GuiGraphics graphics, Font font, String text,
                                        int x, int y, int maxWidth, int maxHeight,
                                        int color, int lineGap) {
        List<String> lines = wrapText(font, text, maxWidth);
        int lineHeight = font.lineHeight + lineGap;
        int currentY = y;

        for (String line : lines) {
            if (currentY + font.lineHeight > y + maxHeight) break;
            graphics.drawString(font, line, x, currentY, color, false);
            currentY += lineHeight;
        }

        return currentY - y;
    }

    /**
     * 渲染多行文本（默认行间距）
     */
    public static int renderWrappedText(GuiGraphics graphics, Font font, String text,
                                        int x, int y, int maxWidth, int maxHeight, int color) {
        return renderWrappedText(graphics, font, text, x, y, maxWidth, maxHeight, color, 2);
    }

    /**
     * 渲染缩放的多行文本
     *
     * @param graphics 渲染上下文
     * @param font     字体
     * @param text     文本
     * @param x        起始X
     * @param y        起始Y
     * @param maxWidth 最大宽度（缩放后的像素宽度）
     * @param scale    缩放比例
     * @param color    文本颜色
     * @param lineGap  行间距
     * @return 换行后的文本列表
     */
    public static List<String> renderScaledWrappedText(GuiGraphics graphics, Font font, String text,
                                                       int x, int y, int maxWidth,
                                                       float scale, int color, int lineGap) {
        // 反算缩放前的最大宽度
        int unscaledMaxWidth = (int) (maxWidth / scale);
        List<String> lines = wrapText(font, text, unscaledMaxWidth);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1f);

        int lineY = 0;
        for (String line : lines) {
            graphics.drawString(font, line, 0, lineY, color, false);
            lineY += font.lineHeight + (int) (lineGap / scale);
        }

        graphics.pose().popPose();

        return lines;
    }

    /**
     * 渲染缩放的多行文本（默认行间距）
     */
    public static List<String> renderScaledWrappedText(GuiGraphics graphics, Font font, String text,
                                                       int x, int y, int maxWidth,
                                                       float scale, int color) {
        return renderScaledWrappedText(graphics, font, text, x, y, maxWidth, scale, color, 1);
    }

    // ==================== 参数值格式化 ====================

    /**
     * 智能格式化参数值
     *
     * @param key   参数键名
     * @param value 参数值
     * @param isInt 是否是整数类型
     * @return 格式化后的字符串
     */
    public static String formatParamValue(String key, float value, boolean isInt) {
        String lowerKey = key.toLowerCase();

        // 时间类型（小时）
        if (lowerKey.contains("hour") || lowerKey.contains("start_time") || lowerKey.contains("end_time")) {
            int hour = ((int) value) % 24;
            return String.format("%02d:00", hour);
        }

        // 百分比类型
        if (lowerKey.contains("percent") || lowerKey.contains("bonus") ||
                lowerKey.contains("multiplier") || lowerKey.contains("rate") ||
                lowerKey.contains("boost") || lowerKey.contains("reduction")) {
            if (isInt) {
                return String.format("%+d%%", (int) value);
            } else {
                return String.format("%+.0f%%", value);
            }
        }

        // 距离/范围类型
        if (lowerKey.contains("range") || lowerKey.contains("distance") || lowerKey.contains("radius")) {
            if (isInt) {
                return String.format("%dm", (int) value);
            } else {
                return String.format("%.1fm", value);
            }
        }

        // 时间类型（秒/tick）
        if (lowerKey.contains("duration") || lowerKey.contains("cooldown") || lowerKey.contains("time")) {
            if (isInt) {
                int seconds = (int) value;
                if (seconds >= 60) {
                    return String.format("%d:%02d", seconds / 60, seconds % 60);
                }
                return seconds + "s";
            } else {
                return String.format("%.1fs", value);
            }
        }

        // 普通数值
        if (isInt) {
            return String.valueOf((int) value);
        } else {
            // 如果接近整数，显示为整数
            if (Math.abs(value - Math.round(value)) < 0.01f) {
                return String.valueOf((int) value);
            }
            return String.format("%.1f", value);
        }
    }

    // ==================== 动态描述渲染 ====================

    /**
     * 在区域内渲染动态描述
     *
     * @param graphics    渲染上下文
     * @param font        字体
     * @param description 描述文本
     * @param x           区域X
     * @param y           区域Y
     * @param width       区域宽度
     * @param height      区域高度
     * @param textColor   文本颜色
     * @param bgColor     背景颜色（0表示不绘制背景）
     */
    public static void renderDescriptionArea(GuiGraphics graphics, Font font,
                                             Component description,
                                             int x, int y, int width, int height,
                                             int textColor, int bgColor) {
        // 背景
        if (bgColor != 0) {
            graphics.fill(x, y, x + width, y + height, bgColor);
        }

        // 文本
        String text = description.getString();
        renderWrappedText(graphics, font, text, x + 4, y + 4, width - 8, height - 8, textColor);
    }

    /**
     * 渲染带标题的描述区域
     */
    public static void renderTitledDescriptionArea(GuiGraphics graphics, Font font,
                                                   String title, Component description,
                                                   int x, int y, int width, int height,
                                                   int titleColor, int textColor, int bgColor) {
        if (bgColor != 0) {
            graphics.fill(x, y, x + width, y + height, bgColor);
        }

        // 标题
        graphics.drawString(font, title, x + 4, y + 4, titleColor, false);

        // 描述
        String text = description.getString();
        renderWrappedText(graphics, font, text, x + 4, y + 16, width - 8, height - 20, textColor);
    }

    // ==================== 计算文本高度 ====================

    /**
     * 计算换行后的文本总高度
     */
    public static int calculateWrappedTextHeight(Font font, String text, int maxWidth, int lineGap) {
        List<String> lines = wrapText(font, text, maxWidth);
        return lines.size() * (font.lineHeight + lineGap);
    }
}
