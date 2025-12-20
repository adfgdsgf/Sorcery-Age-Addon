package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.InfinityFieldData;
import com.jujutsuaddon.addon.network.s2c.InfinityFieldSyncS2CPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 无下限场同步管理器
 */
public class InfinityFieldSyncManager {

    private static final int SYNC_INTERVAL = 4;

    public static void onServerTick(ServerLevel level) {
        if (!PressureConfig.isEnabled()) return;

        long gameTick = level.getGameTime();
        if (gameTick % SYNC_INTERVAL != 0) return;

        for (ServerPlayer player : level.players()) {
            syncFieldsForPlayer(player, gameTick);
        }
    }

    private static void syncFieldsForPlayer(ServerPlayer player, long gameTick) {
        List<InfinityFieldData> fields = findNearbyInfinityFields(player, 30.0);
        InfinityFieldSyncS2CPacket packet = new InfinityFieldSyncS2CPacket(fields, gameTick);
        AddonNetwork.sendToPlayer(packet, player);
    }

    private static List<InfinityFieldData> findNearbyInfinityFields(ServerPlayer player, double searchRange) {
        List<InfinityFieldData> fields = new ArrayList<>();
        AABB searchArea = player.getBoundingBox().inflate(searchRange);

        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
                LivingEntity.class, searchArea, entity -> {
                    if (entity == player) return false;
                    if (!JJKAbilities.hasToggled(entity, JJKAbilities.INFINITY.get())) return false;

                    ISorcererData data = entity.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
                    if (data == null) return false;
                    if (!(data instanceof IInfinityPressureAccessor accessor)) return false;

                    return accessor.jujutsuAddon$getInfinityPressure() > 0;
                });

        for (LivingEntity owner : nearbyEntities) {
            InfinityFieldData fieldData = createFieldData(owner, player);
            if (fieldData != null) {
                fields.add(fieldData);
            }
        }

        return fields;
    }

    private static InfinityFieldData createFieldData(LivingEntity owner, ServerPlayer target) {
        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return null;
        if (!(data instanceof IInfinityPressureAccessor accessor)) return null;

        int pressureLevel = accessor.jujutsuAddon$getInfinityPressure();
        if (pressureLevel <= 0) return null;

        float cursedEnergyOutput = data.getAbilityPower();
        double maxRange = PressureCalculator.calculateRange(pressureLevel);

        // ★★★ 修复：使用统一计算器，不乘 cursedEnergyOutput ★★★
        double balanceRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);
        double distance = ownerCenter.distanceTo(targetCenter);

        if (distance > maxRange + 5) {
            return null;
        }

        return new InfinityFieldData(
                owner.getUUID(),
                owner.position(),
                owner.getBbHeight(),
                balanceRadius,
                maxRange,
                cursedEnergyOutput
        );
    }
}
