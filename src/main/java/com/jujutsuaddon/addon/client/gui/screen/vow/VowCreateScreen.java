package com.jujutsuaddon.addon.client.gui.screen.vow;

import com.jujutsuaddon.addon.api.vow.IBenefit;
import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.client.cache.ClientVowDataCache; // ★ 导入缓存类
import com.jujutsuaddon.addon.client.gui.util.VowGuiColors;
import com.jujutsuaddon.addon.client.gui.widget.vow.SelectionPanelWidget;
import com.jujutsuaddon.addon.client.gui.widget.vow.WeightBarWidget;
import com.jujutsuaddon.addon.client.util.VowGuiHelper;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.CreateVowC2SPacket;
import com.jujutsuaddon.addon.vow.CustomBindingVow; // ★ 导入 vow 类
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.benefit.BenefitParams;
import com.jujutsuaddon.addon.vow.benefit.BenefitRegistry;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.condition.ConditionRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 誓约创建界面 - 重构版 (1对1 交易逻辑)
 */
public class VowCreateScreen extends Screen {

    private final Screen parent;

    // ==================== 数据 (改为单体对象) ====================
    private String vowName = "";
    private VowType vowType = VowType.DISSOLVABLE;

    private ConditionEntry selectedCondition = null;
    private BenefitEntry selectedBenefit = null;

    // ==================== 组件 ====================
    private EditBox nameInput;
    private CycleButton<VowType> typeButton;
    private WeightBarWidget weightBar;
    private SelectionPanelWidget<ICondition> conditionPanel;
    private SelectionPanelWidget<IBenefit> benefitPanel;
    private Button createButton;
    private Button helpButton;

    // ==================== 布局 ====================
    private int leftPanelX, leftPanelWidth;
    private int rightPanelX, rightPanelWidth;
    private int panelY, panelHeight;

    public VowCreateScreen(Screen parent) {
        super(Component.translatable("screen.jujutsuaddon.vow_create"));
        this.parent = parent;
        // ★★★ 初始化默认名称 ★★★
        this.vowName = generateDefaultName();
    }

    /**
     * ★★★ 生成默认名称 (Vow #X) ★★★
     * 避免硬编码，使用 Translation Key
     */
    private String generateDefaultName() {
        int count = 0;
        // 从缓存获取当前列表大小
        List<CustomBindingVow> list = ClientVowDataCache.getAllVows();
        if (list != null) {
            count = list.size();
        }
        // 建议在 lang 文件中添加: "input.jujutsuaddon.vow.default_name": "Vow #%s"
        // 或者中文: "input.jujutsuaddon.vow.default_name": "束缚 #%s"
        return Component.translatable("input.jujutsuaddon.vow.default_name", count + 1).getString();
    }

    @Override
    protected void init() {
        super.init();

        calculateLayout();
        addInputs();
        addPanels();
        addButtons();
        updateWeightBar();
    }

    private void calculateLayout() {
        int margin = 15;
        int gap = 8;
        int topSpace = 55;
        int bottomSpace = 55;
        leftPanelX = margin;
        leftPanelWidth = (this.width - margin * 2 - gap) / 2;
        rightPanelX = leftPanelX + leftPanelWidth + gap;
        rightPanelWidth = leftPanelWidth;
        panelY = topSpace;
        panelHeight = this.height - topSpace - bottomSpace;
    }

    private void addInputs() {
        int inputWidth = 200;
        int inputX = this.width / 2 - inputWidth / 2 - 60;

        nameInput = new EditBox(this.font, inputX, 35, inputWidth, 18,
                Component.translatable("input.jujutsuaddon.vow.name"));
        nameInput.setMaxLength(32);
        // ★ 这里会自动填入构造函数里生成的 vowName
        nameInput.setValue(vowName);
        nameInput.setResponder(s -> {
            vowName = s;
            updateCreateButton();
        });
        this.addRenderableWidget(nameInput);

        int typeX = inputX + inputWidth + 10;
        typeButton = CycleButton.<VowType>builder(type ->
                        Component.translatable("vow.type." + type.name().toLowerCase()))
                .withValues(VowType.values())
                .withInitialValue(vowType)
                .create(typeX, 35, 100, 18,
                        Component.translatable("input.jujutsuaddon.vow.type"),
                        (btn, value) -> {
                            this.vowType = value;
                            this.onModeChanged();
                        });
        this.addRenderableWidget(typeButton);
    }

    /**
     * 模式切换处理：清空已选内容并更新列表
     */
    private void onModeChanged() {
        // 1. 清空数据对象
        this.selectedCondition = null;
        this.selectedBenefit = null;

        // 2. 清空UI选择状态
        if (conditionPanel != null) conditionPanel.clearSelection();
        if (benefitPanel != null) benefitPanel.clearSelection();

        // 3. 更新列表内容 (过滤)
        if (conditionPanel != null) {
            conditionPanel.updateItems(getFilteredConditions());
        }
        if (benefitPanel != null) {
            benefitPanel.updateItems(getFilteredBenefits());
        }

        updateWeightBar();
        updateCreateButton();
    }

    /**
     * 获取当前模式允许的条件列表
     */
    private List<ICondition> getFilteredConditions() {
        return ConditionRegistry.getAll().stream()
                .filter(c -> c.getAllowedVowType() == this.vowType)
                // ★★★ 新增：过滤当前玩家不可用的条件 ★★★
                .filter(c -> c.isAvailable(this.minecraft.player))
                .toList();
    }
    /**
     * 获取当前模式允许的收益列表
     */
    private List<IBenefit> getFilteredBenefits() {
        return BenefitRegistry.getAll().stream()
                .filter(b -> b.getAllowedVowType() == this.vowType)
                // ★★★ 新增：过滤当前玩家不可用的收益 (例如非御厨子玩家看不到世界斩收益) ★★★
                .filter(b -> b.isAvailable(this.minecraft.player))
                .toList();
    }

    private void addPanels() {
        // 条件选择面板
        conditionPanel = new SelectionPanelWidget<>(
                leftPanelX, panelY, leftPanelWidth, panelHeight,
                Component.translatable("panel.jujutsuaddon.conditions"),
                getFilteredConditions(), // 初始化使用过滤列表
                ICondition::getDisplayName,
                condition -> condition.getDescription(new ConditionParams()),
                ICondition::getId,
                this::onConditionSelected,
                this::onConditionRemoved
        );
        conditionPanel.setSlotColor(VowGuiColors.SLOT_CONDITION_BG, VowGuiColors.SLOT_CONDITION_BORDER);
        conditionPanel.setSingleSelect(true);

        conditionPanel.setParamDefGetter(condition ->
                condition.isConfigurable() ? condition.getConfigurableParams() : null
        );

        conditionPanel.setDynamicDescGetter((condition, params) -> {
            ConditionParams cp = mapToConditionParams(params);
            return condition.getDescription(cp);
        });

        conditionPanel.setOnParamChanged((condition, params) -> {
            if (selectedCondition != null && selectedCondition.getCondition().getId().equals(condition.getId())) {
                selectedCondition.setParams(mapToConditionParams(params));
                updateWeightBar();
            }
        });

        // ★★★ 新增：设置具体的搜索提示 ★★★
        conditionPanel.setSearchHint(Component.translatable("widget.jujutsuaddon.search.hint.condition"));

        this.addRenderableWidget(conditionPanel);

        // 收益选择面板
        benefitPanel = new SelectionPanelWidget<>(
                rightPanelX, panelY, rightPanelWidth, panelHeight,
                Component.translatable("panel.jujutsuaddon.benefits"),
                getFilteredBenefits(), // 初始化使用过滤列表
                IBenefit::getDisplayName,
                benefit -> benefit.getDescription(new BenefitParams()),
                IBenefit::getId,
                this::onBenefitSelected,
                this::onBenefitRemoved
        );
        benefitPanel.setSlotColor(VowGuiColors.SLOT_BENEFIT_BG, VowGuiColors.SLOT_BENEFIT_BORDER);
        benefitPanel.setSingleSelect(true);

        benefitPanel.setParamDefGetter(benefit ->
                benefit.isConfigurable() ? benefit.getConfigurableParams() : null
        );

        benefitPanel.setDynamicDescGetter((benefit, params) -> {
            BenefitParams bp = mapToBenefitParams(params);
            return benefit.getDescription(bp);
        });

        benefitPanel.setOnParamChanged((benefit, params) -> {
            if (selectedBenefit != null && selectedBenefit.getBenefit().getId().equals(benefit.getId())) {
                selectedBenefit.setParams(mapToBenefitParams(params));
                updateWeightBar();
            }
        });

        // ★★★ 新增：设置具体的搜索提示 ★★★
        benefitPanel.setSearchHint(Component.translatable("widget.jujutsuaddon.search.hint.benefit"));

        this.addRenderableWidget(benefitPanel);

        // 权重条
        int barY = panelY + panelHeight + 5;
        int barWidth = this.width - 60;
        int barHeight = 10;
        weightBar = new WeightBarWidget(30, barY, barWidth, barHeight);
        this.addRenderableWidget(weightBar);
    }

    private ConditionParams mapToConditionParams(Map<String, Object> params) {
        ConditionParams cp = new ConditionParams();
        params.forEach((k, v) -> {
            if (v instanceof Integer i) cp.setInt(k, i);
            else if (v instanceof Float f) cp.setFloat(k, f);
            else if (v instanceof Boolean b) cp.setBoolean(k, b);
            else if (v instanceof String s) cp.setString(k, s);
        });
        return cp;
    }

    private BenefitParams mapToBenefitParams(Map<String, Object> params) {
        BenefitParams bp = new BenefitParams();
        params.forEach((k, v) -> {
            if (v instanceof Integer i) bp.setInt(k, i);
            else if (v instanceof Float f) bp.setFloat(k, f);
            else if (v instanceof Boolean b) bp.setBoolean(k, b);
            else if (v instanceof String s) bp.setString(k, s);
        });
        return bp;
    }

    private void addButtons() {
        int btnY = panelY + panelHeight + 22;
        int btnWidth = 80;
        int gap = 15;
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.cancel"),
                        btn -> onClose())
                .bounds(centerX - btnWidth - gap, btnY, btnWidth, 20)
                .build());

        createButton = Button.builder(
                        Component.translatable("button.jujutsuaddon.vow.confirm_create"),
                        btn -> createVow())
                .bounds(centerX + gap, btnY, btnWidth, 20)
                .build();
        createButton.active = false;
        this.addRenderableWidget(createButton);

        helpButton = Button.builder(
                        Component.literal("?"),
                        btn -> showWeightExplanation())
                .bounds(this.width - 25, 5, 20, 20)
                .build();
        this.addRenderableWidget(helpButton);
    }

    private void showWeightExplanation() {
        this.minecraft.setScreen(new WeightExplanationScreen(this));
    }

    private void onConditionSelected(ICondition condition) {
        this.selectedCondition = new ConditionEntry(condition, condition.createDefaultParams());
        updateWeightBar();
        updateCreateButton();
    }

    private void onConditionRemoved(ICondition condition) {
        if (selectedCondition != null && selectedCondition.getCondition().getId().equals(condition.getId())) {
            selectedCondition = null;
        }
        updateWeightBar();
        updateCreateButton();
    }

    private void onBenefitSelected(IBenefit benefit) {
        this.selectedBenefit = new BenefitEntry(benefit, benefit.createDefaultParams());
        updateWeightBar();
        updateCreateButton();
    }

    private void onBenefitRemoved(IBenefit benefit) {
        if (selectedBenefit != null && selectedBenefit.getBenefit().getId().equals(benefit.getId())) {
            selectedBenefit = null;
        }
        updateWeightBar();
        updateCreateButton();
    }

    private void updateWeightBar() {
        float conditionWeight = 0;
        if (selectedCondition != null) {
            conditionWeight = selectedCondition.getCondition().calculateWeight(selectedCondition.getParams());
        }

        float benefitCost = 0;
        if (selectedBenefit != null) {
            benefitCost = selectedBenefit.getBenefit().getRequiredWeight(selectedBenefit.getParams());
        }

        weightBar.setWeights(conditionWeight, benefitCost);
    }

    private void updateCreateButton() {
        boolean hasName = !vowName.trim().isEmpty();
        boolean hasCondition = selectedCondition != null;
        boolean hasBenefit = selectedBenefit != null;
        boolean isBalanced = weightBar.isBalanced();

        createButton.active = hasName && hasCondition && hasBenefit && isBalanced;
    }

    private void createVow() {
        if (!createButton.active) return;

        AddonNetwork.sendToServer(new CreateVowC2SPacket(
                vowName.trim(),
                vowType,
                Collections.singletonList(selectedCondition),
                Collections.singletonList(selectedBenefit)
        ));

        onClose();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (conditionPanel != null && conditionPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (benefitPanel != null && benefitPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (conditionPanel != null) {
            handled |= conditionPanel.mouseReleased(mouseX, mouseY, button);
        }
        if (benefitPanel != null) {
            handled |= benefitPanel.mouseReleased(mouseX, mouseY, button);
        }
        if (handled) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, 12, VowGuiColors.TEXT_TITLE);

        graphics.drawString(this.font,
                Component.translatable("label.jujutsuaddon.vow.name"),
                nameInput.getX(), nameInput.getY() - 12, VowGuiColors.TEXT_SECONDARY);

        renderHints(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        renderHelpButtonTooltip(graphics, mouseX, mouseY);
    }

    private void renderHelpButtonTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (helpButton != null && helpButton.isHovered()) {
            List<Component> tooltip = VowGuiHelper.getWeightTooltip();
            graphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderHints(GuiGraphics graphics) {
        if (!weightBar.isBalanced()) {
            Component hint;
            int color;
            if (weightBar.isOverflow()) {
                hint = Component.translatable("hint.jujutsuaddon.vow.overflow");
                color = VowGuiColors.TEXT_ERROR;
            } else {
                hint = Component.translatable("hint.jujutsuaddon.vow.add_more_conditions");
                color = VowGuiColors.TEXT_WARNING;
            }

            int hintY = panelY + panelHeight - 12;
            graphics.drawCenteredString(this.font, hint, this.width / 2, hintY, color);
        }
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
