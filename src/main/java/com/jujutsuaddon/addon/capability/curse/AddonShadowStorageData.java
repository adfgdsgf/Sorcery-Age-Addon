package com.jujutsuaddon.addon.capability.curse;

import com.jujutsuaddon.addon.network.c2s.ShadowStorageActionC2SPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义影子库存 - 支持无限堆叠
 * 充血模型：包含数据和业务逻辑
 */
public class AddonShadowStorageData implements INBTSerializable<CompoundTag> {

    // ==================== Capability 注册 ====================

    public static final Capability<AddonShadowStorageData> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    // ==================== 数据存储 ====================

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

        public ItemStack getTemplate() { return template; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public void addCount(long amount) { this.count += amount; }

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

    // ==================== 业务逻辑 (Action Handling) ====================

    /**
     * 处理来自客户端的操作请求
     */
    public void handleAction(ServerPlayer player, int actionId, int shadowIndex, int playerSlot) {
        ShadowStorageActionC2SPacket.Action action = ShadowStorageActionC2SPacket.Action.values()[actionId];

        switch (action) {
            case STORE_ONE -> storeFromPlayer(player, playerSlot, 1);
            case STORE_HALF -> {
                ItemStack stack = player.getInventory().getItem(playerSlot);
                storeFromPlayer(player, playerSlot, Math.max(1, stack.getCount() / 2));
            }
            case STORE_ALL -> {
                ItemStack stack = player.getInventory().getItem(playerSlot);
                storeFromPlayer(player, playerSlot, stack.getCount());
            }
            case RETRIEVE_ONE -> retrieveToPlayer(player, shadowIndex, 1);
            case RETRIEVE_HALF -> {
                if (shadowIndex >= 0 && shadowIndex < storage.size()) {
                    long count = storage.get(shadowIndex).getCount();
                    retrieveToPlayer(player, shadowIndex, (int) Math.max(1, count / 2));
                }
            }
            case RETRIEVE_ALL -> retrieveToPlayer(player, shadowIndex, 64);
            case RETRIEVE_STACK -> retrieveAllToPlayer(player, shadowIndex);
            case SORT -> this.sort(playerSlot); // playerSlot 复用为 sortMode
        }
    }

    private void storeFromPlayer(ServerPlayer player, int slot, int amount) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) return;

        int toStore = Math.min(amount, stack.getCount());
        ItemStack toStoreStack = stack.copy();
        toStoreStack.setCount(toStore);

        this.store(toStoreStack);

        stack.shrink(toStore);
        if (stack.isEmpty()) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
    }

    private void retrieveToPlayer(ServerPlayer player, int index, int amount) {
        ItemStack retrieved = this.retrieve(index, amount);
        if (!retrieved.isEmpty()) {
            if (!player.getInventory().add(retrieved)) {
                player.drop(retrieved, false);
            }
        }
    }

    private void retrieveAllToPlayer(ServerPlayer player, int index) {
        if (index < 0 || index >= storage.size()) return;
        long totalCount = storage.get(index).getCount();
        List<ItemStack> stacks = this.retrieveMultiple(index, totalCount);

        for (ItemStack stack : stacks) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    // ==================== 基础存取操作 ====================

    public void store(ItemStack stack) {
        if (stack.isEmpty()) return;
        int amount = stack.getCount();

        for (StorageEntry entry : storage) {
            if (entry.matches(stack)) {
                entry.addCount(amount);
                return;
            }
        }
        storage.add(new StorageEntry(stack, amount));
    }

    public ItemStack retrieve(int index, int amount) {
        if (index < 0 || index >= storage.size()) return ItemStack.EMPTY;

        StorageEntry entry = storage.get(index);
        long actualAmount;
        if (amount < 0) {
            actualAmount = Math.min(entry.getCount(), entry.getTemplate().getMaxStackSize());
        } else {
            actualAmount = Math.min(amount, entry.getCount());
            actualAmount = Math.min(actualAmount, entry.getTemplate().getMaxStackSize());
        }

        ItemStack result = entry.getTemplate().copy();
        result.setCount((int) actualAmount);

        entry.addCount(-actualAmount);
        if (entry.getCount() <= 0) {
            storage.remove(index);
        }
        return result;
    }

    public List<ItemStack> retrieveMultiple(int index, long amount) {
        List<ItemStack> result = new ArrayList<>();
        if (index < 0 || index >= storage.size()) return result;

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

        entry.addCount(-Math.min(amount, entry.getCount()));
        if (entry.getCount() <= 0) {
            storage.remove(index);
        }
        return result;
    }

    public List<StorageEntry> getAll() {
        return Collections.unmodifiableList(storage);
    }

    public void sort(int sortMode) {
        switch (sortMode) {
            case 1 -> storage.sort((a, b) -> a.getTemplate().getHoverName().getString()
                    .compareToIgnoreCase(b.getTemplate().getHoverName().getString()));
            case 2 -> storage.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
            case 3 -> storage.sort((a, b) -> Long.compare(a.getCount(), b.getCount()));
            case 4 -> storage.sort((a, b) -> {
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

    // ★★★ 实现接口方法 ★★★
    @Override
    public CompoundTag serializeNBT() {
        return save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        load(nbt);
    }

    // ★★★ 保留 Provider 以兼容旧代码 ★★★
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
