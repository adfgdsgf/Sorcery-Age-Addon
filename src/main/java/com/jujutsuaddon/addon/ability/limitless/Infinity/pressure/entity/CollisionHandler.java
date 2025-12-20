package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.entity;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureCurve;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.damage.PressureDamageCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.damage.PressureDamageConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.effect.PressureEffectRenderer;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class CollisionHandler {

    private static final int VERTICAL_EXPAND_RANGE = 2;

    // ==================== 碰撞检测 ====================

    /**
     * ★★★ 修复：只检测推力方向上的碰撞，忽略地面 ★★★
     */
    public static boolean isCollidingInForceDirection(LivingEntity target, Vec3 forceDirection) {
        // ★★★ 不再使用 horizontalCollision/verticalCollision ★★★
        // 因为站在地上也会 verticalCollision = true

        // 只有当推力主要是水平方向时，才检查水平碰撞
        boolean isHorizontalForce = Math.abs(forceDirection.x) > 0.3 || Math.abs(forceDirection.z) > 0.3;

        // 如果是水平推力，检查水平方向是否有障碍物
        if (isHorizontalForce) {
            // 使用 Minecraft 的碰撞标志，但只看水平
            if (target.horizontalCollision) {
                return true;
            }

            // 手动检查推力方向的方块
            return hasBlockInForceDirection(target, forceDirection);
        }

        // 垂直推力：检查上方/下方
        if (forceDirection.y > 0.3) {
            // 向上推，检查头顶
            return hasBlockAbove(target);
        } else if (forceDirection.y < -0.3) {
            // 向下推，检查脚下（通常就是地面，这种情况很少）
            return target.onGround();
        }

        return false;
    }

    /**
     * 检查推力方向是否有方块
     */
    private static boolean hasBlockInForceDirection(LivingEntity target, Vec3 forceDirection) {
        AABB box = target.getBoundingBox();

        // 向推力方向偏移一小段距离检查
        double checkDist = 0.1;
        double checkX = target.getX() + forceDirection.x * checkDist;
        double checkZ = target.getZ() + forceDirection.z * checkDist;

        // 检查身体高度范围内的方块
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.ceil(box.maxY) - 1;

        for (int y = minY; y <= maxY; y++) {
            BlockPos checkPos;

            // 根据推力方向决定检查哪边
            if (forceDirection.x > 0.3) {
                checkPos = new BlockPos((int) Math.floor(box.maxX + 0.1), y, (int) Math.floor(target.getZ()));
            } else if (forceDirection.x < -0.3) {
                checkPos = new BlockPos((int) Math.floor(box.minX - 0.1), y, (int) Math.floor(target.getZ()));
            } else if (forceDirection.z > 0.3) {
                checkPos = new BlockPos((int) Math.floor(target.getX()), y, (int) Math.floor(box.maxZ + 0.1));
            } else if (forceDirection.z < -0.3) {
                checkPos = new BlockPos((int) Math.floor(target.getX()), y, (int) Math.floor(box.minZ - 0.1));
            } else {
                continue;
            }

            BlockState state = target.level().getBlockState(checkPos);
            if (!state.isAir() && state.isSolid()) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasBlockAbove(LivingEntity target) {
        AABB box = target.getBoundingBox();
        int checkY = (int) Math.ceil(box.maxY);
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockState state = target.level().getBlockState(new BlockPos(x, checkY, z));
                if (!state.isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ... 其余方法保持不变 ...

    public static Set<BlockPos> getCollidingBlocks(LivingEntity owner, LivingEntity target,
                                                   Vec3 forceDirection) {
        Set<BlockPos> blocks = new HashSet<>();

        AABB targetBox = target.getBoundingBox();

        BlockPos ownerFeetPos = owner.blockPosition();
        BlockPos ownerBelowPos = ownerFeetPos.below();

        int minX = (int) Math.floor(targetBox.minX + 0.001);
        int maxX = (int) Math.floor(targetBox.maxX - 0.001);
        int minY = (int) Math.floor(targetBox.minY + 0.001);
        int maxY = (int) Math.ceil(targetBox.maxY - 0.001) - 1;
        int minZ = (int) Math.floor(targetBox.minZ + 0.001);
        int maxZ = (int) Math.floor(targetBox.maxZ - 0.001);

        boolean isVerticalForce = Math.abs(forceDirection.y) > 0.5;
        int expandRange = isVerticalForce ? VERTICAL_EXPAND_RANGE : 0;

        if (forceDirection.x > 0.1) {
            checkXDirection(target, blocks, maxX + 1, minY, maxY, minZ, maxZ, ownerFeetPos, ownerBelowPos);
        } else if (forceDirection.x < -0.1) {
            checkXDirection(target, blocks, minX - 1, minY, maxY, minZ, maxZ, ownerFeetPos, ownerBelowPos);
        }

        if (forceDirection.z > 0.1) {
            checkZDirection(target, blocks, maxZ + 1, minY, maxY, minX, maxX, ownerFeetPos, ownerBelowPos);
        } else if (forceDirection.z < -0.1) {
            checkZDirection(target, blocks, minZ - 1, minY, maxY, minX, maxX, ownerFeetPos, ownerBelowPos);
        }

        if (forceDirection.y > 0.3) {
            int checkY = maxY + 1;
            for (int x = minX - expandRange; x <= maxX + expandRange; x++) {
                for (int z = minZ - expandRange; z <= maxZ + expandRange; z++) {
                    BlockPos pos = new BlockPos(x, checkY, z);
                    if (isCollidableBlock(target, pos, ownerFeetPos, ownerBelowPos)) {
                        blocks.add(pos);
                    }
                }
            }
        } else if (forceDirection.y < -0.3) {
            int checkY = minY - 1;
            for (int x = minX - expandRange; x <= maxX + expandRange; x++) {
                for (int z = minZ - expandRange; z <= maxZ + expandRange; z++) {
                    BlockPos pos = new BlockPos(x, checkY, z);
                    if (isCollidableBlock(target, pos, ownerFeetPos, ownerBelowPos)) {
                        blocks.add(pos);
                    }
                }
            }
        }

        if (Math.abs(forceDirection.x) > 0.3 && Math.abs(forceDirection.z) > 0.3) {
            int checkX = forceDirection.x > 0 ? maxX + 1 : minX - 1;
            int checkZ = forceDirection.z > 0 ? maxZ + 1 : minZ - 1;
            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(checkX, y, checkZ);
                if (isCollidableBlock(target, pos, ownerFeetPos, ownerBelowPos)) {
                    blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    private static void checkXDirection(LivingEntity target, Set<BlockPos> blocks,
                                        int checkX, int minY, int maxY, int minZ, int maxZ,
                                        BlockPos ownerFeetPos, BlockPos ownerBelowPos) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(checkX, y, z);
                if (isCollidableBlock(target, pos, ownerFeetPos, ownerBelowPos)) {
                    blocks.add(pos);
                }
            }
        }
    }

    private static void checkZDirection(LivingEntity target, Set<BlockPos> blocks,
                                        int checkZ, int minY, int maxY, int minX, int maxX,
                                        BlockPos ownerFeetPos, BlockPos ownerBelowPos) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                BlockPos pos = new BlockPos(x, y, checkZ);
                if (isCollidableBlock(target, pos, ownerFeetPos, ownerBelowPos)) {
                    blocks.add(pos);
                }
            }
        }
    }

    private static boolean isCollidableBlock(LivingEntity target, BlockPos pos,
                                             BlockPos ownerFeetPos, BlockPos ownerBelowPos) {
        if (pos.equals(ownerFeetPos) || pos.equals(ownerBelowPos)) {
            return false;
        }
        BlockState state = target.level().getBlockState(pos);
        return !state.isAir() && state.isSolid();
    }

    public static Set<BlockPos> getBreakableBlocks(Set<BlockPos> collidingBlocks,
                                                   LivingEntity target,
                                                   BlockPos ownerFeetPos,
                                                   BlockPos ownerBelowPos) {
        Set<BlockPos> breakable = new HashSet<>();

        for (BlockPos pos : collidingBlocks) {
            if (pos.equals(ownerFeetPos) || pos.equals(ownerBelowPos)) {
                continue;
            }

            BlockState state = target.level().getBlockState(pos);
            if (state.isAir()) continue;

            float hardness = state.getDestroySpeed(target.level(), pos);
            if (hardness >= 0) {
                breakable.add(pos);
            }
        }

        return breakable;
    }

    public static double calculateHardnessBonus(Set<BlockPos> collidingBlocks, LivingEntity target) {
        if (collidingBlocks.isEmpty()) return 1.0;

        double maxHardness = 0;
        boolean hasUnbreakable = false;

        for (BlockPos pos : collidingBlocks) {
            BlockState state = target.level().getBlockState(pos);
            float hardness = state.getDestroySpeed(target.level(), pos);

            if (hardness < 0) {
                hasUnbreakable = true;
            } else {
                maxHardness = Math.max(maxHardness, hardness);
            }
        }

        if (hasUnbreakable) {
            return 2.5;
        }

        return 1.0 + Math.min(maxHardness / 50.0, 1.0);
    }

    // ==================== 伤害系统（保持不变）====================


    /**
     * ★★★ 使用统一伤害系统计算压力伤害 ★★★
     */
    private static float calculateDamage(
            LivingEntity owner,
            LivingEntity target,
            double currentPressure,
            double previousPressure,
            int pinnedTicks,
            int blockCount,
            double hardnessBonus) {
        // 压力变化加成（突然增压造成额外伤害）
        double pressureChange = currentPressure - previousPressure;
        double surgeBonus = 1.0;
        if (pressureChange > 0.5) {
            // 压力突增时，增加伤害
            surgeBonus = 1.0 + Math.min(pressureChange * 0.1, 0.5);
        }
        // 应用突增加成到压力值
        double effectivePressure = currentPressure * surgeBonus;
        // ★★★ 调用统一伤害系统 ★★★
        return PressureDamageCalculator.calculate(
                owner,
                target,
                effectivePressure,
                pinnedTicks,
                blockCount,
                hardnessBonus
        );
    }

    public static void handleDamage(LivingEntity owner, LivingEntity target,
                                    double currentPressure, double previousPressure,
                                    boolean isColliding,
                                    Set<BlockPos> collidingBlocks,
                                    PressureStateManager stateManager) {

        if (target.isDeadOrDying()) return;

        if (!isColliding || collidingBlocks.isEmpty()) {
            stateManager.setDamageCooldown(target.getUUID(), 0);
            stateManager.setDamageWarningTicks(target.getUUID(), 0);
            return;
        }

        if (currentPressure < PressureConfig.getMinPressureForDamage()) {
            stateManager.setDamageWarningTicks(target.getUUID(), 0);
            return;
        }

        double pressureChange = currentPressure - previousPressure;
        boolean isPressureSurge = pressureChange >= PressureConfig.getPressureSurgeThreshold();

        if (isPressureSurge) {
            handleSurgeDamage(owner, target, currentPressure, pressureChange,
                    collidingBlocks, stateManager);
            return;
        }

        int cooldown = stateManager.getDamageCooldown(target.getUUID());

        if (cooldown > 0) {
            stateManager.decrementDamageCooldown(target.getUUID());

            if (cooldown <= 4 && owner.level() instanceof ServerLevel level) {
                PressureEffectRenderer.renderDamageWarning(level, target, currentPressure, cooldown);
            }
            return;
        }

        int warningTicks = stateManager.getDamageWarningTicks(target.getUUID());

        if (warningTicks < PressureConfig.getDamageWarningTicks()) {
            stateManager.incrementDamageWarningTicks(target.getUUID());

            if (owner.level() instanceof ServerLevel level) {
                int remaining = PressureConfig.getDamageWarningTicks() - warningTicks;
                PressureEffectRenderer.renderDamageWarning(level, target, currentPressure, remaining);
            }
            return;
        }

        applyNormalDamage(owner, target, currentPressure, previousPressure,
                collidingBlocks, stateManager);
    }

    public static void handleBreakthroughDamage(LivingEntity owner, LivingEntity target,
                                                double pressure,
                                                PressureStateManager stateManager) {
        if (target.isDeadOrDying()) return;

        float damage = PressureDamageCalculator.calculateSimple(owner, pressure * 0.5);
        damage = Math.min(damage, (float)(PressureDamageConfig.getMaxDamagePerHit() * 0.5));

        if (damage > 0.3F) {
            Vec3 velocityBefore = target.getDeltaMovement();

            DamageSource damageSource = owner.level().damageSources().mobAttack(owner);
            target.hurt(damageSource, damage);

            target.setDeltaMovement(velocityBefore.x, Math.min(velocityBefore.y, 0.1), velocityBefore.z);
            target.hurtMarked = true;

            stateManager.setDamageCooldown(target.getUUID(), 5);

            if (PressureConfig.areParticlesEnabled() && owner.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        1, 0, 0, 0, 0);
            }

            if (PressureConfig.areSoundsEnabled()) {
                owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4F, 1.2F);
            }
        }
    }

    private static void handleSurgeDamage(LivingEntity owner, LivingEntity target,
                                          double currentPressure, double pressureChange,
                                          Set<BlockPos> collidingBlocks,
                                          PressureStateManager stateManager) {

        stateManager.setDamageWarningTicks(target.getUUID(), 0);

        int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());
        int blockCount = collidingBlocks.size();
        double hardnessBonus = calculateHardnessBonus(collidingBlocks, target);

        float baseDamage = calculateDamage(owner, target, currentPressure, 0,
                pinnedTicks, blockCount, hardnessBonus);
        float surgeDamage = baseDamage * 1.5f;  // 固定1.5倍突增
        surgeDamage = Math.min(surgeDamage, (float)(PressureDamageConfig.getMaxDamagePerHit() * 1.5));

        if (surgeDamage > 0.5F) {
            Vec3 velocityBefore = target.getDeltaMovement();

            DamageSource damageSource = owner.level().damageSources().mobAttack(owner);
            target.hurt(damageSource, surgeDamage);

            target.setDeltaMovement(velocityBefore.x, Math.min(velocityBefore.y, 0.05), velocityBefore.z);
            target.hurtMarked = true;

            int dynamicInterval = PressureCurve.calculateDamageInterval(currentPressure);
            stateManager.setDamageCooldown(target.getUUID(), dynamicInterval);

            if (owner.level() instanceof ServerLevel level) {
                PressureEffectRenderer.renderPressureSurge(level, target, pressureChange);
            }
        }
    }

    private static void applyNormalDamage(LivingEntity owner, LivingEntity target,
                                          double currentPressure, double previousPressure,
                                          Set<BlockPos> collidingBlocks,
                                          PressureStateManager stateManager) {

        stateManager.setDamageWarningTicks(target.getUUID(), 0);

        int dynamicInterval = PressureCurve.calculateDamageInterval(currentPressure);

        int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());
        int blockCount = collidingBlocks.size();
        double hardnessBonus = calculateHardnessBonus(collidingBlocks, target);

        float totalDamage = calculateDamage(owner, target, currentPressure, previousPressure,
                pinnedTicks, blockCount, hardnessBonus);

        if (totalDamage > 0.3F) {
            Vec3 velocityBefore = target.getDeltaMovement();

            DamageSource damageSource = owner.level().damageSources().mobAttack(owner);
            target.hurt(damageSource, totalDamage);

            target.setDeltaMovement(velocityBefore.x, Math.min(velocityBefore.y, 0.05), velocityBefore.z);
            target.hurtMarked = true;

            stateManager.setDamageCooldown(target.getUUID(), dynamicInterval);

            playDamageEffects(owner, target, totalDamage, currentPressure, hardnessBonus);
        }
    }

    private static void playDamageEffects(LivingEntity owner, LivingEntity target,
                                          float damage, double pressure, double hardnessBonus) {

        if (PressureConfig.areSoundsEnabled()) {
            float volume = 0.2F + Math.min((float)(pressure * 0.05), 0.5F);
            float pitch = 0.8F - Math.min((float)(pressure * 0.03), 0.3F);

            if (hardnessBonus >= 2.0) {
                owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, volume * 0.7F, 0.4F);
            } else if (pressure > 6.0) {
                owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, volume * 0.5F, pitch);
            } else if (pressure > 3.0) {
                owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, volume, pitch + 0.2F);
            } else {
                owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, volume * 0.8F, pitch + 0.3F);
            }
        }

        if (PressureConfig.areParticlesEnabled() && owner.level() instanceof ServerLevel serverLevel) {
            int particleCount = 1 + (int)(pressure / 2);

            if (hardnessBonus >= 2.0) {
                particleCount *= 2;
            }

            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    particleCount, 0.2, 0.2, 0.2, 0.02);

            if (pressure > 5.0) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        (int)(pressure / 3), 0.3, 0.1, 0.3, 0.01);
            }
        }
    }
}
