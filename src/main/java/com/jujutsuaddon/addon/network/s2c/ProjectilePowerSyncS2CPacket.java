package com.jujutsuaddon.addon.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 同步火焰弹的 power 字段到客户端
 *
 * 原因：xPower, yPower, zPower 是普通字段，不会自动同步
 * 当服务端改变 power 方向后，需要手动同步到客户端
 */
public class ProjectilePowerSyncS2CPacket {

    private final int entityId;
    private final double xPower;
    private final double yPower;
    private final double zPower;

    public ProjectilePowerSyncS2CPacket(int entityId, double xPower, double yPower, double zPower) {
        this.entityId = entityId;
        this.xPower = xPower;
        this.yPower = yPower;
        this.zPower = zPower;
    }

    public ProjectilePowerSyncS2CPacket(AbstractHurtingProjectile projectile) {
        this.entityId = projectile.getId();
        this.xPower = projectile.xPower;
        this.yPower = projectile.yPower;
        this.zPower = projectile.zPower;
    }

    public ProjectilePowerSyncS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.xPower = buf.readDouble();
        this.yPower = buf.readDouble();
        this.zPower = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeDouble(xPower);
        buf.writeDouble(yPower);
        buf.writeDouble(zPower);
    }

    public static ProjectilePowerSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new ProjectilePowerSyncS2CPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient);
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleClient() {
        if (Minecraft.getInstance().level == null) return;

        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity instanceof AbstractHurtingProjectile projectile) {
            projectile.xPower = this.xPower;
            projectile.yPower = this.yPower;
            projectile.zPower = this.zPower;
        }
    }
}
