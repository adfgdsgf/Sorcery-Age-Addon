package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public class PressureEffectRenderer {

    public static void renderPressureEffect(LivingEntity owner, LivingEntity target,
                                            double pressure, double distanceFromHalt,
                                            boolean isBreaching,
                                            PressureStateManager stateManager) {

        if (!(owner.level() instanceof ServerLevel level)) return;

        // 检查开关
        boolean particlesEnabled = PressureConfig.areParticlesEnabled();
        boolean soundsEnabled = PressureConfig.areSoundsEnabled();

        if (!particlesEnabled && !soundsEnabled) return;

        int tick = target.tickCount;
        PressureStage stage = getPressureStage(pressure);

        switch (stage) {
            case NONE:
                break;
            case LIGHT:
                renderLightPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
            case MEDIUM:
                renderMediumPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
            case HEAVY:
                renderHeavyPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
            case CRITICAL:
                renderCriticalPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
        }

        if (isBreaching) {
            renderBreachEffect(level, target, Math.abs(distanceFromHalt), tick, particlesEnabled, soundsEnabled);
        }
    }

    public static void renderDamageWarning(ServerLevel level, LivingEntity target,
                                           double pressure, int ticksUntilDamage) {

        boolean particlesEnabled = PressureConfig.areParticlesEnabled();
        boolean soundsEnabled = PressureConfig.areSoundsEnabled();

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (ticksUntilDamage <= 3) {
            if (particlesEnabled) {
                double shrinkFactor = ticksUntilDamage / 3.0;
                double radius = 0.5 + shrinkFactor * 0.5;

                int particleCount = 8 - ticksUntilDamage * 2;
                for (int i = 0; i < particleCount; i++) {
                    double angle = (i / (double) particleCount) * Math.PI * 2;
                    double px = x + Math.cos(angle) * radius;
                    double pz = z + Math.sin(angle) * radius;

                    double vx = (x - px) * 0.1;
                    double vz = (z - pz) * 0.1;

                    level.sendParticles(ParticleTypes.CRIT,
                            px, y, pz, 1, vx, 0, vz, 0.02);
                }
            }

            if (soundsEnabled) {
                if (ticksUntilDamage == 3) {
                    level.playSound(null, x, y, z,
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                            0.3F, 0.5F);
                }

                if (ticksUntilDamage == 1) {
                    level.playSound(null, x, y, z,
                            SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS,
                            0.2F, 0.3F);
                }
            }
        }
    }

    public static void renderPressureSurge(ServerLevel level, LivingEntity target,
                                           double pressureChange) {

        boolean particlesEnabled = PressureConfig.areParticlesEnabled();
        boolean soundsEnabled = PressureConfig.areSoundsEnabled();

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particlesEnabled) {
            int intensity = (int) Math.min(pressureChange * 2, 20);

            level.sendParticles(ParticleTypes.EXPLOSION,
                    x, y, z, 1, 0, 0, 0, 0);

            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    x, y, z, intensity, 0.5, 0.5, 0.5, 0.1);

            level.sendParticles(ParticleTypes.CRIT,
                    x, y, z, intensity * 2, 0.8, 0.8, 0.8, 0.15);
        }

        if (soundsEnabled) {
            level.playSound(null, x, y, z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    0.6F, 0.8F);

            level.playSound(null, x, y, z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                    0.8F, 0.4F);
        }
    }

    // ==================== 各阶段效果 ====================

    private static void renderLightPressure(ServerLevel level, LivingEntity target,
                                            double pressure, int tick,
                                            boolean particles, boolean sounds) {
        if (tick % 10 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {
            level.sendParticles(ParticleTypes.EFFECT,
                    x, y, z, 2, 0.3, 0.3, 0.3, 0.01);
        }

        if (sounds && tick % 30 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    0.1F, 0.3F);
        }
    }

    private static void renderMediumPressure(ServerLevel level, LivingEntity target,
                                             double pressure, int tick,
                                             boolean particles, boolean sounds) {
        if (tick % 5 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {
            level.sendParticles(ParticleTypes.EFFECT,
                    x, y, z, 4, 0.4, 0.4, 0.4, 0.02);

            level.sendParticles(ParticleTypes.SMOKE,
                    x, y + 0.8, z, 2, 0.3, 0.1, 0.3, 0.01);
        }

        if (sounds && tick % 15 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    0.2F, 0.4F);
        }
    }

    private static void renderHeavyPressure(ServerLevel level, LivingEntity target,
                                            double pressure, int tick,
                                            boolean particles, boolean sounds) {
        if (tick % 3 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {
            level.sendParticles(ParticleTypes.CRIT,
                    x, y, z, 5, 0.4, 0.5, 0.4, 0.01);

            level.sendParticles(ParticleTypes.SMOKE,
                    x, y + 0.8, z, 3, 0.2, 0.1, 0.2, 0.02);

            level.sendParticles(ParticleTypes.POOF,
                    x, target.getY() + 0.1, z, 2, 0.3, 0, 0.3, 0.01);
        }

        if (sounds && tick % 10 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.PLAYERS,
                    0.3F, 0.5F);
        }
    }

    private static void renderCriticalPressure(ServerLevel level, LivingEntity target,
                                               double pressure, int tick,
                                               boolean particles, boolean sounds) {
        if (tick % 2 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {
            double radius = 0.6 + Math.sin(tick * 0.3) * 0.2;
            int particleCount = 6;
            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double) particleCount) * Math.PI * 2 + tick * 0.1;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.CRIT,
                        px, y, pz, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        if (sounds && tick % 8 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.CHAIN_STEP, SoundSource.PLAYERS,
                    0.4F, 0.6F);
        }
    }
    private static void renderBreachEffect(ServerLevel level, LivingEntity target,
                                           double breachDepth, int tick,
                                           boolean particles, boolean sounds) {
        if (tick % 2 != 0) return;
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();
        if (particles) {
            int intensity = (int) Math.min(breachDepth * 2, 8);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    x, y, z, intensity, 0.3, 0.3, 0.3, 0.01);
            level.sendParticles(ParticleTypes.CRIT,
                    x, target.getY() + 0.1, z, intensity, 0.5, 0, 0.5, 0.02);
        }
        if (sounds && tick % 5 == 0) {
            float volume = Math.min(0.3F + (float)(breachDepth * 0.1), 0.6F);
            level.playSound(null, x, y, z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                    volume, 0.4F);
        }
    }
    // ==================== 辅助方法 ====================
    private enum PressureStage {
        NONE, LIGHT, MEDIUM, HEAVY, CRITICAL
    }
    private static PressureStage getPressureStage(double pressure) {
        if (pressure < 1.0) return PressureStage.NONE;
        if (pressure < 2.0) return PressureStage.LIGHT;
        if (pressure < 4.0) return PressureStage.MEDIUM;
        if (pressure < 6.0) return PressureStage.HEAVY;
        return PressureStage.CRITICAL;
    }
}
