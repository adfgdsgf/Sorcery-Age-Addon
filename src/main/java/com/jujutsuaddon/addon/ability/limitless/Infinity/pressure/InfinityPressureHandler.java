package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.entity.base.DomainExpansionEntity;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

import java.util.*;

@Mod.EventBusSubscriber(modid = "jujutsu_addon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfinityPressureHandler {

    private static final Map<UUID, PressureStateManager> stateManagers = new HashMap<>();

    private static PressureStateManager getStateManager(UUID ownerId) {
        return stateManagers.computeIfAbsent(ownerId, id -> new PressureStateManager());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity owner = event.getEntity();
        if (!PressureConfig.isEnabled()) return;
        if (owner.level().isClientSide) return;

        // ★★★ 修复：无下限关闭时也要清理投射物 ★★★
        if (!JJKAbilities.hasToggled(owner, JJKAbilities.INFINITY.get())) {
            PressureStateManager stateManager = stateManagers.get(owner.getUUID());
            if (stateManager != null && stateManager.getTrackedProjectileCount() > 0) {
                stateManager.releaseAllProjectiles(owner);
            }
            return;
        }
        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return;
        if (!(data instanceof IInfinityPressureAccessor accessor)) return;
        int pressureLevel = accessor.jujutsuAddon$getInfinityPressure();
        if (pressureLevel <= 0) {
            PressureStateManager stateManager = stateManagers.get(owner.getUUID());
            if (stateManager != null) {
                stateManager.clearAll(owner);
            }
            return;
        }

        PressureStateManager stateManager = getStateManager(owner.getUUID());
        float cursedEnergyOutput = data.getAbilityPower();
        double maxRange = PressureCalculator.calculateRange(pressureLevel);

        AABB area = owner.getBoundingBox().inflate(maxRange);

        // ★ 检查owner是否处于敌方领域的必中效果下 ★
        boolean ownerInEnemyDomain = isInEnemyDomainWithSureHit(owner);

        // 处理活着的生物
        List<LivingEntity> entities = owner.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> shouldAffectLiving(owner, entity, ownerInEnemyDomain));

        Set<BlockPos> allActiveBlocks = new HashSet<>();

        for (LivingEntity target : entities) {
            processTarget(owner, target, pressureLevel, cursedEnergyOutput,
                    maxRange, stateManager, allActiveBlocks);
        }

        // 处理掉落物
        if (PressureConfig.shouldPushDroppedItems() &&
                pressureLevel >= PressureConfig.getItemPushMinPressure()) {

            List<ItemEntity> items = owner.level().getEntitiesOfClass(ItemEntity.class, area);
            for (ItemEntity item : items) {
                pushItem(owner, item, pressureLevel, cursedEnergyOutput, maxRange);
            }
        }

        // ★ 新增：处理投射物 ★
        ProjectilePressureHandler.handleProjectiles(owner, pressureLevel,
                cursedEnergyOutput, maxRange, stateManager);

        BlockPressureManager.update(owner, allActiveBlocks, pressureLevel,
                cursedEnergyOutput, stateManager);

        if (owner.tickCount % 20 == 0) {
            stateManager.cleanup(owner, maxRange);
        }
    }

    // ★ 新增：检查owner是否在敌方领域的必中范围内 ★
    private static boolean isInEnemyDomainWithSureHit(LivingEntity owner) {
        // 搜索附近的领域
        List<DomainExpansionEntity> domains = owner.level().getEntitiesOfClass(
                DomainExpansionEntity.class,
                owner.getBoundingBox().inflate(100) // 领域可能很大
        );

        for (DomainExpansionEntity domain : domains) {
            // 排除自己的领域
            if (domain.getOwner() == owner) continue;

            // 检查领域是否有必中效果
            if (!domain.hasSureHitEffect()) continue;

            // 检查必中效果是否生效（简易领域等可以抵消）
            if (!domain.checkSureHitEffect()) continue;

            // 检查owner是否在领域范围内
            if (domain.isAffected(owner)) {
                return true;
            }
        }
        return false;
    }

    // ★ 新增：检查target是否是对owner有必中效果的领域的所有者 ★
    private static boolean isTargetDomainOwnerWithSureHit(LivingEntity owner, LivingEntity target) {
        List<DomainExpansionEntity> domains = owner.level().getEntitiesOfClass(
                DomainExpansionEntity.class,
                owner.getBoundingBox().inflate(100)
        );

        for (DomainExpansionEntity domain : domains) {
            // 检查这个领域是否属于target
            if (domain.getOwner() != target) continue;

            // 检查领域是否有必中效果
            if (!domain.hasSureHitEffect()) continue;

            // 检查owner是否在这个领域内
            if (domain.isAffected(owner)) {
                return true;
            }
        }
        return false;
    }

    private static void pushItem(LivingEntity owner, ItemEntity item,
                                 int pressureLevel, float cursedEnergyOutput,
                                 double maxRange) {
        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        // ★★★ 使用通用速度控制器 ★★★
        VelocityController.VelocityResult result = VelocityController.processEntityVelocity(
                item, ownerCenter, pressureLevel, cursedEnergyOutput, maxRange);
        if (result.distance > maxRange) return;
        Vec3 direction = result.directionFromOwner;
        double distance = result.distance;
        // 计算推力
        double levelFactor = PressureCalculator.calculateLevelFactor(pressureLevel);
        double distanceFactor = 1.0 - (distance / maxRange);
        distanceFactor = Math.max(0, distanceFactor);
        double baseForce = PressureConfig.getBasePushForce() * levelFactor * cursedEnergyOutput;
        double itemForce = baseForce * distanceFactor * PressureConfig.getItemPushForceMultiplier();
        itemForce = Math.min(itemForce, PressureConfig.getMaxPushForce() * 1.5);
        // 从限制后的速度开始
        Vec3 currentVel = result.processedVelocity;
        double newVelX = currentVel.x + direction.x * itemForce;
        double newVelY = currentVel.y + direction.y * itemForce * 0.3 + 0.02;
        double newVelZ = currentVel.z + direction.z * itemForce;
        newVelX = Math.max(-2.0, Math.min(2.0, newVelX));
        newVelY = Math.max(-1.0, Math.min(1.5, newVelY));
        newVelZ = Math.max(-2.0, Math.min(2.0, newVelZ));
        item.setDeltaMovement(newVelX, newVelY, newVelZ);
        item.hurtMarked = true;
    }

    private static void processTarget(LivingEntity owner, LivingEntity target,
                                      int pressureLevel, float cursedEnergyOutput,
                                      double maxRange,
                                      PressureStateManager stateManager,
                                      Set<BlockPos> allActiveBlocks) {

        double previousPressure = stateManager.getPreviousPressure(target.getUUID());

        PushForceApplier.CollisionInfo collisionInfo = PushForceApplier.apply(
                owner, target, pressureLevel, cursedEnergyOutput, maxRange, stateManager);

        double currentPressure = collisionInfo.pressureValue;

        PressureEffectRenderer.renderPressureEffect(owner, target,
                currentPressure, collisionInfo.distanceFromHalt,
                collisionInfo.isBreaching, stateManager);

        if (collisionInfo.isColliding) {
            Set<BlockPos> collidedBlocks = CollisionHandler.getCollidingBlocks(
                    owner, target, collisionInfo.forceDirection);

            BlockPos ownerFeetPos = owner.blockPosition();
            BlockPos ownerBelowPos = ownerFeetPos.below();
            Set<BlockPos> breakableBlocks = CollisionHandler.getBreakableBlocks(
                    collidedBlocks, target, ownerFeetPos, ownerBelowPos);

            allActiveBlocks.addAll(breakableBlocks);
            stateManager.setCollidingBlocks(target.getUUID(), collidedBlocks);

            for (BlockPos pos : breakableBlocks) {
                BlockPressureManager.applyPressure(pos, (float) currentPressure, stateManager);
            }

            CollisionHandler.handleDamage(owner, target,
                    currentPressure, previousPressure,
                    true, collidedBlocks, stateManager);

        } else {
            stateManager.setCollidingBlocks(target.getUUID(), Collections.emptySet());
            stateManager.setDamageWarningTicks(target.getUUID(), 0);

            CollisionHandler.handleDamage(owner, target,
                    currentPressure, previousPressure,
                    false, Collections.emptySet(), stateManager);
        }

        stateManager.setPreviousPressure(target.getUUID(), currentPressure);

        if (PressureConfig.areSoundsEnabled() && target.tickCount % 30 == 0 && currentPressure > 3.0D) {
            float volume = Math.min(0.15F + (float) (currentPressure * 0.02), 0.5F);
            owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                    volume, 1.1F);
        }
    }

    // ★ 修改：添加领域必中检查 ★
    private static boolean shouldAffectLiving(LivingEntity owner, LivingEntity target,
                                              boolean ownerInEnemyDomain) {
        if (target == owner) return false;
        if (target.isDeadOrDying()) return false;
        if (target.isPassengerOfSameVehicle(owner)) return false;

        if (target instanceof TamableAnimal tamable &&
                tamable.isTame() && tamable.getOwner() == owner) {
            return false;
        }

        if (target instanceof SummonEntity summon && summon.getOwner() == owner) {
            return false;
        }

        // ★ 领域必中：如果target是对owner展开领域的人，不推开他 ★
        if (ownerInEnemyDomain && isTargetDomainOwnerWithSureHit(owner, target)) {
            return false;
        }

        // ★ 检查target是否开启了领域增幅（Domain Amplification） ★
        ISorcererData targetData = target.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (targetData != null && targetData.hasToggled(JJKAbilities.DOMAIN_AMPLIFICATION.get())) {
            return false; // 领域增幅可以穿透无下限
        }

        return true;
    }
}
