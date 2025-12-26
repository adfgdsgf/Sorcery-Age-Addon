package com.jujutsuaddon.addon.client.gui.screen.vow;

import com.jujutsuaddon.addon.client.gui.util.VowGuiColors;
import com.jujutsuaddon.addon.client.util.VowGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 权重系统说明界面
 * Weight System Explanation Screen
 */
public class WeightExplanationScreen extends Screen {

    private final Screen parent;
    private List<Component> explanationLines;

    // 布局
    private int contentX, contentY, contentWidth, contentHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 15;

    public WeightExplanationScreen(Screen parent) {
        super(Component.translatable("screen.jujutsuaddon.weight_explanation"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // 获取说明文本
        this.explanationLines = VowGuiHelper.getWeightExplanationLines();

        // 计算布局 - 居中弹窗
        contentWidth = Math.min(320, this.width - 40);
        contentHeight = Math.min(240, this.height - 60);
        contentX = (this.width - contentWidth) / 2;
        contentY = (this.height - contentHeight) / 2;

        // 计算最大滚动
        int totalHeight = explanationLines.size() * LINE_HEIGHT;
        int viewHeight = contentHeight - PADDING * 2 - 30; // 减去按钮空间
        maxScroll = Math.max(0, totalHeight - viewHeight);

        // 关闭按钮
        int btnWidth = 80;
        int btnX = contentX + (contentWidth - btnWidth) / 2;
        int btnY = contentY + contentHeight - 25;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        btn -> onClose())
                .bounds(btnX, btnY, btnWidth, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 半透明背景遮罩
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        // 弹窗背景
        graphics.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight,
                0xFF1A1A2E);
        graphics.renderOutline(contentX, contentY, contentWidth, contentHeight,
                0xFF4A4A6A);

        // 标题
        graphics.drawCenteredString(this.font, this.title,
                contentX + contentWidth / 2, contentY + 8, VowGuiColors.TEXT_TITLE);

        // 分隔线
        graphics.fill(contentX + 10, contentY + 22,
                contentX + contentWidth - 10, contentY + 23, 0xFF4A4A6A);

        // 内容区域（裁剪）
        int textAreaY = contentY + 28;
        int textAreaHeight = contentHeight - 58;

        graphics.enableScissor(contentX + PADDING, textAreaY,
                contentX + contentWidth - PADDING, textAreaY + textAreaHeight);

        // 渲染说明文本
        int y = textAreaY - scrollOffset;
        for (Component line : explanationLines) {
            if (y + LINE_HEIGHT > textAreaY && y < textAreaY + textAreaHeight) {
                graphics.drawString(this.font, line, contentX + PADDING, y, 0xFFFFFF);
            }
            y += LINE_HEIGHT;
        }

        graphics.disableScissor();

        // 滚动条
        if (maxScroll > 0) {
            renderScrollbar(graphics, textAreaY, textAreaHeight);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderScrollbar(GuiGraphics graphics, int areaY, int areaHeight) {
        int scrollbarX = contentX + contentWidth - 8;
        int scrollbarWidth = 4;

        // 滚动条轨道
        graphics.fill(scrollbarX, areaY, scrollbarX + scrollbarWidth, areaY + areaHeight,
                0xFF333344);

        // 滚动条滑块
        float ratio = (float) scrollOffset / maxScroll;
        int thumbHeight = Math.max(20, areaHeight * areaHeight / (areaHeight + maxScroll));
        int thumbY = areaY + (int) ((areaHeight - thumbHeight) * ratio);

        graphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight,
                0xFF6666AA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 20));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
