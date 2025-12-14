package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.util.helper.TechniqueAccessHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;

import java.util.List;
import java.util.function.Supplier;

public class StoreShadowItemC2SPacket {

    public StoreShadowItemC2SPacket() {}

    public StoreShadowItemC2SPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!TechniqueAccessHelper.canUseTenShadows(player)) {
                return;
            }

            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty()) return;

            ITenShadowsData data = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (data == null) return;

            // ★★★ 尝试堆叠到现有物品 ★★★
            ItemStack toStore = mainHand.copy();
            List<ItemStack> inventory = data.getShadowInventory();

            int remaining = toStore.getCount();

            for (int i = 0; i < inventory.size() && remaining > 0; i++) {
                ItemStack existing = inventory.get(i);
                if (canStack(existing, toStore)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, remaining);
                        existing.grow(toAdd);
                        remaining -= toAdd;
                    }
                }
            }

            // 剩余的作为新物品添加
            if (remaining > 0) {
                ItemStack newStack = toStore.copy();
                newStack.setCount(remaining);
                data.addShadowInventory(newStack);
            }

            // 清空主手
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

            // ★★★ 强制同步 capability 到客户端 ★★★
            syncTenShadowsData(player);

            // 播放音效
            player.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.3F, 1.5F);

            // 服务端粒子
            for (int i = 0; i < 8; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 0.5;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 0.5;
                player.serverLevel().sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        player.getX() + offsetX, player.getY() + 0.5, player.getZ() + offsetZ,
                        1, 0, 0.05, 0, 0.01
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * ★★★ 强制同步十影数据到客户端 ★★★
     */
    private static void syncTenShadowsData(ServerPlayer player) {
        // 使用 JJK 的同步机制
        player.getCapability(TenShadowsDataHandler.INSTANCE).ifPresent(data -> {
            // 标记为需要同步（如果 JJK 有这个方法的话）
            // 或者直接发送同步包
            radon.jujutsu_kaisen.network.PacketHandler.sendToClient(
                    new radon.jujutsu_kaisen.network.packet.s2c.SyncTenShadowsDataS2CPacket(
                            data.serializeNBT()
                    ), player
            );
        });
    }

    private static boolean canStack(ItemStack existing, ItemStack toAdd) {
        if (existing.isEmpty() || toAdd.isEmpty()) return false;
        if (!existing.is(toAdd.getItem())) return false;
        if (existing.getCount() >= existing.getMaxStackSize()) return false;
        if (existing.isDamageableItem() && existing.getDamageValue() != toAdd.getDamageValue()) return false;
        return ItemStack.isSameItemSameTags(existing, toAdd);
    }
}
