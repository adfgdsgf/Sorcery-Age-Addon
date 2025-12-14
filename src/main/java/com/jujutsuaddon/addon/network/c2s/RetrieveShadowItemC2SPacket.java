package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.util.helper.TechniqueAccessHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;

import java.util.List;
import java.util.function.Supplier;

public class RetrieveShadowItemC2SPacket {

    private final int index;

    public RetrieveShadowItemC2SPacket(int index) {
        this.index = index;
    }

    public RetrieveShadowItemC2SPacket(FriendlyByteBuf buf) {
        this.index = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(index);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!TechniqueAccessHelper.canUseTenShadows(player)) {
                return;
            }

            ITenShadowsData data = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (data == null) return;

            List<ItemStack> inventory = data.getShadowInventory();
            if (index < 0 || index >= inventory.size()) return;

            ItemStack retrieved = inventory.get(index).copy();

            // 尝试给予玩家物品
            if (!player.getInventory().add(retrieved)) {
                player.drop(retrieved, false);
            }

            // 从影子库存移除
            data.removeShadowInventory(index);

            // ★★★ 强制同步 capability 到客户端 ★★★
            radon.jujutsu_kaisen.network.PacketHandler.sendToClient(
                    new radon.jujutsu_kaisen.network.packet.s2c.SyncTenShadowsDataS2CPacket(
                            data.serializeNBT()
                    ), player
            );

            // 播放音效
            player.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.3F, 1.2F);

            // 粒子效果
            for (int i = 0; i < 6; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 0.5;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 0.5;
                player.serverLevel().sendParticles(
                        net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        player.getX() + offsetX, player.getY() + 0.5, player.getZ() + offsetZ,
                        1, 0, 0.08, 0, 0.01
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
