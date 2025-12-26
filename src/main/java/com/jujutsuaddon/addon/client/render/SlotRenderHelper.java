package com.jujutsuaddon.addon.client.render;

import com.jujutsuaddon.addon.client.util.NumberFormatHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * 槽位渲染工具类
 */
public class SlotRenderHelper {

    private static final int SLOT_SIZE = 18;

    // ==================== 颜色常量 ====================

    public static final class SlotColors {
        public static final int SHADOW_BG_NORMAL = 0x80100820;
        public static final int SHADOW_BG_HOVERED = 0xA0201040;
        public static final int SHADOW_BG_DRAGGED = 0xA0402080;
        public static final int SHADOW_BORDER_NORMAL = 0x40FFFFFF;
        public static final int SHADOW_BORDER_HOVERED = 0xFF6644CC;
        public static final int SHADOW_BORDER_DRAGGED = 0xFFAA55FF;

        public static final int PLAYER_BG_NORMAL = 0x80101820;
        public static final int PLAYER_BG_HOVERED = 0xA0103010;
        public static final int PLAYER_BG_DRAGGED = 0xA0204020;
        public static final int PLAYER_BORDER_NORMAL = 0x30FFFFFF;
        public static final int PLAYER_BORDER_HOVERED = 0xFF44AA44;
        public static final int PLAYER_BORDER_DRAGGED = 0xFF55FF55;

        public static final int EMPTY_BG_NORMAL = 0x30082008;
        public static final int EMPTY_BG_HOVERED = 0x40114411;
        public static final int EMPTY_BORDER = 0x3044AA44;
    }

    // ==================== 槽位渲染 ====================

    public static void renderShadowSlot(GuiGraphics graphics, Font font,
                                        int x, int y, ItemStack stack,
                                        boolean hovered, boolean dragged) {
        int bgColor = dragged ? SlotColors.SHADOW_BG_DRAGGED
                : hovered ? SlotColors.SHADOW_BG_HOVERED : SlotColors.SHADOW_BG_NORMAL;
        int borderColor = dragged ? SlotColors.SHADOW_BORDER_DRAGGED
                : hovered ? SlotColors.SHADOW_BORDER_HOVERED : SlotColors.SHADOW_BORDER_NORMAL;

        renderSlotBackground(graphics, x, y, bgColor, borderColor);
        graphics.renderItem(stack, x + 1, y + 1);

        if (stack.getCount() > 1) {
            renderScaledCount(graphics, font, x, y, stack.getCount());
        }
    }

    public static void renderPlayerSlot(GuiGraphics graphics, Font font,
                                        int x, int y, ItemStack stack,
                                        boolean hovered, boolean dragged) {
        int bgColor = dragged ? SlotColors.PLAYER_BG_DRAGGED
                : hovered ? SlotColors.PLAYER_BG_HOVERED : SlotColors.PLAYER_BG_NORMAL;
        int borderColor = dragged ? SlotColors.PLAYER_BORDER_DRAGGED
                : hovered ? SlotColors.PLAYER_BORDER_HOVERED : SlotColors.PLAYER_BORDER_NORMAL;

        renderSlotBackground(graphics, x, y, bgColor, borderColor);

        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    public static void renderEmptySlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        int bgColor = hovered ? SlotColors.EMPTY_BG_HOVERED : SlotColors.EMPTY_BG_NORMAL;
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);
        graphics.renderOutline(x, y, SLOT_SIZE - 1, SLOT_SIZE - 1, SlotColors.EMPTY_BORDER);
    }

    private static void renderSlotBackground(GuiGraphics graphics, int x, int y,
                                             int bgColor, int borderColor) {
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE - 1, borderColor);
    }

    public static void renderScaledCount(GuiGraphics graphics, Font font,
                                         int slotX, int slotY, int count) {
        String countStr = NumberFormatHelper.formatSlotCount(count);
        int originalWidth = font.width(countStr);

        float baseScale = 0.65f;
        int maxAllowedWidth = SLOT_SIZE - 2;
        float scale = baseScale;
        float scaledWidth = originalWidth * scale;

        if (scaledWidth > maxAllowedWidth) {
            scale = Math.max(0.4f, maxAllowedWidth / (float) originalWidth);
            scaledWidth = originalWidth * scale;
        }

        int textColor = NumberFormatHelper.getCountColor(count);
        float textX = slotX + SLOT_SIZE - scaledWidth - 1;
        float textY = slotY + SLOT_SIZE - (8 * scale) - 1;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // 阴影
        graphics.drawString(font, countStr, 1, 1, 0x000000, false);
        graphics.drawString(font, countStr, 1, 0, 0x000000, false);
        graphics.drawString(font, countStr, 0, 1, 0x000000, false);
        graphics.drawString(font, countStr, 0, 0, textColor, false);

        graphics.pose().popPose();
    }

    // ==================== 碰撞检测 ====================

    public static boolean isInSlot(double mx, double my, int slotX, int slotY) {
        return mx >= slotX && mx < slotX + SLOT_SIZE && my >= slotY && my < slotY + SLOT_SIZE;
    }

    public static boolean isInButton(double mx, double my, int btnX, int btnY, int btnSize) {
        return mx >= btnX && mx < btnX + btnSize && my >= btnY && my < btnY + btnSize;
    }
}
