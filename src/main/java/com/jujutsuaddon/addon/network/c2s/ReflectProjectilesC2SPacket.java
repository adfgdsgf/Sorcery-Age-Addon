package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile.ProjectileReflector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReflectProjectilesC2SPacket {

    private final boolean towardsCursor;
    private final Vec3 lookDirection;

    public ReflectProjectilesC2SPacket(boolean towardsCursor, Vec3 lookDirection) {
        this.towardsCursor = towardsCursor;
        this.lookDirection = lookDirection;
    }

    public ReflectProjectilesC2SPacket(FriendlyByteBuf buf) {
        this.towardsCursor = buf.readBoolean();
        this.lookDirection = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(towardsCursor);
        buf.writeDouble(lookDirection.x);
        buf.writeDouble(lookDirection.y);
        buf.writeDouble(lookDirection.z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ProjectileReflector.reflectControlledProjectiles(player, towardsCursor, lookDirection);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
