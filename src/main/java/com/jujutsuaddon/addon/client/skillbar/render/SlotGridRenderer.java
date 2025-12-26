package com.jujutsuaddon.addon.client.skillbar.render;

import com.jujutsuaddon.addon.client.keybind.AddonKeyBindings;
import com.jujutsuaddon.addon.client.skillbar.SkillBarData;
import com.jujutsuaddon.addon.client.skillbar.SkillBarManager;
import com.jujutsuaddon.addon.client.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

public class SlotGridRenderer {

    private int x, y;
    private int slotSize = 32;  // ★★★ 可变槽位尺寸 ★★★
    private int slotPadding = 4;
    private final Font font;

    public SlotGridRenderer() {
        this.font = Minecraft.getInstance().font;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * ★★★ 新增：设置槽位尺寸（响应式） ★★★
     */
    public void setSlotSize(int size) {
        this.slotSize = size;
        this.slotPadding = Math.max(2, size / 8);
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font,
                Component.translatable("gui.jujutsu_addon.skill_bar_config.slots"),
                x, y - 15, 0xFFFFFF);

        for (int i = 0; i < SkillBarData.SLOT_COUNT; i++) {
            renderSlot(graphics, i, mouseX, mouseY);
        }

        // 提示文字位置也要响应式
        int hintY = y + 2 * (slotSize + slotPadding) + 10;
        graphics.drawString(font, Component.translatable("gui.jujutsu_addon.skill_bar_config.hint1"), x, hintY, 0x888888);
        graphics.drawString(font, Component.translatable("gui.jujutsu_addon.skill_bar_config.hint2"), x, hintY + 12, 0x888888);
    }

    private void renderSlot(GuiGraphics graphics, int slot, int mouseX, int mouseY) {
        int[] pos = getSlotPosition(slot);
        int slotX = pos[0];
        int slotY = pos[1];
        boolean isHovered = isInSlot(mouseX, mouseY, slot);

        // 背景和边框
        graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize,
                isHovered ? 0x80555555 : 0x80333333);
        graphics.renderOutline(slotX, slotY, slotSize, slotSize,
                isHovered ? 0xFF00FF00 : 0xFF888888);

        Ability ability = SkillBarManager.getSlot(slot);
        CursedTechnique technique = SkillBarManager.getSlotTechnique(slot);
        boolean isCurseManagement = SkillBarManager.isSlotCurseManagement(slot);

        // 图标尺寸和偏移（响应式）
        int iconOffset = Math.max(2, slotSize / 8);
        int iconSize = slotSize - iconOffset * 2;
        int iconX = slotX + iconOffset;
        int iconY = slotY + iconOffset;

        // 渲染内容
        if (isCurseManagement) {
            RenderHelper.renderCurseManagementIcon(graphics, iconX, iconY, iconSize);
        } else if (ability != null) {
            RenderHelper.renderAbilityIcon(graphics, ability, iconX, iconY, iconSize, false);
        } else if (technique != null) {
            boolean isActive = SkillBarManager.isTechniqueActive(slot);
            RenderHelper.renderTechniqueIcon(graphics, technique, iconX, iconY, iconSize, isActive);
        }

        // 快捷键（左下角）
        if (slot < AddonKeyBindings.SKILL_SLOT_KEYS.size()) {
            String keyName = AddonKeyBindings.SKILL_SLOT_KEYS.get(slot)
                    .getTranslatedKeyMessage().getString().toUpperCase();
            if (keyName.length() > 1) keyName = keyName.substring(0, 1);
            graphics.drawString(font, keyName, slotX + 2, slotY + slotSize - 10, 0xFFFF00, true);
        }

        // 槽位编号（右上角）
        graphics.drawString(font, String.valueOf(slot + 1), slotX + slotSize - 8, slotY + 2, 0x888888, false);
    }

    public int[] getSlotPosition(int slot) {
        int col = slot % 3;
        int row = slot / 3;
        return new int[]{
                x + col * (slotSize + slotPadding),
                y + row * (slotSize + slotPadding)
        };
    }

    public int getSlotAt(int mouseX, int mouseY) {
        for (int i = 0; i < SkillBarData.SLOT_COUNT; i++) {
            if (isInSlot(mouseX, mouseY, i)) return i;
        }
        return -1;
    }

    public boolean isInSlot(int mouseX, int mouseY, int slot) {
        int[] pos = getSlotPosition(slot);
        return mouseX >= pos[0] && mouseX < pos[0] + slotSize &&
                mouseY >= pos[1] && mouseY < pos[1] + slotSize;
    }

    public boolean isInGridArea(int mouseX, int mouseY) {
        int gridWidth = 3 * (slotSize + slotPadding);
        int gridHeight = 2 * (slotSize + slotPadding);
        return mouseX >= x && mouseX < x + gridWidth && mouseY >= y && mouseY < y + gridHeight;
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int slot = getSlotAt(mouseX, mouseY);
        if (slot < 0) return;

        Ability ability = SkillBarManager.getSlot(slot);
        CursedTechnique technique = SkillBarManager.getSlotTechnique(slot);

        if (ability != null) {
            graphics.renderTooltip(font, ability.getName(), mouseX, mouseY);
        } else if (technique != null) {
            graphics.renderTooltip(font, technique.getName(), mouseX, mouseY);
        }
    }
}
