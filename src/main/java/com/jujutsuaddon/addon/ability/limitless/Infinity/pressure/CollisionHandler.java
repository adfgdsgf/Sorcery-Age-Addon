package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

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

    public static Set<BlockPos> getCollidingBlocks(LivingEntity owner, LivingEntity target,
                                                   Vec3 forceDirection) {
        Set<BlockPos> blocks = new HashSet<>();

        AABB targetBox = target.getBoundingBox();

        BlockPos ownerFeetPos = owner.blockPosition();
        BlockPos ownerBelowPos = ownerFeetPos.below();

        int minX = (int) Math.floor(targetBox.minX);
        int maxX = (int) Math.floor(targetBox.maxX);
        int minY = (int) Math.floor(targetBox.minY);
        int maxY = (int) Math.ceil(targetBox.maxY) - 1;
        int minZ = (int) Math.floor(targetBox.minZ);
        int maxZ = (int) Math.floor(targetBox.maxZ);

        boolean isVerticalForce = Math.abs(forceDirection.y) > 0.5;
        int expandRange = isVerticalForce ? VERTICAL_EXPAND_RANGE : 0;

        if (Math.abs(forceDirection.x) > 0.1) {
            int checkX = forceDirection.x > 0 ? maxX + 1 : minX - 1;
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(checkX, y, z);
                    if (isCollidableBlock(target, pos, ownerFeetPos, ownerBelowPos)) {
                        blocks.add(pos);
                    }
                }
            }
        }

        if (Math.abs(forceDirection.z) > 0.1) {
            int checkZ = forceDirection.z > 0 ? maxZ + 1 : minZ - 1;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, checkZ);
                    if (isCollidableBlock(target, pos, ownerFeetPos, ownerBelowPos)) {
                        blocks.add(pos);
                    }
                }
            }
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

    private static boolean isCollidableBlock(LivingEntity target, BlockPos pos,
                                             BlockPos ownerFeetPos, BlockPos ownerBelowPos) {
        if (pos.equals(ownerFeetPos) || pos.equals(ownerBelowPos)) {
            return false;
        }
        BlockState state = target.level().getBlockState(pos);
        return !state.isAir();
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

    public static boolean isCollidingInForceDirection(LivingEntity target, Vec3 forceDirection) {
        if (target.horizontalCollision || target.verticalCollision) {
            return true;
        }

        AABB targetBox = target.getBoundingBox();

        int minX = (int) Math.floor(targetBox.minX);
        int maxX = (int) Math.floor(targetBox.maxX);
        int minY = (int) Math.floor(targetBox.minY);
        int maxY = (int) Math.ceil(targetBox.maxY) - 1;
        int minZ = (int) Math.floor(targetBox.minZ);
        int maxZ = (int) Math.floor(targetBox.maxZ);

        boolean isVerticalForce = Math.abs(forceDirection.y) > 0.5;
        int expandRange = isVerticalForce ? VERTICAL_EXPAND_RANGE : 0;

        if (Math.abs(forceDirection.x) > 0.1) {
            int checkX = forceDirection.x > 0 ? maxX + 1 : minX - 1;
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = target.level().getBlockState(new BlockPos(checkX, y, z));
                    if (!state.isAir()) {
                        return true;
                    }
                }
            }
        }

        if (Math.abs(forceDirection.z) > 0.1) {
            int checkZ = forceDirection.z > 0 ? maxZ + 1 : minZ - 1;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockState state = target.level().getBlockState(new BlockPos(x, y, checkZ));
                    if (!state.isAir()) {
                        return true;
                    }
                }
            }
        }

        if (forceDirection.y > 0.3) {
            int checkY = maxY + 1;
            for (int x = minX - expandRange; x <= maxX + expandRange; x++) {
                for (int z = minZ - expandRange; z <= maxZ + expandRange; z++) {
                    BlockState state = target.level().getBlockState(new BlockPos(x, checkY, z));
                    if (!state.isAir()) {
                        return true;
                    }
                }
            }
        } else if (forceDirection.y < -0.3) {
            int checkY = minY - 1;
            for (int x = minX - expandRange; x <= maxX + expandRange; x++) {
                for (int z = minZ - expandRange; z <= maxZ + expandRange; z++) {
                    BlockState state = target.level().getBlockState(new BlockPos(x, checkY, z));
                    if (!state.isAir()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // ==================== 伤害系统 ====================

    private static int calculateDamageInterval(double pressure) {
        int interval = (int) (PressureConfig.getMaxDamageInterval() -
                pressure * PressureConfig.getIntervalPressureScale());

        return Math.max(PressureConfig.getMinDamageInterval(),
                Math.min(PressureConfig.getMaxDamageInterval(), interval));
    }

    private static float calculateDamage(double currentPressure, double previousPressure,
                                         int pinnedTicks, int blockCount, double hardnessBonus) {

        double baseDamage = currentPressure * PressureConfig.getPressureToDamage();

        double pressureChange = currentPressure - previousPressure;
        double changeDamage = 0;
        if (pressureChange > 0.5) {
            changeDamage = pressureChange * PressureConfig.getPressureChangeDamageMult();
        }

        double pinnedBonus = 1.0;
        if (pinnedTicks > 10) {
            pinnedBonus = 1.0 + Math.min((pinnedTicks - 10) * 0.015, 0.6);
        }

        double blockBonus = 1.0 + Math.min(blockCount * 0.06, 0.3);

        double totalDamage = (baseDamage + changeDamage) * pinnedBonus * blockBonus * hardnessBonus;

        return (float) Math.min(totalDamage, PressureConfig.getMaxDamagePerHit());
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

            // ★ 修复：使用 cooldown ★
            if (cooldown <= 4 && owner.level() instanceof ServerLevel level) {
                PressureEffectRenderer.renderDamageWarning(level, target, currentPressure, cooldown);
            }
            return;
        }

        // ★ 修复：添加缺失的伤害预兆逻辑 ★
        int warningTicks = stateManager.getDamageWarningTicks(target.getUUID());

        if (warningTicks < PressureConfig.getDamageWarningTicks()) {
            stateManager.incrementDamageWarningTicks(target.getUUID());

            if (owner.level() instanceof ServerLevel level) {
                int remaining = PressureConfig.getDamageWarningTicks() - warningTicks;
                PressureEffectRenderer.renderDamageWarning(level, target, currentPressure, remaining);
            }
            return;
        }

        // 警告结束，造成伤害
        applyNormalDamage(owner, target, currentPressure, previousPressure,
                collidingBlocks, stateManager);
    }

    private static void handleSurgeDamage(LivingEntity owner, LivingEntity target,
                                          double currentPressure, double pressureChange,
                                          Set<BlockPos> collidingBlocks,
                                          PressureStateManager stateManager) {

        stateManager.setDamageWarningTicks(target.getUUID(), 0);

        int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());
        int blockCount = collidingBlocks.size();
        double hardnessBonus = calculateHardnessBonus(collidingBlocks, target);

        float baseDamage = calculateDamage(currentPressure, 0,
                pinnedTicks, blockCount, hardnessBonus);
        float surgeDamage = baseDamage * (float) PressureConfig.getSurgeDamageMult();
        surgeDamage = Math.min(surgeDamage, PressureConfig.getMaxDamagePerHit() * 1.5F);

        if (surgeDamage > 0.5F) {
            Vec3 velocityBefore = target.getDeltaMovement();

            DamageSource damageSource = owner.level().damageSources().mobAttack(owner);
            target.hurt(damageSource, surgeDamage);

            target.setDeltaMovement(velocityBefore.x, Math.min(velocityBefore.y, 0.05), velocityBefore.z);
            target.hurtMarked = true;

            int dynamicInterval = calculateDamageInterval(currentPressure);
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

        int dynamicInterval = calculateDamageInterval(currentPressure);

        int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());
        int blockCount = collidingBlocks.size();
        double hardnessBonus = calculateHardnessBonus(collidingBlocks, target);

        float totalDamage = calculateDamage(currentPressure, previousPressure,
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

        // 检查音效开关
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

        // 检查粒子开关
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
