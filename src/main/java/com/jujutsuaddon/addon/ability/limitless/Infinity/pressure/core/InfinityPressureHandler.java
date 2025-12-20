package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.*;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.block.BlockPressureManager;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.conflict.InfinityConflictResolver;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.effect.PressureEffectRenderer;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.entity.CollisionHandler;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.entity.PushForceApplier;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile.ProjectilePressureHandler;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PressureBypassChecker;
import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.entity.base.DomainExpansionEntity;
import radon.jujutsu_kaisen.entity.base.SummonEntity;
//import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.effect.PressureZoneDebugRenderer;

import java.util.*;

@Mod.EventBusSubscriber(modid = "jujutsu_addon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfinityPressureHandler {

    private static final Map<UUID, PressureStateManager> stateManagers = new HashMap<>();

    private static PressureStateManager getStateManager(UUID ownerId) {
        return stateManagers.computeIfAbsent(ownerId, id -> new PressureStateManager());
    }

    public static PressureStateManager getStateManagerFor(UUID ownerId) {
        return stateManagers.get(ownerId);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity owner = event.getEntity();
        if (!PressureConfig.isEnabled()) return;
        if (owner.level().isClientSide) return;
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
        int tick = owner.tickCount;
        // ★★★ 投射物每tick处理（关键功能，不降频）★★★
        ProjectilePressureHandler.handleProjectiles(owner, pressureLevel,
                cursedEnergyOutput, maxRange, stateManager);
        // ★★★ 实体处理：玩家每tick，非玩家每2tick ★★★
        boolean processEntities = (owner instanceof net.minecraft.world.entity.player.Player) || (tick % 2 == 0);

        Set<BlockPos> allActiveBlocks = new HashSet<>();

        if (processEntities) {
            AABB area = owner.getBoundingBox().inflate(maxRange);
            boolean ownerInEnemyDomain = isInEnemyDomainWithSureHit(owner);
            List<LivingEntity> entities = owner.level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> shouldAffectLiving(owner, entity, ownerInEnemyDomain));
            for (LivingEntity target : entities) {
                processTarget(owner, target, pressureLevel, cursedEnergyOutput,
                        maxRange, stateManager, allActiveBlocks);
            }
        }
        // ★★★ 掉落物每4tick处理 ★★★
        if (tick % 4 == 0 && PressureConfig.shouldPushDroppedItems() &&
                pressureLevel >= PressureConfig.getItemPushMinPressure()) {
            AABB area = owner.getBoundingBox().inflate(maxRange);
            List<ItemEntity> items = owner.level().getEntitiesOfClass(ItemEntity.class, area);
            for (ItemEntity item : items) {
                pushItem(owner, item, pressureLevel, cursedEnergyOutput, maxRange);
            }
        }
        // ★★★ 方块更新每2tick ★★★
        if (tick % 2 == 0) {
            BlockPressureManager.update(owner, allActiveBlocks, pressureLevel,
                    cursedEnergyOutput, stateManager);
        }
        // ★★★ 清理每40tick（从20改为40）★★★
        if (tick % 40 == 0) {
            stateManager.cleanup(owner, maxRange);
            if (owner.level() instanceof ServerLevel) {
                PressureEffectRenderer.cleanupCooldowns(owner.level().getGameTime());
            }
        }
    }

    private static boolean isInEnemyDomainWithSureHit(LivingEntity owner) {
        List<DomainExpansionEntity> domains = owner.level().getEntitiesOfClass(
                DomainExpansionEntity.class,
                owner.getBoundingBox().inflate(100)
        );

        for (DomainExpansionEntity domain : domains) {
            if (domain.getOwner() == owner) continue;
            if (!domain.hasSureHitEffect()) continue;
            if (!domain.checkSureHitEffect()) continue;
            if (domain.isAffected(owner)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTargetDomainOwnerWithSureHit(LivingEntity owner, LivingEntity target) {
        List<DomainExpansionEntity> domains = owner.level().getEntitiesOfClass(
                DomainExpansionEntity.class,
                owner.getBoundingBox().inflate(100)
        );

        for (DomainExpansionEntity domain : domains) {
            if (domain.getOwner() != target) continue;
            if (!domain.hasSureHitEffect()) continue;
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
        Vec3 itemPos = item.position();

        double distance = ownerCenter.distanceTo(itemPos);
        if (distance > maxRange) return;
        if (distance < 0.1) distance = 0.1;

        Vec3 pushDirection = itemPos.subtract(ownerCenter).normalize();

        double levelFactor = PressureCalculator.calculateLevelFactor(pressureLevel);
        double normalizedDistance = distance / maxRange;
        double distanceFactor = Math.pow(1.0 - normalizedDistance, 1.5);

        double baseForce = PressureConfig.getBasePushForce() * levelFactor * cursedEnergyOutput;
        double itemForce = baseForce * distanceFactor * PressureConfig.getItemPushForceMultiplier();
        itemForce = Math.min(itemForce, PressureConfig.getMaxPushForce() * 1.5);

        Vec3 currentVel = item.getDeltaMovement();
        double newVelX = currentVel.x + pushDirection.x * itemForce;
        double newVelY = currentVel.y + pushDirection.y * itemForce * 0.5 + 0.02;
        double newVelZ = currentVel.z + pushDirection.z * itemForce;

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
        InfinityConflictResolver.ConflictResult conflict =
                InfinityConflictResolver.resolveConflict(owner, target);
        if (!conflict.canAffect) {
            stateManager.clearEntityState(target.getUUID());
            return;
        }

        float effectiveCursedEnergy = (float) (cursedEnergyOutput * conflict.effectiveMultiplier);

        PushForceApplier.CollisionInfo collisionInfo = PushForceApplier.apply(
                owner, target, pressureLevel, effectiveCursedEnergy, maxRange, stateManager);

        // ========== 调试：渲染目标状态 ==========
        //PressureZoneDebugRenderer.renderTargetStatus(owner, target, pressureLevel, cursedEnergyOutput, maxRange);

        // ★★★ 新增：接近平衡点时的波纹效果 ★★★
        if (owner.level() instanceof ServerLevel serverLevel) {
            double balanceRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);
            double distance = collisionInfo.distance;

            // 渲染接近波纹（目标刚进入边界区域时）
            PressureEffectRenderer.renderApproachRipple(serverLevel, owner, target, balanceRadius, distance);

            // 渲染边界波动（有目标在边界附近时的持续效果）
            if (distance < balanceRadius + 2.0 && distance > balanceRadius - 0.5) {
                PressureEffectRenderer.renderBoundaryFluctuation(serverLevel, owner, balanceRadius, owner.tickCount);
            }
        }

        double currentPressure = collisionInfo.pressureValue;

        Set<BlockPos> collidedBlocks = CollisionHandler.getCollidingBlocks(
                owner, target, collisionInfo.forceDirection);

        BlockPos ownerFeetPos = owner.blockPosition();
        BlockPos ownerBelowPos = ownerFeetPos.below();

        Set<BlockPos> breakableBlocks = CollisionHandler.getBreakableBlocks(
                collidedBlocks, target, ownerFeetPos, ownerBelowPos);

        boolean rawColliding = target.horizontalCollision || !collidedBlocks.isEmpty();

        PressureStateManager.CollisionStateChange stateChange =
                stateManager.updateCollisionState(target.getUUID(), rawColliding);

        if (stateChange == PressureStateManager.CollisionStateChange.STILL_COLLIDING ||
                stateChange == PressureStateManager.CollisionStateChange.JUST_COLLIDED) {
            stateManager.updatePeakPressure(target.getUUID(), currentPressure);
        }

        double prevPressure = stateManager.getPreviousPressure(target.getUUID());

        ServerLevel serverLevel = (ServerLevel) owner.level();

        switch (stateChange) {
            case JUST_COLLIDED:
                stateManager.resetPeakPressure(target.getUUID());
                stateManager.updatePeakPressure(target.getUUID(), currentPressure);
                // fall through
            case STILL_COLLIDING:
                allActiveBlocks.addAll(breakableBlocks);
                stateManager.setCollidingBlocks(target.getUUID(), collidedBlocks);
                for (BlockPos pos : breakableBlocks) {
                    BlockPressureManager.applyPressure(pos, (float) currentPressure, stateManager, serverLevel);
                }
                CollisionHandler.handleDamage(owner, target,
                        currentPressure, prevPressure,
                        true, collidedBlocks, stateManager);
                break;

            case JUST_RELEASED:
                double peakPressure = stateManager.getPeakPressure(target.getUUID());
                if (peakPressure >= PressureConfig.getMinPressureForDamage()) {
                    CollisionHandler.handleBreakthroughDamage(owner, target,
                            peakPressure, stateManager);
                }
                stateManager.resetPeakPressure(target.getUUID());
                stateManager.setCollidingBlocks(target.getUUID(), Collections.emptySet());
                stateManager.setDamageWarningTicks(target.getUUID(), 0);
                stateManager.resetPinnedTicks(target.getUUID());
                break;

            case NOT_COLLIDING:
                stateManager.setCollidingBlocks(target.getUUID(), Collections.emptySet());
                stateManager.setDamageWarningTicks(target.getUUID(), 0);
                CollisionHandler.handleDamage(owner, target,
                        currentPressure, prevPressure,
                        false, Collections.emptySet(), stateManager);
                break;
        }

        stateManager.setPreviousPressure(target.getUUID(), currentPressure);

        if (PressureConfig.areSoundsEnabled() && target.tickCount % 30 == 0 && currentPressure > 3.0D) {
            float volume = Math.min(0.15F + (float) (currentPressure * 0.02), 0.5F);
            owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                    volume, 1.1F);
        }
    }

    private static boolean shouldAffectLiving(LivingEntity owner, LivingEntity target,
                                              boolean ownerInEnemyDomain) {
        if (target == owner) return false;
        if (target.isDeadOrDying()) return false;
        if (target.isPassengerOfSameVehicle(owner)) return false;
        if (PressureBypassChecker.shouldBypassPressure(target)) {
            return false;
        }

        if (target instanceof TamableAnimal tamable &&
                tamable.isTame() && tamable.getOwner() == owner) {
            return false;
        }
        if (target instanceof SummonEntity summon && summon.getOwner() == owner) {
            return false;
        }

        if (ownerInEnemyDomain && isTargetDomainOwnerWithSureHit(owner, target)) {
            return false;
        }

        ISorcererData targetData = target.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (targetData != null && targetData.hasToggled(JJKAbilities.DOMAIN_AMPLIFICATION.get())) {
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        InfinityFieldSyncManager.onServerTick(serverLevel);
    }
}
