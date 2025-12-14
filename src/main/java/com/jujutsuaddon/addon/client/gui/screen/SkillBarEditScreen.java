package com.jujutsuaddon.addon.client.gui.screen;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.config.AddonClientConfig.AnchorPoint;
import com.jujutsuaddon.addon.client.skillbar.SkillBarData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SkillBarEditScreen extends Screen {

    private static final int SLOT_SIZE = 22;
    private static final int SLOT_PADDING = 2;
    private static final int HEADER_HEIGHT = 12;

    private int dragOffsetX, dragOffsetY;
    private boolean isDragging = false;

    // 临时值（千分比 -500~500）
    private AnchorPoint tempAnchor;
    private int tempOffsetXPermille, tempOffsetYPermille;
    private boolean tempHorizontal;
    private float tempScale;

    public SkillBarEditScreen() {
        super(Component.translatable("gui.jujutsu_addon.skill_bar_edit.title"));
    }

    @Override
    protected void init() {
        super.init();

        tempAnchor = AddonClientConfig.CLIENT.skillBarAnchor.get();
        tempOffsetXPermille = AddonClientConfig.CLIENT.skillBarOffsetX.get();
        tempOffsetYPermille = AddonClientConfig.CLIENT.skillBarOffsetY.get();
        tempHorizontal = AddonClientConfig.CLIENT.skillBarHorizontalLayout.get();
        tempScale = AddonClientConfig.CLIENT.skillBarScale.get().floatValue();

        int buttonY = 10;
        int buttonX = 10;

        // 锚点选择
        this.addRenderableWidget(CycleButton.<AnchorPoint>builder(anchor ->
                        Component.translatable("gui.jujutsu_addon.anchor." + anchor.name().toLowerCase()))
                .withValues(AnchorPoint.values())
                .withInitialValue(tempAnchor)
                .create(buttonX, buttonY, 150, 20,
                        Component.translatable("gui.jujutsu_addon.skill_bar_edit.anchor"),
                        (button, value) -> {
                            tempAnchor = value;
                            tempOffsetXPermille = 0;
                            tempOffsetYPermille = 0;
                        }));

        // 布局切换
        this.addRenderableWidget(CycleButton.<Boolean>builder(horizontal ->
                        Component.translatable(horizontal ?
                                "gui.jujutsu_addon.layout.horizontal" :
                                "gui.jujutsu_addon.layout.vertical"))
                .withValues(true, false)
                .withInitialValue(tempHorizontal)
                .create(buttonX, buttonY + 25, 150, 20,
                        Component.translatable("gui.jujutsu_addon.skill_bar_edit.layout"),
                        (button, value) -> tempHorizontal = value));

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
                    tempAnchor = AnchorPoint.BOTTOM_CENTER;
                    tempHorizontal = true;
                    tempScale = 1.0f;
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

        // 打开配置按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.skill_bar_edit.open_config"),
                btn -> {
                    saveConfig();
                    this.minecraft.setScreen(new SkillBarConfigScreen());
                }).bounds(buttonX, buttonY + 115, 150, 20).build());
    }

    private int getPreviewWidth() {
        int baseWidth = tempHorizontal ?
                SkillBarData.SLOT_COUNT * SLOT_SIZE + (SkillBarData.SLOT_COUNT - 1) * SLOT_PADDING :
                SLOT_SIZE;
        return (int) (baseWidth * tempScale);
    }

    private int getPreviewHeight() {
        int baseHeight = tempHorizontal ?
                SLOT_SIZE + HEADER_HEIGHT :
                SkillBarData.SLOT_COUNT * SLOT_SIZE + (SkillBarData.SLOT_COUNT - 1) * SLOT_PADDING + HEADER_HEIGHT;
        return (int) (baseHeight * tempScale);
    }

    /**
     * 根据锚点和偏移百分比计算预览位置
     */
    private int[] getPreviewPosition() {
        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();

        // 偏移百分比转像素
        int offsetX = (int) (this.width * tempOffsetXPermille / 1000.0);
        int offsetY = (int) (this.height * tempOffsetYPermille / 1000.0);

        int x, y;
        switch (tempAnchor) {
            case TOP_LEFT -> { x = 10; y = 140; }
            case TOP_CENTER -> { x = (this.width - previewWidth) / 2; y = 140; }
            case TOP_RIGHT -> { x = this.width - previewWidth - 10; y = 140; }
            case CENTER_LEFT -> { x = 10; y = (this.height - previewHeight) / 2; }
            case CENTER -> { x = (this.width - previewWidth) / 2; y = (this.height - previewHeight) / 2; }
            case CENTER_RIGHT -> { x = this.width - previewWidth - 10; y = (this.height - previewHeight) / 2; }
            case BOTTOM_LEFT -> { x = 10; y = this.height - previewHeight - 50; }
            case BOTTOM_CENTER -> { x = (this.width - previewWidth) / 2; y = this.height - previewHeight - 50; }
            case BOTTOM_RIGHT -> { x = this.width - previewWidth - 10; y = this.height - previewHeight - 50; }
            default -> { x = 0; y = 0; }
        }
        return new int[]{x + offsetX, y + offsetY};
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // 缩放值显示
        String scaleText = String.format("%.0f%%", tempScale * 100);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.skill_bar_edit.scale", scaleText),
                10 + 75, 10 + 60, 0xFFFFFF);

        int[] pos = getPreviewPosition();
        int previewX = pos[0];
        int previewY = pos[1];
        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();

        // 应用缩放渲染预览
        graphics.pose().pushPose();
        graphics.pose().translate(previewX, previewY, 0);
        graphics.pose().scale(tempScale, tempScale, 1.0f);

        int baseWidth = tempHorizontal ?
                SkillBarData.SLOT_COUNT * SLOT_SIZE + (SkillBarData.SLOT_COUNT - 1) * SLOT_PADDING :
                SLOT_SIZE;
        int baseHeight = tempHorizontal ?
                SLOT_SIZE + HEADER_HEIGHT :
                SkillBarData.SLOT_COUNT * SLOT_SIZE + (SkillBarData.SLOT_COUNT - 1) * SLOT_PADDING + HEADER_HEIGHT;

        graphics.fill(-2, -2, baseWidth + 2, baseHeight + 2, 0xCC000000);

        int borderColor = isDragging ? 0xFF00FF00 : 0xFFFFFFFF;
        graphics.renderOutline(-2, -2, baseWidth + 4, baseHeight + 4, borderColor);

        graphics.drawString(this.font, "§a●", 0, 0, 0xFFFFFF, false);
        graphics.drawCenteredString(this.font, "[ 1 ]", baseWidth / 2, 0, 0xFFFF00);

        int slotY = HEADER_HEIGHT;
        String[] defaultKeys = {"Z", "X", "C", "V", "B", "N"};

        for (int i = 0; i < SkillBarData.SLOT_COUNT; i++) {
            int x, y;
            if (tempHorizontal) {
                x = i * (SLOT_SIZE + SLOT_PADDING);
                y = slotY;
            } else {
                x = 0;
                y = slotY + i * (SLOT_SIZE + SLOT_PADDING);
            }

            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
            graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xFF00CC00);
            graphics.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0xFF444444);

            String keyHint = i < defaultKeys.length ? defaultKeys[i] : String.valueOf(i + 1);
            graphics.drawString(this.font, keyHint, x + 2, y + SLOT_SIZE - 9, 0xAAAAAA, true);
        }

        graphics.pose().popPose();

        // 提示文字
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.hud_edit.drag_hint"),
                this.width / 2, 125, 0xAAAAFF);

        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.skill_bar_edit.scroll_hint"),
                this.width / 2, 137, 0x888888);

        // 信息显示（显示百分比）
        graphics.drawString(this.font,
                Component.translatable("gui.jujutsu_addon.hud_edit.info.anchor", tempAnchor.name()),
                10, this.height - 65, 0xCCCCCC);
        graphics.drawString(this.font,
                String.format("Offset: X = %.1f%%, Y = %.1f%%",
                        tempOffsetXPermille / 10.0, tempOffsetYPermille / 10.0),
                10, this.height - 80, 0xCCCCCC);
        graphics.drawString(this.font,
                String.format("Scale: %.0f%%", tempScale * 100),
                10, this.height - 95, 0xCCCCCC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int[] pos = getPreviewPosition();
            int previewX = pos[0];
            int previewY = pos[1];
            int previewWidth = getPreviewWidth();
            int previewHeight = getPreviewHeight();

            if (mouseX >= previewX - 2 && mouseX <= previewX + previewWidth + 2 &&
                    mouseY >= previewY - 2 && mouseY <= previewY + previewHeight + 2) {
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

            // 计算相对于锚点基准位置的偏移
            int[] basePos = getAnchorBasePosition();
            int pixelOffsetX = newX - basePos[0];
            int pixelOffsetY = newY - basePos[1];

            // ★★★ 像素转千分比 ★★★
            tempOffsetXPermille = (int) (pixelOffsetX * 1000.0 / this.width);
            tempOffsetYPermille = (int) (pixelOffsetY * 1000.0 / this.height);

            // 限制范围
            tempOffsetXPermille = Math.max(-500, Math.min(500, tempOffsetXPermille));
            tempOffsetYPermille = Math.max(-500, Math.min(500, tempOffsetYPermille));

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * 获取锚点基准位置（不含偏移）
     */
    private int[] getAnchorBasePosition() {
        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();
        int x, y;

        switch (tempAnchor) {
            case TOP_LEFT -> { x = 10; y = 140; }
            case TOP_CENTER -> { x = (this.width - previewWidth) / 2; y = 140; }
            case TOP_RIGHT -> { x = this.width - previewWidth - 10; y = 140; }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int[] pos = getPreviewPosition();
        boolean inPreview = mouseX >= pos[0] - 2 && mouseX <= pos[0] + getPreviewWidth() + 2 &&
                mouseY >= pos[1] - 2 && mouseY <= pos[1] + getPreviewHeight() + 2;

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
        AddonClientConfig.CLIENT.skillBarAnchor.set(tempAnchor);
        AddonClientConfig.CLIENT.skillBarOffsetX.set(tempOffsetXPermille);
        AddonClientConfig.CLIENT.skillBarOffsetY.set(tempOffsetYPermille);
        AddonClientConfig.CLIENT.skillBarHorizontalLayout.set(tempHorizontal);
        AddonClientConfig.CLIENT.skillBarScale.set((double) tempScale);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
