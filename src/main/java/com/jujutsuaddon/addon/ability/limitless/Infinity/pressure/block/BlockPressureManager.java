package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.block;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.PressureCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class BlockPressureManager {

    public static void applyPressure(BlockPos pos, float pressureValue,
                                     PressureStateManager stateManager,
                                     ServerLevel level) {
        // 使用 BlockValidator 验证
        if (!BlockValidator.isSolidWall(level, pos)) {
            return;
        }

        if (pressureValue < PressureConfig.getMinPressureForBlockBreak()) {
            return;
        }
        // ★★★ 使用可配置的速率 ★★★
        float increment = pressureValue * PressureConfig.getBlockPressureRate();
        stateManager.addBlockPressure(pos, increment);
    }

    public static void update(LivingEntity owner, Set<BlockPos> activeBlocks,
                              int pressureLevel, float cursedEnergyOutput,
                              PressureStateManager stateManager) {

        if (!(owner.level() instanceof ServerLevel level)) return;

        double avgPressure = PressureConfig.getBasePressure() *
                PressureCalculator.calculateLevelFactor(pressureLevel) *
                cursedEnergyOutput;

        boolean canBreakBlocks = avgPressure >= PressureConfig.getMinPressureForBlockBreak();

        long currentTime = System.currentTimeMillis();

        BlockPos ownerFeetPos = owner.blockPosition();
        BlockPos ownerBelowPos = ownerFeetPos.below();

        Set<BlockPos> allBlocks = stateManager.getAllPressuredBlocks();
        Set<BlockPos> toRemove = new HashSet<>();

        for (BlockPos pos : allBlocks) {
            float accumulatedPressure = stateManager.getBlockPressure(pos);

            // 脚下方块不处理
            if (pos.equals(ownerFeetPos) || pos.equals(ownerBelowPos)) {
                toRemove.add(pos);
                stateManager.clearBlockBreakProgress(level, pos);
                continue;
            }

            // 超时
            Long lastTime = stateManager.getBlockLastPressureTime(pos);
            if (lastTime == null || currentTime - lastTime > PressureConfig.getPressureTimeoutMs()) {
                toRemove.add(pos);
                stateManager.clearBlockBreakProgress(level, pos);
                continue;
            }

            BlockState state = level.getBlockState(pos);

            // 空气
            if (state.isAir()) {
                toRemove.add(pos);
                stateManager.clearBlockBreakProgress(level, pos);
                continue;
            }

            // ★ 使用 BlockValidator 验证是否是有效固体墙 ★
            if (!BlockValidator.isSolidWall(level, pos)) {
                toRemove.add(pos);
                stateManager.clearBlockBreakProgress(level, pos);
                continue;
            }

            // ★ 使用 BlockValidator 获取硬度 ★
            float hardness = BlockValidator.getBlockHardness(level, pos);

            // 不可破坏（基岩等）
            if (hardness < 0) {
                toRemove.add(pos);
                continue;
            }

            float breakThreshold = hardness * PressureConfig.getBreakThresholdMult();

            if (activeBlocks.contains(pos)) {
                if (canBreakBlocks && accumulatedPressure >= breakThreshold) {
                    breakBlock(level, pos, owner);
                    toRemove.add(pos);
                    stateManager.clearBlockBreakProgress(level, pos);
                } else {
                    int progress = (int) ((accumulatedPressure / breakThreshold) * PressureConfig.getBreakStages());
                    progress = Math.min(progress, PressureConfig.getBreakStages() - 1);

                    if (!canBreakBlocks) {
                        progress = Math.min(progress, PressureConfig.getBreakStages() / 2);
                    }

                    int breakerId = stateManager.getOrCreateBreakerId(pos);
                    level.destroyBlockProgress(breakerId, pos, progress);

                    if (PressureConfig.areSoundsEnabled() && owner.tickCount % 12 == 0 && progress > 2) {
                        level.playSound(null, pos, SoundEvents.STONE_BREAK,
                                SoundSource.BLOCKS, 0.15F + progress * 0.02F, 0.4F + progress * 0.05F);
                    }
                }
            } else {
                // 不在活动区域，压力衰减
                float newPressure = accumulatedPressure - PressureConfig.getPressureDecayRate();

                if (newPressure <= 0) {
                    toRemove.add(pos);
                    stateManager.clearBlockBreakProgress(level, pos);
                } else {
                    stateManager.setBlockPressure(pos, newPressure);

                    int progress = (int) ((newPressure / breakThreshold) * PressureConfig.getBreakStages());
                    progress = Math.max(0, Math.min(progress, PressureConfig.getBreakStages() - 1));

                    int breakerId = stateManager.getOrCreateBreakerId(pos);
                    level.destroyBlockProgress(breakerId, pos, progress);
                }
            }
        }

        for (BlockPos pos : toRemove) {
            stateManager.setBlockPressure(pos, 0);
        }
    }

    private static void breakBlock(ServerLevel level, BlockPos pos, LivingEntity owner) {
        if (PressureConfig.shouldActuallyBreakBlocks()) {
            boolean dropItems = PressureConfig.shouldDropBlockItems();
            level.destroyBlock(pos, dropItems, owner);
        }

        if (PressureConfig.areSoundsEnabled()) {
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS, 0.5F, 1.1F);
        }

        if (PressureConfig.areParticlesEnabled()) {
            level.sendParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    12, 0.35, 0.35, 0.35, 0.06);
        }
    }
}
