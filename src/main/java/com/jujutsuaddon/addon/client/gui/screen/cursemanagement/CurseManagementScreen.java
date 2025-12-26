package com.jujutsuaddon.addon.client.gui.screen.cursemanagement;

import com.jujutsuaddon.addon.client.render.EntityRenderHelper;
import com.jujutsuaddon.addon.client.util.UIScaleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 咒灵管理主界面
 */
public class CurseManagementScreen extends Screen {

    private CurseManagementLayout layout;
    private CurseManagementDataManager dataManager;
    private CurseManagementRenderer renderer;
    private CurseManagementInputHandler inputHandler;

    public CurseManagementScreen() {
        super(Component.translatable("gui.jujutsu_addon.curse_management.title"));
    }

    @Override
    protected void init() {
        super.init();

        layout = new CurseManagementLayout(this.width, this.height);
        dataManager = new CurseManagementDataManager();
        dataManager.setItemsPerPage(layout.getItemsPerPage());
        renderer = new CurseManagementRenderer(this.font, layout);
        inputHandler = new CurseManagementInputHandler(layout, dataManager);

        dataManager.refresh();
        createButtons();
    }

    private void createButtons() {
        int contentBottom = layout.getGridStartY() + layout.getGridHeight();
        int[] btnLayout = UIScaleHelper.calculateButtonLayout(
                this.width, this.height, contentBottom, 8);

        int buttonY = btnLayout[0];
        int btnHeight = btnLayout[1];
        int btnSmall = btnLayout[2];
        int btnMed = btnLayout[3];
        int btnLarge = btnLayout[4];

        int pageNavWidth = btnSmall * 2 + 50;
        int pageNavX = (this.width - pageNavWidth) / 2;

        // 翻页按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("◀"), btn -> dataManager.prevPage()
        ).bounds(pageNavX, buttonY, btnSmall, btnHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("▶"), btn -> dataManager.nextPage()
        ).bounds(pageNavX + pageNavWidth - btnSmall, buttonY, btnSmall, btnHeight).build());

        // 关闭按钮
        int closeY = buttonY + btnHeight + 2;
        if (closeY + btnHeight > this.height - 3) {
            closeY = this.height - btnHeight - 3;
        }
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), btn -> this.onClose()
        ).bounds((this.width - btnMed) / 2, closeY, btnMed, btnHeight).build());

        // 全部召唤按钮
        int summonY = closeY + btnHeight + 2;
        if (summonY + btnHeight <= this.height - 2) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.jujutsu_addon.curse_management.summon_all"),
                    btn -> {
                        inputHandler.summonAll();
                        this.onClose();
                    }
            ).bounds((this.width - btnLarge) / 2, summonY, btnLarge, btnHeight).build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.tickCount % 20 == 0) {
            dataManager.refresh();
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // 标题和统计
        renderer.renderTitle(graphics, this.title, dataManager.getTotalCount(), dataManager.getUniqueTypeCount());

        // 背景
        renderer.renderBackground(graphics);

        // 更新悬停
        inputHandler.updateHoveredIndex(mouseX, mouseY);

        // 渲染格子
        int startIndex = dataManager.getStartIndex();
        for (int i = 0; i < layout.getItemsPerPage(); i++) {
            int groupIndex = startIndex + i;
            int[] pos = layout.getCellPosition(i);
            int cellX = pos[0];
            int cellY = pos[1];

            boolean isHovered = (groupIndex == inputHandler.getHoveredIndex());

            if (groupIndex < dataManager.getGroupedCurses().size()) {
                CurseManagementDataManager.GroupedCurse group = dataManager.getGroupAt(groupIndex);
                if (group != null) {
                    renderer.renderCell(graphics, group, cellX, cellY, isHovered);
                }
            } else {
                renderer.renderEmptyCell(graphics, cellX, cellY);
            }
        }

        // 页码
        renderer.renderPageIndicator(graphics, dataManager.getCurrentPage(), dataManager.getTotalPages());

        // 父类渲染（按钮等）
        super.render(graphics, mouseX, mouseY, partialTick);

        // Tooltip
        int hoveredIdx = inputHandler.getHoveredIndex();
        if (hoveredIdx >= 0 && hoveredIdx < dataManager.getGroupedCurses().size()) {
            CurseManagementDataManager.GroupedCurse group = dataManager.getGroupAt(hoveredIdx);
            if (group != null) {
                renderer.renderTooltip(graphics, group, dataManager, mouseX, mouseY);
            }
        }
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputHandler.handleMouseClick(mouseX, mouseY, button, dataManager::refresh)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return inputHandler.handleMouseScroll(delta);
    }

    @Override
    public void onClose() {
        EntityRenderHelper.clearCache();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
