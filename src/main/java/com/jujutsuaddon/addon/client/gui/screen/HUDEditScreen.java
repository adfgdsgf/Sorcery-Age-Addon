package com.jujutsuaddon.addon.client.gui.screen;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.config.AddonClientConfig.AnchorPoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class HUDEditScreen extends Screen {

    private int previewX, previewY;
    private int dragOffsetX, dragOffsetY;
    private boolean isDragging = false;

    // 临时值（拖动时使用，保存前不会写入配置）
    private AnchorPoint tempAnchor;
    private int tempOffsetX, tempOffsetY;
    private boolean tempHorizontal;  // 新增：布局方向

    // 预览图标数量
    private static final int PREVIEW_ICON_COUNT = 5;
    private static final int ICON_SIZE = 18;
    private static final int ICON_PADDING = 5;
    private static final int TEXT_HEIGHT = 12;

    public HUDEditScreen() {
        super(Component.translatable("gui.jujutsu_addon.hud_edit.title"));
    }

    @Override
    protected void init() {
        super.init();

        // 读取当前配置
        tempAnchor = AddonClientConfig.CLIENT.hudAnchor.get();
        tempOffsetX = AddonClientConfig.CLIENT.hudOffsetX.get();
        tempOffsetY = AddonClientConfig.CLIENT.hudOffsetY.get();
        tempHorizontal = AddonClientConfig.CLIENT.horizontalLayout.get();

        updatePreviewPosition();

        int buttonY = 10;
        int buttonX = 10;

        // 锚点选择按钮
        this.addRenderableWidget(CycleButton.<AnchorPoint>builder(this::getAnchorName)
                .withValues(AnchorPoint.values())
                .withInitialValue(tempAnchor)
                .create(buttonX, buttonY, 150, 20,
                        Component.translatable("gui.jujutsu_addon.hud_edit.anchor"),
                        (button, value) -> {
                            tempAnchor = value;
                            tempOffsetX = 0;
                            tempOffsetY = 0;
                            updatePreviewPosition();
                        }));

        // 布局切换按钮（水平/垂直）
        this.addRenderableWidget(CycleButton.<Boolean>builder(horizontal ->
                        Component.translatable(horizontal ?
                                "gui.jujutsu_addon.hud_edit.layout.horizontal" :
                                "gui.jujutsu_addon.hud_edit.layout.vertical"))
                .withValues(true, false)
                .withInitialValue(tempHorizontal)
                .create(buttonX, buttonY + 25, 150, 20,
                        Component.translatable("gui.jujutsu_addon.hud_edit.layout"),
                        (button, value) -> {
                            tempHorizontal = value;
                            updatePreviewPosition();
                        }));

        // 重置位置按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.hud_edit.reset"),
                btn -> {
                    tempOffsetX = 0;
                    tempOffsetY = -50;
                    tempAnchor = AnchorPoint.BOTTOM_CENTER;
                    tempHorizontal = true;
                    updatePreviewPosition();
                    // 重新初始化以更新按钮状态
                    this.rebuildWidgets();
                }).bounds(buttonX, buttonY + 50, 80, 20).build());

        // 保存按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.hud_edit.save"),
                btn -> {
                    saveConfig();
                    this.onClose();
                }).bounds(this.width - 90, this.height - 30, 80, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.hud_edit.cancel"),
                btn -> this.onClose()
        ).bounds(this.width - 180, this.height - 30, 80, 20).build());
    }

    private Component getAnchorName(AnchorPoint anchor) {
        return Component.translatable("gui.jujutsu_addon.anchor." + anchor.name().toLowerCase());
    }

    // 计算预览框尺寸
    private int getPreviewWidth() {
        if (tempHorizontal) {
            return PREVIEW_ICON_COUNT * (ICON_SIZE + ICON_PADDING) + ICON_PADDING;
        } else {
            return ICON_SIZE + ICON_PADDING * 2;
        }
    }

    private int getPreviewHeight() {
        if (tempHorizontal) {
            return ICON_SIZE + TEXT_HEIGHT + ICON_PADDING * 2;
        } else {
            return PREVIEW_ICON_COUNT * (ICON_SIZE + TEXT_HEIGHT + ICON_PADDING) + ICON_PADDING;
        }
    }

    private void updatePreviewPosition() {
        int[] base = getAnchorBasePosition(tempAnchor);
        previewX = base[0] + tempOffsetX;
        previewY = base[1] + tempOffsetY;
    }

    private int[] getAnchorBasePosition(AnchorPoint anchor) {
        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();
        int x, y;

        switch (anchor) {
            case TOP_LEFT -> { x = 10; y = 90; }
            case TOP_CENTER -> { x = (this.width - previewWidth) / 2; y = 90; }
            case TOP_RIGHT -> { x = this.width - previewWidth - 10; y = 90; }
            case CENTER_LEFT -> { x = 10; y = (this.height - previewHeight) / 2; }
            case CENTER -> { x = (this.width - previewWidth) / 2; y = (this.height - previewHeight) / 2; }
            case CENTER_RIGHT -> { x = this.width - previewWidth - 10; y = (this.height - previewHeight) / 2; }
            case BOTTOM_LEFT -> { x = 10; y = this.height - previewHeight - 50; }
            case BOTTOM_CENTER -> { x = (this.width - previewWidth) / 2; y = this.height - previewHeight - 50; }
            case BOTTOM_RIGHT -> { x = this.width - previewWidth - 10; y = this.height - previewHeight - 50; }
            default -> { x = 0; y = 0; }
        }
        return new int[]{x, y};
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();

        // ===== 绘制预览框 =====
        // 背景
        graphics.fill(previewX, previewY,
                previewX + previewWidth, previewY + previewHeight,
                0xCC000000);

        // 边框（拖动时变绿）
        int borderColor = isDragging ? 0xFF00FF00 : 0xFFFFFFFF;
        graphics.renderOutline(previewX, previewY, previewWidth, previewHeight, borderColor);

        // 绘制模拟技能图标
        for (int i = 0; i < PREVIEW_ICON_COUNT; i++) {
            int iconX, iconY;

            if (tempHorizontal) {
                iconX = previewX + ICON_PADDING + i * (ICON_SIZE + ICON_PADDING);
                iconY = previewY + ICON_PADDING;
            } else {
                iconX = previewX + ICON_PADDING;
                iconY = previewY + ICON_PADDING + i * (ICON_SIZE + TEXT_HEIGHT + ICON_PADDING);
            }

            // 图标背景
            graphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0xFF444444);
            graphics.renderOutline(iconX, iconY, ICON_SIZE, ICON_SIZE, 0xFF666666);

            // 模拟冷却遮罩（从上往下）
            float progress = 1.0f - (i * 0.2f);  // 第一个100%，最后一个20%
            int maskHeight = (int) (ICON_SIZE * progress);
            if (maskHeight > 0) {
                graphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + maskHeight, 0xAA000000);
            }

            // 时间文字
            String timeText = String.format("%.1f", (PREVIEW_ICON_COUNT - i) * 1.5f);
            int textX = iconX + ICON_SIZE / 2 - this.font.width(timeText) / 2;
            int textY = iconY + ICON_SIZE + 2;
            graphics.drawString(this.font, timeText, textX, textY, 0xFFFFFF);
        }

        // ===== 提示文字 =====
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.hud_edit.drag_hint"),
                this.width / 2, 75, 0xAAAAFF);

        // 预览说明
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.hud_edit.preview_note"),
                this.width / 2, this.height - 65, 0x888888);

        // 显示当前锚点和偏移值
        graphics.drawString(this.font,
                "Anchor: " + tempAnchor.name(),
                10, this.height - 80, 0xCCCCCC);
        graphics.drawString(this.font,
                String.format("Offset: X = %d, Y = %d", tempOffsetX, tempOffsetY),
                10, this.height - 95, 0xCCCCCC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int previewWidth = getPreviewWidth();
            int previewHeight = getPreviewHeight();

            // 检查是否点击在预览框内
            if (mouseX >= previewX && mouseX <= previewX + previewWidth &&
                    mouseY >= previewY && mouseY <= previewY + previewHeight) {
                isDragging = true;
                dragOffsetX = (int) mouseX - previewX;
                dragOffsetY = (int) mouseY - previewY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;

            // 计算相对于锚点的偏移
            int[] base = getAnchorBasePosition(tempAnchor);
            tempOffsetX = newX - base[0];
            tempOffsetY = newY - base[1];

            // 更新预览位置
            previewX = newX;
            previewY = newY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void saveConfig() {
        AddonClientConfig.CLIENT.hudAnchor.set(tempAnchor);
        AddonClientConfig.CLIENT.hudOffsetX.set(tempOffsetX);
        AddonClientConfig.CLIENT.hudOffsetY.set(tempOffsetY);
        AddonClientConfig.CLIENT.horizontalLayout.set(tempHorizontal);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
