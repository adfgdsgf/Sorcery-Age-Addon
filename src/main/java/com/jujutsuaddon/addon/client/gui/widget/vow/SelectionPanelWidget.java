package com.jujutsuaddon.addon.client.gui.widget.vow;

import com.jujutsuaddon.addon.client.gui.util.VowGuiColors;
import com.jujutsuaddon.addon.client.gui.widget.DropdownWidget;
import com.jujutsuaddon.addon.client.util.DynamicTextHelper;
import com.jujutsuaddon.addon.client.util.FuzzySearchHelper;
import com.jujutsuaddon.addon.client.util.GuiControlHelper;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 选择面板组件
 * 修复：输入框无法点击、焦点丢失、坐标不同步的问题
 */
public class SelectionPanelWidget<T> extends AbstractWidget {

    // ==================== 数据 ====================

    private final List<T> allItems;
    private final List<T> filteredItems = new ArrayList<>();
    private final Set<ResourceLocation> selectedIds = new LinkedHashSet<>();
    private final Map<ResourceLocation, Map<String, Object>> itemParams = new HashMap<>();

    private boolean singleSelect = false;

    // ==================== 回调 ====================

    private final Function<T, Component> nameGetter;
    private final Function<T, Component> descGetter;
    private final Function<T, ResourceLocation> idGetter;

    @Nullable
    private Function<T, ParamDefinition> paramDefGetter;
    @Nullable
    private BiFunction<T, Map<String, Object>, Component> dynamicDescGetter;

    private final Consumer<T> onSelect;
    private final Consumer<T> onDeselect;
    @Nullable
    private BiConsumer<T, Map<String, Object>> onParamChanged;

    // ==================== 布局 ====================

    private SelectionPanelLayout layout = SelectionPanelLayout.COMPACT;

    // ==================== 状态 ====================

    private int scrollOffset = 0;
    private int slotBgColor = VowGuiColors.SLOT_EMPTY_BG;
    private int slotBorderColor = VowGuiColors.SLOT_EMPTY_BORDER;

    private boolean isDragging = false;
    @Nullable
    private T draggingItem = null;
    @Nullable
    private ParamDefinition.Entry draggingEntry = null;
    private int draggingSliderX = 0;

    @Nullable
    private DropdownWidget activeDropdown = null;

    private EditBox searchBox;
    private final Font font;
    private T hoveredItem = null;

    // ==================== 构造 ====================

    public SelectionPanelWidget(int x, int y, int width, int height,
                                Component title,
                                Collection<T> items,
                                Function<T, Component> nameGetter,
                                Function<T, Component> descGetter,
                                Function<T, ResourceLocation> idGetter,
                                Consumer<T> onSelect,
                                Consumer<T> onDeselect) {
        super(x, y, width, height, title);

        this.allItems = new ArrayList<>(items);
        this.filteredItems.addAll(items);
        this.nameGetter = nameGetter;
        this.descGetter = descGetter;
        this.idGetter = idGetter;
        this.onSelect = onSelect;
        this.onDeselect = onDeselect;
        this.font = Minecraft.getInstance().font;

        initSearchBox();
    }

    private void initSearchBox() {
        int searchY = getY() + layout.headerHeight + layout.padding;
        // 初始创建，位置稍后会在 render 中同步
        this.searchBox = new EditBox(font, getX() + layout.padding, searchY,
                getWidth() - layout.padding * 2, layout.searchHeight,
                Component.translatable("widget.jujutsuaddon.search"));
        this.searchBox.setHint(Component.translatable("widget.jujutsuaddon.search.hint"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
    }

    public void setSearchHint(Component hint) {
        if (this.searchBox != null) {
            this.searchBox.setHint(hint);
        }
    }

    private Component getLocalizedParamValue(String value) {
        if (value.contains(".")) {
            return Component.translatable(value);
        } else {
            return Component.translatable("ability.jujutsu_kaisen." + value);
        }
    }

    // ==================== 配置方法 ====================

    public void updateItems(Collection<T> newItems) {
        this.allItems.clear();
        this.allItems.addAll(newItems);
        clearSelection();
        onSearchChanged(searchBox.getValue());
        this.scrollOffset = 0;
    }

    public void setSingleSelect(boolean singleSelect) {
        this.singleSelect = singleSelect;
        if (singleSelect && selectedIds.size() > 1) {
            clearSelection();
        }
    }

    public void setLayout(SelectionPanelLayout layout) {
        this.layout = layout;
        initSearchBox();
    }

    public void setSlotColor(int bgColor, int borderColor) {
        this.slotBgColor = bgColor;
        this.slotBorderColor = borderColor;
    }

    public void setParamDefGetter(Function<T, ParamDefinition> getter) {
        this.paramDefGetter = getter;
    }

    public void setDynamicDescGetter(BiFunction<T, Map<String, Object>, Component> getter) {
        this.dynamicDescGetter = getter;
    }

    public void setOnParamChanged(BiConsumer<T, Map<String, Object>> callback) {
        this.onParamChanged = callback;
    }

    // ==================== 搜索 ====================

    private void onSearchChanged(String query) {
        filteredItems.clear();
        scrollOffset = 0;

        if (query.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (T item : allItems) {
                String name = nameGetter.apply(item).getString();
                if (FuzzySearchHelper.contains(name, query)) {
                    filteredItems.add(item);
                }
            }
        }
    }

    // ==================== 选择管理 ====================

    public boolean isSelected(T item) {
        ResourceLocation id = idGetter.apply(item);
        return id != null && selectedIds.contains(id);
    }

    private void toggleSelection(T item) {
        ResourceLocation id = idGetter.apply(item);
        if (id == null) return;

        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
            itemParams.remove(id);
            onDeselect.accept(item);
        } else {
            if (singleSelect) {
                List<ResourceLocation> toRemove = new ArrayList<>(selectedIds);
                for (ResourceLocation oldId : toRemove) {
                    allItems.stream()
                            .filter(i -> Objects.equals(idGetter.apply(i), oldId))
                            .findFirst()
                            .ifPresent(onDeselect);
                }
                selectedIds.clear();
                itemParams.clear();
            }

            selectedIds.add(id);
            initDefaultParams(item, id);
            onSelect.accept(item);
            ensureItemVisible(item);
        }
    }

    private void ensureItemVisible(T targetItem) {
        int listHeight = getListHeight();

        int itemY = 0;
        for (T item : filteredItems) {
            int itemHeight = getItemHeight(item);
            if (item == targetItem) {
                int itemBottom = itemY + itemHeight;
                int visibleBottom = scrollOffset + listHeight;

                if (itemY < scrollOffset) {
                    scrollOffset = itemY;
                } else if (itemBottom > visibleBottom) {
                    scrollOffset = Math.max(0, itemBottom - listHeight);
                }
                break;
            }
            itemY += itemHeight + layout.itemGap;
        }
    }

    private void initDefaultParams(T item, ResourceLocation id) {
        if (paramDefGetter == null) return;

        ParamDefinition def = paramDefGetter.apply(item);
        if (def == null) return;

        Map<String, Object> params = new HashMap<>();
        for (ParamDefinition.Entry entry : def.getEntries()) {
            switch (entry.getType()) {
                case INT -> params.put(entry.getKey(), entry.getDefaultValue().intValue());
                case FLOAT -> params.put(entry.getKey(), entry.getDefaultValue().floatValue());
                case BOOLEAN -> params.put(entry.getKey(), entry.getDefaultValue().intValue() != 0);
                case STRING, SELECTION -> params.put(entry.getKey(), entry.getStringDefault());
            }
        }
        itemParams.put(id, params);
    }

    public Map<String, Object> getParams(T item) {
        ResourceLocation id = idGetter.apply(item);
        return id != null ? itemParams.getOrDefault(id, Map.of()) : Map.of();
    }

    public Map<T, Map<String, Object>> getSelectedWithParams() {
        Map<T, Map<String, Object>> result = new LinkedHashMap<>();
        for (T item : allItems) {
            if (isSelected(item)) {
                result.put(item, getParams(item));
            }
        }
        return result;
    }

    public void clearSelection() {
        for (ResourceLocation id : new ArrayList<>(selectedIds)) {
            allItems.stream()
                    .filter(i -> Objects.equals(idGetter.apply(i), id))
                    .findFirst()
                    .ifPresent(onDeselect);
        }
        selectedIds.clear();
        itemParams.clear();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    // ==================== 布局计算 ====================

    private int getListY() {
        return getY() + layout.headerHeight + layout.searchHeight + layout.padding * 2;
    }

    private int getListHeight() {
        return getHeight() - layout.headerHeight - layout.searchHeight - layout.padding * 3;
    }

    private int getContentWidth() {
        return getWidth() - layout.padding * 4;
    }

    private int getMaxLabelWidth(int contentWidth) {
        return contentWidth - layout.getControlAreaWidth() - layout.itemPadding - layout.labelControlGap;
    }

    private int getParamRowHeight(ParamDefinition.Entry entry, int maxLabelWidth) {
        int scaledMaxWidth = (int) (maxLabelWidth / layout.textScale);
        List<String> lines = DynamicTextHelper.wrapText(font, entry.getDisplayName().getString(), scaledMaxWidth);

        int scaledLineHeight = (int) (font.lineHeight * layout.textScale);
        int labelHeight = lines.size() * scaledLineHeight + Math.max(0, lines.size() - 1) * layout.lineGap;

        int controlHeight = Math.max(layout.sliderHeight, layout.checkboxSize);

        return Math.max(labelHeight, controlHeight) + layout.verticalPad * 2;
    }

    private int getDescriptionHeight(T item, int contentWidth) {
        Component desc = dynamicDescGetter != null
                ? dynamicDescGetter.apply(item, getParams(item))
                : descGetter.apply(item);

        int textWidth = contentWidth - layout.textPadding * 2;
        return DynamicTextHelper.calculateScaledTextHeight(
                font,
                desc.getString(),
                textWidth,
                layout.textScale,
                layout.lineGap,
                layout.descLineHeight,
                layout.textPadding
        );
    }

    private int getItemHeight(T item) {
        if (!isSelected(item)) {
            return layout.itemHeightCollapsed;
        }

        int contentWidth = getContentWidth();
        int descHeight = getDescriptionHeight(item, contentWidth);

        int paramHeight = 0;
        if (paramDefGetter != null) {
            ParamDefinition def = paramDefGetter.apply(item);
            if (def != null) {
                int maxLabelWidth = getMaxLabelWidth(contentWidth);
                for (ParamDefinition.Entry entry : def.getEntries()) {
                    paramHeight += getParamRowHeight(entry, maxLabelWidth);
                }
            }
        }

        return layout.itemHeightCollapsed + descHeight + layout.padding + paramHeight;
    }

    private int calculateTotalHeight() {
        int total = 0;
        for (T item : filteredItems) {
            total += getItemHeight(item) + layout.itemGap;
        }
        return total;
    }

    // ==================== 渲染 ====================

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        // ★★★ 核心修复：强制每帧同步输入框位置 ★★★
        // 确保输入框的判定区域与视觉区域完全一致
        int searchY = y + layout.headerHeight + layout.padding;
        int searchW = w - layout.padding * 2;

        // 只有当位置或大小改变时才更新，避免不必要的计算（虽然 EditBox 内部处理很快）
        if (searchBox.getX() != x + layout.padding || searchBox.getY() != searchY || searchBox.getWidth() != searchW) {
            searchBox.setX(x + layout.padding);
            searchBox.setY(searchY);
            searchBox.setWidth(searchW);
            searchBox.setHeight(layout.searchHeight);
        }

        graphics.fill(x, y, x + w, y + h, VowGuiColors.PANEL_BG);
        graphics.renderOutline(x, y, w, h, VowGuiColors.PANEL_BORDER);

        renderHeader(graphics, x, y, w);
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        int listY = getListY();
        int listHeight = getListHeight();
        renderItemList(graphics, x + layout.padding, listY, w - layout.padding * 2, listHeight, mouseX, mouseY);

        if (hoveredItem != null && !isSelected(hoveredItem)) {
            graphics.renderTooltip(font, descGetter.apply(hoveredItem), mouseX, mouseY);
        }

        if (activeDropdown != null) {
            if (activeDropdown.isOpen()) {
                activeDropdown.renderWidget(graphics, mouseX, mouseY, partialTick);
            } else {
                activeDropdown = null;
            }
        }
    }

    private void renderHeader(GuiGraphics graphics, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + layout.headerHeight, VowGuiColors.PANEL_HEADER_BG);

        int titleFontHeight = (int) (font.lineHeight * 0.9f);
        int titleY = y + (layout.headerHeight - titleFontHeight) / 2;
        renderScaledText(graphics, getMessage().getString(), x + layout.padding, titleY,
                VowGuiColors.TEXT_TITLE, 0.9f);

        if (getSelectedCount() > 0) {
            String countStr = "§a" + getSelectedCount();
            int countWidth = font.width(countStr);
            int countY = y + (layout.headerHeight - font.lineHeight) / 2;
            graphics.drawString(font, countStr, x + w - countWidth - layout.padding, countY, 0xFFFFFF);
        }
    }

    private void renderItemList(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.enableScissor(x, y, x + w, y + h);

        hoveredItem = null;
        int itemY = y - scrollOffset;

        for (T item : filteredItems) {
            int itemHeight = getItemHeight(item);

            if (itemY + itemHeight > y && itemY < y + h) {
                boolean hovered = GuiControlHelper.isInArea(mouseX, mouseY, x, itemY, w, itemHeight)
                        && mouseY >= y && mouseY < y + h;
                boolean selected = isSelected(item);

                if (hovered && mouseY < itemY + layout.itemHeightCollapsed) {
                    hoveredItem = item;
                }

                renderItem(graphics, item, x, itemY, w, itemHeight, selected, hovered, mouseX, mouseY);
            }
            itemY += itemHeight + layout.itemGap;
        }

        graphics.disableScissor();

        int totalHeight = calculateTotalHeight();
        if (totalHeight > h) {
            renderScrollbar(graphics, x + w - 4, y, 4, h, totalHeight);
        }
    }

    private void renderItem(GuiGraphics graphics, T item, int x, int y, int w, int itemHeight,
                            boolean selected, boolean hovered, int mouseX, int mouseY) {
        int bgColor = selected ? VowGuiColors.ENTRY_BG_SELECTED :
                hovered ? VowGuiColors.ENTRY_BG_HOVERED : slotBgColor;
        int borderColor = selected ? VowGuiColors.ENTRY_BORDER_SELECTED :
                hovered ? VowGuiColors.ENTRY_BORDER_HOVERED : slotBorderColor;
        graphics.fill(x, y, x + w, y + itemHeight, bgColor);
        graphics.renderOutline(x, y, w, itemHeight, borderColor);

        int cbX = x + layout.itemPadding;
        int cbY = y + (layout.itemHeightCollapsed - layout.checkboxSize) / 2;
        GuiControlHelper.renderCheckbox(graphics, font, cbX, cbY, layout.checkboxSize, selected, hovered);

        int nameX = cbX + layout.checkboxSize + layout.controlGap;
        int scaledFontHeight = (int) (font.lineHeight * layout.textScale);
        int cbCenterY = cbY + layout.checkboxSize / 2;
        int nameY = cbCenterY - scaledFontHeight / 2;
        renderScaledText(graphics, nameGetter.apply(item).getString(), nameX, nameY,
                selected ? VowGuiColors.TEXT_NORMAL : VowGuiColors.TEXT_SECONDARY, layout.textScale);

        if (selected) {
            int contentX = x + layout.padding;
            int contentW = w - layout.padding * 2;
            int contentY = y + layout.itemHeightCollapsed;

            int descHeight = getDescriptionHeight(item, contentW);
            renderDescription(graphics, item, contentX, contentY, contentW, descHeight);

            contentY += descHeight + layout.padding;
            renderParams(graphics, item, contentX, contentY, contentW, mouseX, mouseY);
        }
    }

    private void renderDescription(GuiGraphics graphics, T item, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0x30000000);

        Component desc = dynamicDescGetter != null
                ? dynamicDescGetter.apply(item, getParams(item))
                : descGetter.apply(item);

        DynamicTextHelper.renderScaledWrappedText(
                graphics, font, desc.getString(),
                x + layout.textPadding, y + layout.textPadding,
                w - layout.textPadding * 2,
                layout.textScale,
                VowGuiColors.TEXT_NORMAL,
                layout.lineGap
        );
    }

    private void renderParams(GuiGraphics graphics, T item, int x, int y, int w, int mouseX, int mouseY) {
        if (paramDefGetter == null) return;
        ParamDefinition def = paramDefGetter.apply(item);
        if (def == null) return;

        ResourceLocation id = idGetter.apply(item);
        Map<String, Object> params = itemParams.getOrDefault(id, Map.of());

        int maxLabelWidth = getMaxLabelWidth(w);
        int rowY = y;

        for (ParamDefinition.Entry entry : def.getEntries()) {
            int rowHeight = getParamRowHeight(entry, maxLabelWidth);
            renderParamRow(graphics, item, entry, params, x, rowY, w, rowHeight, maxLabelWidth, mouseX, mouseY);
            rowY += rowHeight;
        }
    }

    private void renderParamRow(GuiGraphics graphics, T item, ParamDefinition.Entry entry,
                                Map<String, Object> params, int x, int y, int w, int rowHeight,
                                int maxLabelWidth, int mouseX, int mouseY) {
        int controlAreaWidth = layout.getControlAreaWidth();
        int controlX = x + w - controlAreaWidth;
        int controlY = y + (rowHeight - layout.sliderHeight) / 2;

        int labelX = x + layout.itemPadding;
        int scaledMaxWidth = (int) (maxLabelWidth / layout.textScale);
        List<String> lines = DynamicTextHelper.wrapText(font, entry.getDisplayName().getString(), scaledMaxWidth);

        int scaledLineHeight = (int) (font.lineHeight * layout.textScale);
        int totalTextHeight = lines.size() * scaledLineHeight + (lines.size() - 1) * layout.lineGap;
        int labelStartY = y + (rowHeight - totalTextHeight) / 2;

        for (int i = 0; i < lines.size(); i++) {
            int lineY = labelStartY + i * (scaledLineHeight + layout.lineGap);
            renderScaledText(graphics, lines.get(i), labelX, lineY,
                    VowGuiColors.TEXT_SECONDARY, layout.textScale);
        }

        switch (entry.getType()) {
            case INT, FLOAT -> {
                boolean dragging = isDragging && draggingItem == item &&
                        draggingEntry != null && draggingEntry.getKey().equals(entry.getKey());
                boolean hovered = GuiControlHelper.isInSlider(mouseX, mouseY, controlX, controlY,
                        layout.sliderWidth, layout.sliderHeight);

                float current = getParamFloat(params, entry);
                float min = entry.getMin().floatValue();
                float max = entry.getMax().floatValue();
                float ratio = max > min ? Mth.clamp((current - min) / (max - min), 0, 1) : 0;

                boolean isInt = entry.getType() == ParamDefinition.ParamType.INT;
                String valueStr = DynamicTextHelper.formatParamValue(entry.getKey(), current, isInt);

                GuiControlHelper.renderSliderWithValue(graphics, font, controlX, controlY,
                        layout.sliderWidth, layout.sliderHeight, ratio, valueStr,
                        hovered, dragging, VowGuiColors.TEXT_NORMAL);
            }
            case BOOLEAN -> {
                int checkX = x + w - layout.checkboxSize - layout.itemPadding;
                int checkY = y + (rowHeight - layout.checkboxSize) / 2;
                boolean checked = getParamBoolean(params, entry);
                boolean hovered = GuiControlHelper.isInCheckbox(mouseX, mouseY, checkX, checkY, layout.checkboxSize);
                GuiControlHelper.renderCheckbox(graphics, font, checkX, checkY, layout.checkboxSize, checked, hovered);
            }
            case SELECTION -> {
                String currentVal = (String) params.getOrDefault(entry.getKey(), entry.getStringDefault());
                Component displayVal = getLocalizedParamValue(currentVal);

                boolean hovered = GuiControlHelper.isInArea(mouseX, mouseY, controlX, controlY, layout.sliderWidth, layout.sliderHeight);

                int btnColor = hovered ? VowGuiColors.ENTRY_BG_HOVERED : VowGuiColors.SLOT_EMPTY_BG;
                graphics.fill(controlX, controlY, controlX + layout.sliderWidth, controlY + layout.sliderHeight, btnColor);
                graphics.renderOutline(controlX, controlY, layout.sliderWidth, layout.sliderHeight, VowGuiColors.PANEL_BORDER);

                int textWidth = font.width(displayVal);
                if (textWidth > layout.sliderWidth - 12) {
                    String truncated = font.plainSubstrByWidth(displayVal.getString(), layout.sliderWidth - 14) + "..";
                    displayVal = Component.literal(truncated);
                    textWidth = font.width(displayVal);
                }

                int textX = controlX + (layout.sliderWidth - textWidth) / 2;
                int textY = controlY + (layout.sliderHeight - font.lineHeight) / 2 + 1;
                graphics.drawString(font, displayVal, textX, textY, VowGuiColors.TEXT_NORMAL, false);

                graphics.drawString(font, "▼", controlX + layout.sliderWidth - 8, textY, 0x888888, false);
            }
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int w, int h, int totalHeight) {
        graphics.fill(x, y, x + w, y + h, 0x40000000);

        float viewRatio = (float) h / totalHeight;
        int thumbHeight = Math.max(20, (int) (h * viewRatio));
        int maxScroll = totalHeight - h;
        float scrollRatio = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        int thumbY = y + (int) ((h - thumbHeight) * scrollRatio);

        graphics.fill(x, thumbY, x + w, thumbY + thumbHeight, 0xA0FFFFFF);
    }

    // ==================== 工具方法 ====================

    private void renderScaledText(GuiGraphics graphics, String text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private float getParamFloat(Map<String, Object> params, ParamDefinition.Entry entry) {
        Object value = params.get(entry.getKey());
        if (value instanceof Number num) return num.floatValue();
        return entry.getDefaultValue().floatValue();
    }

    private boolean getParamBoolean(Map<String, Object> params, ParamDefinition.Entry entry) {
        Object value = params.get(entry.getKey());
        if (value instanceof Boolean b) return b;
        return entry.getDefaultValue().intValue() != 0;
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeDropdown != null && activeDropdown.isOpen()) {
            if (activeDropdown.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (!activeDropdown.isOpen()) activeDropdown = null;
        }

        clearDragState();

        // ★★★ 核心修复：手动管理焦点 ★★★
        // 1. 尝试让搜索框处理点击
        boolean searchClicked = searchBox.mouseClicked(mouseX, mouseY, button);

        if (searchClicked) {
            // 2. 如果点中了搜索框，强制设为焦点
            searchBox.setFocused(true);
            return true;
        } else {
            // 3. 如果没点中，强制失去焦点（这样点击列表时输入框就不会继续闪烁了）
            searchBox.setFocused(false);
        }

        int listX = getX() + layout.padding;
        int listY = getListY();
        int listW = getWidth() - layout.padding * 2;
        int listHeight = getListHeight();

        if (!GuiControlHelper.isInArea(mouseX, mouseY, listX, listY, listW, listHeight)) {
            return false;
        }

        int contentW = listW - layout.padding * 2;
        int itemY = listY - scrollOffset;

        for (T item : filteredItems) {
            int itemHeight = getItemHeight(item);
            boolean visible = itemY + itemHeight > listY && itemY < listY + listHeight;

            if (visible && mouseY >= Math.max(itemY, listY) && mouseY < Math.min(itemY + itemHeight, listY + listHeight)) {
                if (mouseY >= itemY && mouseY < itemY + layout.itemHeightCollapsed) {
                    toggleSelection(item);
                    return true;
                }

                if (isSelected(item) && paramDefGetter != null) {
                    int descHeight = getDescriptionHeight(item, contentW);
                    int paramY = itemY + layout.itemHeightCollapsed + descHeight + layout.padding;
                    int contentX = listX + layout.padding;

                    if (mouseY >= paramY && handleParamClick(item, contentX, paramY, contentW, mouseX, mouseY)) {
                        return true;
                    }
                }
            }
            itemY += itemHeight + layout.itemGap;
        }
        return false;
    }

    private boolean handleParamClick(T item, int contentX, int paramY, int contentW, double mouseX, double mouseY) {
        ResourceLocation id = idGetter.apply(item);
        ParamDefinition def = paramDefGetter.apply(item);
        if (def == null || id == null) return false;

        Map<String, Object> params = itemParams.get(id);
        if (params == null) return false;

        int maxLabelWidth = getMaxLabelWidth(contentW);
        int rowY = paramY;
        int controlAreaWidth = layout.getControlAreaWidth();
        int controlX = contentX + contentW - controlAreaWidth;

        for (ParamDefinition.Entry entry : def.getEntries()) {
            int rowHeight = getParamRowHeight(entry, maxLabelWidth);
            int controlY = rowY + (rowHeight - layout.sliderHeight) / 2;

            switch (entry.getType()) {
                case INT, FLOAT -> {
                    if (GuiControlHelper.isInSlider(mouseX, mouseY, controlX, controlY, layout.sliderWidth, layout.sliderHeight)) {
                        isDragging = true;
                        draggingItem = item;
                        draggingEntry = entry;
                        draggingSliderX = controlX;
                        updateSliderValue(id, entry, mouseX);
                        notifyParamChanged(item);
                        return true;
                    }
                }
                case BOOLEAN -> {
                    int checkX = contentX + contentW - layout.checkboxSize - layout.itemPadding;
                    int checkY = rowY + (rowHeight - layout.checkboxSize) / 2;
                    if (GuiControlHelper.isInCheckbox(mouseX, mouseY, checkX, checkY, layout.checkboxSize)) {
                        Boolean current = (Boolean) params.getOrDefault(entry.getKey(), false);
                        params.put(entry.getKey(), !current);
                        notifyParamChanged(item);
                        return true;
                    }
                }
                case SELECTION -> {
                    if (GuiControlHelper.isInArea(mouseX, mouseY, controlX, controlY, layout.sliderWidth, layout.sliderHeight)) {
                        List<String> options = entry.getValidValues();
                        if (options != null && !options.isEmpty()) {
                            String current = (String) params.getOrDefault(entry.getKey(), entry.getStringDefault());

                            activeDropdown = new DropdownWidget(
                                    controlX, controlY, layout.sliderWidth, layout.sliderHeight,
                                    options, current,
                                    this::getLocalizedParamValue,
                                    (selected) -> {
                                        params.put(entry.getKey(), selected);
                                        notifyParamChanged(item);
                                    }
                            );
                            return true;
                        }
                    }
                }
            }
            rowY += rowHeight;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && draggingItem != null && draggingEntry != null) {
            ResourceLocation id = idGetter.apply(draggingItem);
            if (id != null) {
                updateSliderValue(id, draggingEntry, mouseX);
                notifyParamChanged(draggingItem);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = isDragging;
        clearDragState();
        return wasDragging;
    }

    private void clearDragState() {
        isDragging = false;
        draggingItem = null;
        draggingEntry = null;
        draggingSliderX = 0;
    }

    private void updateSliderValue(ResourceLocation itemId, ParamDefinition.Entry entry, double mouseX) {
        Map<String, Object> params = itemParams.get(itemId);
        if (params == null) return;

        float min = entry.getMin().floatValue();
        float max = entry.getMax().floatValue();
        boolean asInt = entry.getType() == ParamDefinition.ParamType.INT;

        Number value = GuiControlHelper.calculateSliderValue(mouseX, draggingSliderX, layout.sliderWidth, min, max, asInt);
        params.put(entry.getKey(), asInt ? value.intValue() : value.floatValue());
    }

    private void notifyParamChanged(T item) {
        if (onParamChanged != null) {
            ResourceLocation id = idGetter.apply(item);
            Map<String, Object> params = itemParams.getOrDefault(id, Map.of());
            onParamChanged.accept(item, new HashMap<>(params));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeDropdown != null && activeDropdown.isOpen()) {
            if (activeDropdown.mouseScrolled(mouseX, mouseY, delta)) return true;
        }

        int listY = getListY();
        int listHeight = getListHeight();

        if (GuiControlHelper.isInArea(mouseX, mouseY, getX(), listY, getWidth(), listHeight)) {
            int totalHeight = calculateTotalHeight();
            int maxScroll = Math.max(0, totalHeight - listHeight);
            scrollOffset = Mth.clamp(scrollOffset - (int) (delta * 20), 0, maxScroll);
            return true;
        }
        return false;
    }

    // ★★★ 核心修复：按键事件手动转发 ★★★
    // 只有当搜索框有焦点时，才把按键事件传给它
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }
}
