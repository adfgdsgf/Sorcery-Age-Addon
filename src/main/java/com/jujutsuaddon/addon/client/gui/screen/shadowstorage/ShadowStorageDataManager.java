package com.jujutsuaddon.addon.client.gui.screen.shadowstorage;

import com.jujutsuaddon.addon.capability.AddonShadowStorageData;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.util.FuzzySearchHelper;
import com.jujutsuaddon.addon.util.helper.ShadowStorageSortHelper;
import com.jujutsuaddon.addon.util.helper.ShadowStorageSortHelper.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 影子库存数据管理
 */
public class ShadowStorageDataManager {

    private List<ItemStack> cachedItems = new ArrayList<>();
    private List<ItemStack> displayItems = new ArrayList<>();
    private SortMode sortMode = SortMode.NONE;
    private String searchText = "";

    public ShadowStorageDataManager() {
        // ★★★ 从配置加载上次的排序模式 ★★★
        loadSortModeFromConfig();
    }

    private void loadSortModeFromConfig() {
        try {
            AddonClientConfig.ShadowStorageSortMode saved =
                    AddonClientConfig.CLIENT.shadowStorageSortMode.get();
            sortMode = SortMode.valueOf(saved.name());
        } catch (Exception e) {
            sortMode = SortMode.NONE;
        }
    }

    private void saveSortModeToConfig() {
        try {
            AddonClientConfig.ShadowStorageSortMode configMode =
                    AddonClientConfig.ShadowStorageSortMode.valueOf(sortMode.name());
            AddonClientConfig.CLIENT.shadowStorageSortMode.set(configMode);
        } catch (Exception ignored) {}
    }

    // ==================== 数据加载 ====================

    public void refresh() {
        cachedItems.clear();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            displayItems = new ArrayList<>();
            return;
        }

        player.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(storage -> {
            for (AddonShadowStorageData.StorageEntry entry : storage.getAll()) {
                ItemStack displayStack = entry.getTemplate().copy();
                displayStack.setCount((int) Math.min(entry.getCount(), Integer.MAX_VALUE));
                cachedItems.add(displayStack);
            }
        });

        applyFilter();
    }

    // ==================== 搜索和过滤 ====================

    public void setSearchText(String text) {
        this.searchText = text.trim();
        applyFilter();
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSortMode(SortMode mode) {
        this.sortMode = mode;
        saveSortModeToConfig(); // ★★★ 保存 ★★★
        applyFilter();
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void nextSortMode() {
        sortMode = ShadowStorageSortHelper.getNextMode(sortMode);
        saveSortModeToConfig(); // ★★★ 保存 ★★★
        applyFilter();
    }

    private void applyFilter() {
        List<ItemStack> filtered;

        if (searchText.isEmpty()) {
            filtered = new ArrayList<>(cachedItems);
        } else {
            filtered = new ArrayList<>();
            String search = searchText.toLowerCase();

            for (ItemStack stack : cachedItems) {
                if (matchesSearch(stack, search)) {
                    filtered.add(stack);
                }
            }
        }

        displayItems = new ArrayList<>(ShadowStorageSortHelper.sortItems(filtered, sortMode));
    }

    private boolean matchesSearch(ItemStack stack, String search) {
        String displayName = stack.getHoverName().getString();
        if (FuzzySearchHelper.contains(displayName, search)) {
            return true;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (FuzzySearchHelper.contains(itemId, search)) {
            return true;
        }

        String fullId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return FuzzySearchHelper.contains(fullId, search);
    }

    // ==================== 数据访问 ====================

    public List<ItemStack> getDisplayItems() {
        return displayItems;
    }

    public int getDisplayCount() {
        return displayItems.size();
    }

    public int getTotalCount() {
        return cachedItems.size();
    }

    public long getTotalItemCount() {
        long total = 0;
        for (ItemStack stack : cachedItems) {
            total += stack.getCount();
        }
        return total;
    }

    public ItemStack getDisplayItem(int index) {
        if (index >= 0 && index < displayItems.size()) {
            return displayItems.get(index);
        }
        return ItemStack.EMPTY;
    }

    public int findRealIndex(ItemStack displayStack) {
        for (int i = 0; i < cachedItems.size(); i++) {
            ItemStack cached = cachedItems.get(i);
            if (ItemStack.isSameItemSameTags(cached, displayStack) &&
                    cached.getCount() == displayStack.getCount()) {
                return i;
            }
        }
        for (int i = 0; i < cachedItems.size(); i++) {
            if (ItemStack.isSameItemSameTags(cachedItems.get(i), displayStack)) {
                return i;
            }
        }
        return -1;
    }

    // ==================== 滚动计算 ====================

    public int getTotalRows() {
        int totalSlots = displayItems.size() + 1;
        return Math.max(ShadowStorageLayout.SHADOW_ROWS,
                (totalSlots + ShadowStorageLayout.COLS - 1) / ShadowStorageLayout.COLS);
    }

    public int getMaxScrollRow() {
        return Math.max(0, getTotalRows() - ShadowStorageLayout.SHADOW_ROWS);
    }

    public int clampScrollRow(int scrollRow) {
        return Math.max(0, Math.min(scrollRow, getMaxScrollRow()));
    }
}
