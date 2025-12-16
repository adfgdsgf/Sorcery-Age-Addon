package com.jujutsuaddon.addon.client.gui.screen.cursemanagement;

import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.SummonAbsorbedCurseC2SPacket;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 咒灵管理输入处理器
 */
public class CurseManagementInputHandler {

    private final CurseManagementLayout layout;
    private final CurseManagementDataManager dataManager;

    private int hoveredIndex = -1;

    public CurseManagementInputHandler(CurseManagementLayout layout, CurseManagementDataManager dataManager) {
        this.layout = layout;
        this.dataManager = dataManager;
    }

    // ==================== 悬停检测 ====================

    /**
     * 更新悬停状态，返回当前悬停的全局索引
     */
    public int updateHoveredIndex(double mouseX, double mouseY) {
        int localIndex = layout.getLocalIndexAt(mouseX, mouseY);
        if (localIndex >= 0) {
            hoveredIndex = dataManager.getStartIndex() + localIndex;
        } else {
            hoveredIndex = -1;
        }
        return hoveredIndex;
    }

    public int getHoveredIndex() {
        return hoveredIndex;
    }

    // ==================== 点击处理 ====================

    /**
     * 处理鼠标点击
     * @return true 如果点击被处理
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int button, Runnable onSummonComplete) {
        if (button != 0) return false;

        if (hoveredIndex >= 0 && hoveredIndex < dataManager.getGroupedCurses().size()) {
            CurseManagementDataManager.GroupedCurse group = dataManager.getGroupAt(hoveredIndex);
            if (group == null) return false;

            if (Screen.hasShiftDown() && group.getCount() > 1) {
                // Shift+点击：召唤该组所有
                List<Integer> indices = new ArrayList<>(group.getOriginalIndices());
                Collections.reverse(indices); // 从后往前召唤，避免索引变化
                for (int index : indices) {
                    AddonNetwork.sendToServer(new SummonAbsorbedCurseC2SPacket(index));
                }
            } else {
                // 普通点击：召唤一只
                int index = group.getFirstIndex();
                if (index >= 0) {
                    AddonNetwork.sendToServer(new SummonAbsorbedCurseC2SPacket(index));
                }
            }

            onSummonComplete.run();
            return true;
        }
        return false;
    }

    // ==================== 滚轮翻页 ====================

    /**
     * 处理滚轮滚动
     * @return true 如果滚动被处理
     */
    public boolean handleMouseScroll(double delta) {
        if (delta > 0) {
            dataManager.prevPage();
        } else if (delta < 0) {
            dataManager.nextPage();
        }
        return true;
    }

    // ==================== 召唤全部 ====================

    public void summonAll() {
        AddonNetwork.sendToServer(new SummonAbsorbedCurseC2SPacket(-1));
    }
}
