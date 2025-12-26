package com.jujutsuaddon.addon.client.util;

import com.jujutsuaddon.addon.api.vow.IBenefit;
import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.condition.types.ordinary.OvertimeCondition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 誓约GUI通用工具类
 * 提供誓约界面共用的渲染、格式化、说明文本等
 */
public class VowGuiHelper {

    // ==================== 权重系统说明 ====================

    /**
     * 获取权重系统完整说明文本（多行）
     */
    public static List<Component> getWeightExplanationLines() {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.translatable("vow.guide.title")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.empty());

        lines.add(Component.translatable("vow.guide.what_is_weight.title")
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("vow.guide.what_is_weight.desc1")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("vow.guide.what_is_weight.desc2")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());

        lines.add(Component.translatable("vow.guide.how_it_works.title")
                .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("vow.guide.how_it_works.step1")
                .withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable("vow.guide.how_it_works.step2")
                .withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable("vow.guide.how_it_works.step3")
                .withStyle(ChatFormatting.WHITE));
        lines.add(Component.empty());

        lines.add(Component.translatable("vow.guide.condition_types.title")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("vow.guide.condition_types.violation")
                .withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("vow.guide.condition_types.modifier")
                .withStyle(ChatFormatting.GOLD));
        lines.add(Component.empty());

        lines.add(Component.translatable("vow.guide.tips.title")
                .withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("vow.guide.tips.tip1")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("vow.guide.tips.tip2")
                .withStyle(ChatFormatting.GRAY));

        return lines;
    }

    /**
     * 获取简短的权重说明（用于tooltip）
     */
    public static List<Component> getWeightTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("vow.tooltip.weight.title")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        lines.add(Component.translatable("vow.tooltip.weight.line1")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("vow.tooltip.weight.line2")
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    // ==================== 条件类型判断与显示 ====================

    /**
     * 条件类型枚举
     */
    public enum ConditionType {
        /** 硬性限制：违反触发惩罚 */
        VIOLATION(ChatFormatting.RED, "⚠"),
        /** 收益调节：动态调整收益效果 */
        MODIFIER(ChatFormatting.GOLD, "✦");

        public final ChatFormatting color;
        public final String icon;

        ConditionType(ChatFormatting color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    /**
     * 判断条件类型
     */
    public static ConditionType getConditionType(ICondition condition) {
        // OvertimeCondition 是调节型
        if (condition instanceof OvertimeCondition) {
            return ConditionType.MODIFIER;
        }
        // 默认是硬性限制
        return ConditionType.VIOLATION;
    }

    /**
     * 获取条件类型标签
     */
    public static Component getConditionTypeLabel(ICondition condition) {
        ConditionType type = getConditionType(condition);
        return Component.literal(type.icon + " ")
                .append(Component.translatable("vow.condition_type." + type.name().toLowerCase()))
                .withStyle(type.color);
    }

    /**
     * 获取条件类型的详细说明
     */
    public static Component getConditionTypeDescription(ICondition condition) {
        ConditionType type = getConditionType(condition);
        return Component.translatable("vow.condition_type." + type.name().toLowerCase() + ".desc")
                .withStyle(ChatFormatting.GRAY);
    }

    // ==================== 权重格式化 ====================

    /**
     * 格式化权重值（带颜色）
     */
    public static Component formatWeight(float weight) {
        String sign = weight >= 0 ? "+" : "";
        return Component.literal(sign + String.format("%.1f", weight))
                .withStyle(ChatFormatting.AQUA);
    }

    /**
     * 格式化消耗值（带颜色）
     */
    public static Component formatCost(float cost) {
        return Component.literal("-" + String.format("%.1f", cost))
                .withStyle(ChatFormatting.GOLD);
    }

    /**
     * 格式化剩余权重（带颜色）
     */
    public static Component formatRemaining(float remaining) {
        ChatFormatting color = remaining >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        String sign = remaining >= 0 ? "+" : "";
        return Component.literal(sign + String.format("%.1f", remaining))
                .withStyle(color);
    }

    /**
     * 获取条件的权重预览文本（用于列表显示）
     */
    public static Component getConditionWeightPreview(ICondition condition) {
        ConditionParams defaultParams = condition.createDefaultParams();
        float weight = condition.calculateWeight(defaultParams);

        MutableComponent text = Component.literal(String.format("+%.1f", weight))
                .withStyle(ChatFormatting.AQUA);

        // 如果是调节型条件，标注上限
        if (condition instanceof OvertimeCondition) {
            text.append(Component.literal(" ")
                    .append(Component.translatable("vow.weight.capped"))
                    .withStyle(ChatFormatting.GRAY));
        }

        return text;
    }

    // ==================== 誓约状态显示 ====================

    /**
     * 获取状态图标
     */
    public static String getStateIcon(VowState state) {
        return switch (state) {
            case INACTIVE -> "○";
            case ACTIVE -> "●";
            case DISSOLVED -> "◇";
            case VIOLATED -> "✗";
            default -> "?";  // 添加 default
        };
    }
    /**
     * 获取状态颜色
     */
    public static ChatFormatting getStateColor(VowState state) {
        return switch (state) {
            case INACTIVE -> ChatFormatting.GRAY;
            case ACTIVE -> ChatFormatting.GREEN;
            case DISSOLVED -> ChatFormatting.YELLOW;
            case VIOLATED -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE;  // 添加 default
        };
    }

    /**
     * 获取带图标的状态文本
     */
    public static Component getStateLabel(VowState state) {
        return Component.literal(getStateIcon(state) + " ")
                .append(Component.translatable("vow.state." + state.name().toLowerCase()))
                .withStyle(getStateColor(state));
    }

    // ==================== 誓约类型显示 ====================

    /**
     * 获取类型颜色
     */
    public static ChatFormatting getTypeColor(VowType type) {
        return type == VowType.PERMANENT ? ChatFormatting.RED : ChatFormatting.GREEN;
    }

    /**
     * 获取类型标签
     */
    public static Component getTypeLabel(VowType type) {
        String icon = type == VowType.PERMANENT ? "🔒" : "🔓";
        return Component.literal(icon + " ")
                .append(Component.translatable("vow.type." + type.name().toLowerCase()))
                .withStyle(getTypeColor(type));
    }

    // ==================== 誓约摘要 ====================

    /**
     * 生成誓约摘要信息
     */
    public static List<Component> getVowSummary(CustomBindingVow vow) {
        List<Component> lines = new ArrayList<>();

        // 名称
        lines.add(Component.literal(vow.getName())
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        // 类型和状态
        lines.add(getTypeLabel(vow.getType())
                .copy().append("  ")
                .append(getStateLabel(vow.getState())));

        // 权重信息
        float totalWeight = vow.calculateTotalWeight();
        float totalCost = vow.calculateTotalCost();
        float remaining = vow.calculateRemainingWeight();

        lines.add(Component.translatable("vow.summary.weight",
                formatWeight(totalWeight),
                formatCost(totalCost),
                formatRemaining(remaining)));

        // 条件/收益数量
        lines.add(Component.translatable("vow.summary.counts",
                        vow.getConditions().size(),
                        vow.getBenefits().size())
                .withStyle(ChatFormatting.GRAY));

        return lines;
    }

    // ==================== 渲染辅助 ====================

    /**
     * 渲染权重条（简化版，用于列表项）
     */
    public static void renderMiniWeightBar(GuiGraphics graphics, int x, int y, int width, int height,
                                           float totalWeight, float usedWeight) {
        // 背景
        graphics.fill(x, y, x + width, y + height, 0xFF333333);

        if (totalWeight <= 0) return;

        // 条件权重（蓝色）
        graphics.fill(x, y, x + width, y + height, 0xFF2255AA);

        // 已消耗（橙色覆盖）
        float ratio = Math.min(1.0f, usedWeight / totalWeight);
        int usedWidth = (int) (width * ratio);
        graphics.fill(x, y, x + usedWidth, y + height, 0xFFCC8833);

        // 边框
        graphics.renderOutline(x, y, width, height, 0xFF555555);
    }

    /**
     * 渲染条件类型图标
     */
    public static void renderConditionTypeIcon(GuiGraphics graphics, Font font,
                                               ICondition condition, int x, int y) {
        ConditionType type = getConditionType(condition);
        int color = type.color.getColor() != null ? type.color.getColor() : 0xFFFFFF;
        graphics.drawString(font, type.icon, x, y, color);
    }

    // ==================== Tooltip 辅助 ====================

    /**
     * 获取条件的详细tooltip
     */
    public static List<Component> getConditionTooltip(ICondition condition, ConditionParams params) {
        List<Component> lines = new ArrayList<>();

        // 名称
        lines.add(condition.getDisplayName().copy()
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        // 类型标签
        lines.add(getConditionTypeLabel(condition));

        // 描述
        lines.add(condition.getDescription(params).copy()
                .withStyle(ChatFormatting.GRAY));

        // 权重
        float weight = condition.calculateWeight(params);
        lines.add(Component.empty());
        lines.add(Component.translatable("vow.tooltip.provides_weight",
                formatWeight(weight)));

        // 类型说明
        lines.add(getConditionTypeDescription(condition));

        return lines;
    }

    /**
     * 获取收益的详细tooltip
     */
    public static List<Component> getBenefitTooltip(BenefitEntry entry) {
        List<Component> lines = new ArrayList<>();

        IBenefit benefit = entry.getBenefit();

        // 名称
        lines.add(benefit.getDisplayName().copy()
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        // 描述
        lines.add(benefit.getDescription(entry.getParams()).copy()
                .withStyle(ChatFormatting.GRAY));

        // 消耗
        float cost = benefit.getRequiredWeight(entry.getParams());
        lines.add(Component.empty());
        lines.add(Component.translatable("vow.tooltip.requires_weight",
                formatCost(cost)));

        return lines;
    }
}
