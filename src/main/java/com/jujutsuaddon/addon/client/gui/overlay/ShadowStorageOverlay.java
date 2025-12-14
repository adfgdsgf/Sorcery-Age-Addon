package com.jujutsuaddon.addon.client.gui.overlay;

import com.jujutsuaddon.addon.capability.AddonShadowStorageData;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.config.AddonClientConfig.AnchorPoint;
import com.jujutsuaddon.addon.client.util.HudPositionHelper;
import com.jujutsuaddon.addon.util.helper.TenShadowsHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "jujutsu_addon", value = Dist.CLIENT)
public class ShadowStorageOverlay {

    // 基础尺寸（缩放前）
    public static final int ICON_SIZE = 16;
    public static final int ICON_SPACING = 2;
    public static final int PADDING = 6;

    // 动画
    private static float animationTick = 0;
    private static int lastItemCount = 0;
    private static float countChangeAnim = 0;

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.options.hideGui) return;
        if (mc.screen != null) return;

        // 检查是否启用
        if (!AddonClientConfig.CLIENT.showShadowStorageHUD.get()) return;

        // 检查是否有十影
        if (!TenShadowsHelper.hasTenShadows(player)) return;

        // ★★★ 使用我们自己的 Capability ★★★
        List<ItemStack> items = new ArrayList<>();
        long totalCount = 0;  // 记录总数量（可选，用于显示）

        var capOptional = player.getCapability(AddonShadowStorageData.CAPABILITY).resolve();
        if (capOptional.isEmpty()) return;

        AddonShadowStorageData storage = capOptional.get();
        for (AddonShadowStorageData.StorageEntry entry : storage.getAll()) {
            ItemStack displayStack = entry.getTemplate().copy();
            // 数量设为1用于图标显示，实际数量用entry.getCount()
            displayStack.setCount((int) Math.min(entry.getCount(), Integer.MAX_VALUE));
            items.add(displayStack);
            totalCount += entry.getCount();
        }

        if (items.isEmpty()) return;

        // 更新动画
        float partialTick = event.getPartialTick();
        animationTick += partialTick * 0.1f;
        if (items.size() != lastItemCount) {
            countChangeAnim = 1.0f;
            lastItemCount = items.size();
        }
        if (countChangeAnim > 0) {
            countChangeAnim -= partialTick * 0.05f;
        }

        // ★★★ 获取配置 ★★★
        int maxDisplay = AddonClientConfig.CLIENT.shadowStorageHudMaxItems.get();
        float scale = AddonClientConfig.CLIENT.shadowStorageHudScale.get().floatValue();
        AnchorPoint anchor = AddonClientConfig.CLIENT.shadowStorageHudAnchor.get();
        int offsetX = AddonClientConfig.CLIENT.shadowStorageHudOffsetX.get();
        int offsetY = AddonClientConfig.CLIENT.shadowStorageHudOffsetY.get();

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // 计算内容尺寸（缩放前）
        int displayCount = Math.min(items.size(), maxDisplay);
        int[] contentSize = getContentSize(displayCount);
        int contentWidth = contentSize[0];
        int contentHeight = contentSize[1];

        // 使用工具类计算位置
        int[] pos = HudPositionHelper.calculatePosition(
                anchor, offsetX, offsetY, scale,
                screenWidth, screenHeight,
                contentWidth, contentHeight, 10
        );

        // 应用缩放渲染
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(pos[0], pos[1], 0);
        event.getGuiGraphics().pose().scale(scale, scale, 1.0f);

        renderShadowHUD(event.getGuiGraphics(), items, maxDisplay);

        event.getGuiGraphics().pose().popPose();
    }

    private static void renderShadowHUD(GuiGraphics graphics, List<ItemStack> items, int maxDisplay) {
        int displayCount = Math.min(items.size(), maxDisplay);
        int totalWidth = displayCount * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;

        // 背景容器
        renderShadowContainer(graphics, 0, 0, totalWidth + PADDING * 2, ICON_SIZE + PADDING * 2, items.size());

        // 渲染物品图标
        for (int i = 0; i < displayCount; i++) {
            int index = items.size() - displayCount + i;  // 显示最新的物品
            ItemStack stack = items.get(index);

            int iconX = PADDING + i * (ICON_SIZE + ICON_SPACING);
            int iconY = PADDING;

            // 浮动动画
            float floatOffset = (float) Math.sin(animationTick + i * 0.5f) * 1.5f;

            graphics.pose().pushPose();
            graphics.pose().translate(0, floatOffset, 0);

            // 阴影效果
            RenderSystem.enableBlend();
            graphics.fill(iconX + 2, iconY + 2, iconX + ICON_SIZE + 2, iconY + ICON_SIZE + 2, 0x40000000);

            // 渲染物品
            graphics.renderItem(stack, iconX, iconY);

            // ★★★ 渲染数量（使用缩放版本）★★★
            if (stack.getCount() > 1) {
                renderScaledCount(graphics, iconX, iconY, stack.getCount());
            }

            graphics.pose().popPose();
        }

        // 如果还有更多物品种类，显示数量
        if (items.size() > maxDisplay) {
            String moreText = "+" + (items.size() - maxDisplay);
            int totalW = totalWidth + PADDING * 2;
            int textX = totalW + 4;
            int textY = PADDING + 4;

            graphics.drawString(Minecraft.getInstance().font, moreText, textX + 1, textY + 1, 0x40000000, false);
            graphics.drawString(Minecraft.getInstance().font, moreText, textX, textY, 0xFF8866CC, false);
        }
    }

    /**
     * 渲染缩放的数量文字（与 Screen 中保持一致）
     */
    private static void renderScaledCount(GuiGraphics graphics, int slotX, int slotY, int count) {
        String countStr = formatSlotCount(count);
        var font = Minecraft.getInstance().font;
        int originalWidth = font.width(countStr);

        // 计算缩放比例
        float baseScale = 0.65f;
        int maxAllowedWidth = ICON_SIZE - 2;

        float scale = baseScale;
        float scaledWidth = originalWidth * scale;

        if (scaledWidth > maxAllowedWidth) {
            scale = maxAllowedWidth / (float) originalWidth;
            scale = Math.max(scale, 0.4f);
            scaledWidth = originalWidth * scale;
        }

        // 根据数量决定颜色
        int textColor;
        if (count >= 1_000_000) {
            textColor = 0xFFFF55;
        } else if (count >= 100_000) {
            textColor = 0xFF55FF;
        } else if (count >= 10_000) {
            textColor = 0x55FFFF;
        } else if (count >= 1_000) {
            textColor = 0x55FF55;
        } else if (count >= 100) {
            textColor = 0xFFFF55;
        } else {
            textColor = 0xFFFFFF;
        }

        // 位置（右下角）
        float textX = slotX + ICON_SIZE - scaledWidth - 1;
        float textY = slotY + ICON_SIZE - (8 * scale) - 1;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // 阴影
        graphics.drawString(font, countStr, 1, 1, 0x000000, false);
        graphics.drawString(font, countStr, 1, 0, 0x000000, false);
        graphics.drawString(font, countStr, 0, 1, 0x000000, false);

        // 主文字
        graphics.drawString(font, countStr, 0, 0, textColor, false);

        graphics.pose().popPose();
    }

    /**
     * 格式化槽位数量
     */
    private static String formatSlotCount(int count) {
        if (count >= 1_000_000) {
            return String.format("%.0fM", count / 1_000_000.0);
        } else if (count >= 100_000) {
            return String.format("%.0fK", count / 1_000.0);
        } else if (count >= 10_000) {
            return String.format("%.1fK", count / 1_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }

    private static void renderShadowContainer(GuiGraphics graphics, int x, int y,
                                              int width, int height, int itemCount) {
        // 外层阴影
        for (int i = 3; i >= 0; i--) {
            int alpha = 20 - i * 5;
            graphics.fill(x - i, y - i, x + width + i, y + height + i, (alpha << 24) | 0x0a0020);
        }

        // 主背景
        float pulse = (float) (Math.sin(animationTick * 2) * 0.15 + 0.85);
        int bgAlpha = (int) (180 * pulse);

        if (countChangeAnim > 0) {
            bgAlpha = (int) (bgAlpha + 40 * countChangeAnim);
        }

        graphics.fill(x, y, x + width, y + height, (bgAlpha << 24) | 0x100828);

        // 边框
        int borderAlpha = (int) (150 * pulse);
        int borderColor = (borderAlpha << 24) | 0x4422AA;
        graphics.renderOutline(x, y, width, height, borderColor);

        // 内边框
        graphics.renderOutline(x + 1, y + 1, width - 2, height - 2, 0x30221155);
    }

    /**
     * 获取内容尺寸（供编辑界面使用）
     */
    public static int[] getContentSize(int displayCount) {
        int width = displayCount * (ICON_SIZE + ICON_SPACING) - ICON_SPACING + PADDING * 2;
        int height = ICON_SIZE + PADDING * 2;
        return new int[]{width, height};
    }
}
