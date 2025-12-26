package com.jujutsuaddon.addon.client.gui.screen.vow;

import com.jujutsuaddon.addon.client.cache.ClientVowDataCache;
import com.jujutsuaddon.addon.client.gui.util.VowGuiColors;
import com.jujutsuaddon.addon.client.util.GuiControlHelper;
import com.jujutsuaddon.addon.client.util.UIScaleHelper;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.ActivateVowC2SPacket;
import com.jujutsuaddon.addon.network.c2s.DissolveVowC2SPacket;
import com.jujutsuaddon.addon.network.c2s.RequestVowListC2SPacket;
import com.jujutsuaddon.addon.network.s2c.SyncVowListS2CPacket;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.VowType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 誓约列表界面
 * Vow List Screen
 */
public class VowListScreen extends Screen implements SyncVowListS2CPacket.VowListScreenAccess {

    // ==================== 数据 ====================
    private final List<CustomBindingVow> vows = new ArrayList<>();
    private CustomBindingVow selectedVow = null;

    // ==================== 布局（动态计算） ====================
    private int listX, listY, listWidth, listHeight;
    private int entryHeight, entryGap;
    private int quickBtnWidth, quickBtnHeight, quickBtnMarginRight, quickBtnOffsetY;
    private int scrollOffset = 0;

    // ==================== 状态 ====================
    private boolean loading = true;
    private String errorMessage = null;
    private CustomBindingVow hoveredQuickBtn = null;

    public VowListScreen() {
        super(Component.translatable("screen.jujutsuaddon.vow_list"));
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        addButtons();

        // ★★★ 核心修复：优先从缓存加载 ★★★
        // 这样从详情页返回时，能立即看到状态更新，而不用等网络包
        List<CustomBindingVow> cachedVows = ClientVowDataCache.getAllVows();
        if (cachedVows != null && !cachedVows.isEmpty()) {
            // 直接更新列表，不显示 Loading
            updateVowList(cachedVows);
            this.loading = false;
        } else {
            // 缓存为空才请求
            requestVowList();
        }

        // 即使加载了缓存，也可以静默请求一次最新数据，确保同步
        AddonNetwork.sendToServer(new RequestVowListC2SPacket());
    }

    private void calculateLayout() {
        // 列表区域布局
        int[] areaLayout = UIScaleHelper.calculateVowListAreaLayout(this.width, this.height);
        listX = areaLayout[0];
        listY = areaLayout[1];
        listWidth = areaLayout[2];
        listHeight = areaLayout[3];

        // 条目布局
        int[] entryLayout = UIScaleHelper.calculateVowListEntryLayout();
        entryHeight = entryLayout[0];
        entryGap = entryLayout[1];
        quickBtnWidth = entryLayout[2];
        quickBtnHeight = entryLayout[3];
        quickBtnMarginRight = entryLayout[4];
        quickBtnOffsetY = entryLayout[5];
    }

    private void addButtons() {
        int[] btnLayout = UIScaleHelper.calculateButtonLayout(this.width, this.height,
                listY + listHeight, 10);
        int btnY = btnLayout[0];
        int btnHeight = btnLayout[1];
        int btnWidth = btnLayout[3]; // medium size

        int centerX = this.width / 2;
        int gap = 10;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("button.jujutsuaddon.vow.create"),
                        btn -> openCreateScreen())
                .bounds(centerX - btnWidth - gap, btnY, btnWidth, btnHeight)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("button.jujutsuaddon.vow.refresh"),
                        btn -> requestVowList())
                .bounds(centerX + gap, btnY, btnWidth, btnHeight)
                .build());
    }

    private void requestVowList() {
        loading = true;
        errorMessage = null;
        AddonNetwork.sendToServer(new RequestVowListC2SPacket());
    }

    // ==================== 数据更新 ====================

    public void updateVowList(List<CustomBindingVow> newVows) {
        this.vows.clear();
        this.vows.addAll(newVows);
        this.loading = false;
        this.errorMessage = null;

        if (selectedVow != null) {
            UUID selectedId = selectedVow.getVowId();
            selectedVow = vows.stream()
                    .filter(v -> v.getVowId().equals(selectedId))
                    .findFirst()
                    .orElse(null);
        }
    }

    public void setError(String message) {
        this.loading = false;
        this.errorMessage = message;
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        renderTitle(graphics);
        renderListPanel(graphics, mouseX, mouseY);
        renderStats(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // 快捷按钮 tooltip
        if (hoveredQuickBtn != null) {
            Component tooltip = getQuickButtonTooltip(hoveredQuickBtn);
            graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void renderTitle(GuiGraphics graphics) {
        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, 15, VowGuiColors.TEXT_TITLE);
    }

    private void renderListPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + listHeight + 2,
                VowGuiColors.PANEL_BG);
        graphics.renderOutline(listX - 2, listY - 2, listWidth + 4, listHeight + 4,
                VowGuiColors.PANEL_BORDER);

        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        hoveredQuickBtn = null;

        if (loading) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.jujutsuaddon.loading"),
                    listX + listWidth / 2, listY + listHeight / 2 - 5,
                    VowGuiColors.TEXT_SECONDARY);
        } else if (errorMessage != null) {
            graphics.drawCenteredString(this.font, errorMessage,
                    listX + listWidth / 2, listY + listHeight / 2 - 5,
                    VowGuiColors.TEXT_ERROR);
        } else if (vows.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.jujutsuaddon.vow.empty"),
                    listX + listWidth / 2, listY + listHeight / 2 - 5,
                    VowGuiColors.TEXT_SECONDARY);
        } else {
            renderVowEntries(graphics, mouseX, mouseY);
        }

        graphics.disableScissor();
    }

    private void renderVowEntries(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = listY - scrollOffset;

        for (CustomBindingVow vow : vows) {
            if (y + entryHeight > listY && y < listY + listHeight) {
                boolean hovered = mouseX >= listX && mouseX < listX + listWidth &&
                        mouseY >= y && mouseY < y + entryHeight &&
                        mouseY >= listY && mouseY < listY + listHeight;
                boolean selected = vow == selectedVow;

                renderVowEntry(graphics, vow, listX, y, listWidth, entryHeight,
                        hovered, selected, mouseX, mouseY);
            }
            y += entryHeight + entryGap;
        }
    }

    private void renderVowEntry(GuiGraphics graphics, CustomBindingVow vow,
                                int x, int y, int width, int height,
                                boolean hovered, boolean selected,
                                int mouseX, int mouseY) {
        // 背景
        int bgColor = selected ? VowGuiColors.ENTRY_BG_SELECTED :
                hovered ? VowGuiColors.ENTRY_BG_HOVERED : VowGuiColors.ENTRY_BG_NORMAL;
        graphics.fill(x, y, x + width, y + height, bgColor);

        // 边框
        int borderColor = selected ? VowGuiColors.ENTRY_BORDER_SELECTED :
                hovered ? VowGuiColors.ENTRY_BORDER_HOVERED : VowGuiColors.ENTRY_BORDER_NORMAL;
        graphics.renderOutline(x, y, width, height, borderColor);

        // 状态指示器
        int stateColor = VowGuiColors.getStateColor(vow.getState().name());
        graphics.fill(x + 2, y + 2, x + 6, y + height - 2, stateColor);

        // 名称
        graphics.drawString(this.font, vow.getName(), x + 12, y + 6, VowGuiColors.TEXT_NORMAL);

        // 类型标签
        Component typeLabel = Component.translatable("vow.type." + vow.getType().name().toLowerCase());
        int typeColor = vow.isPermanent() ? VowGuiColors.TYPE_PERMANENT : VowGuiColors.TYPE_DISSOLVABLE;
        graphics.drawString(this.font, typeLabel, x + 12, y + 18, typeColor);

        // 状态
        Component stateLabel = Component.translatable("vow.state." + vow.getState().name().toLowerCase());
        graphics.drawString(this.font, stateLabel, x + 12, y + 30, stateColor);

        // ★ 快捷按钮
        int btnX = x + width - quickBtnWidth - quickBtnMarginRight;
        int btnY = y + quickBtnOffsetY;
        renderQuickButton(graphics, vow, btnX, btnY, mouseX, mouseY);

        // 条件/收益数量（按钮左边）
        int infoX = btnX - 85;
        String condCount = "§b" + vow.getConditions().size() + " §7条件";
        String benefitCount = "§6" + vow.getBenefits().size() + " §7收益";
        graphics.drawString(this.font, condCount, infoX, y + 12, 0xFFFFFF);
        graphics.drawString(this.font, benefitCount, infoX, y + 26, 0xFFFFFF);
    }

    /**
     * ★ 渲染快捷激活/停用按钮
     */
    private void renderQuickButton(GuiGraphics graphics, CustomBindingVow vow,
                                   int x, int y, int mouseX, int mouseY) {
        VowState state = vow.getState();
        boolean canQuickToggle = canQuickToggle(vow);
        if (!canQuickToggle) {
            // 不可操作的状态：灰色背景 + 灰色边框
            graphics.fill(x, y, x + quickBtnWidth, y + quickBtnHeight, VowGuiColors.BTN_DISABLED_BG);
            graphics.renderOutline(x, y, quickBtnWidth, quickBtnHeight, VowGuiColors.BTN_DISABLED_BORDER);
            // ★★★ 修复点：根据原因显示不同图标 ★★★
            Component label;
            if (state == VowState.VIOLATED || state == VowState.EXPIRED) {
                label = Component.literal("✗"); // 失败/违约
            } else if (state == VowState.EXHAUSTED) {
                label = Component.literal("✓"); // ★ 已耗尽/已完成
            } else if (state != VowState.ACTIVE) {
                // ★ 注意：因为 canQuickToggle 已经处理了创造模式逻辑
                // 所以如果能进到这里，说明肯定不是创造模式，或者有其他阻挡原因
                if (ClientVowDataCache.isUnderPenalty()) {
                    label = Component.literal("⏳");
                } else if (ClientVowDataCache.containsOccupiedCondition(vow)) {
                    label = Component.literal("⚠");
                } else {
                    label = Component.literal("—");
                }
            } else {
                label = Component.literal("—");
            }
            int labelX = x + (quickBtnWidth - font.width(label)) / 2;
            int labelY = y + (quickBtnHeight - font.lineHeight) / 2;
            graphics.drawString(font, label, labelX, labelY, VowGuiColors.TEXT_DISABLED);
            if (GuiControlHelper.isInArea(mouseX, mouseY, x, y, quickBtnWidth, quickBtnHeight)
                    && mouseY >= listY && mouseY < listY + listHeight) {
                hoveredQuickBtn = vow;
            }
            return;
        }
        boolean btnHovered = GuiControlHelper.isInArea(mouseX, mouseY, x, y, quickBtnWidth, quickBtnHeight)
                && mouseY >= listY && mouseY < listY + listHeight;
        if (btnHovered) {
            hoveredQuickBtn = vow;
        }
        // 根据状态决定按钮样式
        int bgColor, borderColor, textColor;
        Component label;
        if (state == VowState.ACTIVE) {
            // 激活中 -> 显示停用 (红/Danger)
            bgColor = btnHovered ? VowGuiColors.BTN_DANGER_HOVER : VowGuiColors.BTN_DANGER_BG;
            borderColor = VowGuiColors.BTN_DANGER_BORDER;
            textColor = VowGuiColors.TEXT_NORMAL;
            label = Component.translatable("button.jujutsuaddon.vow.quick_deactivate");
        } else {
            // 未激活 -> 显示激活 (绿/Success)
            bgColor = btnHovered ? VowGuiColors.BTN_SUCCESS_HOVER : VowGuiColors.BTN_SUCCESS_BG;
            borderColor = VowGuiColors.BTN_SUCCESS_BORDER;
            textColor = VowGuiColors.TEXT_NORMAL;
            label = Component.translatable("button.jujutsuaddon.vow.quick_activate");
        }
        // 渲染按钮
        graphics.fill(x, y, x + quickBtnWidth, y + quickBtnHeight, bgColor);
        graphics.renderOutline(x, y, quickBtnWidth, quickBtnHeight, borderColor);
        // 文字居中
        int labelX = x + (quickBtnWidth - font.width(label)) / 2;
        int labelY = y + (quickBtnHeight - font.lineHeight) / 2;
        graphics.drawString(font, label, labelX, labelY, textColor);
    }

    /**
     * 判断是否可以快捷切换状态
     */
    private boolean canQuickToggle(CustomBindingVow vow) {
        VowState state = vow.getState();
        // 获取创造模式状态
        boolean isCreative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();
        // ★★★ 修复点：加入 EXHAUSTED 状态检查 ★★★
        // 已违约 / 已过期 / 已耗尽 -> 不能直接通过快捷按钮激活
        if (state == VowState.VIOLATED || state == VowState.EXPIRED || state == VowState.EXHAUSTED) {
            return false;
        }
        // 激活中的永久誓约不能停用（除非创造模式）
        if (state == VowState.ACTIVE && vow.isPermanent()) {
            return isCreative;
        }
        // ★ 新增：如果要激活，检查惩罚和组合
        if (state != VowState.ACTIVE) {
            // ★★★ 核心修改：如果是创造模式，无视惩罚时间 ★★★
            if (!isCreative && ClientVowDataCache.isUnderPenalty()) return false;

            if (ClientVowDataCache.containsOccupiedCondition(vow)) return false; // 组合占用不可激活
        }
        return true;
    }

    /**
     * 获取快捷按钮的 tooltip
     */
    private Component getQuickButtonTooltip(CustomBindingVow vow) {
        VowState state = vow.getState();
        // 获取创造模式状态
        boolean isCreative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();

        if (state == VowState.ACTIVE) {
            if (vow.isPermanent()) {
                return Component.translatable("tooltip.jujutsuaddon.vow.deactivate_permanent");
            }
            return Component.translatable("tooltip.jujutsuaddon.vow.click_to_deactivate");
        } else {
            // ★ 新增提示
            // ★★★ 核心修改：如果是创造模式，不显示惩罚提示 ★★★
            if (!isCreative && ClientVowDataCache.isUnderPenalty()) {
                long s = ClientVowDataCache.getPenaltySecondsLeft();
                return Component.translatable("tooltip.jujutsuaddon.vow.under_penalty", s);
            }
            if (ClientVowDataCache.containsOccupiedCondition(vow)) {
                return Component.translatable("tooltip.jujutsuaddon.vow.contains_used_pair");
            }
            return Component.translatable("tooltip.jujutsuaddon.vow.click_to_activate");
        }
    }

    private void renderStats(GuiGraphics graphics) {
        if (loading || vows.isEmpty()) return;

        long activeCount = vows.stream().filter(v -> v.getState() == VowState.ACTIVE).count();
        String stats = String.format("§a%d §7激活 §8/ §f%d §7总计", activeCount, vows.size());
        graphics.drawString(this.font, stats, listX, listY + listHeight + 8, 0xFFFFFF);
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {

            int y = listY - scrollOffset;
            for (CustomBindingVow vow : vows) {
                if (mouseY >= Math.max(y, listY) && mouseY < Math.min(y + entryHeight, listY + listHeight)) {

                    // ★ 检查是否点击了快捷按钮
                    int btnX = listX + listWidth - quickBtnWidth - quickBtnMarginRight;
                    int btnY = y + quickBtnOffsetY;

                    if (GuiControlHelper.isInArea(mouseX, mouseY, btnX, btnY, quickBtnWidth, quickBtnHeight)) {
                        if (canQuickToggle(vow)) {
                            handleQuickToggle(vow);
                            return true;
                        }
                    }

                    // 左键选中条目
                    if (button == 0) {
                        selectedVow = vow;
                        if (System.currentTimeMillis() - lastClickTime < 300) {
                            openDetailScreen(vow);
                        }
                        lastClickTime = System.currentTimeMillis();
                    }
                    return true;
                }
                y += entryHeight + entryGap;
            }
        }

        return false;
    }

    /**
     * ★ 处理快捷切换
     */
    private void handleQuickToggle(CustomBindingVow vow) {
        VowState state = vow.getState();

        if (state == VowState.ACTIVE) {
            // 停用
            AddonNetwork.sendToServer(new DissolveVowC2SPacket(vow.getVowId()));
            vow.setState(VowState.DISSOLVED);
        } else {
            // 激活
            AddonNetwork.sendToServer(new ActivateVowC2SPacket(vow.getVowId()));
            vow.setState(VowState.ACTIVE);
        }
    }

    private long lastClickTime = 0;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {

            int totalHeight = vows.size() * (entryHeight + entryGap);
            int maxScroll = Math.max(0, totalHeight - listHeight);
            scrollOffset = UIScaleHelper.clamp(scrollOffset - (int)(delta * 20), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && selectedVow != null) {
            openDetailScreen(selectedVow);
            return true;
        }
        if (keyCode == 261 && selectedVow != null && !selectedVow.isPermanent()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== 导航 ====================

    private void openCreateScreen() {
        this.minecraft.setScreen(new VowCreateScreen(this));
    }

    private void openDetailScreen(CustomBindingVow vow) {
        this.minecraft.setScreen(new VowDetailScreen(this, vow));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
