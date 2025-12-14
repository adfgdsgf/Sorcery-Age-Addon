package com.jujutsuaddon.addon.inventory;

import com.jujutsuaddon.addon.JujutsuAddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncTenShadowsDataS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class ShadowStorageMenu extends AbstractContainerMenu {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, JujutsuAddon.MODID);

    public static final RegistryObject<MenuType<ShadowStorageMenu>> SHADOW_STORAGE_MENU =
            MENUS.register("shadow_storage", () -> IForgeMenuType.create(ShadowStorageMenu::new));

    public static final int VISIBLE_ROWS = 6;
    public static final int COLS = 9;
    public static final int VISIBLE_SLOTS = VISIBLE_ROWS * COLS;

    private final List<ItemStack> allShadowItems;
    private final SimpleContainer visibleContainer;
    private final Player player;
    private int scrollRow = 0;

    // ★★★ 防止循环同步 ★★★
    private boolean isUpdating = false;

    // 客户端构造
    public ShadowStorageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, readItemsFromBuffer(extraData));
    }

    private static List<ItemStack> readItemsFromBuffer(FriendlyByteBuf buf) {
        int itemCount = buf.readVarInt();
        List<ItemStack> items = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(buf.readItem());
        }
        return items;
    }

    // 服务端构造
    public ShadowStorageMenu(int containerId, Inventory playerInventory, List<ItemStack> shadowItems) {
        super(SHADOW_STORAGE_MENU.get(), containerId);
        this.player = playerInventory.player;

        // ★★★ 深拷贝 ★★★
        this.allShadowItems = new ArrayList<>();
        for (ItemStack stack : shadowItems) {
            this.allShadowItems.add(stack.copy());
        }

        this.visibleContainer = new SimpleContainer(VISIBLE_SLOTS);
        updateVisibleSlots();

        // 影子库存槽位
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                this.addSlot(new ShadowSlot(visibleContainer, index, 8 + col * 18, 18 + row * 18));
            }
        }

        // 玩家背包
        int playerInvY = 18 + VISIBLE_ROWS * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        // 快捷栏
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    // ==================== 滚动 ====================
    public void scroll(int delta) {
        int maxRows = getMaxScrollRow();
        int newRow = Math.max(0, Math.min(scrollRow - delta, maxRows));

        if (newRow != scrollRow) {
            // ★★★ 先同步当前可见内容到列表 ★★★
            syncFromVisible();
            scrollRow = newRow;
            updateVisibleSlots();
        }
    }

    public int getScrollRow() { return scrollRow; }

    public int getTotalRows() {
        int itemCount = getActualItemCount();
        // 至少显示可见行数，额外留一行空位
        return Math.max(VISIBLE_ROWS, (itemCount / COLS) + 2);
    }

    public int getMaxScrollRow() {
        return Math.max(0, getTotalRows() - VISIBLE_ROWS);
    }

    public float getScrollProgress() {
        int max = getMaxScrollRow();
        return max > 0 ? (float) scrollRow / max : 0;
    }

    // ★★★ 计算实际物品数量（压缩后） ★★★
    private int getActualItemCount() {
        int count = 0;
        for (ItemStack item : allShadowItems) {
            if (!item.isEmpty()) count++;
        }
        return count;
    }

    // ★★★ 从可见容器同步到列表（只同步有变化的） ★★★
    private void syncFromVisible() {
        if (isUpdating) return;

        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            int actualIndex = scrollRow * COLS + i;
            ItemStack visibleStack = visibleContainer.getItem(i);

            // 确保列表足够大
            while (allShadowItems.size() <= actualIndex) {
                allShadowItems.add(ItemStack.EMPTY);
            }

            // 只在实际不同时更新
            ItemStack existing = allShadowItems.get(actualIndex);
            if (!ItemStack.matches(existing, visibleStack)) {
                allShadowItems.set(actualIndex, visibleStack.copy());
            }
        }
    }

    // ★★★ 更新可见槽位 ★★★
    private void updateVisibleSlots() {
        isUpdating = true;
        try {
            for (int i = 0; i < VISIBLE_SLOTS; i++) {
                int actualIndex = scrollRow * COLS + i;
                if (actualIndex < allShadowItems.size()) {
                    visibleContainer.setItem(i, allShadowItems.get(actualIndex).copy());
                } else {
                    visibleContainer.setItem(i, ItemStack.EMPTY);
                }
            }
        } finally {
            isUpdating = false;
        }
    }

    // ==================== 物品转移 ====================
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < VISIBLE_SLOTS) {
                // 影子库存 -> 玩家背包
                if (!this.moveItemStackTo(slotStack, VISIBLE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                // ★★★ 清除对应的列表项 ★★★
                int actualIndex = scrollRow * COLS + index;
                if (actualIndex < allShadowItems.size()) {
                    allShadowItems.set(actualIndex, ItemStack.EMPTY);
                }
            } else {
                // 玩家背包 -> 影子库存
                if (!this.moveItemStackTo(slotStack, 0, VISIBLE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // ==================== 关闭时保存 ====================
    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 同步当前可见内容
            syncFromVisible();

            // 保存到 Capability（压缩空槽位）
            saveToCapability(player);

            // 同步到客户端
            ITenShadowsData data = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (data != null) {
                PacketHandler.sendToClient(
                        new SyncTenShadowsDataS2CPacket(data.serializeNBT()),
                        serverPlayer
                );
            }
        }
    }

    // ★★★ 保存到 Capability（只保存非空物品） ★★★
    private void saveToCapability(Player player) {
        ITenShadowsData data = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
        if (data == null) return;

        // 清空现有库存
        List<ItemStack> currentInventory = data.getShadowInventory();
        while (!currentInventory.isEmpty()) {
            data.removeShadowInventory(0);
        }

        // ★★★ 只添加非空物品，跳过空槽位 ★★★
        for (ItemStack stack : allShadowItems) {
            if (!stack.isEmpty()) {
                data.addShadowInventory(stack.copy());
            }
        }
    }

    // ==================== 槽位变化处理 ★★★ 核心修复 ★★★ ====================

    // ★★★ 移除 setItem 中的同步逻辑，避免重复写入 ★★★
    // 原来的 setItem 会导致物品被复制到错误位置

    // ==================== 影子槽位 ====================
    private class ShadowSlot extends Slot {
        public ShadowSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();

            // ★★★ 防止在 updateVisibleSlots 时触发 ★★★
            if (isUpdating) return;

            int slotIndex = this.getContainerSlot();
            int actualIndex = scrollRow * COLS + slotIndex;

            // 扩展列表
            while (allShadowItems.size() <= actualIndex) {
                allShadowItems.add(ItemStack.EMPTY);
            }

            // 同步到列表
            allShadowItems.set(actualIndex, this.getItem().copy());
        }
    }

    // ==================== 工具方法 ====================
    public static List<ItemStack> loadFromCapability(Player player) {
        List<ItemStack> items = new ArrayList<>();
        ITenShadowsData data = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
        if (data != null) {
            for (ItemStack stack : data.getShadowInventory()) {
                items.add(stack.copy());
            }
        }
        return items;
    }
}
