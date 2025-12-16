package com.jujutsuaddon.addon.client.gui.screen;

import com.jujutsuaddon.addon.client.gui.screen.cursemanagement.CurseManagementScreen;
import com.jujutsuaddon.addon.client.util.AbilityTriggerHelper;
import com.jujutsuaddon.addon.client.util.UIScaleHelper;
import com.jujutsuaddon.addon.client.skillbar.SkillBarData;
import com.jujutsuaddon.addon.client.skillbar.SkillBarManager;
import com.jujutsuaddon.addon.client.skillbar.render.AbilityListRenderer;
import com.jujutsuaddon.addon.client.gui.components.ConfirmDialog;
import com.jujutsuaddon.addon.client.skillbar.render.DragHandler;
import com.jujutsuaddon.addon.client.skillbar.render.SlotGridRenderer;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.RemoveCopiedTechniqueC2SPacket;
import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

public class SkillBarConfigScreen extends Screen {

    private static final int REFRESH_INTERVAL = 1;

    private final AbilityListRenderer abilityList = new AbilityListRenderer();
    private final SlotGridRenderer slotGrid = new SlotGridRenderer();
    private final DragHandler dragHandler = new DragHandler();
    private final ConfirmDialog confirmDialog;

    private int refreshTimer = 0;

    // 布局参数
    private int titleY;

    public SkillBarConfigScreen() {
        super(Component.translatable("gui.jujutsu_addon.skill_bar_config.title"));
        this.confirmDialog = new ConfirmDialog(0, 0);
    }

    @Override
    protected void init() {
        super.init();

        if (!SkillBarManager.isInitialized()) {
            this.onClose();
            return;
        }

        calculateLayout();
        confirmDialog.updateSize(this.width, this.height);
        abilityList.refresh();
        createButtons();
    }

    private void calculateLayout() {
        // 使用UIScaleHelper计算布局
        int[] layout = UIScaleHelper.calculateSkillConfigLayout(this.width, this.height);
        int listWidth = layout[0];
        int listHeight = layout[1];
        int listX = layout[2];
        int listY = layout[3];
        int slotSize = layout[4];
        int slotAreaX = layout[5];
        int slotAreaY = layout[6];

        titleY = 12;

        abilityList.setBounds(listX, listY, listWidth, listHeight);
        slotGrid.setPosition(slotAreaX, slotAreaY);
        slotGrid.setSlotSize(slotSize);
    }

    private void createButtons() {
        // 计算按钮布局
        int[] btnLayout = UIScaleHelper.calculateButtonLayout(
                this.width, this.height,
                this.height - 55,  // 内容底部
                5                   // 最小底部边距
        );

        int buttonY = btnLayout[0];
        int btnHeight = btnLayout[1];
        int btnSmall = btnLayout[2];
        int btnMed = btnLayout[3];

        int centerX = this.width / 2;

        // 预设切换按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.skill_bar_config.prev_preset"),
                btn -> { SkillBarManager.prevPreset(); abilityList.refresh(); }
        ).bounds(centerX - btnSmall - btnMed / 2 - 5, buttonY, btnSmall, btnHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.skill_bar_config.next_preset"),
                btn -> { SkillBarManager.nextPreset(); abilityList.refresh(); }
        ).bounds(centerX + btnMed / 2 + 5, buttonY, btnSmall, btnHeight).build());

        // 清空按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.skill_bar_config.clear"),
                btn -> { for (int i = 0; i < SkillBarData.SLOT_COUNT; i++) SkillBarManager.clearSlot(i); }
        ).bounds(centerX - btnMed / 2, buttonY, btnMed, btnHeight).build());

        // 右上角按钮
        int topBtnY = 8;
        int topBtnHeight = Math.min(btnHeight, 18);
        int topBtnWidth = Math.min(btnSmall, 55);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> this.onClose()
        ).bounds(this.width - topBtnWidth - 8, topBtnY, topBtnWidth, topBtnHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jujutsu_addon.skill_bar_config.edit_position"),
                btn -> this.minecraft.setScreen(new SkillBarEditScreen())
        ).bounds(this.width - topBtnWidth * 2 - 15, topBtnY, topBtnWidth + 5, topBtnHeight).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0;
            abilityList.refresh();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        if (confirmDialog.isVisible()) {
            confirmDialog.render(graphics, mouseX, mouseY);
            return;
        }

        // 标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.skill_bar_config.preset",
                        SkillBarManager.getCurrentPresetIndex() + 1),
                this.width / 2, titleY + 13, 0xFFFF00);

        abilityList.render(graphics, mouseX, mouseY);
        slotGrid.render(graphics, mouseX, mouseY);
        dragHandler.render(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (!dragHandler.isDragging()) {
            abilityList.renderTooltip(graphics, mouseX, mouseY);
            slotGrid.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    // ========== 鼠标事件（保持不变） ==========

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmDialog.isVisible()) {
            return confirmDialog.handleClick(mouseX, mouseY, button);
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (button == 0) {
            if (abilityList.isInBounds(mx, my)) {
                if (abilityList.getCurseManagementDraggableAt(mx, my)) {
                    dragHandler.startDragCurseManagement(mx, my, -1);
                    return true;
                }
                CursedTechnique deleteTech = abilityList.getHoveredDeleteTechnique();
                if (deleteTech != null) {
                    showDeleteConfirm(deleteTech);
                    playClickSound();
                    return true;
                }
                CursedTechnique activateTech = abilityList.getHoveredActivateTechnique();
                if (activateTech != null) {
                    AbilityTriggerHelper.toggleExtraTechnique(activateTech);
                    abilityList.refresh();
                    playClickSound();
                    return true;
                }
                if (abilityList.isToggleButtonHovered(mx, my)) {
                    Ability toggleAbility = abilityList.getHoveredToggleAbility();
                    if (toggleAbility != null) {
                        AbilityTriggerHelper.toggleAbility(toggleAbility);
                        playClickSound();
                        return true;
                    }
                }
                CursedTechnique draggableTech = abilityList.getDraggableTechniqueAt(mx, my);
                if (draggableTech != null) {
                    dragHandler.startDragTechnique(draggableTech, mx, my, -1);
                    return true;
                }
                TechniqueHelper.TechniqueSource mainHeader = abilityList.getHoveredMainHeader();
                if (mainHeader != null && mainHeader != TechniqueHelper.TechniqueSource.NONE) {
                    abilityList.toggleMainSectionCollapse(mainHeader);
                    playClickSound();
                    return true;
                }
                Ability ability = abilityList.getAbilityAt(mx, my);
                if (ability != null) {
                    dragHandler.startDragAbility(ability, mx, my, -1);
                    return true;
                }
            }

            int slot = slotGrid.getSlotAt(mx, my);
            if (slot >= 0) {
                Ability slotAbility = SkillBarManager.getSlot(slot);
                CursedTechnique slotTech = SkillBarManager.getSlotTechnique(slot);
                boolean isCurseSlot = SkillBarManager.isSlotCurseManagement(slot);

                if (isCurseSlot) {
                    dragHandler.startDragCurseManagement(mx, my, slot);
                    return true;
                } else if (slotAbility != null) {
                    dragHandler.startDragAbility(slotAbility, mx, my, slot);
                    return true;
                } else if (slotTech != null) {
                    dragHandler.startDragTechnique(slotTech, mx, my, slot);
                    return true;
                }
            }
        }

        if (button == 1) {
            int slot = slotGrid.getSlotAt(mx, my);
            if (slot >= 0) {
                SkillBarManager.clearSlot(slot);
                playClickSound();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragHandler.isDragging()) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            int targetSlot = slotGrid.getSlotAt(mx, my);
            int sourceSlot = dragHandler.getSourceSlot();
            boolean isClick = !dragHandler.hasMovedEnough();

            if (dragHandler.isDraggingAbility()) {
                Ability ability = dragHandler.getDraggingAbility();
                if (!isClick && targetSlot >= 0) {
                    if (sourceSlot >= 0 && sourceSlot != targetSlot) {
                        swapSlots(sourceSlot, targetSlot);
                        SkillBarManager.setSlot(targetSlot, ability);
                    } else if (sourceSlot < 0) {
                        SkillBarManager.setSlot(targetSlot, ability);
                    }
                    playClickSound();
                } else if (sourceSlot >= 0 && !slotGrid.isInGridArea(mx, my) && !isClick) {
                    SkillBarManager.clearSlot(sourceSlot);
                    playClickSound();
                }
            } else if (dragHandler.isDraggingTechnique()) {
                CursedTechnique technique = dragHandler.getDraggingTechnique();
                if (isClick && sourceSlot < 0) {
                    abilityList.toggleTechniqueCollapse(technique);
                    playClickSound();
                } else if (targetSlot >= 0 && !isClick) {
                    if (sourceSlot >= 0 && sourceSlot != targetSlot) {
                        swapSlots(sourceSlot, targetSlot);
                        SkillBarManager.setSlotTechnique(targetSlot, technique);
                    } else if (sourceSlot < 0) {
                        SkillBarManager.setSlotTechnique(targetSlot, technique);
                    }
                    playClickSound();
                } else if (sourceSlot >= 0 && !slotGrid.isInGridArea(mx, my) && !isClick) {
                    SkillBarManager.clearSlot(sourceSlot);
                    playClickSound();
                }
            } else if (dragHandler.isDraggingCurseManagement()) {
                if (isClick && sourceSlot < 0) {
                    this.minecraft.setScreen(new CurseManagementScreen());
                    playClickSound();
                } else if (targetSlot >= 0 && !isClick) {
                    if (sourceSlot >= 0 && sourceSlot != targetSlot) {
                        swapSlots(sourceSlot, targetSlot);
                        SkillBarManager.setSlotCurseManagement(targetSlot);
                    } else if (sourceSlot < 0) {
                        SkillBarManager.setSlotCurseManagement(targetSlot);
                    }
                    playClickSound();
                } else if (sourceSlot >= 0 && !slotGrid.isInGridArea(mx, my) && !isClick) {
                    SkillBarManager.clearSlot(sourceSlot);
                    playClickSound();
                }
            }
            dragHandler.stopDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 交换槽位内容的辅助方法
     */
    private void swapSlots(int sourceSlot, int targetSlot) {
        Ability targetAbility = SkillBarManager.getSlot(targetSlot);
        CursedTechnique targetTech = SkillBarManager.getSlotTechnique(targetSlot);
        boolean targetIsCurse = SkillBarManager.isSlotCurseManagement(targetSlot);

        if (targetIsCurse) {
            SkillBarManager.setSlotCurseManagement(sourceSlot);
        } else if (targetTech != null) {
            SkillBarManager.setSlotTechnique(sourceSlot, targetTech);
        } else if (targetAbility != null) {
            SkillBarManager.setSlot(sourceSlot, targetAbility);
        } else {
            SkillBarManager.clearSlot(sourceSlot);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragHandler.isDragging()) {
            dragHandler.updatePosition((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (abilityList.handleScroll(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void showDeleteConfirm(CursedTechnique technique) {
        confirmDialog.show(
                Component.translatable("gui.jujutsu_addon.delete_confirm.title"),
                Component.translatable("gui.jujutsu_addon.delete_confirm.message", technique.getName()),
                () -> {
                    AddonNetwork.sendToServer(new RemoveCopiedTechniqueC2SPacket(technique));
                    abilityList.refresh();
                },
                () -> {}
        );
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
