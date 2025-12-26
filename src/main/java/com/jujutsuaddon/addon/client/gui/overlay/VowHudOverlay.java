package com.jujutsuaddon.addon.client.gui.overlay;

import com.jujutsuaddon.addon.client.cache.ClientVowDataCache;
import com.jujutsuaddon.addon.client.gui.util.VowGuiColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class VowHudOverlay implements IGuiOverlay {

    public static final VowHudOverlay INSTANCE = new VowHudOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // 创造模式不显示
        if (mc.player.isCreative()) return;

        // 无惩罚不显示
        if (!ClientVowDataCache.isUnderPenalty()) return;

        long secondsLeft = ClientVowDataCache.getPenaltySecondsLeft();

        // 使用本地化文本
        Component text = Component.translatable("vow.hud.penalty_active", secondsLeft);

        // 使用工具类定义的颜色 (TEXT_ERROR = 红色)
        int color = VowGuiColors.TEXT_ERROR;

        // 计算位置 (右下角，稍微上移)
        int textWidth = mc.font.width(text);
        int x = screenWidth - textWidth - 10;
        int y = screenHeight - 40;

        graphics.drawString(mc.font, text, x, y, color, true);
    }
}
