package com.jujutsuaddon.addon.client.gui.screen.shadowstorage;

import com.jujutsuaddon.addon.client.gui.screen.ShadowStorageHudEditScreen;
import com.jujutsuaddon.addon.client.util.SlotRenderHelper;
import com.jujutsuaddon.addon.util.helper.ShadowStorageSortHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 影子库存主界面
 */
public class ShadowStorageScreen extends Screen {

    private ShadowStorageLayout layout;
    private ShadowStorageDataManager dataManager;
    private ShadowStorageRenderer renderer;
    private ShadowStorageInputHandler inputHandler;

    private EditBox searchBox;
    private int searchBoxX;

    public ShadowStorageScreen() {
        super(Component.translatable("gui.jujutsu_addon.shadow_storage.title"));
    }

    @Override
    protected void init() {
        super.init();

        layout = new ShadowStorageLayout(this.width, this.height);
        dataManager = new ShadowStorageDataManager();
        renderer = new ShadowStorageRenderer(this.font, layout);
        inputHandler = new ShadowStorageInputHandler(layout, dataManager);

        initSearchBox();
        dataManager.refresh();
    }

    private void initSearchBox() {
        int boxWidth = 78;
        int boxHeight = 12;
        searchBoxX = layout.getLeftPos() + ShadowStorageLayout.CONTAINER_WIDTH - boxWidth - 6;
        int boxY = layout.getTopPos() + 4;

        searchBox = new EditBox(this.font, searchBoxX, boxY, boxWidth, boxHeight,
                Component.translatable("gui.jujutsu_addon.shadow_storage.search"));
        searchBox.setMaxLength(50);
        searchBox.setBordered(true);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setHint(Component.translatable("gui.jujutsu_addon.shadow_storage.search.hint")
                .withStyle(s -> s.withColor(0x808080)));
        searchBox.setResponder(text -> {
            dataManager.setSearchText(text);
            inputHandler.clampScroll();
        });

        this.addRenderableWidget(searchBox);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.tickCount % 5 == 0) {
            if (!inputHandler.isDragging()) {
                dataManager.refresh();
                inputHandler.clampScroll();
            }
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderer.tick(partialTick);
        renderer.renderBackground(graphics, this.width, this.height);
        renderer.renderContainer(graphics, this.title, dataManager, dataManager.getSearchText(), searchBoxX);
        renderer.renderShadowInventory(graphics, mouseX, mouseY,
                dataManager.getDisplayItems(), inputHandler.getScrollRow(),
                inputHandler.getDraggedShadowIndices(), inputHandler.hasDraggedShadow(),
                dataManager.getSearchText());
        renderer.renderPlayerInventory(graphics, mouseX, mouseY,
                inputHandler.getDraggedPlayerSlots(), inputHandler.hasDraggedPlayer());
        renderer.renderScrollbar(graphics, inputHandler.getScrollRow(),
                dataManager.getMaxScrollRow(), dataManager.getTotalRows());
        renderer.renderSideButtons(graphics, mouseX, mouseY,
                dataManager.getSortMode(), dataManager.getSearchText());

        // ★★★ 传递屏幕高度 ★★★
        renderer.renderStats(graphics, dataManager, dataManager.getSearchText());

        super.render(graphics, mouseX, mouseY, partialTick);

        // 拖拽提示
        if (inputHandler.isDragging()) {
            int shadowCount = inputHandler.getDraggedShadowIndices().size();
            int playerCount = inputHandler.getDraggedPlayerSlots().size();
            renderer.renderDragHint(graphics, this.width, this.height,
                    layout.getTopPos(), ShadowStorageLayout.CONTAINER_HEIGHT,
                    shadowCount, playerCount);
        }

        if (!inputHandler.isDragging()) {
            renderTooltips(graphics, mouseX, mouseY);
        }
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnX = layout.getSideButtonX();
        int btnSize = layout.getSideButtonSize();
        int sortBtnY = layout.getTopPos() + 18;
        int hudBtnY = sortBtnY + btnSize + layout.getSideButtonGap();

        if (SlotRenderHelper.isInButton(mouseX, mouseY, btnX, sortBtnY, btnSize)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.sort.title"));
            tooltip.add(Component.translatable(ShadowStorageSortHelper.getSortModeName(dataManager.getSortMode()))
                    .withStyle(s -> s.withColor(0xAAAAFF)));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.sort.click_hint")
                    .withStyle(s -> s.withColor(0x888888)));
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.sort.shift_hint")
                    .withStyle(s -> s.withColor(0x888888)));
            graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }

        if (SlotRenderHelper.isInButton(mouseX, mouseY, btnX, hudBtnY, btnSize)) {
            graphics.renderTooltip(this.font,
                    Component.translatable("gui.jujutsu_addon.shadow_storage.edit_hud"), mouseX, mouseY);
            return;
        }

        if (!dataManager.getSearchText().isEmpty()) {
            int clearBtnY = hudBtnY + btnSize + layout.getSideButtonGap();
            if (SlotRenderHelper.isInButton(mouseX, mouseY, btnX, clearBtnY, btnSize)) {
                graphics.renderTooltip(this.font,
                        Component.translatable("gui.jujutsu_addon.shadow_storage.search.clear"), mouseX, mouseY);
                return;
            }
        }

        int shadowIdx = layout.getShadowSlotIndex(mouseX, mouseY, inputHandler.getScrollRow());
        if (shadowIdx >= 0 && shadowIdx < dataManager.getDisplayCount()) {
            ItemStack stack = dataManager.getDisplayItem(shadowIdx);
            List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            if (stack.getCount() > 64) {
                tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.total_count", stack.getCount())
                        .withStyle(s -> s.withColor(0xFFAA00)));
            }
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.left_click")
                    .withStyle(s -> s.withColor(0x55FF55)));
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.shift_click")
                    .withStyle(s -> s.withColor(0x55AAFF)));
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.shift_drag")
                    .withStyle(s -> s.withColor(0xFFAA55)));
            graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }

        int playerSlot = layout.getPlayerSlotIndex(mouseX, mouseY);
        if (playerSlot >= 0 && this.minecraft != null && this.minecraft.player != null) {
            ItemStack stack = this.minecraft.player.getInventory().getItem(playerSlot);
            if (!stack.isEmpty()) {
                List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.click_store")
                        .withStyle(s -> s.withColor(0x55FF55)));
                tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.shift_store")
                        .withStyle(s -> s.withColor(0x55AAFF)));
                tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.shift_drag")
                        .withStyle(s -> s.withColor(0xFFAA55)));
                graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    // ==================== 输入处理 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(searchBox);
            return true;
        }
        if (searchBox.isFocused() && !searchBox.isMouseOver(mouseX, mouseY)) {
            searchBox.setFocused(false);
        }
        if (inputHandler.handleMouseClick(mouseX, mouseY, button,
                () -> {},
                () -> Minecraft.getInstance().setScreen(new ShadowStorageHudEditScreen()),
                () -> {
                    searchBox.setValue("");
                    dataManager.setSearchText("");
                })) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        inputHandler.handleMouseRelease();
        dataManager.refresh();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (inputHandler.handleMouseDrag(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!searchBox.isFocused()) {
            if (inputHandler.handleMouseScroll(delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == 258) {
                searchBox.setFocused(false);
                return true;
            }
        } else {
            if (keyCode == 70 && Screen.hasControlDown()) {
                searchBox.setFocused(true);
                this.setFocused(searchBox);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
