package com.jujutsuaddon.addon.client.gui.widget.vow;

import com.jujutsuaddon.addon.client.gui.util.VowGuiColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 权重平衡条组件 - 简化版
 */
public class WeightBarWidget extends AbstractWidget {

    private float conditionWeight = 0f;
    private float benefitCost = 0f;

    private static final float TOLERANCE = 0.05f;
    private final Font font;

    public WeightBarWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.font = Minecraft.getInstance().font;
    }

    public void setWeights(float conditionWeight, float benefitCost) {
        this.conditionWeight = Math.max(0, conditionWeight);
        this.benefitCost = Math.max(0, benefitCost);
    }

    public boolean isBalanced() {
        if (conditionWeight <= 0) return false;
        return benefitCost <= conditionWeight * (1 + TOLERANCE);
    }

    public boolean isOverflow() {
        return benefitCost > conditionWeight * (1 + TOLERANCE);
    }

    public float getRemainingWeight() {
        return Math.max(0, conditionWeight - benefitCost);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        // 背景
        graphics.fill(x, y, x + w, y + h, VowGuiColors.WEIGHT_BAR_BG);
        graphics.renderOutline(x, y, w, h, VowGuiColors.PANEL_BORDER);

        int innerX = x + 2;
        int innerY = y + 2;
        int innerW = w - 4;
        int innerH = h - 4;

        if (conditionWeight <= 0 && benefitCost <= 0) {
            // 空状态：只显示简单提示
            Component hint = Component.translatable("widget.jujutsuaddon.weight.hint");
            int hintWidth = font.width(hint);
            graphics.drawString(font, hint, x + (w - hintWidth) / 2, y + (h - 8) / 2,
                    VowGuiColors.TEXT_SECONDARY, false);
            return;
        }

        // 渲染进度条
        renderBar(graphics, innerX, innerY, innerW, innerH);

        // 渲染数值（条内显示）
        renderValues(graphics, x, y, w, h);
    }

    private void renderBar(GuiGraphics graphics, int x, int y, int w, int h) {
        float maxValue = Math.max(conditionWeight, benefitCost);
        if (maxValue <= 0) return;

        // 条件权重（蓝色底）
        float weightRatio = conditionWeight / maxValue;
        int weightWidth = (int)(w * weightRatio);
        graphics.fill(x, y, x + weightWidth, y + h, VowGuiColors.WEIGHT_CONDITION);

        // 收益消耗（覆盖）
        float costRatio = benefitCost / maxValue;
        int costWidth = (int)(w * costRatio);

        if (isOverflow()) {
            graphics.fill(x, y, x + costWidth, y + h, VowGuiColors.WEIGHT_OVERFLOW);
        } else {
            graphics.fill(x, y, x + costWidth, y + h, VowGuiColors.WEIGHT_BENEFIT);
            // 剩余空间
            if (costWidth < weightWidth) {
                graphics.fill(x + costWidth, y, x + weightWidth, y + h, VowGuiColors.WEIGHT_REMAINING);
            }
        }
    }

    private void renderValues(GuiGraphics graphics, int x, int y, int w, int h) {
        int textY = y + (h - 8) / 2;

        // 左侧：可用权重
        String leftStr = String.format("%.1f", conditionWeight);
        graphics.drawString(font, leftStr, x + 4, textY, 0xFF55FFFF, true);

        // 右侧：消耗 / 可用
        String rightStr = String.format("%.1f / %.1f", benefitCost, conditionWeight);
        int rightColor = isOverflow() ? 0xFFFF5555 : 0xFFFFFFFF;
        int rightWidth = font.width(rightStr);
        graphics.drawString(font, rightStr, x + w - rightWidth - 4, textY, rightColor, true);

        // 中间：剩余或超出
        if (isOverflow()) {
            String overStr = String.format("-%.1f", benefitCost - conditionWeight);
            int overWidth = font.width(overStr);
            graphics.drawString(font, overStr, x + (w - overWidth) / 2, textY, 0xFFFF5555, true);
        } else if (getRemainingWeight() > 0) {
            String remainStr = String.format("+%.1f", getRemainingWeight());
            int remainWidth = font.width(remainStr);
            graphics.drawString(font, remainStr, x + (w - remainWidth) / 2, textY, 0xFF55FF55, true);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }

    public float getConditionWeight() {
        return conditionWeight;
    }

    public float getBenefitCost() {
        return benefitCost;
    }
}
