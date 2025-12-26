package com.jujutsuaddon.addon.client.gui.screen.cursemanagement;

import com.jujutsuaddon.addon.capability.curse.AddonCurseBaselineData;
import com.jujutsuaddon.addon.capability.CurseListMonitor;
import com.jujutsuaddon.addon.client.render.EntityRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 咒灵管理数据管理器
 */
public class CurseManagementDataManager {

    // ==================== 分组数据结构 ====================

    /**
     * 分组后的咒灵条目
     */
    public static class GroupedCurse {
        private final EntityType<?> entityType;
        private final Component displayName;
        private final List<Integer> originalIndices;
        private final int dataHash;
        private boolean isVariant;

        public GroupedCurse(EntityType<?> type, Component name, int dataHash) {
            this.entityType = type;
            this.displayName = name;
            this.originalIndices = new ArrayList<>();
            this.dataHash = dataHash;
            this.isVariant = false;
        }

        public EntityType<?> getEntityType() { return entityType; }
        public Component getDisplayName() { return displayName; }
        public List<Integer> getOriginalIndices() { return originalIndices; }
        public int getDataHash() { return dataHash; }
        public boolean isVariant() { return isVariant; }
        public void setVariant(boolean variant) { this.isVariant = variant; }
        public int getCount() { return originalIndices.size(); }
        public int getFirstIndex() { return originalIndices.isEmpty() ? -1 : originalIndices.get(0); }

        /**
         * ★★★ 是否应该显示变体标签（只有JJK生物才显示）★★★
         */
        public boolean shouldShowVariantTag() {
            return isVariant && EntityRenderHelper.isJJKEntity(entityType);
        }
    }

    /**
     * 分组键
     */
    private static class CurseGroupKey {
        final EntityType<?> entityType;
        final int dataHash;

        CurseGroupKey(EntityType<?> type, @Nullable CompoundTag data) {
            this.entityType = type;
            this.dataHash = (data != null && !data.isEmpty())
                    ? data.toString().hashCode()
                    : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CurseGroupKey that = (CurseGroupKey) o;
            return dataHash == that.dataHash && Objects.equals(entityType, that.entityType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityType, dataHash);
        }
    }

    // ==================== 字段 ====================

    private List<AbsorbedCurse> rawCurses = new ArrayList<>();
    private List<GroupedCurse> groupedCurses = new ArrayList<>();
    private int uniqueTypeCount = 0;
    private int currentPage = 0;
    private int totalPages = 1;
    private int itemsPerPage = 12;

    // ==================== 公共方法 ====================

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public void refresh() {
        rawCurses.clear();
        groupedCurses.clear();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return;

        AddonCurseBaselineData baselineData = player.getCapability(AddonCurseBaselineData.CAPABILITY).orElse(null);
        rawCurses.addAll(sorcererData.getCurses());

        // 分组
        Map<CurseGroupKey, GroupedCurse> groupMap = new LinkedHashMap<>();

        for (int i = 0; i < rawCurses.size(); i++) {
            AbsorbedCurse curse = rawCurses.get(i);
            EntityType<?> type = curse.getType();
            CompoundTag nbt = curse.getData();
            int dataHash = CurseListMonitor.computeDataHash(nbt);

            CurseGroupKey key = new CurseGroupKey(type, nbt);
            GroupedCurse group = groupMap.computeIfAbsent(key,
                    k -> new GroupedCurse(type, curse.getName(), dataHash));
            group.originalIndices.add(i);

            // ★★★ 只有 JJK 生物才标记为变体 ★★★
            if (baselineData != null && baselineData.isVariant(type, dataHash)) {
                if (EntityRenderHelper.isJJKEntity(type)) {
                    group.setVariant(true);
                }
            }
        }

        groupedCurses.addAll(groupMap.values());

        // 计算不同 EntityType 数量
        uniqueTypeCount = (int) groupedCurses.stream()
                .map(GroupedCurse::getEntityType)
                .distinct()
                .count();

        // 排序：类型 -> 基准优先 -> 数量
        groupedCurses.sort((a, b) -> {
            String typeA = EntityType.getKey(a.entityType).toString();
            String typeB = EntityType.getKey(b.entityType).toString();
            int typeCompare = typeA.compareTo(typeB);
            if (typeCompare != 0) return typeCompare;

            if (a.isVariant != b.isVariant) {
                return a.isVariant ? 1 : -1;
            }

            return Integer.compare(b.getCount(), a.getCount());
        });

        totalPages = Math.max(1, (groupedCurses.size() + itemsPerPage - 1) / itemsPerPage);
        currentPage = Math.min(currentPage, totalPages - 1);
    }

    // ==================== 翻页 ====================

    public void nextPage() {
        if (currentPage < totalPages - 1) currentPage++;
    }

    public void prevPage() {
        if (currentPage > 0) currentPage--;
    }

    // ==================== Getters ====================

    public List<AbsorbedCurse> getRawCurses() { return rawCurses; }
    public List<GroupedCurse> getGroupedCurses() { return groupedCurses; }
    public int getUniqueTypeCount() { return uniqueTypeCount; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public int getTotalCount() { return rawCurses.size(); }

    public int getStartIndex() {
        return currentPage * itemsPerPage;
    }

    @Nullable
    public GroupedCurse getGroupAt(int globalIndex) {
        if (globalIndex >= 0 && globalIndex < groupedCurses.size()) {
            return groupedCurses.get(globalIndex);
        }
        return null;
    }

    @Nullable
    public AbsorbedCurse getRawCurseAt(int index) {
        if (index >= 0 && index < rawCurses.size()) {
            return rawCurses.get(index);
        }
        return null;
    }

    /**
     * 获取咒灵的经验值
     */
    public float getCurseExperience(AbsorbedCurse curse) {
        if (curse == null) return 0;
        CompoundTag data = curse.getData();
        if (data == null || data.isEmpty()) return 0;
        if (data.contains("experience")) {
            return data.getFloat("experience");
        }
        return 0;
    }
}
