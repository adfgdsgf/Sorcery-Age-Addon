package com.jujutsuaddon.addon.client.gui.screen.vow;

import com.jujutsuaddon.addon.client.cache.ClientVowDataCache;
import com.jujutsuaddon.addon.client.gui.util.VowGuiColors;
import com.jujutsuaddon.addon.client.util.UIScaleHelper;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.ActivateVowC2SPacket;
import com.jujutsuaddon.addon.network.c2s.DeleteVowC2SPacket;
import com.jujutsuaddon.addon.network.c2s.DissolveVowC2SPacket;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 誓约详情界面
 * Vow Detail Screen - 重新设计版
 */
public class VowDetailScreen extends Screen {

    // ==================== 数据 ====================
    private final Screen parent;
    private final CustomBindingVow vow;

    // ==================== 布局 ====================
    private int contentX, contentY, contentWidth, contentHeight;
    private int leftPanelX, leftPanelWidth;
    private int rightPanelX, rightPanelWidth;
    private int scrollOffsetConditions = 0;
    private int scrollOffsetBenefits = 0;

    // ★ 新增：持有主要操作按钮的引用，以便在 render 中动态更新 Tooltip
    private Button actionBtn;

    // ★ 增加条目高度以容纳更多信息
    private static final int ENTRY_HEIGHT = 52;
    private static final int ENTRY_GAP = 4;
    private static final int PANEL_PADDING = 8;

    public VowDetailScreen(Screen parent, CustomBindingVow vow) {
        super(Component.translatable("screen.jujutsuaddon.vow_detail"));
        this.parent = parent;
        this.vow = vow;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        addButtons();
    }

    private void calculateLayout() {
        int margin = 20;
        int topSpace = 70;
        int bottomSpace = 70; // 增加底部空间给状态栏
        int gap = 10;

        contentX = margin;
        contentY = topSpace;
        contentWidth = this.width - margin * 2;
        contentHeight = this.height - topSpace - bottomSpace;

        leftPanelX = contentX;
        leftPanelWidth = (contentWidth - gap) / 2;
        rightPanelX = leftPanelX + leftPanelWidth + gap;
        rightPanelWidth = leftPanelWidth;
    }

    private void addButtons() {
        int btnY = this.height - 35;
        int btnWidth = 80;
        int gap = 10;
        int centerX = this.width / 2;

        VowState state = vow.getState();
        boolean isCreative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();
        boolean canDeactivate = vow.getType() == VowType.DISSOLVABLE || isCreative;

        // 重置按钮引用
        this.actionBtn = null;

        if (state == VowState.DISSOLVED) {
            // ========== 已解除状态：返回 | 重新激活 | 删除 ==========
            this.addRenderableWidget(Button.builder(
                            Component.translatable("gui.back"),
                            btn -> onClose())
                    .bounds(centerX - btnWidth - gap - btnWidth / 2, btnY, btnWidth, 20)
                    .build());

            // ★ 重新激活按钮 (保存引用)
            this.actionBtn = Button.builder(
                            Component.translatable("button.jujutsuaddon.vow.reactivate"),
                            btn -> activateVow())
                    .bounds(centerX - btnWidth / 2, btnY, btnWidth, 20)
                    .build();
            this.addRenderableWidget(this.actionBtn);

            this.addRenderableWidget(Button.builder(
                            Component.translatable("button.jujutsuaddon.vow.delete"),
                            btn -> deleteVow())
                    .bounds(centerX + btnWidth / 2 + gap, btnY, btnWidth, 20)
                    .build());

        } else if (state == VowState.ACTIVE) {
            // ========== 激活状态 ==========
            if (canDeactivate) {
                this.addRenderableWidget(Button.builder(
                                Component.translatable("gui.back"),
                                btn -> onClose())
                        .bounds(centerX - btnWidth - gap / 2, btnY, btnWidth, 20)
                        .build());

                this.addRenderableWidget(Button.builder(
                                Component.translatable("button.jujutsuaddon.vow.deactivate"),
                                btn -> deactivateVow())
                        .bounds(centerX + gap / 2, btnY, btnWidth, 20)
                        .build());
            } else {
                this.addRenderableWidget(Button.builder(
                                Component.translatable("gui.back"),
                                btn -> onClose())
                        .bounds(centerX - btnWidth / 2, btnY, btnWidth, 20)
                        .build());
            }

        } else if (state == VowState.VIOLATED || state == VowState.EXPIRED) {
            // ========== 违约/过期状态：返回 | 重置 | 删除 ==========
            // ★★★ 所有人都可以重置 ★★★

            // 1. 返回
            this.addRenderableWidget(Button.builder(
                            Component.translatable("gui.back"),
                            btn -> onClose())
                    .bounds(centerX - btnWidth - gap - btnWidth / 2, btnY, btnWidth, 20)
                    .build());

            // 2. 重置 (Reset)
            this.addRenderableWidget(Button.builder(
                            Component.translatable("button.jujutsuaddon.vow.reset"),
                            btn -> resetVow())
                    .bounds(centerX - btnWidth / 2, btnY, btnWidth, 20)
                    .build());

            // 3. 删除
            this.addRenderableWidget(Button.builder(
                            Component.translatable("button.jujutsuaddon.vow.delete"),
                            btn -> deleteVow())
                    .bounds(centerX + btnWidth / 2 + gap, btnY, btnWidth, 20)
                    .build());

        } else if (state == VowState.EXHAUSTED) {
            // ========== ★ 新增：已耗尽状态：返回 | 删除 ==========
            // 已耗尽的誓约无法重置，因为它是正常完成的

            // 1. 返回
            this.addRenderableWidget(Button.builder(
                            Component.translatable("gui.back"),
                            btn -> onClose())
                    .bounds(centerX - btnWidth - gap / 2, btnY, btnWidth, 20)
                    .build());

            // 2. 删除
            this.addRenderableWidget(Button.builder(
                            Component.translatable("button.jujutsuaddon.vow.delete"),
                            btn -> deleteVow())
                    .bounds(centerX + gap / 2, btnY, btnWidth, 20)
                    .build());

        } else {
            // ========== 未激活状态（INACTIVE）：返回 | 激活 | 删除 ==========
            this.addRenderableWidget(Button.builder(
                            Component.translatable("gui.back"),
                            btn -> onClose())
                    .bounds(centerX - btnWidth - gap - btnWidth / 2, btnY, btnWidth, 20)
                    .build());

            // ★ 激活按钮 (保存引用)
            this.actionBtn = Button.builder(
                            Component.translatable("button.jujutsuaddon.vow.activate"),
                            btn -> activateVow())
                    .bounds(centerX - btnWidth / 2, btnY, btnWidth, 20)
                    .build();
            this.addRenderableWidget(this.actionBtn);

            this.addRenderableWidget(Button.builder(
                            Component.translatable("button.jujutsuaddon.vow.delete"),
                            btn -> deleteVow())
                    .bounds(centerX + btnWidth / 2 + gap, btnY, btnWidth, 20)
                    .build());
        }

        // 初始化时先更新一次状态
        updateActionBtnState();
    }

    /**
     * ★★★ 核心修复：每帧更新按钮状态和 Tooltip ★★★
     * 解决倒计时不刷新的问题
     */
    private void updateActionBtnState() {
        if (this.actionBtn == null) return;

        // 只有在需要激活（INACTIVE 或 DISSOLVED）时才需要检查条件
        VowState state = vow.getState();
        if (state != VowState.INACTIVE && state != VowState.DISSOLVED) return;

        boolean isCreative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();
        boolean combinationAvailable = !ClientVowDataCache.containsOccupiedCondition(vow);
        boolean notUnderPenaltyRaw = !ClientVowDataCache.isUnderPenalty();

        // 创造模式视为不在惩罚期
        boolean effectiveNotUnderPenalty = notUnderPenaltyRaw || isCreative;
        boolean canActivate = combinationAvailable && effectiveNotUnderPenalty;

        // 1. 更新按钮激活状态
        this.actionBtn.active = canActivate;

        // 2. 动态更新 Tooltip (实现倒计时刷新)
        if (!canActivate) {
            if (!effectiveNotUnderPenalty) {
                // 惩罚中：实时获取剩余秒数
                long seconds = ClientVowDataCache.getPenaltySecondsLeft();
                this.actionBtn.setTooltip(Tooltip.create(
                        Component.translatable("tooltip.jujutsuaddon.vow.under_penalty", seconds)
                                .withStyle(ChatFormatting.RED)
                ));
            } else if (!combinationAvailable) {
                this.actionBtn.setTooltip(Tooltip.create(
                        Component.translatable("tooltip.jujutsuaddon.vow.combination_used")
                                .withStyle(ChatFormatting.RED)
                ));
            }
        } else {
            // 可以激活时，清除 Tooltip
            this.actionBtn.setTooltip(null);
        }
    }

    // ==================== 操作方法 ====================

    private void activateVow() {
        AddonNetwork.sendToServer(new ActivateVowC2SPacket(vow.getVowId()));

        // ★ 本地预测：更新当前对象状态
        vow.setState(VowState.ACTIVE);
        // ★★★ 关键：同步更新全局缓存，这样返回列表时状态才是对的 ★★★
        ClientVowDataCache.updateVowState(vow.getVowId(), VowState.ACTIVE);

        refreshScreen();
    }

    private void deactivateVow() {
        AddonNetwork.sendToServer(new DissolveVowC2SPacket(vow.getVowId()));

        // ★ 本地预测
        vow.setState(VowState.DISSOLVED);
        // ★★★ 关键：同步更新全局缓存 ★★★
        ClientVowDataCache.updateVowState(vow.getVowId(), VowState.DISSOLVED);

        refreshScreen();
    }

    // ★ 新增：重置束缚
    private void resetVow() {
        // 重置其实就是调用解除
        AddonNetwork.sendToServer(new DissolveVowC2SPacket(vow.getVowId()));

        // ★ 本地预测
        vow.setState(VowState.DISSOLVED);
        // ★★★ 关键：同步更新全局缓存 ★★★
        ClientVowDataCache.updateVowState(vow.getVowId(), VowState.DISSOLVED);

        refreshScreen();
    }

    private void deleteVow() {
        AddonNetwork.sendToServer(new DeleteVowC2SPacket(vow.getVowId()));
        // 删除后关闭界面
        onClose();
    }

    /**
     * 刷新界面
     */
    private void refreshScreen() {
        this.clearWidgets();
        this.init();
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // ★★★ 核心修复：在渲染前更新按钮状态，确保倒计时跳动 ★★★
        updateActionBtnState();

        this.renderBackground(graphics);

        // 标题
        graphics.drawCenteredString(this.font, vow.getName(),
                this.width / 2, 15, VowGuiColors.TEXT_TITLE);

        // 基本信息
        renderBasicInfo(graphics);

        // 条件面板
        renderConditionPanel(graphics, mouseX, mouseY);

        // 收益面板
        renderBenefitPanel(graphics, mouseX, mouseY);

        // ★ 新的状态信息栏
        renderStatusBar(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBasicInfo(GuiGraphics graphics) {
        int y = 35;
        int centerX = this.width / 2;

        Component typeLabel = Component.translatable("vow.type." + vow.getType().name().toLowerCase());
        int typeColor = vow.isPermanent() ? VowGuiColors.TYPE_PERMANENT : VowGuiColors.TYPE_DISSOLVABLE;
        graphics.drawCenteredString(this.font, typeLabel, centerX - 60, y, typeColor);

        Component stateLabel = Component.translatable("vow.state." + vow.getState().name().toLowerCase());
        int stateColor = VowGuiColors.getStateColor(vow.getState().name());
        graphics.drawCenteredString(this.font, stateLabel, centerX + 60, y, stateColor);
    }

    private void renderConditionPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelHeight = contentHeight;

        // 面板背景
        graphics.fill(leftPanelX, contentY, leftPanelX + leftPanelWidth, contentY + panelHeight,
                VowGuiColors.PANEL_BG);
        graphics.renderOutline(leftPanelX, contentY, leftPanelWidth, panelHeight,
                VowGuiColors.SLOT_CONDITION_BORDER);

        // 标题
        graphics.drawString(this.font,
                Component.translatable("panel.jujutsuaddon.conditions"),
                leftPanelX + PANEL_PADDING, contentY + PANEL_PADDING,
                VowGuiColors.WEIGHT_CONDITION);

        int listY = contentY + 22;
        int listHeight = panelHeight - 28;

        graphics.enableScissor(leftPanelX, listY, leftPanelX + leftPanelWidth, listY + listHeight);

        int y = listY - scrollOffsetConditions;
        for (ConditionEntry entry : vow.getConditions()) {
            if (y + ENTRY_HEIGHT > listY && y < listY + listHeight) {
                renderConditionEntry(graphics, entry, leftPanelX + PANEL_PADDING, y,
                        leftPanelWidth - PANEL_PADDING * 2);
            }
            y += ENTRY_HEIGHT + ENTRY_GAP;
        }

        graphics.disableScissor();
    }

    private void renderBenefitPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelHeight = contentHeight;

        // 面板背景
        graphics.fill(rightPanelX, contentY, rightPanelX + rightPanelWidth, contentY + panelHeight,
                VowGuiColors.PANEL_BG);
        graphics.renderOutline(rightPanelX, contentY, rightPanelWidth, panelHeight,
                VowGuiColors.SLOT_BENEFIT_BORDER);

        // 标题
        graphics.drawString(this.font,
                Component.translatable("panel.jujutsuaddon.benefits"),
                rightPanelX + PANEL_PADDING, contentY + PANEL_PADDING,
                VowGuiColors.WEIGHT_BENEFIT);

        int listY = contentY + 22;
        int listHeight = panelHeight - 28;

        graphics.enableScissor(rightPanelX, listY, rightPanelX + rightPanelWidth, listY + listHeight);

        int y = listY - scrollOffsetBenefits;
        for (BenefitEntry entry : vow.getBenefits()) {
            if (y + ENTRY_HEIGHT > listY && y < listY + listHeight) {
                renderBenefitEntry(graphics, entry, rightPanelX + PANEL_PADDING, y,
                        rightPanelWidth - PANEL_PADDING * 2);
            }
            y += ENTRY_HEIGHT + ENTRY_GAP;
        }

        graphics.disableScissor();
    }

    /**
     * ★ 渲染条件条目 - 改进版
     * 显示：名称、描述、权重
     */
    private void renderConditionEntry(GuiGraphics graphics, ConditionEntry entry,
                                      int x, int y, int width) {
        // 背景
        graphics.fill(x, y, x + width, y + ENTRY_HEIGHT, VowGuiColors.SLOT_CONDITION_BG);
        graphics.renderOutline(x, y, width, ENTRY_HEIGHT, VowGuiColors.SLOT_CONDITION_BORDER);

        int textX = x + 6;
        int textWidth = width - 12;

        // 第一行：名称
        Component name = entry.getCondition().getDisplayName();
        graphics.drawString(this.font, name, textX, y + 4, VowGuiColors.TEXT_NORMAL);

        // 第二行：描述（具体参数）
        Component description = entry.getCondition().getDescription(entry.getParams());
        // 截断过长的描述
        String descStr = description.getString();
        if (font.width(descStr) > textWidth) {
            descStr = font.plainSubstrByWidth(descStr, textWidth - 10) + "...";
        }
        graphics.drawString(this.font, descStr, textX, y + 18, VowGuiColors.TEXT_DIM);

        // 第三行：权重信息
        float weight = entry.getCondition().calculateWeight(entry.getParams());
        Component weightText = Component.translatable("vow.detail.weight_provided",
                String.format("%.1f", weight));
        graphics.drawString(this.font, weightText, textX, y + 32, VowGuiColors.WEIGHT_CONDITION);
    }

    /**
     * ★ 渲染收益条目 - 改进版
     * 显示：名称、描述、消耗
     */
    private void renderBenefitEntry(GuiGraphics graphics, BenefitEntry entry,
                                    int x, int y, int width) {
        // 背景
        graphics.fill(x, y, x + width, y + ENTRY_HEIGHT, VowGuiColors.SLOT_BENEFIT_BG);
        graphics.renderOutline(x, y, width, ENTRY_HEIGHT, VowGuiColors.SLOT_BENEFIT_BORDER);

        int textX = x + 6;
        int textWidth = width - 12;

        // 第一行：名称
        Component name = entry.getBenefit().getDisplayName();
        graphics.drawString(this.font, name, textX, y + 4, VowGuiColors.TEXT_NORMAL);

        // 第二行：描述（具体效果）
        Component description = entry.getBenefit().getDescription(entry.getParams());
        String descStr = description.getString();
        if (font.width(descStr) > textWidth) {
            descStr = font.plainSubstrByWidth(descStr, textWidth - 10) + "...";
        }
        graphics.drawString(this.font, descStr, textX, y + 18, VowGuiColors.TEXT_DIM);

        // 第三行：消耗权重
        float cost = entry.getBenefit().getRequiredWeight(entry.getParams());
        Component costText = Component.translatable("vow.detail.weight_consumed",
                String.format("%.1f", cost));
        graphics.drawString(this.font, costText, textX, y + 32, VowGuiColors.WEIGHT_BENEFIT);
    }

    /**
     * ★ 渲染状态信息栏 - 替代原来的权重栏
     * 显示有意义的信息：状态、类型、运行时间等
     */
    private void renderStatusBar(GuiGraphics graphics) {
        int barY = contentY + contentHeight + 8;
        int barX = contentX;
        int barWidth = contentWidth;
        int barHeight = 24;

        // 背景
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, VowGuiColors.PANEL_BG);
        graphics.renderOutline(barX, barY, barWidth, barHeight, VowGuiColors.PANEL_BORDER);

        // 分成三个部分显示信息
        int sectionWidth = barWidth / 3;
        int textY = barY + 8;

        // === 左侧：状态 ===
        VowState state = vow.getState();
        Component stateLabel = Component.translatable("vow.state." + state.name().toLowerCase());
        int stateColor = VowGuiColors.getStateColor(state.name());

        String stateIcon = getStateIcon(state);
        Component stateDisplay = Component.literal(stateIcon + " ").append(stateLabel);
        graphics.drawString(this.font, stateDisplay, barX + 10, textY, stateColor);

        // === 中间：类型 ===
        Component typeLabel = Component.translatable("vow.type." + vow.getType().name().toLowerCase());
        int typeColor = vow.isPermanent() ? VowGuiColors.TYPE_PERMANENT : VowGuiColors.TYPE_DISSOLVABLE;

        String typeIcon = vow.isPermanent() ? "🔒" : "🔓";
        Component typeDisplay = Component.literal(typeIcon + " ").append(typeLabel);
        int typeX = barX + sectionWidth + (sectionWidth - font.width(typeDisplay)) / 2;
        graphics.drawString(this.font, typeDisplay, typeX, textY, typeColor);

        // === 右侧：额外信息 ===
        Component extraInfo = getExtraInfo();
        int extraColor = getExtraInfoColor();
        int extraX = barX + sectionWidth * 2 + 10;
        graphics.drawString(this.font, extraInfo, extraX, textY, extraColor);
    }

    /**
     * 获取状态图标
     */
    private String getStateIcon(VowState state) {
        return switch (state) {
            case ACTIVE -> "●";      // 激活中
            case INACTIVE -> "○";    // 未激活
            case DISSOLVED -> "◐";   // 已解除
            case VIOLATED -> "✖";    // 已违约
            case EXPIRED -> "◇";     // 已过期
            case EXHAUSTED -> "☒";   // ★ 新增：已耗尽
        };
    }

    /**
     * 获取额外信息（根据状态显示不同内容）
     */
    private Component getExtraInfo() {
        VowState state = vow.getState();
        boolean isCreative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();

        // ★ 优先显示惩罚信息
        if (state != VowState.ACTIVE) {
            // ★★★ 核心修复：如果是创造模式，不显示惩罚倒计时 ★★★
            if (!isCreative && ClientVowDataCache.isUnderPenalty()) {
                long s = ClientVowDataCache.getPenaltySecondsLeft();
                return Component.translatable("vow.detail.penalty_active", s);
            }
            if (ClientVowDataCache.containsOccupiedCondition(vow)) {
                return Component.translatable("vow.detail.contains_used_pair");
            }
        }

        switch (state) {
            case ACTIVE:
                // 显示效果概览
                float totalWeight = vow.calculateTotalWeight();
                float totalCost = vow.calculateTotalCost();
                float efficiency = totalWeight > 0 ? (totalCost / totalWeight * 100) : 0;
                return Component.translatable("vow.detail.efficiency",
                        String.format("%.0f%%", efficiency));
            case DISSOLVED:
                // 显示可重新激活
                return Component.translatable("vow.detail.can_reactivate");
            case VIOLATED:
                // ★★★ 核心修改：删除了“无法恢复”的废话 ★★★
                // 如果是创造模式，提示可重置；生存模式留空（因为按钮已经很直观了）
                if (isCreative) {
                    return Component.translatable("vow.detail.can_reset");
                } else {
                    return Component.empty();
                }
            case EXPIRED:
                // 显示已过期
                return Component.translatable("vow.detail.expired");
            case EXHAUSTED:
                // ★ 新增：已耗尽提示
                return Component.translatable("vow.detail.exhausted");
            case INACTIVE:
            default:
                // 显示条件和收益数量
                int condCount = vow.getConditions().size();
                int beneCount = vow.getBenefits().size();
                return Component.translatable("vow.detail.counts", condCount, beneCount);
        }
    }

    /**
     * ★ 获取额外信息的颜色
     */
    private int getExtraInfoColor() {
        VowState state = vow.getState();
        boolean isCreative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();

        // 惩罚或占用时显示红色
        if (state != VowState.ACTIVE) {
            // ★★★ 核心修复：如果是创造模式，无视惩罚红色警告 ★★★
            if ((!isCreative && ClientVowDataCache.isUnderPenalty()) || ClientVowDataCache.containsOccupiedCondition(vow)) {
                return VowGuiColors.TEXT_ERROR; // 红色
            }
        }

        // 创造模式的可重置提示用黄色/警告色
        if (state == VowState.VIOLATED && isCreative) {
            return VowGuiColors.TEXT_WARNING;
        }

        // 其他情况用灰色
        return VowGuiColors.TEXT_DIM;
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelHeight = contentHeight;
        int listHeight = panelHeight - 28;

        if (mouseX >= leftPanelX && mouseX < leftPanelX + leftPanelWidth &&
                mouseY >= contentY && mouseY < contentY + panelHeight) {

            int totalHeight = vow.getConditions().size() * (ENTRY_HEIGHT + ENTRY_GAP);
            int maxScroll = Math.max(0, totalHeight - listHeight);
            scrollOffsetConditions = UIScaleHelper.clamp(
                    scrollOffsetConditions - (int) (delta * 20), 0, maxScroll);
            return true;
        }

        if (mouseX >= rightPanelX && mouseX < rightPanelX + rightPanelWidth &&
                mouseY >= contentY && mouseY < contentY + panelHeight) {

            int totalHeight = vow.getBenefits().size() * (ENTRY_HEIGHT + ENTRY_GAP);
            int maxScroll = Math.max(0, totalHeight - listHeight);
            scrollOffsetBenefits = UIScaleHelper.clamp(
                    scrollOffsetBenefits - (int) (delta * 20), 0, maxScroll);
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
