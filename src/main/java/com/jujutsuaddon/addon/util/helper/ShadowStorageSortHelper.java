package com.jujutsuaddon.addon.util.helper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * 影子库存排序工具类
 */
public class ShadowStorageSortHelper {

    public enum SortMode {
        NONE,       // 不排序（按存入顺序）
        TYPE,       // 按物品类型（相同物品放一起）
        NAME,       // 按名称首字母
        COUNT_DESC, // 按数量降序（多的在前）
        COUNT_ASC   // 按数量升序（少的在前）
    }

    /**
     * 获取下一个排序模式（循环切换）
     */
    public static SortMode getNextMode(SortMode current) {
        SortMode[] modes = SortMode.values();
        int next = (current.ordinal() + 1) % modes.length;
        return modes[next];
    }

    /**
     * 获取排序模式的显示名称
     */
    public static String getSortModeName(SortMode mode) {
        return switch (mode) {
            case NONE -> "gui.jujutsu_addon.shadow_storage.sort.none";
            case TYPE -> "gui.jujutsu_addon.shadow_storage.sort.type";
            case NAME -> "gui.jujutsu_addon.shadow_storage.sort.name";
            case COUNT_DESC -> "gui.jujutsu_addon.shadow_storage.sort.count_desc";
            case COUNT_ASC -> "gui.jujutsu_addon.shadow_storage.sort.count_asc";
        };
    }

    /**
     * 获取排序模式的图标/简写
     */
    public static String getSortModeIcon(SortMode mode) {
        return switch (mode) {
            case NONE -> "—";
            case TYPE -> "▦";
            case NAME -> "A";
            case COUNT_DESC -> "9↓";
            case COUNT_ASC -> "1↑";
        };
    }

    /**
     * 对物品列表进行排序（返回新列表，不修改原列表）
     */
    public static List<ItemStack> sortItems(List<ItemStack> items, SortMode mode) {
        if (mode == SortMode.NONE || items.isEmpty()) {
            return items;
        }

        return items.stream()
                .sorted(getComparator(mode))
                .toList();
    }

    /**
     * 获取对应模式的比较器
     */
    public static Comparator<ItemStack> getComparator(SortMode mode) {
        return switch (mode) {
            case TYPE -> Comparator
                    .comparing((ItemStack stack) -> getItemId(stack))
                    .thenComparing(ItemStack::getCount, Comparator.reverseOrder());

            case NAME -> Comparator
                    .comparing((ItemStack stack) -> stack.getHoverName().getString().toLowerCase())
                    .thenComparing(ItemStack::getCount, Comparator.reverseOrder());

            case COUNT_DESC -> Comparator
                    .comparing(ItemStack::getCount, Comparator.reverseOrder())
                    .thenComparing(ShadowStorageSortHelper::getItemId);

            case COUNT_ASC -> Comparator
                    .comparing(ItemStack::getCount)
                    .thenComparing(ShadowStorageSortHelper::getItemId);

            default -> (a, b) -> 0; // NONE: 保持原顺序
        };
    }

    /**
     * 获取物品的注册ID（用于按类型排序）
     */
    private static String getItemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : "";
    }

    /**
     * 合并同类物品（整理功能的核心）
     * 返回合并后的新列表
     */
    public static List<ItemStack> mergeItems(List<ItemStack> items) {
        if (items.isEmpty()) return items;

        java.util.Map<ItemStackKey, Long> merged = new java.util.LinkedHashMap<>();

        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;

            ItemStackKey key = new ItemStackKey(stack);
            merged.merge(key, (long) stack.getCount(), Long::sum);
        }

        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        for (var entry : merged.entrySet()) {
            ItemStack template = entry.getKey().template;
            long count = entry.getValue();

            // ★★★ 无限堆叠：直接用 int 存储（最大约21亿）★★★
            ItemStack newStack = template.copy();
            newStack.setCount((int) Math.min(count, Integer.MAX_VALUE));
            result.add(newStack);
        }

        return result;
    }

    /**
     * 物品堆叠键（用于合并判断）
     */
    private static class ItemStackKey {
        final ItemStack template;
        final String id;
        final String nbt;

        ItemStackKey(ItemStack stack) {
            this.template = stack.copyWithCount(1);
            this.id = getItemId(stack);
            this.nbt = stack.getTag() != null ? stack.getTag().toString() : "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemStackKey other)) return false;
            return id.equals(other.id) && nbt.equals(other.nbt);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + nbt.hashCode();
        }
    }
}
