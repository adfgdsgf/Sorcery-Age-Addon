package com.jujutsuaddon.addon.client.gui.screen;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.config.AddonClientConfig.AnchorPoint;
import com.jujutsuaddon.addon.client.gui.overlay.ShadowStorageOverlay;
import com.jujutsuaddon.addon.client.render.HudPositionHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ShadowStorageHudEditScreen extends Screen {

    // 临时值
    private AnchorPoint tempAnchor;
    private int tempOffsetXPermille, tempOffsetYPermille;
    private float tempScale;
    private int tempMaxItems;

    // 拖拽状态
    private boolean isDragging = false;
    private int dragOffsetX, dragOffsetY;

    // 动画
    private float animTick = 0;

    public ShadowStorageHudEditScreen() {
        super(Component.translatable("gui.jujutsu_addon.shadow_hud_edit.title"));
    }

    @Override
    protected void init() {
        super.init();

        // 读取当前配置
        tempAnchor = AddonClientConfig.CLIENT.shadowStorageHudAnchor.get();
        tempOffsetXPermille = AddonClientConfig.CLIENT.shadowStorageHudOffsetX.get();
        tempOffsetYPermille = AddonClientConfig.CLIENT.shadowStorageHudOffsetY.get();
        tempScale = AddonClientConfig.CLIENT.shadowStorageHudScale.get().floatValue();
        tempMaxItems = AddonClientConfig.CLIENT.shadowStorageHudMaxItems.get();

        int buttonY = 10;
        int buttonX = 10;

        // 锚点选择
        this.addRenderableWidget(CycleButton.<AnchorPoint>builder(anchor ->
                        Component.translatable("gui.jujutsu_addon.anchor." + anchor.name().toLowerCase()))
                .withValues(AnchorPoint.values())
                .withInitialValue(tempAnchor)
                .create(buttonX, buttonY, 150, 20,
                        Component.translatable("gui.jujutsu_addon.shadow_hud_edit.anchor"),
                        (button, value) -> {
                            tempAnchor = value;
                            tempOffsetXPermille = 0;
                            tempOffsetYPermille = 0;
                        }));

        // 最大显示数量
        this.addRenderableWidget(CycleButton.<Integer>builder(count -> Component.literal(String.valueOf(count)))
                .withValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .withInitialValue(tempMaxItems)
                .create(buttonX, buttonY + 25, 150, 20,
                        Component.translatable("gui.jujutsu_addon.shadow_hud_edit.max_items"),
                        (button, value) -> tempMaxItems = value));

        // 缩放控制
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            tempScale = Math.max(0.5f, tempScale - 0.1f);
        }).bounds(buttonX, buttonY + 55, 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            tempScale = Math.min(2.0f, tempScale + 0.1f);
        }).bounds(buttonX + 130, buttonY + 55, 20, 20).build());

        // 重置按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.hud_edit.reset"),
                btn -> {
                    tempOffsetXPermille = 0;
                    tempOffsetYPermille = 0;
                    tempAnchor = AnchorPoint.BOTTOM_RIGHT;
                    tempScale = 1.0f;
                    tempMaxItems = 5;
                    this.rebuildWidgets();
                }).bounds(buttonX, buttonY + 85, 80, 20).build());

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

    private int[] getContentSize() {
        return ShadowStorageOverlay.getContentSize(tempMaxItems);
    }

    private int getPreviewWidth() {
        return (int) (getContentSize()[0] * tempScale);
    }

    private int getPreviewHeight() {
        return (int) (getContentSize()[1] * tempScale);
    }

    private int[] getPreviewPosition() {
        int[] contentSize = getContentSize();
        return HudPositionHelper.calculatePosition(
                tempAnchor, tempOffsetXPermille, tempOffsetYPermille, tempScale,
                this.width, this.height, contentSize[0], contentSize[1], 10
        );
    }

    private int[] getAnchorBasePosition() {
        int[] contentSize = getContentSize();
        return HudPositionHelper.getAnchorBasePosition(
                tempAnchor, tempScale,
                this.width, this.height,
                contentSize[0], contentSize[1], 10, 40
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        animTick += partialTick * 0.1f;

        // 缩放值显示
        String scaleText = String.format("%.0f%%", tempScale * 100);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.shadow_hud_edit.scale", scaleText),
                10 + 75, 10 + 60, 0xFFFFFF);

        // 渲染预览
        int[] pos = getPreviewPosition();
        int previewX = pos[0];
        int previewY = pos[1];

        graphics.pose().pushPose();
        graphics.pose().translate(previewX, previewY, 0);
        graphics.pose().scale(tempScale, tempScale, 1.0f);

        renderPreview(graphics);

        graphics.pose().popPose();

        // 预览边框
        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();
        int borderColor = isDragging ? 0xFF00FF00 : 0xFFFFFFFF;
        graphics.renderOutline(previewX - 2, previewY - 2, previewWidth + 4, previewHeight + 4, borderColor);

        // 提示文字
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.hud_edit.drag_hint"),
                this.width / 2, 125, 0xAAAAFF);

        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.shadow_hud_edit.scroll_hint"),
                this.width / 2, 137, 0x888888);

        // 信息显示
        graphics.drawString(this.font,
                Component.translatable("gui.jujutsu_addon.hud_edit.info.anchor", tempAnchor.name()),
                10, this.height - 65, 0xCCCCCC);
        graphics.drawString(this.font,
                String.format("Offset: X = %.1f%%, Y = %.1f%%",
                        tempOffsetXPermille / 10.0, tempOffsetYPermille / 10.0),
                10, this.height - 80, 0xCCCCCC);
    }

    private void renderPreview(GuiGraphics graphics) {
        int displayCount = tempMaxItems;
        int totalWidth = displayCount * (ShadowStorageOverlay.ICON_SIZE + ShadowStorageOverlay.ICON_SPACING)
                - ShadowStorageOverlay.ICON_SPACING;
        int padding = ShadowStorageOverlay.PADDING;
        int iconSize = ShadowStorageOverlay.ICON_SIZE;

        // 背景
        float pulse = (float) (Math.sin(animTick * 2) * 0.15 + 0.85);
        int bgAlpha = (int) (180 * pulse);
        graphics.fill(0, 0, totalWidth + padding * 2, iconSize + padding * 2, (bgAlpha << 24) | 0x100828);

        // 边框
        int borderAlpha = (int) (150 * pulse);
        graphics.renderOutline(0, 0, totalWidth + padding * 2, iconSize + padding * 2,
                (borderAlpha << 24) | 0x4422AA);

        // 模拟物品槽
        for (int i = 0; i < displayCount; i++) {
            int iconX = padding + i * (iconSize + ShadowStorageOverlay.ICON_SPACING);
            int iconY = padding;

            float floatOffset = (float) Math.sin(animTick + i * 0.5f) * 1.5f;

            graphics.pose().pushPose();
            graphics.pose().translate(0, floatOffset, 0);

            // 阴影
            graphics.fill(iconX + 2, iconY + 2, iconX + iconSize + 2, iconY + iconSize + 2, 0x40000000);

            // 槽位背景
            graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xFF442266);
            graphics.renderOutline(iconX, iconY, iconSize, iconSize, 0xFF6644AA);

            // 模拟数量
            String countText = String.valueOf((i + 1) * 16);
            graphics.drawString(this.font, countText,
                    iconX + iconSize - this.font.width(countText),
                    iconY + iconSize - 8, 0xFFFFFF, true);

            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int[] pos = getPreviewPosition();
            int previewWidth = getPreviewWidth();
            int previewHeight = getPreviewHeight();

            if (mouseX >= pos[0] - 2 && mouseX <= pos[0] + previewWidth + 2 &&
                    mouseY >= pos[1] - 2 && mouseY <= pos[1] + previewHeight + 2) {
                isDragging = true;
                dragOffsetX = (int) mouseX - pos[0];
                dragOffsetY = (int) mouseY - pos[1];
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

            // 获取锚点基准位置
            int[] basePos = getAnchorBasePosition();

            // 计算像素偏移
            int pixelOffsetX = newX - basePos[0];
            int pixelOffsetY = newY - basePos[1];

            // 像素转千分比
            tempOffsetXPermille = HudPositionHelper.pixelToPermille(pixelOffsetX, this.width);
            tempOffsetYPermille = HudPositionHelper.pixelToPermille(pixelOffsetY, this.height);

            // 限制范围
            tempOffsetXPermille = HudPositionHelper.clampPermille(tempOffsetXPermille);
            tempOffsetYPermille = HudPositionHelper.clampPermille(tempOffsetYPermille);

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int[] pos = getPreviewPosition();
        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();

        boolean inPreview = mouseX >= pos[0] - 2 && mouseX <= pos[0] + previewWidth + 2 &&
                mouseY >= pos[1] - 2 && mouseY <= pos[1] + previewHeight + 2;

        if (inPreview || hasControlDown()) {
            if (delta > 0) {
                tempScale = Math.min(2.0f, tempScale + 0.1f);
            } else if (delta < 0) {
                tempScale = Math.max(0.5f, tempScale - 0.1f);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void saveConfig() {
        AddonClientConfig.CLIENT.shadowStorageHudAnchor.set(tempAnchor);
        AddonClientConfig.CLIENT.shadowStorageHudOffsetX.set(tempOffsetXPermille);
        AddonClientConfig.CLIENT.shadowStorageHudOffsetY.set(tempOffsetYPermille);
        AddonClientConfig.CLIENT.shadowStorageHudScale.set((double) tempScale);
        AddonClientConfig.CLIENT.shadowStorageHudMaxItems.set(tempMaxItems);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
