package com.jujutsuaddon.addon.client.gui.widget;

import com.jujutsuaddon.addon.client.util.GuiControlHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DropdownWidget extends AbstractWidget {

    private final List<String> options;
    private int selectedIndex;
    private final Consumer<String> onSelect;
    // ★ 新增：用于把 String 转换成 Component 的函数
    private final Function<String, Component> labeler;
    private boolean isOpen = true;

    // 滚动相关
    private float scrollAmount = 0;
    private static final int MAX_VISIBLE_ITEMS = 6;
    private static final int ITEM_HEIGHT = 14;

    /**
     * @param labeler 用于将选项字符串转换为显示文本的函数
     */
    public DropdownWidget(int x, int y, int width, int height,
                          List<String> options, String current,
                          Function<String, Component> labeler,
                          Consumer<String> onSelect) {
        super(x, y, width, height, Component.empty());
        this.options = options;
        this.labeler = labeler; // 保存翻译器
        this.onSelect = onSelect;
        this.selectedIndex = Math.max(0, options.indexOf(current));

        if (selectedIndex > 2) {
            int maxScroll = Math.max(0, options.size() * ITEM_HEIGHT - MAX_VISIBLE_ITEMS * ITEM_HEIGHT);
            this.scrollAmount = Mth.clamp((selectedIndex - 2) * ITEM_HEIGHT, 0, maxScroll);
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isOpen) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int listHeight = Math.min(options.size(), MAX_VISIBLE_ITEMS) * ITEM_HEIGHT;
        int listY = getY() + height;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);

        graphics.fill(getX(), listY, getX() + width, listY + listHeight + 2, 0xFF222222);
        graphics.renderOutline(getX(), listY, width, listHeight + 2, 0xFF666666);

        double scale = mc.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) (getX() * scale),
                (int) (mc.getWindow().getHeight() - (listY + listHeight + 1) * scale),
                (int) (width * scale),
                (int) (listHeight * scale)
        );

        for (int i = 0; i < options.size(); i++) {
            int itemTop = listY + 1 + i * ITEM_HEIGHT - (int)scrollAmount;

            if (itemTop + ITEM_HEIGHT < listY || itemTop > listY + listHeight) continue;

            boolean isHovered = GuiControlHelper.isInArea(mouseX, mouseY, getX(), itemTop, width, ITEM_HEIGHT);
            boolean isSelected = (i == selectedIndex);

            if (isSelected) {
                graphics.fill(getX() + 1, itemTop, getX() + width - 1, itemTop + ITEM_HEIGHT, 0xFF444455);
            } else if (isHovered) {
                graphics.fill(getX() + 1, itemTop, getX() + width - 1, itemTop + ITEM_HEIGHT, 0xFF333344);
            }

            // ★ 使用传入的 labeler 进行翻译
            String opt = options.get(i);
            Component displayComp = labeler.apply(opt);

            int textColor = isSelected ? 0xFFFF55 : (isHovered ? 0xFFFFFF : 0xAAAAAA);
            graphics.drawString(font, displayComp, getX() + 4, itemTop + 3, textColor, false);
        }

        RenderSystem.disableScissor();

        if (options.size() > MAX_VISIBLE_ITEMS) {
            int totalHeight = options.size() * ITEM_HEIGHT;
            int viewHeight = listHeight;
            float viewRatio = (float) viewHeight / totalHeight;
            int barHeight = Math.max(10, (int) (viewHeight * viewRatio));
            int maxScroll = totalHeight - viewHeight;
            float scrollRatio = maxScroll > 0 ? scrollAmount / maxScroll : 0;
            int barY = listY + 1 + (int) ((viewHeight - barHeight) * scrollRatio);

            graphics.fill(getX() + width - 3, barY, getX() + width - 1, barY + barHeight, 0xFF888888);
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isOpen) return false;

        int listHeight = Math.min(options.size(), MAX_VISIBLE_ITEMS) * ITEM_HEIGHT;
        int listY = getY() + height;

        if (GuiControlHelper.isInArea(mouseX, mouseY, getX(), listY, width, listHeight)) {
            int clickedIndex = (int) ((mouseY - listY + scrollAmount) / ITEM_HEIGHT);
            if (clickedIndex >= 0 && clickedIndex < options.size()) {
                this.selectedIndex = clickedIndex;
                onSelect.accept(options.get(clickedIndex));
                this.isOpen = false;
                playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }

        this.isOpen = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isOpen) {
            int maxScroll = Math.max(0, options.size() * ITEM_HEIGHT - MAX_VISIBLE_ITEMS * ITEM_HEIGHT);
            scrollAmount = (float) Mth.clamp(scrollAmount - delta * ITEM_HEIGHT, 0, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
