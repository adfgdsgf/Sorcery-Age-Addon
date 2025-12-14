package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.capability.AddonShadowStorageData;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.ShadowStorageSyncS2CPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ShadowStorageActionC2SPacket {

    public enum Action {
        STORE_ONE,
        STORE_HALF,
        STORE_ALL,
        RETRIEVE_ONE,
        RETRIEVE_HALF,
        RETRIEVE_ALL,
        RETRIEVE_STACK,
        SORT
    }

    private final Action action;
    private final int shadowIndex;
    private final int playerSlot;

    public ShadowStorageActionC2SPacket(Action action, int shadowIndex, int playerSlot) {
        this.action = action;
        this.shadowIndex = shadowIndex;
        this.playerSlot = playerSlot;
    }

    public ShadowStorageActionC2SPacket(FriendlyByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
        this.shadowIndex = buf.readInt();
        this.playerSlot = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(action.ordinal());
        buf.writeInt(shadowIndex);
        buf.writeInt(playerSlot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(storage -> {
                switch (action) {
                    case STORE_ONE -> storeItems(player, storage, playerSlot, 1);
                    case STORE_HALF -> {
                        ItemStack stack = player.getInventory().getItem(playerSlot);
                        storeItems(player, storage, playerSlot, Math.max(1, stack.getCount() / 2));
                    }
                    case STORE_ALL -> {
                        ItemStack stack = player.getInventory().getItem(playerSlot);
                        storeItems(player, storage, playerSlot, stack.getCount());
                    }
                    case RETRIEVE_ONE -> retrieveItems(player, storage, shadowIndex, 1);
                    case RETRIEVE_HALF -> {
                        List<AddonShadowStorageData.StorageEntry> entries = storage.getAll();
                        if (shadowIndex >= 0 && shadowIndex < entries.size()) {
                            long count = entries.get(shadowIndex).getCount();
                            retrieveItems(player, storage, shadowIndex, (int) Math.max(1, count / 2));
                        }
                    }
                    case RETRIEVE_ALL -> retrieveItems(player, storage, shadowIndex, 64);
                    case RETRIEVE_STACK -> retrieveAll(player, storage, shadowIndex);
                    case SORT -> storage.sort(playerSlot);
                }

                // ★★★ 操作完成后同步到客户端 ★★★
                syncToClient(player, storage);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    // ★★★ 新增：同步方法 ★★★
    private void syncToClient(ServerPlayer player, AddonShadowStorageData storage) {
        AddonNetwork.sendToPlayer(new ShadowStorageSyncS2CPacket(storage.save()), player);
    }

    private void storeItems(ServerPlayer player, AddonShadowStorageData storage, int slot, int amount) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) return;

        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) return;

        int toStore = Math.min(amount, stack.getCount());

        ItemStack toStoreStack = stack.copy();
        toStoreStack.setCount(toStore);

        storage.store(toStoreStack);

        stack.shrink(toStore);
        if (stack.isEmpty()) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
    }

    private void retrieveItems(ServerPlayer player, AddonShadowStorageData storage, int index, int amount) {
        ItemStack retrieved = storage.retrieve(index, amount);
        if (!retrieved.isEmpty()) {
            if (!player.getInventory().add(retrieved)) {
                player.drop(retrieved, false);
            }
        }
    }

    private void retrieveAll(ServerPlayer player, AddonShadowStorageData storage, int index) {
        List<AddonShadowStorageData.StorageEntry> entries = storage.getAll();
        if (index < 0 || index >= entries.size()) return;

        long totalCount = entries.get(index).getCount();
        List<ItemStack> stacks = storage.retrieveMultiple(index, totalCount);

        for (ItemStack stack : stacks) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }
}
