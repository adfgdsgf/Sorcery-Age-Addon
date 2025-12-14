package com.jujutsuaddon.addon.client.skillbar.render;

import com.jujutsuaddon.addon.client.skillbar.SkillBarConstants;
import com.jujutsuaddon.addon.client.util.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import javax.annotation.Nullable;

import static com.jujutsuaddon.addon.client.skillbar.SkillBarConstants.*;

public class DragHandler {

    @Nullable private Ability draggingAbility = null;
    @Nullable private CursedTechnique draggingTechnique = null;
    private boolean draggingCurseManagement = false;

    private int dragX, dragY;
    private int startX, startY;
    private int sourceSlot = -1;

    public void startDragAbility(Ability ability, int x, int y, int fromSlot) {
        this.draggingAbility = ability;
        this.draggingTechnique = null;
        this.dragX = x;
        this.dragY = y;
        this.startX = x;
        this.startY = y;
        this.sourceSlot = fromSlot;
    }

    public void startDragTechnique(CursedTechnique technique, int x, int y, int fromSlot) {
        this.draggingTechnique = technique;
        this.draggingAbility = null;
        this.dragX = x;
        this.dragY = y;
        this.startX = x;
        this.startY = y;
        this.sourceSlot = fromSlot;
    }

    public void startDragCurseManagement(int x, int y, int fromSlot) {
        this.draggingCurseManagement = true;
        this.draggingAbility = null;
        this.draggingTechnique = null;
        this.dragX = x;
        this.dragY = y;
        this.startX = x;
        this.startY = y;
        this.sourceSlot = fromSlot;
    }
    public boolean isDraggingCurseManagement() {
        return draggingCurseManagement;
    }

    public void updatePosition(int x, int y) {
        this.dragX = x;
        this.dragY = y;
    }

    public void stopDrag() {
        draggingAbility = null;
        draggingTechnique = null;
        draggingCurseManagement = false;  // ★ 新增
        sourceSlot = -1;
    }

    public boolean isDragging() {
        return draggingAbility != null || draggingTechnique != null || draggingCurseManagement;  // ★ 修改
    }

    public boolean isDraggingAbility() {
        return draggingAbility != null;
    }

    public boolean isDraggingTechnique() {
        return draggingTechnique != null;
    }

    @Nullable
    public Ability getDraggingAbility() {
        return draggingAbility;
    }

    @Nullable
    public CursedTechnique getDraggingTechnique() {
        return draggingTechnique;
    }

    public int getSourceSlot() {
        return sourceSlot;
    }

    public boolean hasMovedEnough() {
        return Math.abs(dragX - startX) > DRAG_THRESHOLD || Math.abs(dragY - startY) > DRAG_THRESHOLD;
    }

    public void render(GuiGraphics graphics) {
        int halfSize = DRAG_ICON_SIZE / 2;
        if (draggingAbility != null) {
            RenderHelper.renderAbilityIcon(graphics, draggingAbility,
                    dragX - halfSize, dragY - halfSize, DRAG_ICON_SIZE, false);
        } else if (draggingTechnique != null) {
            RenderHelper.renderTechniqueIcon(graphics, draggingTechnique,
                    dragX - halfSize, dragY - halfSize, DRAG_ICON_SIZE, false);
        } else if (draggingCurseManagement) {  // ★ 新增
            RenderHelper.renderCurseManagementIcon(graphics,
                    dragX - halfSize, dragY - halfSize, DRAG_ICON_SIZE);
        }
    }
}
