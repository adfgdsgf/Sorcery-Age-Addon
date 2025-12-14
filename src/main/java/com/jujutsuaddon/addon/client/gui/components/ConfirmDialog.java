package com.jujutsuaddon.addon.client.gui.components;

import com.jujutsuaddon.addon.client.util.UIScaleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class ConfirmDialog {

    private int screenWidth;
    private int screenHeight;
    private final Font font;

    private boolean visible = false;
    private Component title = Component.empty();
    private Component message = Component.empty();
    private Component warning = Component.empty();

    @Nullable private Runnable onConfirm;
    @Nullable private Runnable onCancel;

    private int dialogWidth, dialogHeight;
    private int btnWidth, btnHeight;

    public ConfirmDialog(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.font = Minecraft.getInstance().font;
        calculateSizes();
    }

    public void updateSize(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        calculateSizes();
    }

    private void calculateSizes() {
        int[] dialogSize = UIScaleHelper.getDialogSize(screenWidth, screenHeight);
        dialogWidth = dialogSize[0];
        dialogHeight = dialogSize[1];
        btnWidth = UIScaleHelper.clamp(dialogWidth / 3, 55, 90);
        btnHeight = UIScaleHelper.clamp(dialogHeight / 5, 16, 22);
    }

    public void show(Component title, Component message, Runnable onConfirm, Runnable onCancel) {
        show(title, message, Component.empty(), onConfirm, onCancel);
    }

    public void show(Component title, Component message, Component warning,
                     Runnable onConfirm, Runnable onCancel) {
        this.title = title;
        this.message = message;
        this.warning = warning;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
        this.onConfirm = null;
        this.onCancel = null;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!visible) return;

        graphics.fill(0, 0, screenWidth, screenHeight, 0xAA000000);

        int dialogX = UIScaleHelper.centerX(screenWidth, dialogWidth);
        int dialogY = UIScaleHelper.centerY(screenHeight, dialogHeight);

        graphics.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF222222);
        graphics.renderOutline(dialogX, dialogY, dialogWidth, dialogHeight, 0xFFFF4444);

        graphics.drawCenteredString(font, title, screenWidth / 2, dialogY + 8, 0xFFFF44);
        graphics.drawCenteredString(font, message, screenWidth / 2, dialogY + 24, 0xFFFFFF);

        if (warning != Component.empty()) {
            graphics.drawCenteredString(font, warning, screenWidth / 2, dialogY + 38, 0xFF8888);
        }

        int btnSpacing = 8;
        int totalBtnWidth = btnWidth * 2 + btnSpacing;
        int confirmX = dialogX + (dialogWidth - totalBtnWidth) / 2;
        int cancelX = confirmX + btnWidth + btnSpacing;
        int btnY = dialogY + dialogHeight - btnHeight - 6;

        boolean confirmHovered = isInBounds(mouseX, mouseY, confirmX, btnY, btnWidth, btnHeight);
        boolean cancelHovered = isInBounds(mouseX, mouseY, cancelX, btnY, btnWidth, btnHeight);

        graphics.fill(confirmX, btnY, confirmX + btnWidth, btnY + btnHeight,
                confirmHovered ? 0xFFCC3333 : 0xFFAA2222);
        graphics.renderOutline(confirmX, btnY, btnWidth, btnHeight, 0xFFFF4444);
        graphics.drawCenteredString(font, Component.translatable("gui.jujutsu_addon.confirm"),
                confirmX + btnWidth / 2, btnY + (btnHeight - 8) / 2, 0xFFFFFF);

        graphics.fill(cancelX, btnY, cancelX + btnWidth, btnY + btnHeight,
                cancelHovered ? 0xFF666666 : 0xFF444444);
        graphics.renderOutline(cancelX, btnY, btnWidth, btnHeight, 0xFF888888);
        graphics.drawCenteredString(font, Component.translatable("gui.jujutsu_addon.cancel"),
                cancelX + btnWidth / 2, btnY + (btnHeight - 8) / 2, 0xFFFFFF);
    }

    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;

        int dialogX = UIScaleHelper.centerX(screenWidth, dialogWidth);
        int dialogY = UIScaleHelper.centerY(screenHeight, dialogHeight);

        int btnSpacing = 8;
        int totalBtnWidth = btnWidth * 2 + btnSpacing;
        int confirmX = dialogX + (dialogWidth - totalBtnWidth) / 2;
        int cancelX = confirmX + btnWidth + btnSpacing;
        int btnY = dialogY + dialogHeight - btnHeight - 6;

        if (isInBounds(mouseX, mouseY, confirmX, btnY, btnWidth, btnHeight)) {
            if (onConfirm != null) onConfirm.run();
            hide();
            return true;
        }

        if (isInBounds(mouseX, mouseY, cancelX, btnY, btnWidth, btnHeight)) {
            if (onCancel != null) onCancel.run();
            hide();
            return true;
        }

        if (!isInBounds(mouseX, mouseY, dialogX, dialogY, dialogWidth, dialogHeight)) {
            hide();
            return true;
        }

        return true;
    }

    private boolean isInBounds(double x, double y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }
}
