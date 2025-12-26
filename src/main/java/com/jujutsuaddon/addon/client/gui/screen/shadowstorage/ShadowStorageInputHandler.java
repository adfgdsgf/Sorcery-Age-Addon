package com.jujutsuaddon.addon.client.gui.screen.shadowstorage;

import com.jujutsuaddon.addon.client.render.SlotRenderHelper;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.ShadowStorageActionC2SPacket;
import com.jujutsuaddon.addon.network.c2s.ShadowStorageActionC2SPacket.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * 影子库存输入处理器
 */
public class ShadowStorageInputHandler {

    private final ShadowStorageLayout layout;
    private final ShadowStorageDataManager dataManager;

    private int scrollRow = 0;
    private boolean isScrolling = false;

    // ★★★ 改进：只要 Shift+左键按下就进入拖拽模式 ★★★
    private boolean isShiftDragging = false;

    // 仅用于渲染高亮
    private final Set<Integer> highlightedPlayerSlots = new HashSet<>();
    private final Set<Integer> highlightedShadowIndices = new HashSet<>();

    // 上一次拖拽经过的槽位（防止同一格连续触发）
    private int lastDragPlayerSlot = -1;
    private int lastDragShadowIndex = -1;

    public ShadowStorageInputHandler(ShadowStorageLayout layout, ShadowStorageDataManager dataManager) {
        this.layout = layout;
        this.dataManager = dataManager;
    }

    // ==================== 状态获取 ====================

    public int getScrollRow() { return scrollRow; }

    public Set<Integer> getDraggedPlayerSlots() { return highlightedPlayerSlots; }
    public Set<Integer> getDraggedShadowIndices() { return highlightedShadowIndices; }

    public boolean hasDraggedShadow() { return !highlightedShadowIndices.isEmpty(); }
    public boolean hasDraggedPlayer() { return !highlightedPlayerSlots.isEmpty(); }

    public boolean isDragging() { return isShiftDragging; }

    public void clampScroll() {
        scrollRow = dataManager.clampScrollRow(scrollRow);
    }

    // ==================== 鼠标点击 ====================

    public boolean handleMouseClick(double mouseX, double mouseY, int button,
                                    Runnable onSortClick, Runnable onHudEditClick,
                                    Runnable onClearSearch) {
        int btnX = layout.getSideButtonX();
        int btnSize = layout.getSideButtonSize();
        int sortBtnY = layout.getTopPos() + 18;
        int hudBtnY = sortBtnY + btnSize + layout.getSideButtonGap();

        // 排序按钮
        if (SlotRenderHelper.isInButton(mouseX, mouseY, btnX, sortBtnY, btnSize)) {
            if (Screen.hasShiftDown()) {
                AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(
                        Action.SORT, -1, dataManager.getSortMode().ordinal()));
            } else {
                dataManager.nextSortMode();
                onSortClick.run();
            }
            return true;
        }

        // HUD编辑按钮
        if (SlotRenderHelper.isInButton(mouseX, mouseY, btnX, hudBtnY, btnSize)) {
            onHudEditClick.run();
            return true;
        }

        // 清除搜索按钮
        if (!dataManager.getSearchText().isEmpty()) {
            int clearBtnY = hudBtnY + btnSize + layout.getSideButtonGap();
            if (SlotRenderHelper.isInButton(mouseX, mouseY, btnX, clearBtnY, btnSize)) {
                onClearSearch.run();
                return true;
            }
        }

        if (button == 0 || button == 1) {
            // ★★★ Shift+左键：无论点在哪里都进入拖拽模式 ★★★
            if (button == 0 && Screen.hasShiftDown()) {
                // 先尝试处理当前位置的物品
                boolean hitItem = tryProcessSlotAt(mouseX, mouseY);

                // 无论是否点到物品，都进入拖拽模式
                isShiftDragging = true;

                if (hitItem) {
                    return true;
                }
                // 即使没点到物品，也返回 true 进入拖拽准备状态
                return true;
            }

            // 普通点击 - 影子库存取出
            int shadowIdx = layout.getShadowSlotIndex(mouseX, mouseY, scrollRow);
            if (shadowIdx >= 0 && shadowIdx < dataManager.getDisplayCount()) {
                handleShadowSlotClick(shadowIdx, button);
                return true;
            }

            // 普通点击 - 玩家背包存入
            int playerSlot = layout.getPlayerSlotIndex(mouseX, mouseY);
            if (playerSlot >= 0) {
                handlePlayerSlotClick(playerSlot, button);
                return true;
            }
        }

        // 滚动条
        if (layout.isInScrollbar(mouseX, mouseY)) {
            isScrolling = true;
            return true;
        }

        return false;
    }

    /**
     * ★★★ 尝试处理指定位置的槽位，返回是否成功处理了物品 ★★★
     */
    private boolean tryProcessSlotAt(double mouseX, double mouseY) {
        // 玩家背包 -> 存入
        int playerSlot = layout.getPlayerSlotIndex(mouseX, mouseY);
        if (playerSlot >= 0) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !player.getInventory().getItem(playerSlot).isEmpty()) {
                lastDragPlayerSlot = playerSlot;
                lastDragShadowIndex = -1;
                highlightedPlayerSlots.add(playerSlot);
                AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.STORE_ALL, -1, playerSlot));
                return true;
            }
        }

        // 影子库存 -> 取出
        int displayIdx = layout.getShadowSlotIndex(mouseX, mouseY, scrollRow);
        if (displayIdx >= 0 && displayIdx < dataManager.getDisplayCount()) {
            ItemStack stack = dataManager.getDisplayItem(displayIdx);
            if (!stack.isEmpty()) {
                lastDragShadowIndex = displayIdx;
                lastDragPlayerSlot = -1;
                highlightedShadowIndices.add(displayIdx);
                int realIdx = dataManager.findRealIndex(stack);
                if (realIdx >= 0) {
                    AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.RETRIEVE_ALL, realIdx, -1));
                }
                return true;
            }
        }

        return false;
    }

    private void handleShadowSlotClick(int displayIdx, int button) {
        ItemStack clicked = dataManager.getDisplayItem(displayIdx);
        int realIdx = dataManager.findRealIndex(clicked);
        if (realIdx < 0) return;

        Action action;
        if (Screen.hasShiftDown() || button == 1) {
            action = Action.RETRIEVE_ALL;
        } else if (Screen.hasControlDown() && clicked.getCount() > 1) {
            action = Action.RETRIEVE_HALF;
        } else {
            action = Action.RETRIEVE_ONE;
        }
        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(action, realIdx, -1));
    }

    private void handlePlayerSlotClick(int playerSlot, int button) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack stack = player.getInventory().getItem(playerSlot);
        if (stack.isEmpty()) return;

        Action action;
        if (Screen.hasShiftDown() || button == 1) {
            action = Action.STORE_ALL;
        } else if (Screen.hasControlDown() && stack.getCount() > 1) {
            action = Action.STORE_HALF;
        } else {
            action = Action.STORE_ONE;
        }
        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(action, -1, playerSlot));
    }

    // ==================== 鼠标释放 ====================

    public void handleMouseRelease() {
        isScrolling = false;
        isShiftDragging = false;
        highlightedPlayerSlots.clear();
        highlightedShadowIndices.clear();
        lastDragPlayerSlot = -1;
        lastDragShadowIndex = -1;
    }

    // ==================== 鼠标拖拽 ====================

    public boolean handleMouseDrag(double mouseX, double mouseY, int button) {
        if (isScrolling) {
            updateScrollFromMouse(mouseY);
            return true;
        }

        // ★★★ 只要在拖拽模式中，就持续检测槽位 ★★★
        if (isShiftDragging && button == 0) {
            // 检查玩家背包区域 -> 存入
            int playerSlot = layout.getPlayerSlotIndex(mouseX, mouseY);
            if (playerSlot >= 0 && playerSlot != lastDragPlayerSlot) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && !player.getInventory().getItem(playerSlot).isEmpty()) {
                    lastDragPlayerSlot = playerSlot;
                    lastDragShadowIndex = -1;
                    highlightedPlayerSlots.add(playerSlot);
                    AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.STORE_ALL, -1, playerSlot));
                }
                return true;
            }

            // 检查影子库存区域 -> 取出
            int displayIdx = layout.getShadowSlotIndex(mouseX, mouseY, scrollRow);
            if (displayIdx >= 0 && displayIdx < dataManager.getDisplayCount() && displayIdx != lastDragShadowIndex) {
                ItemStack stack = dataManager.getDisplayItem(displayIdx);
                if (!stack.isEmpty()) {
                    lastDragShadowIndex = displayIdx;
                    lastDragPlayerSlot = -1;
                    highlightedShadowIndices.add(displayIdx);
                    int realIdx = dataManager.findRealIndex(stack);
                    if (realIdx >= 0) {
                        AddonNetwork.sendToServer(new ShadowStorageActionC2SPacket(Action.RETRIEVE_ALL, realIdx, -1));
                    }
                }
                return true;
            }

            return true;
        }

        return false;
    }

    private void updateScrollFromMouse(double mouseY) {
        int gridY = layout.getShadowGridY();
        int gridH = layout.getShadowGridHeight();
        int maxScroll = dataManager.getMaxScrollRow();

        if (maxScroll > 0) {
            float progress = (float) (mouseY - gridY) / gridH;
            scrollRow = (int) (progress * (maxScroll + 1));
            scrollRow = Math.max(0, Math.min(scrollRow, maxScroll));
        }
    }

    // ==================== 鼠标滚轮 ====================

    public boolean handleMouseScroll(double delta) {
        scrollRow = Math.max(0, Math.min(scrollRow - (int) delta, dataManager.getMaxScrollRow()));
        return true;
    }
}
