package com.jujutsuaddon.addon.client.gui.screen;

import com.jujutsuaddon.addon.capability.AddonShadowStorageData;
import com.jujutsuaddon.addon.client.util.UIScaleHelper;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.ShadowStorageActionC2SPacket;
import com.jujutsuaddon.addon.network.c2s.ShadowStorageActionC2SPacket.Action;
import com.jujutsuaddon.addon.util.helper.ShadowStorageSortHelper;
import com.jujutsuaddon.addon.util.helper.ShadowStorageSortHelper.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShadowStorageScreen extends Screen {

    // 布局常量
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int SHADOW_ROWS = 6;
    private static final int PADDING = 7;

    // 界面尺寸
    private int containerWidth = 176;
    private int containerHeight = 222;
    private int leftPos, topPos;

    // 滚动
    private int scrollRow = 0;
    private boolean isScrolling = false;

    // 动画
    private float shadowPulse = 0;

    // ★★★ 排序模式 ★★★
    private SortMode currentSortMode = SortMode.NONE;

    // 数据缓存
    private List<ItemStack> cachedItems = new ArrayList<>();
    private List<ItemStack> displayItems = new ArrayList<>();

    public ShadowStorageScreen() {
        super(Component.translatable("gui.jujutsu_addon.shadow_storage.title"));
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - containerWidth) / 2;
        this.topPos = (this.height - containerHeight) / 2;

        refreshItems();
    }

    private void refreshItems() {
        cachedItems.clear();
        // ★★★ 不要调用 displayItems.clear()，直接创建新列表 ★★★

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            displayItems = new ArrayList<>();  // 确保是可变列表
            return;
        }
        // ★★★ 使用我们自己的 Capability ★★★
        player.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(storage -> {
            for (AddonShadowStorageData.StorageEntry entry : storage.getAll()) {
                ItemStack displayStack = entry.getTemplate().copy();
                displayStack.setCount((int) Math.min(entry.getCount(), Integer.MAX_VALUE));
                cachedItems.add(displayStack);
            }
        });
        // ★★★ 包装成可变 ArrayList，防止不可变列表崩溃 ★★★
        displayItems = new ArrayList<>(ShadowStorageSortHelper.sortItems(cachedItems, currentSortMode));

        scrollRow = Math.min(scrollRow, getMaxScrollRow());
    }
    @Override
    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.tickCount % 5 == 0) {
            refreshItems();
        }
    }

    // ==================== 布局计算 ====================
    private int getShadowGridX() { return leftPos + PADDING + 1; }
    private int getShadowGridY() { return topPos + 18; }
    private int getShadowGridHeight() { return SHADOW_ROWS * SLOT_SIZE; }
    private int getPlayerInvY() { return topPos + 18 + SHADOW_ROWS * SLOT_SIZE + 14; }
    private int getPlayerHotbarY() { return getPlayerInvY() + 3 * SLOT_SIZE + 4; }
    // ★★★ 侧边按钮区域（调用工具类）★★★
    private int[] getSideButtonLayout() {
        return UIScaleHelper.calculateShadowStorageSideButtonLayout();
    }
    private int getSideButtonSize() {
        return getSideButtonLayout()[0];
    }
    private int getSideButtonGap() {
        return getSideButtonLayout()[1];
    }
    private int getSideButtonX() {
        int[] layout = getSideButtonLayout();
        return leftPos - layout[0] - layout[2];  // 容器左边 - 按钮尺寸 - 间隙
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        shadowPulse += partialTick * 0.1f;

        renderShadowBackground(graphics);
        renderContainer(graphics, mouseX, mouseY);
        renderSideButtons(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);

        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderShadowBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0xD0050510);
    }

    private void renderContainer(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // 主背景
        graphics.fill(x, y, x + containerWidth, y + containerHeight, 0xF0100818);

        // 边框
        float pulse = (float) (Math.sin(shadowPulse * 2) * 0.3 + 0.7);
        int borderAlpha = (int) (200 * pulse);
        graphics.renderOutline(x - 1, y - 1, containerWidth + 2, containerHeight + 2,
                (borderAlpha << 24) | 0x6622AA);
        graphics.renderOutline(x, y, containerWidth, containerHeight, 0xFF3311AA);

        // 标题
        float hue = (float) (Math.sin(shadowPulse * 0.5) * 0.1 + 0.75);
        int titleColor = java.awt.Color.HSBtoRGB(hue, 0.6f, 1.0f) & 0xFFFFFF;
        graphics.drawString(this.font, this.title, x + 8, y + 6, titleColor, false);

        // 物品计数（支持大数字）
        long totalCount = 0;
        for (ItemStack stack : cachedItems) totalCount += stack.getCount();
        Component countText = Component.translatable("gui.jujutsu_addon.shadow_storage.count",
                displayItems.size(), formatCount(totalCount));
        int countWidth = this.font.width(countText);
        graphics.drawString(this.font, countText, x + containerWidth - countWidth - 8, y + 6, 0x8866AAFF, false);

        // ==================== 影子库存区域 ====================
        renderShadowInventory(graphics, mouseX, mouseY);

        // ==================== 玩家背包区域 ====================
        renderPlayerInventory(graphics, mouseX, mouseY);

        // ==================== 滚动条 ====================
        if (getMaxScrollRow() > 0) {
            renderScrollbar(graphics, x + containerWidth - 5, getShadowGridY(), getShadowGridHeight());
        }
    }

    // ★★★ 渲染侧边按钮 ★★★
    private void renderSideButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnX = getSideButtonX();
        int btnSize = getSideButtonSize();
        int btnY = topPos + 18;

        // ========== 排序按钮 ==========
        boolean sortHovered = isInButton(mouseX, mouseY, btnX, btnY, btnSize);

        int sortBgColor = sortHovered ? 0xE0302050 : 0xC0201030;
        graphics.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, sortBgColor);

        int sortBorderColor = sortHovered ? 0xFF8855CC : 0xFF6633AA;
        graphics.renderOutline(btnX, btnY, btnSize, btnSize, sortBorderColor);

        String sortIcon = ShadowStorageSortHelper.getSortModeIcon(currentSortMode);
        int sortIconWidth = this.font.width(sortIcon);
        graphics.drawString(this.font, sortIcon,
                btnX + (btnSize - sortIconWidth) / 2,
                btnY + (btnSize - 8) / 2,
                0xFFFFFF, false);

        // ========== HUD编辑按钮 ==========
        int hudBtnY = btnY + btnSize + getSideButtonGap();
        boolean hudHovered = isInButton(mouseX, mouseY, btnX, hudBtnY, btnSize);

        int hudBgColor = hudHovered ? 0xE0203050 : 0xC0102030;
        graphics.fill(btnX, hudBtnY, btnX + btnSize, hudBtnY + btnSize, hudBgColor);

        int hudBorderColor = hudHovered ? 0xFF5588CC : 0xFF3366AA;
        graphics.renderOutline(btnX, hudBtnY, btnSize, btnSize, hudBorderColor);

        String hudIcon = "⚙";
        int hudIconWidth = this.font.width(hudIcon);
        graphics.drawString(this.font, hudIcon,
                btnX + (btnSize - hudIconWidth) / 2,
                hudBtnY + (btnSize - 8) / 2,
                0xFFFFFF, false);
    }

    private void renderShadowInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        int gridX = getShadowGridX();
        int gridY = getShadowGridY();
        int gridHeight = getShadowGridHeight();

        // 背景
        graphics.fill(gridX, gridY, gridX + COLS * SLOT_SIZE, gridY + gridHeight, 0xC0080410);
        graphics.renderOutline(gridX, gridY, COLS * SLOT_SIZE, gridHeight, 0x804422AA);

        // 渲染槽位
        int startIndex = scrollRow * COLS;

        for (int row = 0; row < SHADOW_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = startIndex + row * COLS + col;
                int slotX = gridX + col * SLOT_SIZE;
                int slotY = gridY + row * SLOT_SIZE;

                boolean isHovered = isInSlot(mouseX, mouseY, slotX, slotY);

                if (index < displayItems.size()) {
                    renderShadowSlot(graphics, slotX, slotY, displayItems.get(index), isHovered);
                } else if (index == displayItems.size()) {
                    renderEmptySlot(graphics, slotX, slotY, isHovered);
                }
            }
        }
    }

    private void renderPlayerInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Inventory inv = player.getInventory();
        int gridX = getShadowGridX();

        // "物品栏" 标签
        graphics.drawString(this.font,
                Component.translatable("container.inventory"),
                gridX, getPlayerInvY() - 11, 0x606080, false);

        // 主背包 (9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                int slotX = gridX + col * SLOT_SIZE;
                int slotY = getPlayerInvY() + row * SLOT_SIZE;

                boolean isHovered = isInSlot(mouseX, mouseY, slotX, slotY);
                renderPlayerSlot(graphics, slotX, slotY, inv.getItem(slotIndex), isHovered);
            }
        }

        // 快捷栏 (0-8)
        for (int col = 0; col < 9; col++) {
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = getPlayerHotbarY();

            boolean isHovered = isInSlot(mouseX, mouseY, slotX, slotY);
            renderPlayerSlot(graphics, slotX, slotY, inv.getItem(col), isHovered);
        }
    }

    private void renderShadowSlot(GuiGraphics graphics, int x, int y, ItemStack stack, boolean hovered) {
        int bgColor = hovered ? 0xA0201040 : 0x80100820;
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);
        int borderColor = hovered ? 0xFF6644CC : 0x40FFFFFF;
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE - 1, borderColor);

        // 渲染物品图标
        graphics.renderItem(stack, x + 1, y + 1);

        // ★★★ 自定义数量渲染（缩小 + 自动缩放）★★★
        if (stack.getCount() > 1) {
            renderScaledCount(graphics, x, y, stack.getCount());
        }
    }

    /**
     * 渲染缩放的数量文字
     * - 基础大小比原版小35%
     * - 数字过长时自动进一步缩小
     * - 根据数量级显示不同颜色
     */
    private void renderScaledCount(GuiGraphics graphics, int slotX, int slotY, int count) {
        String countStr = formatSlotCount(count);
        int originalWidth = this.font.width(countStr);

        // 计算缩放比例
        float baseScale = 0.65f;
        int maxAllowedWidth = SLOT_SIZE - 2;

        float scale = baseScale;
        float scaledWidth = originalWidth * scale;

        if (scaledWidth > maxAllowedWidth) {
            scale = maxAllowedWidth / (float) originalWidth;
            scale = Math.max(scale, 0.4f);
            scaledWidth = originalWidth * scale;
        }

        // 根据数量决定颜色
        int textColor;
        if (count >= 1_000_000) {
            textColor = 0xFFFF55;  // 金色
        } else if (count >= 100_000) {
            textColor = 0xFF55FF;  // 粉色
        } else if (count >= 10_000) {
            textColor = 0x55FFFF;  // 青色
        } else if (count >= 1_000) {
            textColor = 0x55FF55;  // 绿色
        } else if (count >= 100) {
            textColor = 0xFFFF55;  // 黄色
        } else {
            textColor = 0xFFFFFF;  // 白色
        }

        // 位置（右下角）
        float textX = slotX + SLOT_SIZE - scaledWidth - 1;
        float textY = slotY + SLOT_SIZE - (8 * scale) - 1;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // 多层阴影使边缘更清晰
        graphics.drawString(this.font, countStr, 1, 1, 0x000000, false);
        graphics.drawString(this.font, countStr, 1, 0, 0x000000, false);
        graphics.drawString(this.font, countStr, 0, 1, 0x000000, false);

        // 主文字
        graphics.drawString(this.font, countStr, 0, 0, textColor, false);

        graphics.pose().popPose();
    }

    private void renderPlayerSlot(GuiGraphics graphics, int x, int y, ItemStack stack, boolean hovered) {
        int bgColor = hovered ? 0xA0103010 : 0x80101820;
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);

        int borderColor = hovered ? 0xFF44AA44 : 0x30FFFFFF;
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE - 1, borderColor);

        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private void renderEmptySlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        int bgColor = hovered ? 0x40114411 : 0x30082008;
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);

        int borderColor = 0x3044AA44;
        graphics.renderOutline(x, y, SLOT_SIZE - 1, SLOT_SIZE - 1, borderColor);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
        int maxScroll = getMaxScrollRow();
        if (maxScroll <= 0) return;

        graphics.fill(x - 2, y, x + 2, y + height, 0x60100820);

        int thumbHeight = Math.max(10, height * SHADOW_ROWS / getTotalRows());
        int thumbY = y + (height - thumbHeight) * scrollRow / maxScroll;

        float pulse = (float) (Math.sin(shadowPulse) * 0.2 + 0.8);
        int thumbColor = (int) (200 * pulse) << 24 | 0x6622AA;
        graphics.fill(x - 1, thumbY, x + 1, thumbY + thumbHeight, thumbColor);
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        // ★★★ 排序按钮提示 ★★★
        int btnX = getSideButtonX();
        int btnSize = getSideButtonSize();
        int sortBtnY = topPos + 18;

        if (isInButton(mouseX, mouseY, btnX, sortBtnY, btnSize)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.sort.title"));
            tooltip.add(Component.translatable(ShadowStorageSortHelper.getSortModeName(currentSortMode))
                    .withStyle(s -> s.withColor(0xAAAAFF)));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.sort.click_hint")
                    .withStyle(s -> s.withColor(0x888888)));
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.sort.shift_hint")
                    .withStyle(s -> s.withColor(0x888888)));
            graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }

        // ★★★ HUD编辑按钮提示 ★★★
        int hudBtnY = sortBtnY + btnSize + getSideButtonGap();
        if (isInButton(mouseX, mouseY, btnX, hudBtnY, btnSize)) {
            graphics.renderTooltip(this.font,
                    Component.translatable("gui.jujutsu_addon.shadow_storage.edit_hud"),
                    mouseX, mouseY);
            return;
        }

        // 影子库存物品提示
        int shadowIndex = getShadowSlotIndex(mouseX, mouseY);
        if (shadowIndex >= 0 && shadowIndex < displayItems.size()) {
            ItemStack stack = displayItems.get(shadowIndex);
            List<Component> tooltip = new ArrayList<>();
            tooltip.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));

            // ★★★ 显示实际数量（如果超过64）★★★
            if (stack.getCount() > 64) {
                tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.total_count",
                        stack.getCount()).withStyle(s -> s.withColor(0xFFAA00)));
            }

            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.left_click")
                    .withStyle(s -> s.withColor(0x55FF55)));
            tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.shift_click")
                    .withStyle(s -> s.withColor(0x55AAFF)));
            if (stack.getCount() > 1) {
                tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.ctrl_click")
                        .withStyle(s -> s.withColor(0xFFAA55)));
            }
            graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }

        // 玩家背包物品提示
        int playerSlot = getPlayerSlotIndex(mouseX, mouseY);
        if (playerSlot >= 0) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                ItemStack stack = player.getInventory().getItem(playerSlot);
                if (!stack.isEmpty()) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
                    tooltip.add(Component.empty());
                    tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.store_left")
                            .withStyle(s -> s.withColor(0x55FF55)));
                    tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.store_shift")
                            .withStyle(s -> s.withColor(0x55AAFF)));
                    if (stack.getCount() > 1) {
                        tooltip.add(Component.translatable("gui.jujutsu_addon.shadow_storage.tip.store_ctrl")
                                .withStyle(s -> s.withColor(0xFFAA55)));
                    }
                    graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
                }
            }
        }
    }

    // ==================== 交互 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isInSlot(double mx, double my, int slotX, int slotY) {
        return mx >= slotX && mx < slotX + SLOT_SIZE && my >= slotY && my < slotY + SLOT_SIZE;
    }

    private boolean isInButton(double mx, double my, int btnX, int btnY, int btnSize) {
        return mx >= btnX && mx < btnX + btnSize && my >= btnY && my < btnY + btnSize;
    }

    private int getShadowSlotIndex(double mouseX, double mouseY) {
        int gridX = getShadowGridX();
        int gridY = getShadowGridY();

        if (mouseX < gridX || mouseX >= gridX + COLS * SLOT_SIZE ||
                mouseY < gridY || mouseY >= gridY + getShadowGridHeight()) {
            return -1;
        }

        int col = (int) (mouseX - gridX) / SLOT_SIZE;
        int row = (int) (mouseY - gridY) / SLOT_SIZE;
        return scrollRow * COLS + row * COLS + col;
    }

    private int getPlayerSlotIndex(double mouseX, double mouseY) {
        int gridX = getShadowGridX();

        int invY = getPlayerInvY();
        if (mouseX >= gridX && mouseX < gridX + COLS * SLOT_SIZE &&
                mouseY >= invY && mouseY < invY + 3 * SLOT_SIZE) {
            int col = (int) (mouseX - gridX) / SLOT_SIZE;
            int row = (int) (mouseY - invY) / SLOT_SIZE;
            return 9 + row * 9 + col;
        }

        int hotbarY = getPlayerHotbarY();
        if (mouseX >= gridX && mouseX < gridX + COLS * SLOT_SIZE &&
                mouseY >= hotbarY && mouseY < hotbarY + SLOT_SIZE) {
            int col = (int) (mouseX - gridX) / SLOT_SIZE;
            return col;
        }

        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // ★★★ 侧边按钮 ★★★
        int btnX = getSideButtonX();
        int btnSize = getSideButtonSize();
        int sortBtnY = topPos + 18;
        int hudBtnY = sortBtnY + btnSize + getSideButtonGap();

        // 排序按钮
        if (isInButton(mouseX, mouseY, btnX, sortBtnY, btnSize)) {
            if (hasShiftDown()) {
                // Shift+点击：发送整理命令到服务端
                AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(
                        Action.SORT, -1, currentSortMode.ordinal()));
            } else {
                // 普通点击：切换排序模式（仅客户端显示）
                currentSortMode = ShadowStorageSortHelper.getNextMode(currentSortMode);
                refreshItems();
            }
            return true;
        }

        // HUD编辑按钮
        if (isInButton(mouseX, mouseY, btnX, hudBtnY, btnSize)) {
            this.minecraft.setScreen(new ShadowStorageHudEditScreen());
            return true;
        }

        if (button == 0 || button == 1) {
            // ★★★ 影子库存区域 - 取出 ★★★
            int shadowIndex = getShadowSlotIndex(mouseX, mouseY);
            if (shadowIndex >= 0 && shadowIndex < displayItems.size()) {
                // ★★★ 需要找到原始索引（因为显示是排序后的）★★★
                ItemStack clickedStack = displayItems.get(shadowIndex);
                int realIndex = findRealIndex(clickedStack);

                if (realIndex >= 0) {
                    if (hasShiftDown() || button == 1) {
                        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.RETRIEVE_ALL, realIndex, -1));
                    } else if (hasControlDown() && clickedStack.getCount() > 1) {
                        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.RETRIEVE_HALF, realIndex, -1));
                    } else {
                        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.RETRIEVE_ONE, realIndex, -1));
                    }
                }
                return true;
            }

            // ★★★ 玩家背包区域 - 存入 ★★★
            int playerSlot = getPlayerSlotIndex(mouseX, mouseY);
            if (playerSlot >= 0) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && !player.getInventory().getItem(playerSlot).isEmpty()) {
                    ItemStack stack = player.getInventory().getItem(playerSlot);

                    if (hasShiftDown() || button == 1) {
                        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.STORE_ALL, -1, playerSlot));
                    } else if (hasControlDown() && stack.getCount() > 1) {
                        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.STORE_HALF, -1, playerSlot));
                    } else {
                        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.STORE_ONE, -1, playerSlot));
                    }
                    return true;
                }
            }
        }

        // 滚动条
        int scrollX = leftPos + containerWidth - 5;
        int gridY = getShadowGridY();
        int gridHeight = getShadowGridHeight();
        if (mouseX >= scrollX - 3 && mouseX <= scrollX + 3 &&
                mouseY >= gridY && mouseY < gridY + gridHeight) {
            isScrolling = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 根据显示的物品找到原始列表中的索引
     */
    private int findRealIndex(ItemStack displayStack) {
        for (int i = 0; i < cachedItems.size(); i++) {
            ItemStack cached = cachedItems.get(i);
            if (ItemStack.isSameItemSameTags(cached, displayStack) &&
                    cached.getCount() == displayStack.getCount()) {
                return i;
            }
        }
        // 备用：只比较物品类型
        for (int i = 0; i < cachedItems.size(); i++) {
            if (ItemStack.isSameItemSameTags(cachedItems.get(i), displayStack)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrolling) {
            int gridY = getShadowGridY();
            int gridHeight = getShadowGridHeight();
            int maxScroll = getMaxScrollRow();

            if (maxScroll > 0) {
                float progress = (float) (mouseY - gridY) / gridHeight;
                scrollRow = (int) (progress * (maxScroll + 1));
                scrollRow = Math.max(0, Math.min(scrollRow, maxScroll));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollRow = Math.max(0, Math.min(scrollRow - (int) delta, getMaxScrollRow()));
        return true;
    }

    private int getTotalRows() {
        int totalSlots = displayItems.size() + 1;
        return Math.max(SHADOW_ROWS, (totalSlots + COLS - 1) / COLS);
    }

    private int getMaxScrollRow() {
        return Math.max(0, getTotalRows() - SHADOW_ROWS);
    }

    // ==================== 数量格式化 ====================

    /**
     * 格式化大数字（用于标题栏统计）
     */
    private String formatCount(long count) {
        if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 10_000) {
            return String.format("%.1fK", count / 1_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }

    /**
     * 格式化槽位数量（用于物品格子显示）
     */
    private String formatSlotCount(int count) {
        if (count >= 1_000_000) {
            return String.format("%.0fM", count / 1_000_000.0);
        } else if (count >= 100_000) {
            return String.format("%.0fK", count / 1_000.0);
        } else if (count >= 10_000) {
            return String.format("%.1fK", count / 1_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}