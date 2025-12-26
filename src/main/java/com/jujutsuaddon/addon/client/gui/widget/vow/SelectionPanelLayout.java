package com.jujutsuaddon.addon.client.gui.widget.vow;

/**
 * 选择面板布局配置
 * 所有间距、尺寸都在这里定义，避免硬编码
 */
public class SelectionPanelLayout {

    // ==================== 尺寸参数 ====================
    public final int headerHeight;
    public final int searchHeight;
    public final int itemHeightCollapsed;
    public final int itemGap;
    public final int padding;
    public final int sliderWidth;
    public final int sliderHeight;
    public final int checkboxSize;
    public final int descLineHeight;
    public final float textScale;

    // ==================== 间距参数（消除硬编码） ====================
    public final int itemPadding;       // 条目内边距（复选框到边框的距离）
    public final int controlGap;        // 控件之间的间隙（复选框和名称之间）
    public final int labelControlGap;   // 标签和控件之间的间隙
    public final int textPadding;       // 文本内边距（描述区域文字到边框的距离）
    public final int lineGap;           // 多行文本行间距
    public final int verticalPad;       // 参数行垂直内边距
    public final int valueDisplayWidth; // 滑块旁边数值显示的宽度

    // ==================== 预设配置 ====================
    public static final SelectionPanelLayout COMPACT = new SelectionPanelLayout(
            18,     // headerHeight
            14,     // searchHeight
            22,     // itemHeightCollapsed
            2,      // itemGap
            3,      // padding
            80,     // sliderWidth
            10,     // sliderHeight
            10,     // checkboxSize
            10,     // descLineHeight
            1.0f,   // textScale
            4,      // itemPadding
            6,      // controlGap
            6,      // labelControlGap
            2,      // textPadding
            1,      // lineGap
            4,      // verticalPad
            40      // valueDisplayWidth
    );

    public static final SelectionPanelLayout NORMAL = new SelectionPanelLayout(
            24,     // headerHeight
            18,     // searchHeight
            32,     // itemHeightCollapsed
            3,      // itemGap
            5,      // padding
            100,    // sliderWidth
            14,     // sliderHeight
            12,     // checkboxSize
            12,     // descLineHeight
            1.0f,   // textScale
            5,      // itemPadding
            8,      // controlGap
            8,      // labelControlGap
            3,      // textPadding
            1,      // lineGap
            5,      // verticalPad
            45      // valueDisplayWidth
    );

    public static final SelectionPanelLayout LARGE = new SelectionPanelLayout(
            28,     // headerHeight
            20,     // searchHeight
            44,     // itemHeightCollapsed
            4,      // itemGap
            6,      // padding
            120,    // sliderWidth
            16,     // sliderHeight
            14,     // checkboxSize
            14,     // descLineHeight
            1.0f,   // textScale
            6,      // itemPadding
            10,     // controlGap
            10,     // labelControlGap
            4,      // textPadding
            2,      // lineGap
            6,      // verticalPad
            50      // valueDisplayWidth
    );

    public SelectionPanelLayout(int headerHeight, int searchHeight, int itemHeightCollapsed,
                                int itemGap, int padding, int sliderWidth, int sliderHeight,
                                int checkboxSize, int descLineHeight, float textScale,
                                int itemPadding, int controlGap, int labelControlGap,
                                int textPadding, int lineGap, int verticalPad, int valueDisplayWidth) {
        this.headerHeight = headerHeight;
        this.searchHeight = searchHeight;
        this.itemHeightCollapsed = itemHeightCollapsed;
        this.itemGap = itemGap;
        this.padding = padding;
        this.sliderWidth = sliderWidth;
        this.sliderHeight = sliderHeight;
        this.checkboxSize = checkboxSize;
        this.descLineHeight = descLineHeight;
        this.textScale = textScale;
        this.itemPadding = itemPadding;
        this.controlGap = controlGap;
        this.labelControlGap = labelControlGap;
        this.textPadding = textPadding;
        this.lineGap = lineGap;
        this.verticalPad = verticalPad;
        this.valueDisplayWidth = valueDisplayWidth;
    }

    /** 获取控件区域总宽度（滑块 + 数值显示） */
    public int getControlAreaWidth() {
        return sliderWidth + valueDisplayWidth;
    }
}
