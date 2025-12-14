package com.jujutsuaddon.addon.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 自定义影子库存 - 支持无限堆叠
 */
public class AddonShadowStorageData {

    // ==================== Capability 注册 ====================

    public static final Capability<AddonShadowStorageData> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    // ==================== 数据存储 ====================

    /**
     * 存储结构：物品类型 -> 数量
     * 使用 ItemStack 作为模板（count=1），数量单独存储
     */
    private final List<StorageEntry> storage = new ArrayList<>();

    /**
     * 存储条目
     */
    public static class StorageEntry {
        private final ItemStack template;  // 物品模板（count固定为1）
        private long count;                // 实际数量（支持超大数字）

        public StorageEntry(ItemStack template, long count) {
            this.template = template.copy();
            this.template.setCount(1);
            this.count = count;
        }

        public ItemStack getTemplate() {
            return template;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public void addCount(long amount) {
            this.count += amount;
        }

        /**
         * 创建用于显示的 ItemStack（数量会被截断为 int）
         */
        public ItemStack createDisplayStack() {
            ItemStack display = template.copy();
            display.setCount((int) Math.min(count, Integer.MAX_VALUE));
            return display;
        }

        /**
         * 检查物品是否匹配
         */
        public boolean matches(ItemStack stack) {
            return ItemStack.isSameItemSameTags(template, stack);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.put("Item", template.save(new CompoundTag()));
            tag.putLong("Count", count);
            return tag;
        }

        public static StorageEntry load(CompoundTag tag) {
            ItemStack template = ItemStack.of(tag.getCompound("Item"));
            long count = tag.getLong("Count");
            return new StorageEntry(template, count);
        }
    }

    // ==================== 存取操作 ====================

    /**
     * 存入物品
     */
    public void store(ItemStack stack) {
        if (stack.isEmpty()) return;

        int amount = stack.getCount();

        // 查找是否已有相同物品
        for (StorageEntry entry : storage) {
            if (entry.matches(stack)) {
                entry.addCount(amount);
                return;
            }
        }

        // 新物品类型
        storage.add(new StorageEntry(stack, amount));
    }

    /**
     * 取出物品
     * @param index 物品索引
     * @param amount 取出数量（-1 表示全部）
     * @return 实际取出的物品
     */
    public ItemStack retrieve(int index, int amount) {
        if (index < 0 || index >= storage.size()) {
            return ItemStack.EMPTY;
        }

        StorageEntry entry = storage.get(index);

        // 计算实际取出数量
        long actualAmount;
        if (amount < 0) {
            // 全部取出，但限制为一组
            actualAmount = Math.min(entry.getCount(), entry.getTemplate().getMaxStackSize());
        } else {
            actualAmount = Math.min(amount, entry.getCount());
            actualAmount = Math.min(actualAmount, entry.getTemplate().getMaxStackSize());
        }

        // 创建结果物品
        ItemStack result = entry.getTemplate().copy();
        result.setCount((int) actualAmount);

        // 更新存储
        entry.addCount(-actualAmount);
        if (entry.getCount() <= 0) {
            storage.remove(index);
        }

        return result;
    }

    /**
     * 取出指定数量（可以超过一组）
     */
    public List<ItemStack> retrieveMultiple(int index, long amount) {
        List<ItemStack> result = new ArrayList<>();

        if (index < 0 || index >= storage.size()) {
            return result;
        }

        StorageEntry entry = storage.get(index);
        long remaining = Math.min(amount, entry.getCount());
        int maxStack = entry.getTemplate().getMaxStackSize();

        while (remaining > 0) {
            int takeAmount = (int) Math.min(remaining, maxStack);
            ItemStack stack = entry.getTemplate().copy();
            stack.setCount(takeAmount);
            result.add(stack);
            remaining -= takeAmount;
        }

        // 更新存储
        entry.addCount(-Math.min(amount, entry.getCount()));
        if (entry.getCount() <= 0) {
            storage.remove(index);
        }

        return result;
    }

    /**
     * 获取所有存储的物品（用于显示）
     */
    public List<StorageEntry> getAll() {
        return Collections.unmodifiableList(storage);
    }

    /**
     * 获取物品种类数
     */
    public int getTypeCount() {
        return storage.size();
    }

    /**
     * 获取总物品数量
     */
    public long getTotalCount() {
        long total = 0;
        for (StorageEntry entry : storage) {
            total += entry.getCount();
        }
        return total;
    }

    /**
     * 根据排序模式排序
     */
    public void sort(int sortMode) {
        switch (sortMode) {
            case 1 -> // 按名称
                    storage.sort((a, b) -> a.getTemplate().getHoverName().getString()
                            .compareToIgnoreCase(b.getTemplate().getHoverName().getString()));
            case 2 -> // 按数量（多到少）
                    storage.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
            case 3 -> // 按数量（少到多）
                    storage.sort((a, b) -> Long.compare(a.getCount(), b.getCount()));
            case 4 -> // 按物品ID
                    storage.sort((a, b) -> {
                        String idA = a.getTemplate().getItem().toString();
                        String idB = b.getTemplate().getItem().toString();
                        return idA.compareToIgnoreCase(idB);
                    });
        }
    }

    // ==================== 序列化 ====================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (StorageEntry entry : storage) {
            list.add(entry.save());
        }
        tag.put("Storage", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        storage.clear();
        ListTag list = tag.getList("Storage", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            StorageEntry entry = StorageEntry.load(list.getCompound(i));
            if (!entry.getTemplate().isEmpty() && entry.getCount() > 0) {
                storage.add(entry);
            }
        }
    }

    // ==================== Capability Provider ====================

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final AddonShadowStorageData data = new AddonShadowStorageData();
        private final LazyOptional<AddonShadowStorageData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
