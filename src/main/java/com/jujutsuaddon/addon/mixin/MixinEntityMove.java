package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.PressureCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PressureBypassChecker;
import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import com.jujutsuaddon.addon.client.InfinityFieldClientCache;
import com.jujutsuaddon.addon.network.InfinityFieldData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.List;

@Mixin(Entity.class)
public abstract class MixinEntityMove {

    @ModifyVariable(
            method = "move",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Vec3 jujutsuAddon$modifyMovementForInfinity(Vec3 movement, MoverType moverType) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof LivingEntity living)) return movement;
        if (moverType != MoverType.SELF && moverType != MoverType.PLAYER) return movement;
        if (!PressureConfig.isEnabled()) return movement;

        if (self.level().isClientSide) {
            return jujutsuAddon$handleClient(living, movement);
        } else {
            return jujutsuAddon$handleServer(living, movement);
        }
    }

    @Unique
    private Vec3 jujutsuAddon$handleClient(LivingEntity living, Vec3 movement) {
        // ★★★ 新增：天逆鉾豁免 ★★★
        if (PressureBypassChecker.shouldBypassPressure(living)) {
            return movement;
        }
        List<InfinityFieldData> fields = InfinityFieldClientCache.getFields();
        if (fields.isEmpty()) return movement;

        Vec3 result = movement;
        for (InfinityFieldData field : fields) {
            if (field.ownerId.equals(living.getUUID())) continue;
            result = jujutsuAddon$modifyForField(living, field, result);
        }
        return result;
    }

    @Unique
    private Vec3 jujutsuAddon$handleServer(LivingEntity living, Vec3 movement) {
        // ★★★ 新增：天逆鉾豁免 ★★★
        if (PressureBypassChecker.shouldBypassPressure(living)) {
            return movement;
        }
        AABB searchArea = living.getBoundingBox().inflate(30);
        List<LivingEntity> owners = living.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e != living && JJKAbilities.hasToggled(e, JJKAbilities.INFINITY.get()));

        Vec3 result = movement;
        for (LivingEntity owner : owners) {
            ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
            if (data == null) continue;
            if (!(data instanceof IInfinityPressureAccessor accessor)) continue;

            int level = accessor.jujutsuAddon$getInfinityPressure();
            if (level <= 0) continue;

            double maxRange = PressureCalculator.calculateRange(level);
            double balanceRadius = BalancePointCalculator.getBalanceRadius(level, maxRange);

            InfinityFieldData field = new InfinityFieldData(
                    owner.getUUID(),
                    owner.position(),
                    owner.getBbHeight(),
                    balanceRadius,
                    maxRange,
                    1.0f
            );

            result = jujutsuAddon$modifyForField(living, field, result);
        }
        return result;
    }

    @Unique
    private Vec3 jujutsuAddon$modifyForField(LivingEntity target, InfinityFieldData field, Vec3 movement) {
        Vec3 targetPos = target.position();
        double distance = field.getDistanceTo(targetPos);

        if (distance > field.maxRange) return movement;
        if (movement.lengthSqr() < 0.0001) return movement;

        double zenoMultiplier = field.calculateZenoMultiplier(targetPos);
        if (zenoMultiplier < 0.05) return movement;

        double blockRatio = Math.min(zenoMultiplier, 1.0);

        // ==================== 方向计算 ====================
        Vec3 pushDir3D = field.getFullPushDirection(targetPos);
        Vec3 pushDir = field.getHorizontalPushDirection(targetPos);

        // ==================== 水平处理（完全保持原样） ====================
        Vec3 horizMove = new Vec3(movement.x, 0, movement.z);
        double newX = movement.x;
        double newZ = movement.z;

        if (horizMove.length() >= 0.001) {
            double radialComponent = horizMove.dot(pushDir);

            // 红圈边缘检测
            boolean nearBalance = distance < field.balanceRadius + 0.5 && distance >= field.balanceRadius;

            // 接近时处理
            if (radialComponent < 0) {
                Vec3 radialVec = pushDir.scale(radialComponent);
                Vec3 tangentVec = horizMove.subtract(radialVec);

                Vec3 newHoriz;
                if (nearBalance && blockRatio > 0.9) {
                    // 红圈边缘：整体阻止（防止滑动）
                    newHoriz = horizMove.scale(1.0 - blockRatio);
                } else {
                    // 减速区：只阻止接近分量，切向保留
                    Vec3 newRadial = radialVec.scale(1.0 - blockRatio);
                    newHoriz = newRadial.add(tangentVec);
                }

                newX = newHoriz.x;
                newZ = newHoriz.z;
            }
        }

        // ==================== Y轴处理（新增） ====================
        double newY = movement.y;

        // pushDir3D.y > 0 = 玩家在施术者上方
        // pushDir3D.y < 0 = 玩家在施术者下方
        if (pushDir3D.y > 0.3 && movement.y < -0.01) {
            // 玩家在上方且下落 = Y轴接近，阻止
            newY = movement.y * (1.0 - blockRatio);
        }
        if (pushDir3D.y < -0.3 && movement.y > 0.01) {
            // 玩家在下方且上升 = Y轴接近，阻止
            newY = movement.y * (1.0 - blockRatio);
        }

        return new Vec3(newX, newY, newZ);
    }
}
