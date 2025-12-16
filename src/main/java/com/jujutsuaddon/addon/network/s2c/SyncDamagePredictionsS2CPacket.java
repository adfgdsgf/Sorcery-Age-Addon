package com.jujutsuaddon.addon.network.s2c;

import com.jujutsuaddon.addon.client.cache.DamagePredictionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncDamagePredictionsS2CPacket {

    private final Map<String, PredictionData> predictions;

    public static class PredictionData {
        public final float vanillaDamage;
        public final float addonDamage;
        public final float critDamage;
        public final boolean isMelee;
        public final int damageType;

        public PredictionData(float vanillaDamage, float addonDamage, float critDamage, boolean isMelee, int damageType) {
            this.vanillaDamage = vanillaDamage;
            this.addonDamage = addonDamage;
            this.critDamage = critDamage;
            this.isMelee = isMelee;
            this.damageType = damageType;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeFloat(vanillaDamage);
            buf.writeFloat(addonDamage);
            buf.writeFloat(critDamage);
            buf.writeBoolean(isMelee);
            buf.writeInt(damageType);
        }

        public static PredictionData read(FriendlyByteBuf buf) {
            return new PredictionData(
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readInt()
            );
        }
    }

    public SyncDamagePredictionsS2CPacket(Map<String, PredictionData> predictions) {
        this.predictions = predictions;
    }

    public static void encode(SyncDamagePredictionsS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.predictions.size());
        for (Map.Entry<String, PredictionData> entry : msg.predictions.entrySet()) {
            buf.writeUtf(entry.getKey());
            entry.getValue().write(buf);
        }
    }

    public static SyncDamagePredictionsS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, PredictionData> predictions = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            PredictionData data = PredictionData.read(buf);
            predictions.put(key, data);
        }
        return new SyncDamagePredictionsS2CPacket(predictions);
    }

    public static void handle(SyncDamagePredictionsS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(SyncDamagePredictionsS2CPacket msg) {
        if (Minecraft.getInstance().player != null) {
            DamagePredictionCache.update(msg.predictions);
        }
    }
}
