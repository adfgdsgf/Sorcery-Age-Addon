package com.jujutsuaddon.addon.network.s2c;

import com.jujutsuaddon.addon.client.cache.ProjectileLerpCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：投射物平滑移动
 * 客户端收到后进行插值，而不是瞬移
 */
public class ProjectileSmoothMoveS2CPacket {

    private final int entityId;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final int lerpTicks;

    public ProjectileSmoothMoveS2CPacket(int entityId, Vec3 targetPos, int lerpTicks) {
        this.entityId = entityId;
        this.targetX = targetPos.x;
        this.targetY = targetPos.y;
        this.targetZ = targetPos.z;
        this.lerpTicks = lerpTicks;
    }

    public ProjectileSmoothMoveS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
        this.lerpTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeDouble(targetX);
        buf.writeDouble(targetY);
        buf.writeDouble(targetZ);
        buf.writeVarInt(lerpTicks);
    }

    public static ProjectileSmoothMoveS2CPacket decode(FriendlyByteBuf buf) {
        return new ProjectileSmoothMoveS2CPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient);
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleClient() {
        ProjectileLerpCache.addLerp(entityId, new Vec3(targetX, targetY, targetZ), lerpTicks);
    }
}
